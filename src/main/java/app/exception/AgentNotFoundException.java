package app.exception;

/**
 * Exception thrown when an agent is not found
 */
public class AgentNotFoundException extends ApplicationException {
    public AgentNotFoundException(String message) {
        super(message);
    }

    public AgentNotFoundException(java.util.UUID id) {
        super("Agent not found with ID: " + id);
    }
}

