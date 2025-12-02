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
        List<Agent> agents = Collections.singletonList(testAgent);
        when(agentRepository.findAll()).thenReturn(agents);

        List<Agent> result = agentService.findAllAgents();

        assertEquals(1, result.size());
        verify(agentRepository, times(1)).findAll();
    }

    @Test
    void testFindAgentById_Found() {
        when(agentRepository.findById(agentId)).thenReturn(Optional.of(testAgent));

        Optional<Agent> result = agentService.findAgentById(agentId);

        assertTrue(result.isPresent());
        assertEquals("LIC-001", result.get().getLicenseNumber());
    }

    @Test
    void testFindAgentByLicenseNumber() {
        when(agentRepository.findByLicenseNumber("LIC-001")).thenReturn(Optional.of(testAgent));

        Optional<Agent> result = agentService.findAgentByLicenseNumber("LIC-001");

        assertTrue(result.isPresent());
        assertEquals("LIC-001", result.get().getLicenseNumber());
    }

    @Test
    void testCreateAgent_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(agentRepository.existsByUserId(userId)).thenReturn(false);
        when(agentRepository.existsByLicenseNumber("LIC-001")).thenReturn(false);
        when(agentRepository.save(any(Agent.class))).thenReturn(testAgent);

        Agent result = agentService.createAgent(userId, "LIC-001", "Bio", 5, "Residential");

        assertNotNull(result);
        verify(agentRepository).save(any(Agent.class));
    }

    @Test
    void testCreateAgent_UserNotFound_ThrowsException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () ->
                agentService.createAgent(userId, "LIC-001", "Bio", 5, "Residential"));
    }

    @Test
    void testCreateAgent_DuplicateLicense_ThrowsException() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(agentRepository.existsByUserId(userId)).thenReturn(false);
        when(agentRepository.existsByLicenseNumber("LIC-001")).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                agentService.createAgent(userId, "LIC-001", "Bio", 5, "Residential"));

        assertTrue(exception.getMessage().contains("already exists"));
    }

    @Test
    void testUpdateAgentRating() {
        when(agentRepository.findById(agentId)).thenReturn(Optional.of(testAgent));
        when(agentRepository.save(any(Agent.class))).thenReturn(testAgent);

        Agent result = agentService.updateAgentRating(agentId, new BigDecimal("4.8"));

        assertEquals(new BigDecimal("4.8"), result.getRating());
        verify(agentRepository).save(testAgent);
    }

    @Test
    void testIncrementAgentListings() {
        when(agentRepository.findById(agentId)).thenReturn(Optional.of(testAgent));
        when(agentRepository.save(any(Agent.class))).thenReturn(testAgent);

        agentService.incrementAgentListings(agentId);

        assertEquals(11, testAgent.getTotalListings());
        verify(agentRepository).save(testAgent);
    }

    @Test
    void testDecrementAgentListings() {
        when(agentRepository.findById(agentId)).thenReturn(Optional.of(testAgent));
        when(agentRepository.save(any(Agent.class))).thenReturn(testAgent);

        agentService.decrementAgentListings(agentId);

        assertEquals(9, testAgent.getTotalListings());
        verify(agentRepository).save(testAgent);
    }

    @Test
    void testDecrementAgentListings_DoesNotGoBelowZero() {
        testAgent.setTotalListings(0);
        when(agentRepository.findById(agentId)).thenReturn(Optional.of(testAgent));
        when(agentRepository.save(any(Agent.class))).thenReturn(testAgent);

        agentService.decrementAgentListings(agentId);

        assertEquals(0, testAgent.getTotalListings());
    }

    @Test
    void testFindAgentsByRating() {
        Agent agent1 = Agent.builder().rating(new BigDecimal("4.5")).build();
        Agent agent2 = Agent.builder().rating(new BigDecimal("3.0")).build();
        List<Agent> allAgents = Arrays.asList(agent1, agent2);
        when(agentRepository.findAll()).thenReturn(allAgents);

        List<Agent> result = agentService.findAgentsByRating(new BigDecimal("4.0"));

        assertEquals(1, result.size());
        assertEquals(new BigDecimal("4.5"), result.get(0).getRating());
    }

    @Test
    void testGetAgentStatistics() {
        when(agentRepository.count()).thenReturn(5L);
        when(agentRepository.findAll()).thenReturn(Collections.singletonList(testAgent));

        AgentService.AgentStatistics stats = agentService.getAgentStatistics();

        assertNotNull(stats);
        assertEquals(5L, stats.getTotalAgents());
    }

    @Test
    void testCountActivePropertiesByAgent() {
        PropertyDto property = PropertyDto.builder().status("ACTIVE").build();
        when(propertyServiceClient.getPropertiesByAgent(agentId))
                .thenReturn(Collections.singletonList(property));

        long result = agentService.countActivePropertiesByAgent(agentId);

        assertEquals(1, result);
    }

    @Test
    void testCountActivePropertiesByAgent_Exception_ReturnsZero() {
        when(propertyServiceClient.getPropertiesByAgent(agentId))
                .thenThrow(new RuntimeException("Service error"));

        long result = agentService.countActivePropertiesByAgent(agentId);

        assertEquals(0, result);
    }

    @Test
    void testCountAllAgents() {
        when(agentRepository.count()).thenReturn(5L);

        long result = agentService.countAllAgents();

        assertEquals(5L, result);
    }

    @Test
    void testSaveAgent() {
        when(agentRepository.save(testAgent)).thenReturn(testAgent);

        Agent result = agentService.saveAgent(testAgent);

        assertNotNull(result);
        verify(agentRepository).save(testAgent);
    }

    @Test
    void testIncrementAgentListings_AgentNotFound() {
        when(agentRepository.findById(agentId)).thenReturn(Optional.empty());

        agentService.incrementAgentListings(agentId);

        verify(agentRepository, never()).save(any());
    }

    @Test
    void testFindAgentByUserId() {
        when(agentRepository.findByUserId(userId)).thenReturn(Optional.of(testAgent));

        Optional<Agent> result = agentService.findAgentByUserId(userId);

        assertTrue(result.isPresent());
        assertEquals(agentId, result.get().getId());
    }

    @Test
    void testLicenseNumberExists() {
        when(agentRepository.existsByLicenseNumber("LIC-001")).thenReturn(true);

        boolean result = agentService.licenseNumberExists("LIC-001");

        assertTrue(result);
    }

    @Test
    void testFindAgentsByExperience() {
        Agent agent1 = Agent.builder().experienceYears(5).build();
        Agent agent2 = Agent.builder().experienceYears(3).build();
        when(agentRepository.findAll()).thenReturn(Arrays.asList(agent1, agent2));

        List<Agent> result = agentService.findAgentsByExperience(4);

        assertEquals(1, result.size());
        assertEquals(5, result.get(0).getExperienceYears());
    }

    @Test
    void testFindTopRatedAgents() {
        Agent agent1 = Agent.builder().rating(new BigDecimal("4.5")).build();
        Agent agent2 = Agent.builder().rating(new BigDecimal("3.0")).build();
        when(agentRepository.findAll()).thenReturn(Arrays.asList(agent1, agent2));

        List<Agent> result = agentService.findTopRatedAgents(org.springframework.data.domain.PageRequest.of(0, 10));

        assertEquals(2, result.size());
        assertEquals(new BigDecimal("4.5"), result.get(0).getRating());
    }

    @Test
    void testFindAgentsByExperienceRange() {
        Agent agent1 = Agent.builder().experienceYears(5).build();
        Agent agent2 = Agent.builder().experienceYears(3).build();
        Agent agent3 = Agent.builder().experienceYears(10).build();
        when(agentRepository.findAll()).thenReturn(Arrays.asList(agent1, agent2, agent3));

        List<Agent> result = agentService.findAgentsByExperienceRange(4, 8);

        assertEquals(1, result.size());
        assertEquals(5, result.get(0).getExperienceYears());
    }

    @Test
    void testFindAgentsByRatingRange() {
        Agent agent1 = Agent.builder().rating(new BigDecimal("4.5")).build();
        Agent agent2 = Agent.builder().rating(new BigDecimal("3.0")).build();
        Agent agent3 = Agent.builder().rating(new BigDecimal("2.0")).build();
        when(agentRepository.findAll()).thenReturn(Arrays.asList(agent1, agent2, agent3));

        List<Agent> result = agentService.findAgentsByRatingRange(new BigDecimal("3.0"), new BigDecimal("4.0"));

        assertEquals(1, result.size());
        assertEquals(new BigDecimal("3.0"), result.get(0).getRating());
    }

    @Test
    void testFindAgentsByMostListings() {
        Agent agent1 = Agent.builder().totalListings(10).build();
        Agent agent2 = Agent.builder().totalListings(5).build();
        when(agentRepository.findAll()).thenReturn(Arrays.asList(agent1, agent2));

        List<Agent> result = agentService.findAgentsByMostListings(org.springframework.data.domain.PageRequest.of(0, 10));

        assertEquals(2, result.size());
        assertEquals(10, result.get(0).getTotalListings());
    }

    @Test
    void testFindAgentsBySpecialization() {
        testAgent.setSpecializations("[\"Residential\", \"Commercial\"]");
        when(agentRepository.findAll()).thenReturn(Collections.singletonList(testAgent));

        List<Agent> result = agentService.findAgentsBySpecialization("Residential");

        assertEquals(1, result.size());
    }

    @Test
    void testFindAgentsBySpecialization_NoMatch() {
        testAgent.setSpecializations("[\"Commercial\"]");
        when(agentRepository.findAll()).thenReturn(Collections.singletonList(testAgent));

        List<Agent> result = agentService.findAgentsBySpecialization("Residential");

        assertTrue(result.isEmpty());
    }

    @Test
    void testSearchAgentsByName() {
        when(agentRepository.findAll()).thenReturn(Collections.singletonList(testAgent));

        List<Agent> result = agentService.searchAgentsByName("Test");

        assertEquals(1, result.size());
    }

    @Test
    void testSearchAgentsByEmail() {
        when(agentRepository.findAll()).thenReturn(Collections.singletonList(testAgent));

        List<Agent> result = agentService.searchAgentsByEmail("agent");

        assertEquals(1, result.size());
    }

    @Test
    void testFindAgentsWithActiveProperties() {
        when(agentRepository.findAll()).thenReturn(Collections.singletonList(testAgent));
        when(propertyUtilityService.agentHasActiveProperties(agentId)).thenReturn(true);

        List<Agent> result = agentService.findAgentsWithActiveProperties();

        assertEquals(1, result.size());
    }

    @Test
    void testFindAgentsWithBio() {
        testAgent.setBio("Test bio");
        when(agentRepository.findAll()).thenReturn(Collections.singletonList(testAgent));

        List<Agent> result = agentService.findAgentsWithBio();

        assertEquals(1, result.size());
    }

    @Test
    void testFindAgentsWithBio_NoBio() {
        testAgent.setBio(null);
        when(agentRepository.findAll()).thenReturn(Collections.singletonList(testAgent));

        List<Agent> result = agentService.findAgentsWithBio();

        assertTrue(result.isEmpty());
    }

    @Test
    void testFindAgentsCreatedAfter() {
        testAgent.setCreatedAt(java.time.LocalDateTime.now());
        when(agentRepository.findAll()).thenReturn(Collections.singletonList(testAgent));

        List<Agent> result = agentService.findAgentsCreatedAfter(java.time.LocalDateTime.now().minusDays(1));

        assertEquals(1, result.size());
    }

    @Test
    void testCountAgentsByExperience() {
        Agent agent1 = Agent.builder().experienceYears(5).build();
        Agent agent2 = Agent.builder().experienceYears(3).build();
        when(agentRepository.findAll()).thenReturn(Arrays.asList(agent1, agent2));

        long result = agentService.countAgentsByExperience(4);

        assertEquals(1, result);
    }

    @Test
    void testCountAgentsByRating() {
        Agent agent1 = Agent.builder().rating(new BigDecimal("4.5")).build();
        Agent agent2 = Agent.builder().rating(new BigDecimal("3.0")).build();
        when(agentRepository.findAll()).thenReturn(Arrays.asList(agent1, agent2));

        long result = agentService.countAgentsByRating(new BigDecimal("4.0"));

        assertEquals(1, result);
    }

    @Test
    void testGetAverageRating() {
        Agent agent1 = Agent.builder().rating(new BigDecimal("4.0")).build();
        Agent agent2 = Agent.builder().rating(new BigDecimal("2.0")).build();
        when(agentRepository.findAll()).thenReturn(Arrays.asList(agent1, agent2));
        when(agentRepository.count()).thenReturn(2L);

        BigDecimal result = agentService.getAverageRating();

        assertEquals(new BigDecimal("3.00"), result);
    }

    @Test
    void testGetTotalListings() {
        Agent agent1 = Agent.builder().totalListings(10).build();
        Agent agent2 = Agent.builder().totalListings(5).build();
        when(agentRepository.findAll()).thenReturn(Arrays.asList(agent1, agent2));

        Long result = agentService.getTotalListings();

        assertEquals(15L, result);
    }

    @Test
    void testUpdateAgent() {
        when(agentRepository.save(testAgent)).thenReturn(testAgent);

        Agent result = agentService.updateAgent(testAgent);

        assertNotNull(result);
        verify(agentRepository).save(testAgent);
    }

    @Test
    void testDeleteAgent() {
        doNothing().when(agentRepository).deleteById(agentId);

        agentService.deleteAgent(agentId);

        verify(agentRepository).deleteById(agentId);
    }

    @Test
    void testUpdateAgentExperience() {
        when(agentRepository.findById(agentId)).thenReturn(Optional.of(testAgent));
        when(agentRepository.save(testAgent)).thenReturn(testAgent);

        Agent result = agentService.updateAgentExperience(agentId, 7);

        assertEquals(7, result.getExperienceYears());
        verify(agentRepository).save(testAgent);
    }

    @Test
    void testUpdateAgentBio() {
        when(agentRepository.findById(agentId)).thenReturn(Optional.of(testAgent));
        when(agentRepository.save(testAgent)).thenReturn(testAgent);

        Agent result = agentService.updateAgentBio(agentId, "New bio");

        assertEquals("New bio", result.getBio());
        verify(agentRepository).save(testAgent);
    }

    @Test
    void testUpdateAgentSpecializations() {
        when(agentRepository.findById(agentId)).thenReturn(Optional.of(testAgent));
        when(agentRepository.save(testAgent)).thenReturn(testAgent);

        Agent result = agentService.updateAgentSpecializations(agentId, "[\"New\"]");

        assertEquals("[\"New\"]", result.getSpecializations());
        verify(agentRepository).save(testAgent);
    }

    @Test
    void testSyncAgentListingsFromPropertyService() {
        when(agentRepository.findAll()).thenReturn(Collections.singletonList(testAgent));
        when(propertyServiceClient.getPropertiesByAgent(agentId))
                .thenReturn(Collections.singletonList(PropertyDto.builder().status("ACTIVE").build()));
        when(agentRepository.save(testAgent)).thenReturn(testAgent);

        int result = agentService.syncAgentListingsFromPropertyService();

        assertEquals(1, result);
        verify(agentRepository).save(testAgent);
    }

    @Test
    void testSyncAgentListingsFromPropertyService_NoChange() {
        testAgent.setTotalListings(1);
        when(agentRepository.findAll()).thenReturn(Collections.singletonList(testAgent));
        when(propertyServiceClient.getPropertiesByAgent(agentId))
                .thenReturn(Collections.singletonList(PropertyDto.builder().status("ACTIVE").build()));

        int result = agentService.syncAgentListingsFromPropertyService();

        assertEquals(0, result);
    }

    @Test
    void testSyncAgentListingsFromPropertyService_Exception() {
        Agent agentWithSameCount = Agent.builder()
                .id(agentId)
                .totalListings(0)
                .build();
        when(agentRepository.findAll()).thenReturn(Collections.singletonList(agentWithSameCount));
        when(propertyServiceClient.getPropertiesByAgent(agentId))
                .thenThrow(new RuntimeException("Error"));

        int result = agentService.syncAgentListingsFromPropertyService();

        assertEquals(0, result);
        verify(agentRepository, never()).save(any());
    }

    @Test
    void testRecalculateAgentRatings() {
        testAgent.setTotalListings(10);
        testAgent.setExperienceYears(5);
        when(agentRepository.findAll()).thenReturn(Collections.singletonList(testAgent));
        when(agentRepository.save(testAgent)).thenReturn(testAgent);

        int result = agentService.recalculateAgentRatings();

        assertEquals(1, result);
        verify(agentRepository).save(testAgent);
    }

    @Test
    void testRecalculateAgentRatings_NoListings() {
        testAgent.setTotalListings(0);
        when(agentRepository.findAll()).thenReturn(Collections.singletonList(testAgent));

        int result = agentService.recalculateAgentRatings();

        assertEquals(0, result);
        verify(agentRepository, never()).save(any());
    }

    @Test
    void testCalculateAgentListStatistics() {
        Agent agent1 = Agent.builder().totalListings(5).experienceYears(3).build();
        Agent agent2 = Agent.builder().totalListings(0).experienceYears(5).build();
        List<Agent> agents = Arrays.asList(agent1, agent2);

        AgentService.AgentListStatistics stats = agentService.calculateAgentListStatistics(agents);

        assertNotNull(stats);
        assertEquals(2, stats.getTotalAgents());
        assertEquals(1, stats.getAgentsWithProperties());
    }

    @Test
    void testCalculateAgentListStatistics_EmptyList() {
        AgentService.AgentListStatistics stats = agentService.calculateAgentListStatistics(Collections.emptyList());

        assertNotNull(stats);
        assertEquals(0, stats.getTotalAgents());
    }

    @Test
    void testCalculateAgentListStatistics_NullList() {
        AgentService.AgentListStatistics stats = agentService.calculateAgentListStatistics(null);

        assertNotNull(stats);
        assertEquals(0, stats.getTotalAgents());
    }

    @Test
    void testParseSpecializationsFromJson() {
        String result = agentService.parseSpecializationsFromJson("[\"Residential\", \"Commercial\"]");

        assertNotNull(result);
        assertTrue(result.contains("Residential"));
    }

    @Test
    void testParseSpecializationsFromJson_Empty() {
        String result = agentService.parseSpecializationsFromJson("[]");

        assertEquals("", result);
    }

    @Test
    void testParseSpecializationsFromJson_Null() {
        String result = agentService.parseSpecializationsFromJson(null);

        assertEquals("", result);
    }

    @Test
    void testParseSpecializationsFromJson_Invalid() {
        String invalid = "invalid json";
        String result = agentService.parseSpecializationsFromJson(invalid);

        assertEquals(invalid, result);
    }
}
