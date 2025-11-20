package app.service;

import app.dto.PropertyDto;
import app.entity.Agent;
import app.entity.City;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service for enriching PropertyDto objects with additional data
 * such as city names and agent information for display purposes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PropertyEnrichmentService {

    private final CityService cityService;
    private final AgentService agentService;

    /**
     * Enrich a list of PropertyDto objects with city and agent names for template display
     */
    public List<PropertyDto> enrichPropertiesWithNames(List<PropertyDto> properties) {
        log.debug("Enriching {} properties with city and agent information", properties.size());
        return properties.stream().map(this::enrichPropertyWithNames).toList();
    }

    /**
     * Enrich a single PropertyDto with city and agent names for template display
     */
    public PropertyDto enrichPropertyWithNames(PropertyDto property) {
        try {
            // Add city name
            if (property.getCityId() != null) {
                Optional<City> cityOpt = cityService.findCityById(property.getCityId());
                if (cityOpt.isPresent()) {
                    City city = cityOpt.get();
                    property.setCityName(city.getName());
                    log.trace("Enriched property {} with city name: {}", property.getId(), city.getName());
                } else {
                    log.warn("City not found for property {} with cityId: {}", property.getId(), property.getCityId());
                }
            } else {
                log.debug("Property {} has no cityId", property.getId());
            }

            // Add agent information
            if (property.getAgentId() != null) {
                Optional<Agent> agentOpt = agentService.findAgentById(property.getAgentId());
                if (agentOpt.isPresent()) {
                    Agent agent = agentOpt.get();
                    property.setAgentName(agent.getAgentName());
                    property.setAgentEmail(agent.getAgentEmail());
                    property.setAgentProfilePictureUrl(agent.getProfilePictureUrl());
                    property.setAgentRating(agent.getRating());
                    property.setAgentTotalListings(agent.getTotalListings());
                    log.trace("Enriched property {} with agent: {} ({})", property.getId(), agent.getAgentName(), agent.getId());
                } else {
                    log.warn("Agent not found for property {} with agentId: {}. Agent may not exist in main app database.",
                            property.getId(), property.getAgentId());
                }
            } else {
                log.debug("Property {} has no agentId", property.getId());
            }
        } catch (Exception e) {
            log.error("Error enriching property {}: {}", property.getId(), e.getMessage(), e);
        }
        return property;
    }
}


