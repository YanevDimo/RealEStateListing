package app.exception;

/**
 * Exception thrown when an email already exists
 */
public class DuplicateEmailException extends ApplicationException {
    public DuplicateEmailException(String email) {
        super("Email already exists: " + email);
    }
}

