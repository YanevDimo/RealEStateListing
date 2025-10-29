package app.service;

import app.dto.AgentRegistrationDto;
import app.dto.PropertyDto;
import app.entity.*;
import app.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AgentRegistrationService {

    private final UserService userService;
    private final AgentService agentService;
    private final PropertyService propertyService;
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
     * Create a property for an agent
     */
    public Property createPropertyForAgent(UUID agentId, PropertyDto propertyDto) {
        log.info("Creating property for agent: {}", agentId);

        // Get agent
        Agent agent = agentService.findAgentById(agentId)
                .orElseThrow(() -> new AgentNotFoundException(agentId));

        // Get property type
        PropertyType propertyType = propertyTypeService.findPropertyTypeById(propertyDto.getPropertyTypeId())
                .orElseThrow(() -> new PropertyTypeNotFoundException(propertyDto.getPropertyTypeId()));

        // Get city
        City city = cityService.findCityById(propertyDto.getCityId())
                .orElseThrow(() -> new CityNotFoundException(propertyDto.getCityId()));

        // Create property
        Property property = Property.builder()
                .title(propertyDto.getTitle())
                .description(propertyDto.getDescription())
                .propertyType(propertyType)
                .city(city)
                .address(propertyDto.getAddress())
                .price(propertyDto.getPrice())
                .beds(propertyDto.getBeds() != null ? propertyDto.getBeds() : 0)
                .baths(propertyDto.getBaths() != null ? propertyDto.getBaths() : 0)
                .areaSqm(propertyDto.getAreaSqm())
                .yearBuilt(propertyDto.getYearBuilt())
                .agent(agent)  // Link to agent
                .latitude(propertyDto.getLatitude())
                .longitude(propertyDto.getLongitude())
                .status(PropertyStatus.ACTIVE)
                .featured(propertyDto.getFeatured() != null ? propertyDto.getFeatured() : false)
                .build();

        // Save property
        Property savedProperty = propertyService.saveProperty(property);

        // Update agent's listing count
        agentService.incrementAgentListings(agentId);

        log.info("Property created with ID: {} for agent: {}", savedProperty.getId(), agentId);

        return savedProperty;
    }

    /**
     * Get agent with their properties
     */
    public Agent getAgentWithProperties(UUID agentId) {
        log.debug("Getting agent with properties for ID: {}", agentId);
        
        Agent agent = agentService.findAgentById(agentId)
                .orElseThrow(() -> new AgentNotFoundException(agentId));

        // Load properties (this will trigger lazy loading)
        if (agent.getProperties() != null) {
            agent.getProperties().size(); // Force lazy loading
        }

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

        // Force lazy loading of properties
        if (agentWithUser.getProperties() != null) {
            agentWithUser.getProperties().size(); // Force lazy loading
        }

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
     */
    public void deleteAgentAndProperties(UUID agentId) {
        log.info("Deleting agent and properties for ID: {}", agentId);

        Agent agent = agentService.findAgentById(agentId)
                .orElseThrow(() -> new AgentNotFoundException(agentId));

        // Delete all agent's properties
        if (agent.getProperties() != null) {
            for (Property property : agent.getProperties()) {
                propertyService.deleteProperty(property.getId());
            }
        }

        // Delete agent profile
        agentService.deleteAgent(agentId);

        // Delete user account
        userService.deleteUser(agent.getUser().getId());

        log.info("Agent and all properties deleted for ID: {}", agentId);
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
