package com.softnexis.contactmanager;

import com.softnexis.contactmanager.util.ContactValidator;
import com.softnexis.contactmanager.util.ValidationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Contact Validator Tests")
public class ContactValidatorTest {

    @Test
    @DisplayName("Submit empty form -> Field-specific errors for Name and Email")
    void testEmptyFormSubmission() {
        ValidationResult result = ContactValidator.validate("", "", "");

        assertFalse(result.isValid(), "Empty form submission must be invalid");
        assertTrue(result.hasError("name"), "Must have error on name");
        assertTrue(result.hasError("email"), "Must have error on email");
        assertFalse(result.hasError("phone"), "Optional phone should not have error when empty");

        assertEquals("Please enter valid name", result.getError("name"));
        assertEquals("Invalid email address", result.getError("email"));
    }

    @Test
    @DisplayName("Name validation: 2-50 characters boundary tests")
    void testNameValidationBoundaries() {
        // Less than 2 characters
        ValidationResult r1 = ContactValidator.validate("A", "test@example.com", "5551234567");
        assertFalse(r1.isValid());
        assertEquals("Please enter valid name", r1.getError("name"));

        // Exactly 2 characters (Valid)
        ValidationResult r2 = ContactValidator.validate("Jo", "test@example.com", "5551234567");
        assertTrue(r2.isValid());

        // Exactly 50 characters (Valid)
        String fiftyChars = "A".repeat(50);
        ValidationResult r3 = ContactValidator.validate(fiftyChars, "test@example.com", "5551234567");
        assertTrue(r3.isValid());

        // 51 characters (Invalid)
        String fiftyOneChars = "A".repeat(51);
        ValidationResult r4 = ContactValidator.validate(fiftyOneChars, "test@example.com", "5551234567");
        assertFalse(r4.isValid());
        assertEquals("Please enter valid name", r4.getError("name"));

        // Null name
        ValidationResult r5 = ContactValidator.validate(null, "test@example.com", null);
        assertFalse(r5.isValid());
        assertEquals("Please enter valid name", r5.getError("name"));
    }

    @Test
    @DisplayName("Email validation: Format verification")
    void testEmailValidation() {
        String[] invalidEmails = {
                "plainaddress",
                "@missingusername.com",
                "missingatsign.com",
                "username@.com",
                "username@domain..com",
                "username@domain"
        };

        for (String invalidEmail : invalidEmails) {
            ValidationResult result = ContactValidator.validate("John Doe", invalidEmail, "");
            assertFalse(result.isValid(), "Expected email to be invalid: " + invalidEmail);
            assertEquals("Invalid email address", result.getError("email"));
        }

        String[] validEmails = {
                "user@example.com",
                "first.last@company.org",
                "john.doe+tag@domain.co.uk",
                "a@b.cd"
        };

        for (String validEmail : validEmails) {
            ValidationResult result = ContactValidator.validate("John Doe", validEmail, "");
            assertTrue(result.isValid(), "Expected email to be valid: " + validEmail);
        }
    }

    @Test
    @DisplayName("Phone validation: Optional, 10-digit format if provided")
    void testPhoneValidation() {
        // Optional: empty, blank or null are valid
        assertTrue(ContactValidator.validate("John Doe", "john@example.com", "").isValid());
        assertTrue(ContactValidator.validate("John Doe", "john@example.com", "   ").isValid());
        assertTrue(ContactValidator.validate("John Doe", "john@example.com", null).isValid());

        // Valid 10-digit phone
        assertTrue(ContactValidator.validate("John Doe", "john@example.com", "1234567890").isValid());
        assertTrue(ContactValidator.validate("John Doe", "john@example.com", "123-456-7890").isValid());
        assertTrue(ContactValidator.validate("John Doe", "john@example.com", "123 456 7890").isValid());
        assertTrue(ContactValidator.validate("John Doe", "john@example.com", "(123) 456-7890").isValid());

        // Invalid phone numbers
        String[] invalidPhones = {
                "123",              // Too short
                "12345678901",      // Too long (11 digits)
                "abcdefghij",       // Letters
                "123-456-789a"      // Mixed
        };

        for (String invalidPhone : invalidPhones) {
            ValidationResult result = ContactValidator.validate("John Doe", "john@example.com", invalidPhone);
            assertFalse(result.isValid(), "Expected phone to be invalid: " + invalidPhone);
            assertEquals("Use 10-digit format", result.getError("phone"));
        }
    }

    @Test
    @DisplayName("Input Sanitization: strips null bytes and trims")
    void testInputSanitization() {
        assertEquals("John Doe", ContactValidator.sanitize("  John Doe  "));
        assertEquals("test@example.com", ContactValidator.sanitize("test@example.com\0"));
        assertEquals("", ContactValidator.sanitize(null));
    }
}
