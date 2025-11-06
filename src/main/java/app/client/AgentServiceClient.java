package app.client;

import app.dto.AgentDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Feign Client for calling Agent Service from Property Service.
 * This interface will be used when Property Service is extracted.
 */
@FeignClient(name = "agent-service", url = "${agent.service.url:http://localhost:8080}")
public interface AgentServiceClient {

    @GetMapping("/api/v1/agents/{agentId}")
    AgentDto getAgent(@PathVariable UUID agentId);

    @GetMapping("/api/v1/agents/{agentId}/exists")
    boolean agentExists(@PathVariable UUID agentId);
}





