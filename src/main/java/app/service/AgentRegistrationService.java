package app.service;

import app.client.PropertyServiceClient;
import app.dto.AgentRegistrationDto;
import app.dto.PropertyCreateDto;
import app.dto.PropertyDto;
import app.entity.Agent;
import app.entity.User;
import app.entity.UserRole;
import app.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AgentRegistrationService {

    private final UserService userService;
    private final AgentService agentService;
    private final PropertyServiceClient propertyServiceClient;
    private final CityService cityService;
    private final PropertyTypeService propertyTypeService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Complete agent registration process
     * Creates both User account and Agent profile
     */
    public Agent createAgentWithProfile(AgentRegistrationDto registrationDto) {
        log.info("Creating agent with profile for email: {}", registrationDto.getEmail());

        // Step 1: Validate email uniqueness
        if (userService.userExistsByEmail(registrationDto.getEmail())) {
            throw new DuplicateEmailException(registrationDto.getEmail());
        }

        // Step 2: Validate license number uniqueness
        if (agentService.licenseNumberExists(registrationDto.getLicenseNumber())) {
            throw new DuplicateLicenseNumberException(registrationDto.getLicenseNumber());
        }

        // Step 3: Create User account
        User user = User.builder()
                .name(registrationDto.getName())
                .email(registrationDto.getEmail())
                .passwordHash(passwordEncoder.encode(registrationDto.getPassword()))
                .phone(registrationDto.getPhone())
                .role(UserRole.AGENT)  // Set as AGENT
                .isActive(true)
                .build();

        User savedUser = userService.saveUser(user);
        log.info("User account created with ID: {}", savedUser.getId());

        // Step 4: Create Agent profile
        Agent agent = Agent.builder()
                .user(savedUser)
                .licenseNumber(registrationDto.getLicenseNumber())
                .bio(registrationDto.getBio())
                .experienceYears(registrationDto.getExperienceYears() != null ? 
                    registrationDto.getExperienceYears() : 0)
                .specializations(formatSpecializationsAsJson(registrationDto.getSpecializations()))
                .rating(BigDecimal.ZERO)
                .totalListings(0)
                .build();

        Agent savedAgent = agentService.saveAgent(agent);
        log.info("Agent profile created with ID: {}", savedAgent.getId());

        return savedAgent;
    }

    /**
     * Create a property for an agent via property-service
     */
    public PropertyDto createPropertyForAgent(UUID agentId, PropertyDto propertyDto) {
        log.info("Creating property for agent: {} via property-service", agentId);

        // Validate agent exists
        agentService.findAgentById(agentId)
                .orElseThrow(() -> new AgentNotFoundException(agentId));

        // Validate property type exists
        propertyTypeService.findPropertyTypeById(propertyDto.getPropertyTypeId())
                .orElseThrow(() -> new PropertyTypeNotFoundException(propertyDto.getPropertyTypeId()));

        // Validate city exists
        cityService.findCityById(propertyDto.getCityId())
                .orElseThrow(() -> new CityNotFoundException(propertyDto.getCityId()));

        // Convert PropertyDto to PropertyCreateDto for property-service
        PropertyCreateDto createDto = PropertyCreateDto.builder()
                .title(propertyDto.getTitle())
                .description(propertyDto.getDescription())
                .price(propertyDto.getPrice())
                .agentId(agentId)
                .cityId(propertyDto.getCityId())
                .propertyTypeId(propertyDto.getPropertyTypeId())
                .status(propertyDto.getStatus() != null ? propertyDto.getStatus() : "DRAFT")
                .bedrooms(propertyDto.getBedrooms() != null ? propertyDto.getBedrooms() : 
                         (propertyDto.getBeds() != null ? propertyDto.getBeds() : 0))
                .bathrooms(propertyDto.getBathrooms() != null ? propertyDto.getBathrooms() : 
                          (propertyDto.getBaths() != null ? propertyDto.getBaths() : 0))
                .squareFeet(propertyDto.getSquareFeet() != null ? propertyDto.getSquareFeet() : 
                           (propertyDto.getAreaSqm() != null ? propertyDto.getAreaSqm().intValue() : null))
                .address(propertyDto.getAddress())
                .features(propertyDto.getFeatures())
                .imageUrls(propertyDto.getImageUrls()) // Include image URLs from uploaded images
                .build();

        // Create property via property-service
        log.info("Sending property creation request to Property Service with DTO: {}", createDto);
        log.info("Image URLs being sent: {}", createDto.getImageUrls());
        if (createDto.getImageUrls() == null || createDto.getImageUrls().isEmpty()) {
            log.warn("⚠️ No image URLs in PropertyCreateDto! PropertyDto had: {}", propertyDto.getImageUrls());
        } else {
            log.info("✅ Sending {} image URLs to property-service", createDto.getImageUrls().size());
        }
        PropertyDto savedProperty = propertyServiceClient.createProperty(createDto);
        
        if (savedProperty == null) {
            log.error("Property Service returned null. Property may not have been created.");
            throw new ApplicationException("Property Service failed to create property. Received null response.");
        }
        
        if (savedProperty.getId() == null) {
            log.error("Property Service returned property without ID. Property creation failed.");
            throw new ApplicationException("Property Service returned property without ID. Property may not have been saved.");
        }
        
        log.info("Property created successfully via Property Service with ID: {} for agent: {}", 
                savedProperty.getId(), agentId);
        
        // Update agent's listing count (increment by 1)
        agentService.incrementAgentListings(agentId);
        
        return savedProperty;
    }

    /**
     * Get agent with their properties from property-service
     */
    public Agent getAgentWithProperties(UUID agentId) {
        log.debug("Getting agent with properties for ID: {}", agentId);
        
        Agent agent = agentService.findAgentById(agentId)
                .orElseThrow(() -> new AgentNotFoundException(agentId));

        // Properties are now managed in property-service, so we don't load them here
        // If needed, properties can be fetched via propertyServiceClient.getPropertiesByAgent(agentId)
        
        return agent;
    }

    /**
     * Creates both User account and Agent profile with profile picture
     */
    public Agent createAgentWithProfile(AgentRegistrationDto registrationDto, String profilePictureUrl) {
        log.info("Creating agent with profile for email: {} with profile picture: {}", 
                registrationDto.getEmail(), profilePictureUrl != null ? "Yes" : "No");

        // Step 1: Validate email uniqueness
        if (userService.userExistsByEmail(registrationDto.getEmail())) {
            throw new DuplicateEmailException(registrationDto.getEmail());
        }

        // Step 2: Validate license number uniqueness
        if (agentService.licenseNumberExists(registrationDto.getLicenseNumber())) {
            throw new DuplicateLicenseNumberException(registrationDto.getLicenseNumber());
        }

        // Step 3: Create User account
        User user = User.builder()
                .name(registrationDto.getName())
                .email(registrationDto.getEmail())
                .passwordHash(passwordEncoder.encode(registrationDto.getPassword()))
                .phone(registrationDto.getPhone())
                .role(UserRole.AGENT)
                .isActive(true)
                .build();

        User savedUser = userService.saveUser(user);
        log.info("User created with ID: {}", savedUser.getId());

        // Step 4: Create Agent profile
        Agent agent = Agent.builder()
                .user(savedUser)
                .licenseNumber(registrationDto.getLicenseNumber())
                .bio(registrationDto.getBio())
                .experienceYears(registrationDto.getExperienceYears() != null ? registrationDto.getExperienceYears() : 0)
                .specializations(formatSpecializationsAsJson(registrationDto.getSpecializations()))
                .rating(BigDecimal.ZERO)
                .totalListings(0)
                .profilePictureUrl(profilePictureUrl) // Add profile picture URL
                .build();

        Agent savedAgent = agentService.saveAgent(agent);
        log.info("Agent profile created with ID: {}", savedAgent.getId());

        // Step 5: Load agent with user for return
        Agent agentWithUser = agentService.findAgentById(savedAgent.getId())
                .orElseThrow(() -> new AgentNotFoundException(savedAgent.getId()));

        // Properties are now managed in property-service, no need to load them here

        return agentWithUser;
    }

    /**
     * Update agent profile
     */
    public Agent updateAgentProfile(UUID agentId, AgentRegistrationDto updateDto) {
        log.info("Updating agent profile for ID: {}", agentId);

        Agent agent = agentService.findAgentById(agentId)
                .orElseThrow(() -> new AgentNotFoundException(agentId));

        // Update agent fields
        if (updateDto.getBio() != null) {
            agent.setBio(updateDto.getBio());
        }
        if (updateDto.getExperienceYears() != null) {
            agent.setExperienceYears(updateDto.getExperienceYears());
        }
        if (updateDto.getSpecializations() != null) {
            agent.setSpecializations(formatSpecializationsAsJson(updateDto.getSpecializations()));
        }

        // Update user fields
        User user = agent.getUser();
        if (updateDto.getName() != null) {
            user.setName(updateDto.getName());
        }
        if (updateDto.getPhone() != null) {
            user.setPhone(updateDto.getPhone());
        }

        // Save updates
        userService.updateUser(user);
        Agent updatedAgent = agentService.updateAgent(agent);

        log.info("Agent profile updated for ID: {}", agentId);

        return updatedAgent;
    }

    /**
     * Delete agent and all their properties
     * Note: Properties are managed in property-service, 
     * so property deletion should be handled there or via API calls
     */
    public void deleteAgentAndProperties(UUID agentId) {
        log.info("Deleting agent and properties for ID: {}", agentId);

        Agent agent = agentService.findAgentById(agentId)
                .orElseThrow(() -> new AgentNotFoundException(agentId));

        // Get all properties for this agent from property-service
        try {
            List<PropertyDto> properties = propertyServiceClient.getPropertiesByAgent(agentId);
            log.warn("Agent {} has {} properties in property-service. " +
                    "These should be deleted via property-service API.", 
                    agentId, properties.size());
            // Note: Property deletion should be handled by property-service
            // or via direct API calls to property-service delete endpoints
        } catch (Exception e) {
            log.error("Error fetching properties for agent {} from property-service", agentId, e);
        }

        // Delete agent profile
        agentService.deleteAgent(agentId);

        // Delete user account
        userService.deleteUser(agent.getUser().getId());

        log.info("Agent deleted for ID: {}. Note: Properties in property-service may need separate cleanup.", agentId);
    }

    /**
     * Format specializations string as JSON array
     */
    private String formatSpecializationsAsJson(String specializations) {
        if (specializations == null || specializations.trim().isEmpty()) {
            return "[]";
        }
        
        // Split by comma and clean up each specialization
        String[] specs = specializations.split(",");
        StringBuilder jsonBuilder = new StringBuilder("[");
        boolean first = true;
        
        for (String spec : specs) {
            String trimmed = spec.trim();
            if (!trimmed.isEmpty()) {
                if (!first) {
                    jsonBuilder.append(",");
                }
                // Escape quotes and backslashes in JSON
                String escaped = trimmed.replace("\\", "\\\\").replace("\"", "\\\"");
                jsonBuilder.append("\"").append(escaped).append("\"");
                first = false;
            }
        }
        
        jsonBuilder.append("]");
        return jsonBuilder.toString();
    }
}
