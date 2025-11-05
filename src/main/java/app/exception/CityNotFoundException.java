package app.exception;

import java.util.UUID;

/**
 * Exception thrown when a city is not found
 */
public class CityNotFoundException extends ApplicationException {
    public CityNotFoundException(String message) {
        super(message);
    }

    public CityNotFoundException(UUID id) {
        super("City not found with ID: " + id);
    }
}

