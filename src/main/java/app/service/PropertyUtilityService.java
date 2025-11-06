package app.service;

import app.client.PropertyServiceClient;
import app.dto.PropertyDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Utility service for common property operations to reduce code duplication.
 * Provides cached and optimized methods for property-related operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PropertyUtilityService {

    private final PropertyServiceClient propertyServiceClient;

    /**
     * Get all properties with caching to avoid repeated calls.
     * This replaces multiple calls to getAllProperties(null, null, null, null)
     */
    @Cacheable(value = "allProperties", unless = "#result == null or #result.isEmpty()")
    public List<PropertyDto> getAllProperties() {
        log.debug("Fetching all properties from property-service");
        try {
            List<PropertyDto> properties = propertyServiceClient.getAllProperties(null, null, null, null);
            log.debug("Retrieved {} properties from property-service", properties != null ? properties.size() : 0);
            return properties != null ? properties : List.of();
        } catch (Exception e) {
            log.error("Error fetching all properties from property-service", e);
            return List.of();
        }
    }

    /**
     * Check if a property is active
     * @param property The property to check
     * @return true if property status is null or "ACTIVE"
     */
    public boolean isActiveProperty(PropertyDto property) {
        return property != null && 
               (property.getStatus() == null || "ACTIVE".equals(property.getStatus()));
    }

    /**
     * Filter properties to only include active ones
     * @param properties List of properties to filter
     * @return List of active properties
     */
    public List<PropertyDto> filterActiveProperties(List<PropertyDto> properties) {
        if (properties == null) {
            return List.of();
        }
        return properties.stream()
                .filter(this::isActiveProperty)
                .collect(Collectors.toList());
    }

    /**
     * Get active properties matching a city ID
     * @param cityId The city ID to filter by
     * @return List of active properties for the city
     */
    public List<PropertyDto> getActivePropertiesByCityId(UUID cityId) {
        if (cityId == null) {
            return List.of();
        }
        return getAllProperties().stream()
                .filter(p -> cityId.equals(p.getCityId()))
                .filter(this::isActiveProperty)
                .collect(Collectors.toList());
    }

    /**
     * Get active properties matching an agent ID
     * @param agentId The agent ID to filter by
     * @return List of active properties for the agent
     */
    public List<PropertyDto> getActivePropertiesByAgentId(UUID agentId) {
        if (agentId == null) {
            return List.of();
        }
        return getAllProperties().stream()
                .filter(p -> agentId.equals(p.getAgentId()))
                .filter(this::isActiveProperty)
                .collect(Collectors.toList());
    }

    /**
     * Get active properties matching a property type ID
     * @param propertyTypeId The property type ID to filter by
     * @return List of active properties for the property type
     */
    public List<PropertyDto> getActivePropertiesByPropertyTypeId(UUID propertyTypeId) {
        if (propertyTypeId == null) {
            return List.of();
        }
        return getAllProperties().stream()
                .filter(p -> propertyTypeId.equals(p.getPropertyTypeId()))
                .filter(this::isActiveProperty)
                .collect(Collectors.toList());
    }

    /**
     * Check if a city has any active properties
     * @param cityId The city ID to check
     * @return true if city has at least one active property
     */
    public boolean hasActiveProperties(UUID cityId) {
        return !getActivePropertiesByCityId(cityId).isEmpty();
    }

    /**
     * Check if an agent has any active properties
     * @param agentId The agent ID to check
     * @return true if agent has at least one active property
     */
    public boolean agentHasActiveProperties(UUID agentId) {
        return !getActivePropertiesByAgentId(agentId).isEmpty();
    }

    /**
     * Check if a property type has any active properties
     * @param propertyTypeId The property type ID to check
     * @return true if property type has at least one active property
     */
    public boolean propertyTypeHasActiveProperties(UUID propertyTypeId) {
        return !getActivePropertiesByPropertyTypeId(propertyTypeId).isEmpty();
    }

    /**
     * Count active properties for a city
     * @param cityId The city ID
     * @return Number of active properties
     */
    public long countActivePropertiesByCityId(UUID cityId) {
        return getActivePropertiesByCityId(cityId).size();
    }

    /**
     * Count active properties for an agent
     * @param agentId The agent ID
     * @return Number of active properties
     */
    public long countActivePropertiesByAgentId(UUID agentId) {
        return getActivePropertiesByAgentId(agentId).size();
    }

    /**
     * Count active properties for a property type
     * @param propertyTypeId The property type ID
     * @return Number of active properties
     */
    public long countActivePropertiesByPropertyTypeId(UUID propertyTypeId) {
        return getActivePropertiesByPropertyTypeId(propertyTypeId).size();
    }
}

