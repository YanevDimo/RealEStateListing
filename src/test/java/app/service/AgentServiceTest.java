package app.service;


import app.client.PropertyServiceClient;
import app.dto.PropertyDto;
import app.entity.Agent;
import app.entity.User;
import app.entity.UserRole;
import app.exception.UserNotFoundException;
import app.repository.AgentRepository;
import app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentServiceTest {

    @Mock
    private AgentRepository agentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PropertyServiceClient propertyServiceClient;

    @Mock
    private PropertyUtilityService propertyUtilityService;

    @InjectMocks
    private AgentService agentService;

    private Agent testAgent;
    private User testUser;
    private UUID agentId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        agentId = UUID.randomUUID();
        userId = UUID.randomUUID();

        testUser = User.builder()
                .id(userId)
                .email("agent@example.com")
                .name("Test Agent")
                .role(UserRole.AGENT)
                .build();

        testAgent = Agent.builder()
                .id(agentId)
                .user(testUser)
                .licenseNumber("LIC-001")
                .experienceYears(5)
                .rating(new BigDecimal("4.5"))
                .totalListings(10)
                .build();
    }

    @Test
    void testFindAllAgents() {
        // Given
        List<Agent> agents = Collections.singletonList(testAgent);
        when(agentRepository.findAll()).thenReturn(agents);

        // When
        List<Agent> result = agentService.findAllAgents();

        // Then
        assertEquals(1, result.size());
        verify(agentRepository, times(1)).findAll();
    }

    @Test
    void testFindAgentById_Found() {
        // Given
        when(agentRepository.findById(agentId)).thenReturn(Optional.of(testAgent));

        // When
        Optional<Agent> result = agentService.findAgentById(agentId);

        // Then
        assertTrue(result.isPresent());
        assertEquals("LIC-001", result.get().getLicenseNumber());
    }

    @Test
    void testFindAgentByLicenseNumber() {
        // Given
        when(agentRepository.findByLicenseNumber("LIC-001")).thenReturn(Optional.of(testAgent));

        // When
        Optional<Agent> result = agentService.findAgentByLicenseNumber("LIC-001");

        // Then
        assertTrue(result.isPresent());
        assertEquals("LIC-001", result.get().getLicenseNumber());
    }

    @Test
    void testCreateAgent_Success() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(agentRepository.existsByUserId(userId)).thenReturn(false);
        when(agentRepository.existsByLicenseNumber("LIC-001")).thenReturn(false);
        when(agentRepository.save(any(Agent.class))).thenReturn(testAgent);

        // When
        Agent result = agentService.createAgent(userId, "LIC-001", "Bio", 5, "Residential");

        // Then
        assertNotNull(result);
        verify(agentRepository).save(any(Agent.class));
    }

    @Test
    void testCreateAgent_UserNotFound_ThrowsException() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UserNotFoundException.class, () ->
                agentService.createAgent(userId, "LIC-001", "Bio", 5, "Residential"));
    }

    @Test
    void testCreateAgent_DuplicateLicense_ThrowsException() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(agentRepository.existsByUserId(userId)).thenReturn(false);
        when(agentRepository.existsByLicenseNumber("LIC-001")).thenReturn(true);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                agentService.createAgent(userId, "LIC-001", "Bio", 5, "Residential"));

        assertTrue(exception.getMessage().contains("already exists"));
    }

    @Test
    void testUpdateAgentRating() {
        // Given
        when(agentRepository.findById(agentId)).thenReturn(Optional.of(testAgent));
        when(agentRepository.save(any(Agent.class))).thenReturn(testAgent);

        // When
        Agent result = agentService.updateAgentRating(agentId, new BigDecimal("4.8"));

        // Then
        assertEquals(new BigDecimal("4.8"), result.getRating());
        verify(agentRepository).save(testAgent);
    }

    @Test
    void testIncrementAgentListings() {
        // Given
        when(agentRepository.findById(agentId)).thenReturn(Optional.of(testAgent));
        when(agentRepository.save(any(Agent.class))).thenReturn(testAgent);

        // When
        agentService.incrementAgentListings(agentId);

        // Then
        assertEquals(11, testAgent.getTotalListings());
        verify(agentRepository).save(testAgent);
    }

    @Test
    void testDecrementAgentListings() {
        // Given
        when(agentRepository.findById(agentId)).thenReturn(Optional.of(testAgent));
        when(agentRepository.save(any(Agent.class))).thenReturn(testAgent);

        // When
        agentService.decrementAgentListings(agentId);

        // Then
        assertEquals(9, testAgent.getTotalListings());
        verify(agentRepository).save(testAgent);
    }

    @Test
    void testDecrementAgentListings_DoesNotGoBelowZero() {
        // Given
        testAgent.setTotalListings(0);
        when(agentRepository.findById(agentId)).thenReturn(Optional.of(testAgent));
        when(agentRepository.save(any(Agent.class))).thenReturn(testAgent);

        // When
        agentService.decrementAgentListings(agentId);

        // Then
        assertEquals(0, testAgent.getTotalListings());
    }

    @Test
    void testFindAgentsByRating() {
        // Given
        Agent agent1 = Agent.builder().rating(new BigDecimal("4.5")).build();
        Agent agent2 = Agent.builder().rating(new BigDecimal("3.0")).build();
        List<Agent> allAgents = Arrays.asList(agent1, agent2);
        when(agentRepository.findAll()).thenReturn(allAgents);

        // When
        List<Agent> result = agentService.findAgentsByRating(new BigDecimal("4.0"));

        // Then
        assertEquals(1, result.size());
        assertEquals(new BigDecimal("4.5"), result.get(0).getRating());
    }

    @Test
    void testGetAgentStatistics() {
        // Given
        when(agentRepository.count()).thenReturn(5L);
        when(agentRepository.findAll()).thenReturn(Collections.singletonList(testAgent));

        // When
        AgentService.AgentStatistics stats = agentService.getAgentStatistics();

        // Then
        assertNotNull(stats);
        assertEquals(5L, stats.getTotalAgents());
    }

    @Test
    void testCountActivePropertiesByAgent() {
        // Given
        PropertyDto property = PropertyDto.builder().status("ACTIVE").build();
        when(propertyServiceClient.getPropertiesByAgent(agentId))
                .thenReturn(Collections.singletonList(property));

        // When
        long result = agentService.countActivePropertiesByAgent(agentId);

        // Then
        assertEquals(1, result);
    }

    @Test
    void testCountActivePropertiesByAgent_Exception_ReturnsZero() {
        // Given
        when(propertyServiceClient.getPropertiesByAgent(agentId))
                .thenThrow(new RuntimeException("Service error"));

        // When
        long result = agentService.countActivePropertiesByAgent(agentId);

        // Then
        assertEquals(0, result);
    }
}
