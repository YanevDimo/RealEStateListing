package app.controller.api;

import app.dto.AgentDto;
import app.entity.Agent;
import app.entity.User;
import app.entity.UserRole;
import app.service.AgentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AgentRestController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
class AgentRestControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AgentService agentService;

    @Autowired
    private ObjectMapper objectMapper;

    private Agent testAgent;
    private User testUser;
    private UUID agentId;

    @BeforeEach
    void setUp() {
        agentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        testUser = User.builder()
                .id(userId)
                .name("John Doe")
                .email("john@example.com")
                .phone("1234567890")
                .role(UserRole.AGENT)
                .isActive(true)
                .build();

        testAgent = Agent.builder()
                .id(agentId)
                .user(testUser)
                .licenseNumber("LIC123")
                .build();
    }

    @Test
    void testGetAgent_Success() throws Exception {
        when(agentService.findAgentById(agentId)).thenReturn(Optional.of(testAgent));

        String response = mockMvc.perform(get("/api/v1/agents/{agentId}", agentId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();

        AgentDto dto = objectMapper.readValue(response, AgentDto.class);
        assertEquals(agentId, dto.getId());
        assertEquals("John", dto.getFirstName());
        assertEquals("Doe", dto.getLastName());
        assertEquals("john@example.com", dto.getEmail());
        assertEquals("1234567890", dto.getPhone());

        verify(agentService, times(1)).findAgentById(agentId);
    }

    @Test
    void testGetAgent_SingleName() throws Exception {
        User singleNameUser = User.builder()
                .id(UUID.randomUUID())
                .name("John")
                .email("john@example.com")
                .role(UserRole.AGENT)
                .build();

        Agent singleNameAgent = Agent.builder()
                .id(agentId)
                .user(singleNameUser)
                .build();

        when(agentService.findAgentById(agentId)).thenReturn(Optional.of(singleNameAgent));

        String response = mockMvc.perform(get("/api/v1/agents/{agentId}", agentId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        AgentDto dto = objectMapper.readValue(response, AgentDto.class);
        assertEquals("John", dto.getFirstName());
        assertEquals("", dto.getLastName());
    }

    @Test
    void testGetAgent_NotFound() throws Exception {
        when(agentService.findAgentById(agentId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/agents/{agentId}", agentId))
                .andExpect(status().isNotFound());

        verify(agentService, times(1)).findAgentById(agentId);
    }

    @Test
    void testGetAgent_NoUser() throws Exception {
        Agent agentNoUser = Agent.builder()
                .id(agentId)
                .user(null)
                .build();

        when(agentService.findAgentById(agentId)).thenReturn(Optional.of(agentNoUser));

        String response = mockMvc.perform(get("/api/v1/agents/{agentId}", agentId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        AgentDto dto = objectMapper.readValue(response, AgentDto.class);
        assertNull(dto.getFirstName());
        assertNull(dto.getLastName());
        assertNull(dto.getEmail());
    }

    @Test
    void testAgentExists_True() throws Exception {
        when(agentService.findAgentById(agentId)).thenReturn(Optional.of(testAgent));

        String response = mockMvc.perform(get("/api/v1/agents/{agentId}/exists", agentId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertEquals("true", response);

        verify(agentService, times(1)).findAgentById(agentId);
    }

    @Test
    void testAgentExists_False() throws Exception {
        when(agentService.findAgentById(agentId)).thenReturn(Optional.empty());

        String response = mockMvc.perform(get("/api/v1/agents/{agentId}/exists", agentId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertEquals("false", response);

        verify(agentService, times(1)).findAgentById(agentId);
    }

    @Test
    void testGetAgent_InvalidUUID() throws Exception {
        mockMvc.perform(get("/api/v1/agents/{agentId}", "invalid-uuid"))
                .andExpect(status().is5xxServerError());
    }
}

