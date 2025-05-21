package com.nix.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.nix.service.ImageService;

@RestController
@RequestMapping("/images")
public class ImageController {

	@Autowired
	private ImageService imageService;

	@PostMapping("/upload")
	public ResponseEntity<String> uploadImage(@RequestParam("file") MultipartFile file,
			@RequestParam("username") String username, @RequestParam("folderName") String folderName) {
		try {
			String imagePath = imageService.uploadImage(file, username, folderName);
			return ResponseEntity.ok(imagePath);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		} catch (Exception e) {
			return ResponseEntity.internalServerError().body("Failed to upload image: " + e.getMessage());
		}
	}

	@DeleteMapping("/delete")
	public ResponseEntity<String> deleteImage(@RequestParam("imagePath") String imagePath) {
		try {
			imageService.deleteImage(imagePath);
			return ResponseEntity.ok("Image deleted successfully");
		} catch (Exception e) {
			return ResponseEntity.internalServerError().body("Failed to delete image: " + e.getMessage());
		}
	}

	@DeleteMapping("/delete-entity")
	public ResponseEntity<String> deleteEntityImages(@RequestParam("username") String username,
			@RequestParam("folderName") String folderName, @RequestParam("entityId") String entityId) {
		try {
			imageService.deleteEntityImages(username, folderName, entityId);
			return ResponseEntity.ok("Entity images deleted successfully");
		} catch (Exception e) {
			return ResponseEntity.internalServerError().body("Failed to delete entity images: " + e.getMessage());
		}
	}
}