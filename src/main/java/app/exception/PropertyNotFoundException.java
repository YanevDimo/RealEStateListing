package app.exception;

/**
 * Exception thrown when a property is not found
 */
public class PropertyNotFoundException extends ApplicationException {
    public PropertyNotFoundException(String message) {
        super(message);
    }

    public PropertyNotFoundException(java.util.UUID id) {
        super("Property not found with ID: " + id);
    }
}

