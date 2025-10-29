package app.exception;

/**
 * Exception thrown when a user is not found
 */
public class UserNotFoundException extends ApplicationException {
    public UserNotFoundException(String message) {
        super(message);
    }
}



