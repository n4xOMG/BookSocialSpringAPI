package com.nix.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.nix.response.ApiResponseWithData;
import com.nix.service.ImageService;

@RestController
@RequestMapping("/images")
public class ImageController {

	@Autowired
	private ImageService imageService;

	@PostMapping("/upload")
	public ResponseEntity<ApiResponseWithData<String>> uploadImage(@RequestParam("file") MultipartFile file,
			@RequestParam("username") String username, @RequestParam("folderName") String folderName) {
		try {
			String imagePath = imageService.uploadImage(file, username, folderName);
			return buildSuccessResponse(HttpStatus.CREATED, "Image uploaded successfully.", imagePath);
		} catch (IllegalArgumentException e) {
			return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					"Failed to upload image: " + e.getMessage());
		}
	}

	@DeleteMapping("/delete")
	public ResponseEntity<ApiResponseWithData<Void>> deleteImage(@RequestParam("imagePath") String imagePath) {
		try {
			imageService.deleteImage(imagePath);
			return buildSuccessResponse("Image deleted successfully.", null);
		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					"Failed to delete image: " + e.getMessage());
		}
	}

	@DeleteMapping("/delete-entity")
	public ResponseEntity<ApiResponseWithData<Void>> deleteEntityImages(@RequestParam("username") String username,
			@RequestParam("folderName") String folderName, @RequestParam("entityId") String entityId) {
		try {
			imageService.deleteEntityImages(username, folderName, entityId);
			return buildSuccessResponse("Entity images deleted successfully.", null);
		} catch (Exception e) {
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					"Failed to delete entity images: " + e.getMessage());
		}
	}

	private <T> ResponseEntity<ApiResponseWithData<T>> buildSuccessResponse(String message, T data) {
		return buildSuccessResponse(HttpStatus.OK, message, data);
	}

	private <T> ResponseEntity<ApiResponseWithData<T>> buildSuccessResponse(HttpStatus status, String message, T data) {
		return ResponseEntity.status(status).body(new ApiResponseWithData<>(message, true, data));
	}

	private <T> ResponseEntity<ApiResponseWithData<T>> buildErrorResponse(HttpStatus status, String message) {
		return ResponseEntity.status(status).body(new ApiResponseWithData<>(message, false));
	}
}