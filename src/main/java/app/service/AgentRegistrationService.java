package app.service;

import app.client.PropertyServiceClient;
import app.dto.AgentRegistrationDto;
import app.dto.PropertyCreateDto;
import app.dto.PropertyDto;
import app.dto.PropertyUpdateDto;
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
    private final PropertyUtilityService propertyUtilityService;
    private final PasswordEncoder passwordEncoder;

    public Agent createAgentWithProfile(AgentRegistrationDto registrationDto) {
        log.info("Creating agent with profile for email: {}", registrationDto.getEmail());

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
        log.info("User account created with ID: {}", savedUser.getId());

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

    public PropertyDto createPropertyForAgent(UUID agentId, PropertyDto propertyDto) {
        log.info("Creating property for agent: {} via property-service", agentId);

        agentService.findAgentById(agentId)
                .orElseThrow(() -> new AgentNotFoundException(agentId));

        propertyTypeService.findPropertyTypeById(propertyDto.getPropertyTypeId())
                .orElseThrow(() -> new PropertyTypeNotFoundException(propertyDto.getPropertyTypeId()));

        cityService.findCityById(propertyDto.getCityId())
                .orElseThrow(() -> new CityNotFoundException(propertyDto.getCityId()));

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
                .imageUrls(propertyDto.getImageUrls())
                .build();

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
        
        propertyUtilityService.evictAllPropertiesCache();
        
        agentService.incrementAgentListings(agentId);
        
        return savedProperty;
    }

    public Agent getAgentWithProperties(UUID agentId) {
        log.debug("Getting agent with properties for ID: {}", agentId);
        
        return agentService.findAgentById(agentId)
                .orElseThrow(() -> new AgentNotFoundException(agentId));
    }

    public PropertyUpdateDto buildPropertyUpdateDto(PropertyDto propertyDto, PropertyDto existingProperty) {
        return PropertyUpdateDto.builder()
                .title(propertyDto.getTitle())
                .description(propertyDto.getDescription())
                .price(propertyDto.getPrice())
                .agentId(existingProperty.getAgentId())
                .cityId(propertyDto.getCityId())
                .propertyTypeId(propertyDto.getPropertyTypeId())
                .status(propertyDto.getStatus() != null ? propertyDto.getStatus() : existingProperty.getStatus())
                .bedrooms(propertyDto.getBedrooms() != null ? propertyDto.getBedrooms() :
                         (propertyDto.getBeds() != null ? propertyDto.getBeds() : existingProperty.getBedrooms()))
                .bathrooms(propertyDto.getBathrooms() != null ? propertyDto.getBathrooms() :
                          (propertyDto.getBaths() != null ? propertyDto.getBaths() : existingProperty.getBathrooms()))
                .squareFeet(propertyDto.getSquareFeet() != null ? propertyDto.getSquareFeet() :
                           (propertyDto.getAreaSqm() != null ? propertyDto.getAreaSqm().intValue() : existingProperty.getSquareFeet()))
                .address(propertyDto.getAddress())
                .features(propertyDto.getFeatures())
                .build();
    }

    public Agent createAgentWithProfile(AgentRegistrationDto registrationDto, String profilePictureUrl) {
        log.info("Creating agent with profile for email: {} with profile picture: {}", 
                registrationDto.getEmail(), profilePictureUrl != null ? "Yes" : "No");

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

        Agent agent = Agent.builder()
                .user(savedUser)
                .licenseNumber(registrationDto.getLicenseNumber())
                .bio(registrationDto.getBio())
                .experienceYears(registrationDto.getExperienceYears() != null ? registrationDto.getExperienceYears() : 0)
                .specializations(formatSpecializationsAsJson(registrationDto.getSpecializations()))
                .rating(BigDecimal.ZERO)
                .totalListings(0)
                .profilePictureUrl(profilePictureUrl)
                .build();

        Agent savedAgent = agentService.saveAgent(agent);
        log.info("Agent profile created with ID: {}", savedAgent.getId());

        return agentService.findAgentById(savedAgent.getId())
                .orElseThrow(() -> new AgentNotFoundException(savedAgent.getId()));
    }

    public void updateAgentProfile(UUID agentId, AgentRegistrationDto updateDto) {
        log.info("Updating agent profile for ID: {}", agentId);

        Agent agent = agentService.findAgentById(agentId)
                .orElseThrow(() -> new AgentNotFoundException(agentId));

        if (updateDto.getBio() != null) {
            agent.setBio(updateDto.getBio());
        }
        if (updateDto.getExperienceYears() != null) {
            agent.setExperienceYears(updateDto.getExperienceYears());
        }
        if (updateDto.getSpecializations() != null) {
            agent.setSpecializations(formatSpecializationsAsJson(updateDto.getSpecializations()));
        }
        if (updateDto.getLicenseNumber() != null && !updateDto.getLicenseNumber().trim().isEmpty()) {
            agent.setLicenseNumber(updateDto.getLicenseNumber());
        }

        User user = agent.getUser();
        if (updateDto.getName() != null && !updateDto.getName().trim().isEmpty()) {
            user.setName(updateDto.getName());
        }
        if (updateDto.getPhone() != null) {
                user.setPhone(updateDto.getPhone().trim().isEmpty() ? null : updateDto.getPhone());
        }

        userService.updateUser(user);
        Agent updatedAgent = agentService.updateAgent(agent);

        log.info("Agent profile updated for ID: {}", agentId);

    }

    private String formatSpecializationsAsJson(String specializations) {
        if (specializations == null || specializations.trim().isEmpty()) {
            return "[]";
        }
        
        String[] specs = specializations.split(",");
        StringBuilder jsonBuilder = new StringBuilder("[");
        boolean first = true;
        
        for (String spec : specs) {
            String trimmed = spec.trim();
            if (!trimmed.isEmpty()) {
                if (!first) {
                    jsonBuilder.append(",");
                }
                String escaped = trimmed.replace("\\", "\\\\").replace("\"", "\\\"");
                jsonBuilder.append("\"").append(escaped).append("\"");
                first = false;
            }
        }
        
        jsonBuilder.append("]");
        return jsonBuilder.toString();
    }
}
