package app.service;

import app.client.PropertyServiceClient;
import app.dto.AgentRegistrationDto;
import app.dto.PropertyCreateDto;
import app.dto.PropertyDto;
import app.dto.PropertyUpdateDto;
import app.entity.Agent;
import app.entity.City;
import app.entity.PropertyType;
import app.entity.User;
import app.entity.UserRole;
import app.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentRegistrationServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private AgentService agentService;

    @Mock
    private PropertyServiceClient propertyServiceClient;

    @Mock
    private CityService cityService;

    @Mock
    private PropertyTypeService propertyTypeService;

    @Mock
    private PropertyUtilityService propertyUtilityService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AgentRegistrationService agentRegistrationService;

    private AgentRegistrationDto registrationDto;
    private User testUser;
    private Agent testAgent;
    private PropertyDto propertyDto;
    private City testCity;
    private PropertyType testPropertyType;
    private UUID agentId;
    private UUID userId;
    private UUID cityId;
    private UUID propertyTypeId;
    private UUID propertyId;

    @BeforeEach
    void setUp() {
        agentId = UUID.randomUUID();
        userId = UUID.randomUUID();
        cityId = UUID.randomUUID();
        propertyTypeId = UUID.randomUUID();
        propertyId = UUID.randomUUID();

        registrationDto = AgentRegistrationDto.builder()
                .name("Test Agent")
                .email("agent@example.com")
                .password("password123")
                .phone("123456789")
                .licenseNumber("LIC-001")
                .bio("Test bio")
                .experienceYears(5)
                .specializations("Residential, Commercial")
                .build();

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
                .bio("Test bio")
                .experienceYears(5)
                .build();

        testCity = City.builder()
                .id(cityId)
                .name("Sofia")
                .build();

        testPropertyType = PropertyType.builder()
                .id(propertyTypeId)
                .name("Apartment")
                .build();

        propertyDto = PropertyDto.builder()
                .id(propertyId)
                .title("Test Property")
                .description("Test Description")
                .price(new BigDecimal("100000"))
                .cityId(cityId)
                .propertyTypeId(propertyTypeId)
                .agentId(agentId)
                .bedrooms(3)
                .bathrooms(2)
                .status("ACTIVE")
                .build();
    }

    @Test
    void testCreateAgentWithProfile_Success() {
        when(userService.userExistsByEmail(registrationDto.getEmail())).thenReturn(false);
        when(agentService.licenseNumberExists(registrationDto.getLicenseNumber())).thenReturn(false);
        when(passwordEncoder.encode(registrationDto.getPassword())).thenReturn("encoded");
        when(userService.saveUser(any(User.class))).thenReturn(testUser);
        when(agentService.saveAgent(any(Agent.class))).thenReturn(testAgent);

        Agent result = agentRegistrationService.createAgentWithProfile(registrationDto);

        assertNotNull(result);
        verify(userService).saveUser(any(User.class));
        verify(agentService).saveAgent(any(Agent.class));
    }

    @Test
    void testCreateAgentWithProfile_DuplicateEmail() {
        when(userService.userExistsByEmail(registrationDto.getEmail())).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () ->
                agentRegistrationService.createAgentWithProfile(registrationDto));
    }

    @Test
    void testCreateAgentWithProfile_DuplicateLicense() {
        when(userService.userExistsByEmail(registrationDto.getEmail())).thenReturn(false);
        when(agentService.licenseNumberExists(registrationDto.getLicenseNumber())).thenReturn(true);

        assertThrows(DuplicateLicenseNumberException.class, () ->
                agentRegistrationService.createAgentWithProfile(registrationDto));
    }

    @Test
    void testCreateAgentWithProfile_WithProfilePicture() {
        when(userService.userExistsByEmail(registrationDto.getEmail())).thenReturn(false);
        when(agentService.licenseNumberExists(registrationDto.getLicenseNumber())).thenReturn(false);
        when(passwordEncoder.encode(registrationDto.getPassword())).thenReturn("encoded");
        when(userService.saveUser(any(User.class))).thenReturn(testUser);
        when(agentService.saveAgent(any(Agent.class))).thenReturn(testAgent);
        when(agentService.findAgentById(agentId)).thenReturn(Optional.of(testAgent));

        Agent result = agentRegistrationService.createAgentWithProfile(registrationDto, "http://example.com/pic.jpg");

        assertNotNull(result);
        verify(agentService).saveAgent(any(Agent.class));
    }

    @Test
    void testCreatePropertyForAgent_Success() {
        when(agentService.findAgentById(agentId)).thenReturn(Optional.of(testAgent));
        when(propertyTypeService.findPropertyTypeById(propertyTypeId)).thenReturn(Optional.of(testPropertyType));
        when(cityService.findCityById(cityId)).thenReturn(Optional.of(testCity));
        when(propertyServiceClient.createProperty(any(PropertyCreateDto.class))).thenReturn(propertyDto);
        doNothing().when(propertyUtilityService).evictAllPropertiesCache();
        doNothing().when(agentService).incrementAgentListings(agentId);

        PropertyDto result = agentRegistrationService.createPropertyForAgent(agentId, propertyDto);

        assertNotNull(result);
        assertEquals(propertyId, result.getId());
        verify(propertyServiceClient).createProperty(any(PropertyCreateDto.class));
        verify(agentService).incrementAgentListings(agentId);
    }

    @Test
    void testCreatePropertyForAgent_AgentNotFound() {
        when(agentService.findAgentById(agentId)).thenReturn(Optional.empty());

        assertThrows(AgentNotFoundException.class, () ->
                agentRegistrationService.createPropertyForAgent(agentId, propertyDto));
    }

    @Test
    void testCreatePropertyForAgent_PropertyTypeNotFound() {
        when(agentService.findAgentById(agentId)).thenReturn(Optional.of(testAgent));
        when(propertyTypeService.findPropertyTypeById(propertyTypeId)).thenReturn(Optional.empty());

        assertThrows(PropertyTypeNotFoundException.class, () ->
                agentRegistrationService.createPropertyForAgent(agentId, propertyDto));
    }

    @Test
    void testCreatePropertyForAgent_CityNotFound() {
        when(agentService.findAgentById(agentId)).thenReturn(Optional.of(testAgent));
        when(propertyTypeService.findPropertyTypeById(propertyTypeId)).thenReturn(Optional.of(testPropertyType));
        when(cityService.findCityById(cityId)).thenReturn(Optional.empty());

        assertThrows(CityNotFoundException.class, () ->
                agentRegistrationService.createPropertyForAgent(agentId, propertyDto));
    }

    @Test
    void testCreatePropertyForAgent_NullResponse() {
        when(agentService.findAgentById(agentId)).thenReturn(Optional.of(testAgent));
        when(propertyTypeService.findPropertyTypeById(propertyTypeId)).thenReturn(Optional.of(testPropertyType));
        when(cityService.findCityById(cityId)).thenReturn(Optional.of(testCity));
        when(propertyServiceClient.createProperty(any(PropertyCreateDto.class))).thenReturn(null);

        assertThrows(ApplicationException.class, () ->
                agentRegistrationService.createPropertyForAgent(agentId, propertyDto));
    }

    @Test
    void testCreatePropertyForAgent_NoIdInResponse() {
        PropertyDto propertyWithoutId = PropertyDto.builder()
                .title("Test")
                .build();
        when(agentService.findAgentById(agentId)).thenReturn(Optional.of(testAgent));
        when(propertyTypeService.findPropertyTypeById(propertyTypeId)).thenReturn(Optional.of(testPropertyType));
        when(cityService.findCityById(cityId)).thenReturn(Optional.of(testCity));
        when(propertyServiceClient.createProperty(any(PropertyCreateDto.class))).thenReturn(propertyWithoutId);

        assertThrows(ApplicationException.class, () ->
                agentRegistrationService.createPropertyForAgent(agentId, propertyDto));
    }

    @Test
    void testCreatePropertyForAgent_WithBedsAndBaths() {
        propertyDto.setBeds(4);
        propertyDto.setBaths(3);
        when(agentService.findAgentById(agentId)).thenReturn(Optional.of(testAgent));
        when(propertyTypeService.findPropertyTypeById(propertyTypeId)).thenReturn(Optional.of(testPropertyType));
        when(cityService.findCityById(cityId)).thenReturn(Optional.of(testCity));
        when(propertyServiceClient.createProperty(any(PropertyCreateDto.class))).thenReturn(propertyDto);
        doNothing().when(propertyUtilityService).evictAllPropertiesCache();
        doNothing().when(agentService).incrementAgentListings(agentId);

        PropertyDto result = agentRegistrationService.createPropertyForAgent(agentId, propertyDto);

        assertNotNull(result);
        verify(propertyServiceClient).createProperty(any(PropertyCreateDto.class));
    }

    @Test
    void testCreatePropertyForAgent_WithAreaSqm() {
        propertyDto.setAreaSqm(new BigDecimal("150"));
        when(agentService.findAgentById(agentId)).thenReturn(Optional.of(testAgent));
        when(propertyTypeService.findPropertyTypeById(propertyTypeId)).thenReturn(Optional.of(testPropertyType));
        when(cityService.findCityById(cityId)).thenReturn(Optional.of(testCity));
        when(propertyServiceClient.createProperty(any(PropertyCreateDto.class))).thenReturn(propertyDto);
        doNothing().when(propertyUtilityService).evictAllPropertiesCache();
        doNothing().when(agentService).incrementAgentListings(agentId);

        PropertyDto result = agentRegistrationService.createPropertyForAgent(agentId, propertyDto);

        assertNotNull(result);
        verify(propertyServiceClient).createProperty(any(PropertyCreateDto.class));
    }

    @Test
    void testGetAgentWithProperties() {
        when(agentService.findAgentById(agentId)).thenReturn(Optional.of(testAgent));

        Agent result = agentRegistrationService.getAgentWithProperties(agentId);

        assertNotNull(result);
        assertEquals(agentId, result.getId());
    }

    @Test
    void testGetAgentWithProperties_NotFound() {
        when(agentService.findAgentById(agentId)).thenReturn(Optional.empty());

        assertThrows(AgentNotFoundException.class, () ->
                agentRegistrationService.getAgentWithProperties(agentId));
    }

    @Test
    void testBuildPropertyUpdateDto() {
        PropertyDto existingProperty = PropertyDto.builder()
                .agentId(agentId)
                .status("ACTIVE")
                .bedrooms(2)
                .bathrooms(1)
                .squareFeet(80)
                .build();

        PropertyUpdateDto result = agentRegistrationService.buildPropertyUpdateDto(propertyDto, existingProperty);

        assertNotNull(result);
        assertEquals(propertyDto.getTitle(), result.getTitle());
        assertEquals(agentId, result.getAgentId());
    }

    @Test
    void testBuildPropertyUpdateDto_WithBedsAndBaths() {
        propertyDto.setBeds(5);
        propertyDto.setBaths(4);
        PropertyDto existingProperty = PropertyDto.builder()
                .agentId(agentId)
                .bedrooms(2)
                .bathrooms(1)
                .squareFeet(80)
                .build();

        PropertyUpdateDto result = agentRegistrationService.buildPropertyUpdateDto(propertyDto, existingProperty);

        assertNotNull(result);
        assertEquals(5, result.getBedrooms());
        assertEquals(4, result.getBathrooms());
    }

    @Test
    void testUpdateAgentProfile() {
        when(agentService.findAgentById(agentId)).thenReturn(Optional.of(testAgent));
        doNothing().when(userService).updateUser(any(User.class));
        when(agentService.updateAgent(any(Agent.class))).thenReturn(testAgent);

        agentRegistrationService.updateAgentProfile(agentId, registrationDto);

        verify(agentService).updateAgent(any(Agent.class));
        verify(userService).updateUser(any(User.class));
    }

    @Test
    void testUpdateAgentProfile_AgentNotFound() {
        when(agentService.findAgentById(agentId)).thenReturn(Optional.empty());

        assertThrows(AgentNotFoundException.class, () ->
                agentRegistrationService.updateAgentProfile(agentId, registrationDto));
    }

    @Test
    void testUpdateAgentProfile_UpdateBio() {
        registrationDto.setBio("New bio");
        when(agentService.findAgentById(agentId)).thenReturn(Optional.of(testAgent));
        doNothing().when(userService).updateUser(any(User.class));
        when(agentService.updateAgent(any(Agent.class))).thenReturn(testAgent);

        agentRegistrationService.updateAgentProfile(agentId, registrationDto);

        assertEquals("New bio", testAgent.getBio());
    }

    @Test
    void testUpdateAgentProfile_UpdateExperience() {
        registrationDto.setExperienceYears(10);
        when(agentService.findAgentById(agentId)).thenReturn(Optional.of(testAgent));
        doNothing().when(userService).updateUser(any(User.class));
        when(agentService.updateAgent(any(Agent.class))).thenReturn(testAgent);

        agentRegistrationService.updateAgentProfile(agentId, registrationDto);

        assertEquals(10, testAgent.getExperienceYears());
    }

    @Test
    void testUpdateAgentProfile_UpdateSpecializations() {
        registrationDto.setSpecializations("New, Specializations");
        when(agentService.findAgentById(agentId)).thenReturn(Optional.of(testAgent));
        doNothing().when(userService).updateUser(any(User.class));
        when(agentService.updateAgent(any(Agent.class))).thenReturn(testAgent);

        agentRegistrationService.updateAgentProfile(agentId, registrationDto);

        assertNotNull(testAgent.getSpecializations());
    }

    @Test
    void testUpdateAgentProfile_UpdateLicenseNumber() {
        registrationDto.setLicenseNumber("LIC-002");
        when(agentService.findAgentById(agentId)).thenReturn(Optional.of(testAgent));
        doNothing().when(userService).updateUser(any(User.class));
        when(agentService.updateAgent(any(Agent.class))).thenReturn(testAgent);

        agentRegistrationService.updateAgentProfile(agentId, registrationDto);

        assertEquals("LIC-002", testAgent.getLicenseNumber());
    }

    @Test
    void testUpdateAgentProfile_UpdateName() {
        registrationDto.setName("New Name");
        when(agentService.findAgentById(agentId)).thenReturn(Optional.of(testAgent));
        doNothing().when(userService).updateUser(any(User.class));
        when(agentService.updateAgent(any(Agent.class))).thenReturn(testAgent);

        agentRegistrationService.updateAgentProfile(agentId, registrationDto);

        assertEquals("New Name", testUser.getName());
    }

    @Test
    void testUpdateAgentProfile_UpdatePhone() {
        registrationDto.setPhone("987654321");
        when(agentService.findAgentById(agentId)).thenReturn(Optional.of(testAgent));
        doNothing().when(userService).updateUser(any(User.class));
        when(agentService.updateAgent(any(Agent.class))).thenReturn(testAgent);

        agentRegistrationService.updateAgentProfile(agentId, registrationDto);

        assertEquals("987654321", testUser.getPhone());
    }

    @Test
    void testUpdateAgentProfile_EmptyPhone() {
        registrationDto.setPhone("");
        when(agentService.findAgentById(agentId)).thenReturn(Optional.of(testAgent));
        doNothing().when(userService).updateUser(any(User.class));
        when(agentService.updateAgent(any(Agent.class))).thenReturn(testAgent);

        agentRegistrationService.updateAgentProfile(agentId, registrationDto);

        assertNull(testUser.getPhone());
    }
}

