package com.nix.enums;

/**
 * Represents the safety bucket returned by the NSFW detector service.
 */
public enum NsfwLevel {
    SAFE,
    MILD,
    EXPLICIT,
    UNKNOWN;

    public boolean requiresBlur() {
        return this == MILD;
    }

    public boolean isRejected() {
        return this == EXPLICIT;
    }
}
