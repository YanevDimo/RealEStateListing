package app.exception;

/**
 * Exception thrown when a license number already exists
 */
public class DuplicateLicenseNumberException extends ApplicationException {
    public DuplicateLicenseNumberException(String licenseNumber) {
        super("License number already exists: " + licenseNumber);
    }
}

