package app.exception;

import java.util.UUID;

/**
 * Exception thrown when an inquiry is not found.
 * This makes error handling clearer and more consistent.
 */
public class InquiryNotFoundException extends ApplicationException {
    
    /**
     * Creates an exception with a custom message
     */
    public InquiryNotFoundException(String message) {
        super(message);
    }

    /**
     * Creates an exception with a standard message using the inquiry ID.
     * This is the most common way we'll use it.
     */
    public InquiryNotFoundException(UUID id) {
        super("Inquiry not found with ID: " + id);
    }
}

