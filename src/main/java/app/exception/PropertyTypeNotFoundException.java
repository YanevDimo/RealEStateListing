package app.exception;

import java.util.UUID;

/**
 * Exception thrown when a property type is not found
 */
public class PropertyTypeNotFoundException extends ApplicationException {
    public PropertyTypeNotFoundException(String message) {
        super(message);
    }

    public PropertyTypeNotFoundException(UUID id) {
        super("Property type not found with ID: " + id);
    }
}

