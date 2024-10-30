package com.nix.exception;

public class SensitiveWordException extends RuntimeException {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public SensitiveWordException(String message) {
        super(message);
    }
}
