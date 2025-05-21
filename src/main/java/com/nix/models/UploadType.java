package com.nix.models;

public enum UploadType {
	AVATAR("avatar"), BOOK_COVER("book_cover"), CHAPTER_CONTENT("chapter_content"), POST("post");

	private final String folderName;

	UploadType(String folderName) {
		this.folderName = folderName;
	}

	public String getFolderName() {
		return folderName;
	}
}
