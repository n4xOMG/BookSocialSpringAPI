package com.nix.service;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import net.coobird.thumbnailator.Thumbnails;

import com.nix.response.ImageSafetyAssessment;
import com.nix.response.ImageUploadResponse;

@Service
public class ImageService {

	private static final Logger logger = LoggerFactory.getLogger(ImageService.class);

	@Value("${file.upload-dir}")
	private String uploadDir;

	@Value("${file.max-size}")
	private long maxFileSize;

	private final NsfwDetectionService nsfwDetectionService;

	private static final float COMPRESSION_QUALITY = 0.65f;
	private static final int MAX_WIDTH = 1920;
	private static final int MAX_HEIGHT = 1080;
	private static final String[] ALLOWED_TYPES = { "image/jpeg", "image/jpg", "image/png", "image/webp" };

	public ImageService(NsfwDetectionService nsfwDetectionService) {
		this.nsfwDetectionService = nsfwDetectionService;
	}

	public ImageUploadResponse uploadImage(MultipartFile file, String username, String folderName) throws IOException {
		validateFile(file);
		validateFolderName(folderName);

		ImageSafetyAssessment safety = nsfwDetectionService.analyse(file);
		if (safety.getLevel() != null && safety.getLevel().isRejected()) {
			throw new IllegalArgumentException("Image rejected because it contains explicit content");
		}

		// Save to temporary directory: uploads/username/folderName
		String uniqueFileName = UUID.randomUUID() + ".webp";
		Path tempDir = Paths.get(uploadDir, username, folderName);
		Files.createDirectories(tempDir);
		Path filePath = tempDir.resolve(uniqueFileName);

		byte[] processedImage = processImage(file);
		Files.write(filePath, processedImage);

		String relativePath = toRelativePath(filePath);
		String fileUrl = ServletUriComponentsBuilder.fromCurrentContextPath().path(relativePath).build().toUriString();

		return new ImageUploadResponse(fileUrl, relativePath, safety);
	}

	public void deleteTempImages(String username, String folderName) throws IOException {
		validateFolderName(folderName);
		Path tempDir = Paths.get(uploadDir, username, folderName);
		if (Files.exists(tempDir)) {
			Files.walk(tempDir).sorted(Comparator.reverseOrder()).forEach(path -> {
				try {
					Files.deleteIfExists(path);
				} catch (IOException e) {
					throw new UncheckedIOException("Failed to delete temp file: " + path, e);
				}
			});
		}
	}

