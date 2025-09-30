package com.nix.enums;

public enum NotificationEntityType {
	BOOK("Book"), CHAPTER("Chapter"), COMMENT("Comment"), PAYMENT("Payment"), POST("Post"), REPORT("Report"),
	GLOBAL("Global");

	private final String displayName;

	NotificationEntityType(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}