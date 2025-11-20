package app.exception;

import java.util.UUID;

/**
 * Exception thrown when a user is not found.
 * This makes error handling clearer and more consistent.
 */
public class UserNotFoundException extends ApplicationException {
    
    /**
     * Creates an exception with a custom message
     */
    public UserNotFoundException(String message) {
        super(message);
    }

    /**
     * Creates an exception with a standard message using the user ID.
     * This is the most common way we'll use it.
     */
    public UserNotFoundException(UUID id) {
        super("User not found with ID: " + id);
    }
}



