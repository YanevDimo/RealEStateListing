package app.exception;

import java.util.UUID;


public class AgentNotFoundException extends ApplicationException {
    public AgentNotFoundException(String message) {
        super(message);
    }

    public AgentNotFoundException(UUID id) {
        super("Agent not found with ID: " + id);
    }
}

