package com.softnexis.contactmanager.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates the results of contact form validation.
 */
public class ValidationResult {
    private final Map<String, String> errors = new HashMap<>();
    private String generalError;

    public void addError(String field, String message) {
        errors.put(field, message);
    }

    public void setGeneralError(String generalError) {
        this.generalError = generalError;
    }

    public String getGeneralError() {
        return generalError;
    }

    public boolean isValid() {
        return errors.isEmpty() && (generalError == null || generalError.trim().isEmpty());
    }

    public Map<String, String> getErrors() {
        return Collections.unmodifiableMap(errors);
    }

    public String getError(String field) {
        return errors.get(field);
    }

    public boolean hasError(String field) {
        return errors.containsKey(field);
    }

    /**
     * Gets the first field name that has an error, useful for auto-focusing.
     */
    public String getFirstInvalidField() {
        if (errors.containsKey("name")) return "name";
        if (errors.containsKey("email")) return "email";
        if (errors.containsKey("phone")) return "phone";
        return errors.keySet().stream().findFirst().orElse(null);
    }
}
