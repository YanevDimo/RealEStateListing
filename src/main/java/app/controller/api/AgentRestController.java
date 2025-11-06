package app.controller.api;

import app.dto.AgentDto;
import app.entity.Agent;
import app.exception.AgentNotFoundException;
import app.service.AgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/agents")
@RequiredArgsConstructor
public class AgentRestController {

    private final AgentService agentService;

    @GetMapping("/{agentId}")
    public ResponseEntity<AgentDto> getAgent(@PathVariable UUID agentId) {
        Agent agent = agentService.findAgentById(agentId)
                .orElseThrow(() -> new AgentNotFoundException("Agent not found with ID: " + agentId));
        
        // Map to DTO format expected by property-service
        String firstName = null;
        String lastName = null;
        String email = null;
        String phone = null;
        
        if (agent.getUser() != null) {
            if (agent.getUser().getName() != null) {
                String fullName = agent.getUser().getName().trim();
                if (fullName.contains(" ")) {
                    String[] nameParts = fullName.split(" ", 2);
                    firstName = nameParts[0];
                    lastName = nameParts.length > 1 ? nameParts[1] : "";
                } else {
                    firstName = fullName;
                    lastName = "";
                }
            }
            email = agent.getUser().getEmail();
            phone = agent.getUser().getPhone();
        }
        
        AgentDto dto = AgentDto.builder()
                .id(agent.getId())
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .phone(phone)
                .build();
        
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{agentId}/exists")
    public ResponseEntity<Boolean> agentExists(@PathVariable UUID agentId) {
        boolean exists = agentService.findAgentById(agentId).isPresent();
        return ResponseEntity.ok(exists);
    }
}