	private void validateFile(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("File cannot be empty");
		}
		if (file.getSize() > maxFileSize) {
			throw new IllegalArgumentException("File size exceeds maximum limit of " + maxFileSize + " bytes");
		}
		if (!isValidImageType(file.getContentType())) {
			throw new IllegalArgumentException("Invalid file type. Only JPEG, PNG, and WebP are allowed");
		}
	}

	private void validateFolderName(String folderName) {
		if (folderName == null || folderName.trim().isEmpty()) {
			throw new IllegalArgumentException("Folder name cannot be empty");
		}
		if (folderName.contains("..") || folderName.contains("/") || folderName.contains("\\")) {
			throw new IllegalArgumentException("Invalid folder name. It must not contain '..', '/', or '\\'");
		}
	}

	private boolean isValidImageType(String contentType) {
		for (String allowedType : ALLOWED_TYPES) {
			if (allowedType.equalsIgnoreCase(contentType)) {
				return true;
			}
		}
		return false;
	}

	private byte[] processImage(MultipartFile file) throws IOException {
		logger.debug("Processing image: contentType={}, size={}", file.getContentType(), file.getSize());
		// Read original image
		BufferedImage originalImage = ImageIO.read(file.getInputStream());
		if (originalImage == null) {
			logger.warn("Failed to read image file: contentType={}", file.getContentType());
			throw new IOException("Failed to read image file");
		}
		logger.debug("Original image dimensions: {}x{}", originalImage.getWidth(), originalImage.getHeight());

		// Normalize to RGB to handle JPEG color model issues (e.g., CMYK)
		BufferedImage rgbImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(),
				BufferedImage.TYPE_INT_RGB);
		Graphics2D g = rgbImage.createGraphics();
		g.drawImage(originalImage, 0, 0, null);
		g.dispose();
		logger.debug("Converted image to RGB: {}x{}", rgbImage.getWidth(), rgbImage.getHeight());

		// Calculate optimal dimensions
		int[] dimensions = calculateOptimalDimensions(rgbImage.getWidth(), rgbImage.getHeight());
		logger.debug("Calculated dimensions: {}x{}", dimensions[0], dimensions[1]);

		// Resize and convert to WebP using Thumbnails
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try {
			Thumbnails.of(rgbImage).size(dimensions[0], dimensions[1]).outputFormat("webp")
					.outputQuality(COMPRESSION_QUALITY).useExifOrientation(false).toOutputStream(outputStream);
		} catch (Exception e) {
			logger.error("Failed to process image with Thumbnails: contentType={}", file.getContentType(), e);
			throw new IOException("Failed to process image: " + e.getMessage(), e);
		}
		logger.debug("Image processed successfully, output size: {}", outputStream.size());

		return outputStream.toByteArray();
	}

	private byte[] removeMetadata(byte[] imageData) throws IOException {
		try {
			BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
			if (image == null) {
				throw new IOException("Invalid image data");
			}
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			ImageIO.write(image, "png", outputStream);
			return outputStream.toByteArray();
		} catch (Exception e) {
			throw new IOException("Failed to remove metadata: " + e.getMessage());
		}
	}

	private int[] calculateOptimalDimensions(int originalWidth, int originalHeight) {
		if (originalWidth <= 0 || originalHeight <= 0) {
			logger.debug("Invalid image dimensions: {}x{}", originalWidth, originalHeight);
			throw new IllegalArgumentException("Invalid image dimensions: " + originalWidth + "x" + originalHeight);
		}

		double aspectRatio = (double) originalWidth / originalHeight;
		int targetWidth = MAX_WIDTH;
		int targetHeight = MAX_HEIGHT;

		if (aspectRatio > 1) { // Landscape
			targetHeight = (int) (MAX_WIDTH / aspectRatio);
			if (targetHeight > MAX_HEIGHT) {
				targetHeight = MAX_HEIGHT;
				targetWidth = (int) (MAX_HEIGHT * aspectRatio);
			}
		} else { // Portrait or square
			targetWidth = (int) (MAX_HEIGHT * aspectRatio);
			if (targetWidth > MAX_WIDTH) {
				targetWidth = MAX_WIDTH;
				targetHeight = (int) (MAX_WIDTH / aspectRatio);
			}
		}

		// Ensure dimensions are at least 1
		targetWidth = Math.max(1, targetWidth);
		targetHeight = Math.max(1, targetHeight);
		logger.debug("Final dimensions: {}x{}", targetWidth, targetHeight);
		return new int[] { targetWidth, targetHeight };
	}

	public void deleteImage(String imagePath) throws IOException {
		Path filePath = Paths.get(uploadDir + imagePath);
		Files.deleteIfExists(filePath);
	}

	public void deleteEntityImages(String username, String folderName, String entityId) throws IOException {
		validateFolderName(folderName);
		Path entityDir = Paths.get(uploadDir, username, folderName, entityId);
		if (Files.exists(entityDir)) {
			Files.walk(entityDir).sorted(Comparator.reverseOrder()).forEach(path -> {
				try {
					Files.deleteIfExists(path);
				} catch (IOException e) {
					throw new UncheckedIOException("Failed to delete file: " + path, e);
				}
			});
		}
	}

	public String getUploadDir() {
		return uploadDir;
	}

	private String toRelativePath(Path filePath) {
		String normalized = filePath.toString().replace("\\", "/");
		if (!normalized.startsWith("/")) {
			normalized = "/" + normalized;
		}
		return normalized;
	}
}