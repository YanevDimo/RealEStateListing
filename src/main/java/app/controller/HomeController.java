package app.controller;

import app.entity.Agent;
import app.entity.Property;
import app.entity.PropertyImage;
import app.service.AgentService;
import app.service.PropertyService;
import app.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class HomeController {

    private final PropertyService propertyService;
    private final AgentService agentService;
    private final SearchService searchService;

    @GetMapping("/")
    public ModelAndView home() {
        log.debug("Loading home page");
        ModelAndView modelAndView = new ModelAndView("index");
        try {
            modelAndView.addObject("featuredProperties", searchService.getFeaturedProperties());
            modelAndView.addObject("propertyTypes", searchService.getAvailablePropertyTypes());
            modelAndView.addObject("cities", searchService.getAvailableCities());
        } catch (Exception e) {
            log.error("Error loading home page data", e);
            // Fallback to empty lists if database is not available
            modelAndView.addObject("featuredProperties", List.of());
            modelAndView.addObject("propertyTypes", List.of());
            modelAndView.addObject("cities", List.of());
        }
        return modelAndView;
    }

    @GetMapping("/properties")
    public ModelAndView properties(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String maxPrice) {
        
        log.debug("Loading properties page with search: {}, city: {}, type: {}, maxPrice: {}", 
                search, city, type, maxPrice);
        
        ModelAndView modelAndView = new ModelAndView("properties/list");
        try {
            List<Property> properties;
            
            if (search != null && !search.trim().isEmpty()) {
                // Text search
                properties = searchService.searchPropertiesByText(search.trim());
            } else {
                // Advanced search
                SearchService.SearchCriteria criteria = new SearchService.SearchCriteria();
                criteria.setCityName(city);
                criteria.setPropertyTypeName(type);
                if (maxPrice != null && !maxPrice.trim().isEmpty()) {
                    try {
                        criteria.setMaxPrice(new BigDecimal(maxPrice));
                    } catch (NumberFormatException e) {
                        log.warn("Invalid maxPrice format: {}", maxPrice);
                    }
                }
                properties = searchService.searchProperties(criteria);
            }
            
            // Load images and agent information for each property
            for (Property property : properties) {

                List<PropertyImage>images = property.getImages();
                
                // Agent information is already loaded via the relationship
                // but we can ensure it's properly fetched
                if (property.getAgent() != null && property.getAgent().getUser() != null) {
                    // Agent and user information should be available
                    log.debug("Property {} has agent: {}", property.getTitle(), property.getAgent().getAgentName());
                }
            }
            
            modelAndView.addObject("properties", properties);
            modelAndView.addObject("search", search);
            modelAndView.addObject("selectedCity", city);
            modelAndView.addObject("selectedType", type);
            modelAndView.addObject("selectedMaxPrice", maxPrice);
            modelAndView.addObject("propertyTypes", searchService.getAvailablePropertyTypes());
            modelAndView.addObject("cities", searchService.getAvailableCities());
        } catch (Exception e) {
            log.error("Error loading properties", e);
            modelAndView.addObject("properties", List.of());
            modelAndView.addObject("propertyTypes", List.of());
            modelAndView.addObject("cities", List.of());
        }
        
        return modelAndView;
    }

    @GetMapping("/properties/detail")
    public ModelAndView propertyDetail(@RequestParam String id) {
        log.debug("Loading property detail for ID: {}", id);
        ModelAndView modelAndView = new ModelAndView("properties/detail");
        try {
            UUID propertyId = UUID.fromString(id);
            propertyService.findPropertyById(propertyId)
                    .ifPresentOrElse(
                            property -> modelAndView.addObject("property", property),
                            () -> modelAndView.addObject("error", "Property not found")
                    );
        } catch (IllegalArgumentException e) {
            log.error("Invalid property ID format: {}", id, e);
            modelAndView.addObject("error", "Invalid property ID");
        } catch (Exception e) {
            log.error("Error loading property detail", e);
            modelAndView.addObject("error", "Error loading property");
        }
        return modelAndView;
    }

    @GetMapping("/test-agents")
    public ModelAndView testAgents() {
        log.debug("Loading test agents page");
        ModelAndView modelAndView = new ModelAndView("agents/list");
        modelAndView.addObject("message", "Test agents page works!");
        return modelAndView;
    }

    @GetMapping("/agents")
    public ModelAndView agents() {
        log.debug("Loading agents page - using ModelAndView");
        ModelAndView modelAndView = new ModelAndView("agents/list");
        
        try {
            // Load agents from database
            log.debug("About to call agentService.findAllAgents()");
            List<Agent> agents = agentService.findAllAgents();
            log.info("Found {} agents in database", agents != null ? agents.size() : 0);
            
            if (agents == null || agents.isEmpty()) {
                log.info("No agents found in database");
                modelAndView.addObject("agents", List.of());
                modelAndView.addObject("totalAgents", 0);
                modelAndView.addObject("agentsWithProperties", 0);
                modelAndView.addObject("averageExperience", 0.0);
            } else {
                modelAndView.addObject("agents", agents);
                modelAndView.addObject("totalAgents", agents.size());
                
                // Calculate basic statistics
                long agentsWithProperties = agents.stream()
                        .filter(agent -> agent != null && agent.getTotalListings() != null && agent.getTotalListings() > 0)
                        .count();
                modelAndView.addObject("agentsWithProperties", agentsWithProperties);
                
                // Calculate average experience
                double avgExperience = agents.stream()
                        .filter(agent -> agent != null && agent.getExperienceYears() != null)
                        .mapToInt(Agent::getExperienceYears)
                        .average()
                        .orElse(0.0);
                modelAndView.addObject("averageExperience", Math.round(avgExperience * 10.0) / 10.0);
                
                // Set empty properties for now
                for (Agent agent : agents) {
                    if (agent != null) {
                        agent.setProperties(List.of());
                    }
                }
            }
            
            log.info("Agents page loaded successfully with ModelAndView");
            
        } catch (Exception e) {
            log.error("Error loading agents", e);
            modelAndView.addObject("agents", List.of());
            modelAndView.addObject("totalAgents", 0);
            modelAndView.addObject("agentsWithProperties", 0);
            modelAndView.addObject("averageExperience", 0.0);
        }
        
        return modelAndView;
    }

    @GetMapping("/agents/detail")
    public ModelAndView agentDetail(@RequestParam String id) {
        log.debug("Loading agent detail for ID: {}", id);
        ModelAndView modelAndView = new ModelAndView("agents/detail");
        try {
            UUID agentId = UUID.fromString(id);
            agentService.findAgentById(agentId)
                    .ifPresentOrElse(
                            agent -> {
                                modelAndView.addObject("agent", agent);
                                modelAndView.addObject("agentProperties", propertyService.findPropertiesByAgent(agentId));
                            },
                            () -> modelAndView.addObject("error", "Agent not found")
                    );
        } catch (IllegalArgumentException e) {
            log.error("Invalid agent ID format: {}", id, e);
            modelAndView.addObject("error", "Invalid agent ID");
        } catch (Exception e) {
            log.error("Error loading agent detail", e);
            modelAndView.addObject("error", "Error loading agent");
        }
        return modelAndView;
    }

    @GetMapping("/about")
    public ModelAndView about() {
        log.debug("Loading about page");
        ModelAndView modelAndView = new ModelAndView("about");
        
        try {
            // Add some statistics for the about page
            long totalAgents = agentService.countAllAgents();
            long totalProperties = propertyService.countAllProperties();
            
            modelAndView.addObject("totalAgents", totalAgents);
            modelAndView.addObject("totalProperties", totalProperties);
            
            log.info("About page loaded successfully");
            
        } catch (Exception e) {
            log.error("Error loading about page", e);
            // Set default values if there's an error
            modelAndView.addObject("totalAgents", 50);
            modelAndView.addObject("totalProperties", 500);
        }
        
        return modelAndView;
    }

    @GetMapping("/contact")
    public ModelAndView contact() {
        return new ModelAndView("contact");
    }


}
