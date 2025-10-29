package app.exception;

/**
 * Exception thrown when invalid role is provided
 */
public class InvalidRoleException extends ApplicationException {
    public InvalidRoleException(String role) {
        super("Invalid role: " + role + ". Valid roles are: USER, AGENT, ADMIN");
    }
}

