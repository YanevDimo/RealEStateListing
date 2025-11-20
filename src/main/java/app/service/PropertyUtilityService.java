package app.service;

import app.client.PropertyServiceClient;
import app.dto.PropertyDto;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

 // Utility service for common property operations to reduce code duplication.
 //Provides cached and optimized methods for property-related operations.

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
            log.info("Retrieved {} properties from property-service via getAllProperties()", properties != null ? properties.size() : 0);
            return properties != null ? properties : List.of();
        } catch (FeignException e) {
            log.error("Error fetching all properties from property-service: Status {} - {}", e.status(), e.getMessage());
            if (e.status() == 500) {
                log.warn("Property-service returned 500 error. This may indicate a Hibernate issue in the property-service.");
            }
            return List.of();
        } catch (Exception e) {
            log.error("Unexpected error fetching all properties from property-service", e);
            return List.of();
        }
    }


     // Check if a property is active

    public boolean isActiveProperty(PropertyDto property) {
        return property != null && 
               (property.getStatus() == null || "ACTIVE".equals(property.getStatus()));
    }


     // Filter properties to only include active ones

    public List<PropertyDto> filterActiveProperties(List<PropertyDto> properties) {
        if (properties == null) {
            return List.of();
        }
        return properties.stream()
                .filter(this::isActiveProperty)
                .collect(Collectors.toList());
    }


     // Get active properties matching a city ID

    public List<PropertyDto> getActivePropertiesByCityId(UUID cityId) {
        if (cityId == null) {
            return List.of();
        }
        return getAllProperties().stream()
                .filter(p -> cityId.equals(p.getCityId()))
                .filter(this::isActiveProperty)
                .collect(Collectors.toList());
    }

     // Get active properties matching an agent ID

    public List<PropertyDto> getActivePropertiesByAgentId(UUID agentId) {
        if (agentId == null) {
            return List.of();
        }
        return getAllProperties().stream()
                .filter(p -> agentId.equals(p.getAgentId()))
                .filter(this::isActiveProperty)
                .collect(Collectors.toList());
    }


     // Get active properties matching a property type ID

    public List<PropertyDto> getActivePropertiesByPropertyTypeId(UUID propertyTypeId) {
        if (propertyTypeId == null) {
            return List.of();
        }
        return getAllProperties().stream()
                .filter(p -> propertyTypeId.equals(p.getPropertyTypeId()))
                .filter(this::isActiveProperty)
                .collect(Collectors.toList());
    }


     // Check if a city has any active properties

    public boolean hasActiveProperties(UUID cityId) {
        return !getActivePropertiesByCityId(cityId).isEmpty();
    }


     // Check if an agent has any active properties

    public boolean agentHasActiveProperties(UUID agentId) {
        return !getActivePropertiesByAgentId(agentId).isEmpty();
    }


     // Check if a property type has any active properties

    public boolean propertyTypeHasActiveProperties(UUID propertyTypeId) {
        return !getActivePropertiesByPropertyTypeId(propertyTypeId).isEmpty();
    }


     // Count active properties for a city

    public long countActivePropertiesByCityId(UUID cityId) {
        return getActivePropertiesByCityId(cityId).size();
    }


     // Count active properties for an agent

    public long countActivePropertiesByAgentId(UUID agentId) {
        return getActivePropertiesByAgentId(agentId).size();
    }


     // Count active properties for a property type

    public long countActivePropertiesByPropertyTypeId(UUID propertyTypeId) {
        return getActivePropertiesByPropertyTypeId(propertyTypeId).size();
    }

    /**
     * Get properties by agent ID with fallback handling for 500 errors.
     * If the getPropertiesByAgent endpoint returns 500 (Hibernate bug), 
     * falls back to getAllProperties() and filters by agentId in memory.
     * 
     * @param agentId The agent ID
     * @return List of properties for the agent (all statuses, not just active)
     */
    public List<PropertyDto> getPropertiesByAgent(UUID agentId) {
        if (agentId == null) {
            return List.of();
        }
        
        log.debug("Fetching properties for agent: {}", agentId);
        try {
            List<PropertyDto> properties = propertyServiceClient.getPropertiesByAgent(agentId);
            log.info("Retrieved {} properties for agent {} via getPropertiesByAgent()", 
                    properties != null ? properties.size() : 0, agentId);
            return properties != null ? properties : List.of();
        } catch (FeignException e) {
            log.error("Error fetching properties for agent {}: Status {} - {}", agentId, e.status(), e.getMessage());
            
            // If it's a 500 error (Hibernate bug), try fallback to getAllProperties() and filter in memory
            if (e.status() == 500) {
                log.warn("Property-service getPropertiesByAgent returned 500 error (Hibernate MultipleBagFetchException). " +
                        "Attempting fallback to getAllProperties() with in-memory filtering");
                try {
                    List<PropertyDto> allProperties = getAllProperties();
                    if (allProperties != null && !allProperties.isEmpty()) {
                        List<PropertyDto> filteredProperties = allProperties.stream()
                                .filter(p -> agentId.equals(p.getAgentId()))
                                .collect(Collectors.toList());
                        log.info("Fallback successful: Retrieved {} properties for agent {} via getAllProperties()", 
                                filteredProperties.size(), agentId);
                        return filteredProperties;
                    } else {
                        log.warn("Fallback getAllProperties() returned null or empty list");
                    }
                } catch (Exception fallbackEx) {
                    log.error("Fallback to getAllProperties() also failed for agent {}", agentId, fallbackEx);
                }
            }
            
            log.warn("Returning empty list due to property-service error for agent {}", agentId);
            return List.of();
        } catch (Exception e) {
            log.error("Unexpected error fetching properties for agent {}", agentId, e);
            return List.of();
        }
    }

    /**
     * Evict the allProperties cache.
     * This should be called after creating, updating, or deleting a property
     * to ensure the cache reflects the current state.
     */
    @CacheEvict(value = "allProperties", allEntries = true)
    public void evictAllPropertiesCache() {
        log.debug("Evicting allProperties cache");
    }
}

