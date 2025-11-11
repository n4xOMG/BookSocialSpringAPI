package com.nix.enums;

import java.util.Locale;

/**
 * Enumerates the supported contexts in which an OTP verification can be used.
 * Centralising the values avoids scattering string literals across the
 * codebase.
 */
public enum VerificationContext {
    REGISTER,
    RESET_PASSWORD,
    UPDATE_PROFILE;

    public static VerificationContext from(String value) {
        if (value == null) {
            return null;
        }

        String normalised = value.trim();
        if (normalised.isEmpty()) {
            return null;
        }

        normalised = normalised.replace('-', '_').replace(' ', '_');
        normalised = normalised.replaceAll("([a-z])([A-Z])", "$1_$2");
        normalised = normalised.toUpperCase(Locale.ROOT);

        try {
            return VerificationContext.valueOf(normalised);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
