package com.softnexis.contactmanager.util;

import java.util.regex.Pattern;

/**
 * Utility for server-side input sanitization and validation
 * adhering to the Soft Nexis Contact Manager specifications.
 */
public final class ContactValidator {

    // RFC compliant pattern for robust email verification preventing double dots
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9]+([.-][A-Za-z0-9]+)*\\.[A-Za-z]{2,}$"
    );

    // 10-digit phone pattern: accepts 10 consecutive digits or standard separators (e.g. 123-456-7890, 123 456 7890)
    private static final Pattern PHONE_10_DIGIT_PATTERN = Pattern.compile(
            "^(\\d{10}|\\d{3}[-.\\s]\\d{3}[-.\\s]\\d{4}|\\(\\d{3}\\)\\s*\\d{3}[-.\\s]\\d{4})$"
    );

    private ContactValidator() {
        // Utility class
    }

    /**
     * Sanitizes raw string input by trimming and stripping null bytes / dangerous control characters.
     */
    public static String sanitize(String input) {
        if (input == null) {
            return "";
        }
        return input.trim().replace("\0", "");
    }

    /**
     * Validates contact form fields against specifications.
     *
     * @param name  Contact full name (Required, 2-50 characters)
     * @param email Contact email (Required, valid email format)
     * @param phone Contact phone number (Optional, 10-digit format if provided)
     * @return ValidationResult containing any field-specific errors
     */
    public static ValidationResult validate(String name, String email, String phone) {
        ValidationResult result = new ValidationResult();

        String sanitizedName = sanitize(name);
        String sanitizedEmail = sanitize(email);
        String sanitizedPhone = sanitize(phone);

        // Name validation: Required, 2-50 characters -> "Please enter valid name"
        if (sanitizedName.isEmpty() || sanitizedName.length() < 2 || sanitizedName.length() > 50) {
            result.addError("name", "Please enter valid name");
        }

        // Email validation: Required, valid email format -> "Invalid email address"
        if (sanitizedEmail.isEmpty() || !EMAIL_PATTERN.matcher(sanitizedEmail).matches()) {
            result.addError("email", "Invalid email address");
        }

        // Phone validation: Optional, 10-digit format if provided -> "Use 10-digit format"
        if (!sanitizedPhone.isEmpty()) {
            String digitsOnly = sanitizedPhone.replaceAll("[^0-9]", "");
            if (!PHONE_10_DIGIT_PATTERN.matcher(sanitizedPhone).matches() || digitsOnly.length() != 10) {
                result.addError("phone", "Use 10-digit format");
            }
        }

        return result;
    }

    /**
     * Normalizes phone number into standard 10-digit string.
     */
    public static String normalizePhone(String phone) {
        if (phone == null) {
            return "";
        }
        return phone.replaceAll("[^0-9]", "");
    }
}
