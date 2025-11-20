package app.controller;

import app.client.PropertyServiceClient;
import app.dto.PropertyDto;
import app.dto.InquiryDto;
import app.service.AgentService;
import app.service.InquiryService;
import app.service.PropertyEnrichmentService;
import app.service.PropertyUtilityService;
import app.service.SearchService;
import app.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import app.entity.Agent;
import app.entity.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class HomeController {

    private final PropertyServiceClient propertyServiceClient;
    private final AgentService agentService;
    private final SearchService searchService;
    private final UserService userService;
    private final PropertyUtilityService propertyUtilityService;
    private final PropertyEnrichmentService propertyEnrichmentService;
    private final InquiryService inquiryService;

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
            List<PropertyDto> properties;
            
            if (search != null && !search.trim().isEmpty()) {
                // Text search via property-service
                log.debug("Using text search for: {}", search);
                properties = searchService.searchPropertiesByText(search.trim());
            } else if ((city != null && !city.trim().isEmpty()) || 
                      (type != null && !type.trim().isEmpty()) ||
                      (maxPrice != null && !maxPrice.trim().isEmpty())) {
                // Advanced search with filters via property-service
                log.debug("Using advanced search with filters");
                SearchService.SearchCriteria criteria = searchService.buildSearchCriteria(null, city, type, maxPrice);
                properties = searchService.searchProperties(criteria);
            } else {
                // No filters - use getAllProperties() directly to avoid search endpoint 500 error
                log.debug("No filters applied, using getAllProperties() directly");
                properties = propertyUtilityService.getAllProperties();
                log.info("Retrieved {} properties via getAllProperties()", properties != null ? properties.size() : 0);
            }
            
            log.info("Found {} properties to display", properties != null ? properties.size() : 0);
            if (properties == null || properties.isEmpty()) {
                log.warn("No properties found. Check if Property Service (port 8083) has data in its database.");
                modelAndView.addObject("warningMessage", "No properties found. Property Service may be empty.");
            }
            
            // Enrich properties with city and agent data for template display
            if (properties != null && !properties.isEmpty()) {
                try {
                    properties = propertyEnrichmentService.enrichPropertiesWithNames(properties);
                    log.debug("Enriched {} properties with city and agent names", properties.size());
                } catch (Exception e) {
                    log.warn("Error enriching properties, displaying without enrichment: {}", e.getMessage());
                    // Continue with unenriched properties rather than failing
                }
            }
            
            modelAndView.addObject("properties", properties != null ? properties : List.of());
            modelAndView.addObject("search", search);
            modelAndView.addObject("selectedCity", city);
            modelAndView.addObject("selectedType", type);
            modelAndView.addObject("selectedMaxPrice", maxPrice);
            modelAndView.addObject("propertyTypes", searchService.getAvailablePropertyTypes());
            modelAndView.addObject("cities", searchService.getAvailableCities());
        } catch (Exception e) {
            log.error("Error loading properties from property-service", e);
            modelAndView.addObject("errorMessage", "Error loading properties. Is Property Service running on port 8083?");
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
            PropertyDto property = propertyServiceClient.getPropertyById(propertyId);
            if (property != null) {
                // Enrich property with city and agent information
                PropertyDto enrichedProperty = propertyEnrichmentService.enrichPropertyWithNames(property);
                modelAndView.addObject("property", enrichedProperty);
                modelAndView.addObject("inquiryDto", new InquiryDto());
            } else {
                modelAndView.addObject("error", "Property not found");
            }
        } catch (IllegalArgumentException e) {
            log.error("Invalid property ID format: {}", id, e);
            modelAndView.addObject("error", "Invalid property ID");
        } catch (Exception e) {
            log.error("Error loading property detail from property-service", e);
            modelAndView.addObject("error", "Error loading property");
        }
        return modelAndView;
    }

    @PostMapping("/properties/{id}/inquiry")
    public ModelAndView submitInquiry(@PathVariable String id,
                                     @Valid @ModelAttribute InquiryDto inquiryDto,
                                     BindingResult bindingResult,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        log.info("Processing inquiry submission for property: {}", id);
        
        try {
            UUID propertyId = UUID.fromString(id);
            
            // Verify property exists
            PropertyDto property = propertyServiceClient.getPropertyById(propertyId);
            if (property == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Property not found");
                return new ModelAndView("redirect:/properties");
            }
            
            if (bindingResult.hasErrors()) {
                log.warn("Inquiry validation errors for property: {}", id);
                ModelAndView modelAndView = new ModelAndView("properties/detail");
                PropertyDto enrichedProperty = propertyEnrichmentService.enrichPropertyWithNames(property);
                modelAndView.addObject("property", enrichedProperty);
                modelAndView.addObject("inquiryDto", inquiryDto);
                return modelAndView;
            }
            
            // Set property ID in DTO
            inquiryDto.setPropertyId(propertyId);
            
            // Create inquiry
            inquiryService.createInquiry(inquiryDto, propertyId, authentication);
            log.info("Inquiry created successfully for property: {}", id);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Thank you! Your inquiry has been submitted successfully. We'll contact you soon.");
            return new ModelAndView("redirect:/properties/detail?id=" + id);
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid property ID format: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid property ID");
            return new ModelAndView("redirect:/properties");
        } catch (Exception e) {
            log.error("Error creating inquiry for property: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                    "Failed to submit inquiry. Please try again.");
            return new ModelAndView("redirect:/properties/detail?id=" + id);
        }
    }

    @GetMapping("/test-agents")
    public ModelAndView testAgents() {
        log.debug("Loading test agents page");
        ModelAndView modelAndView = new ModelAndView("agents/list");
        modelAndView.addObject("message", "Test agents page works!");
        return modelAndView;
    }

    @GetMapping("/agents")
    public ModelAndView agents(Authentication authentication) {
        log.debug("Loading agents page - using ModelAndView");
        ModelAndView modelAndView = new ModelAndView("agents/list");
        
        try {
            // Load agents from database
            log.debug("About to call agentService.findAllAgents()");
            List<Agent> agents = agentService.findAllAgents();
            log.info("Found {} agents in database", agents != null ? agents.size() : 0);
            
            // Check if current user is an agent and pass as String for Thymeleaf comparison
            String currentAgentIdString = null;
            if (authentication != null && authentication.isAuthenticated()) {
                try {
                    String email = authentication.getName();
                    Optional<User> user = userService.findUserByEmail(email);
                    if (user.isPresent()) {
                        Optional<Agent> agent = agentService.findAgentByUserId(user.get().getId());
                        if (agent.isPresent()) {
                            currentAgentIdString = agent.get().getId().toString();
                            log.debug("Current logged-in agent ID: {}", currentAgentIdString);
                        }
                    }
                } catch (Exception e) {
                    log.debug("Current user is not an agent or error getting agent: {}", e.getMessage());
                }
            }
            modelAndView.addObject("currentAgentId", currentAgentIdString);
            
            if (agents == null || agents.isEmpty()) {
                log.info("No agents found in database");
                modelAndView.addObject("agents", List.of());
                modelAndView.addObject("totalAgents", 0);
                modelAndView.addObject("agentsWithProperties", 0);
                modelAndView.addObject("averageExperience", 0.0);
            } else {
                modelAndView.addObject("agents", agents);
                
                // Calculate statistics using service
                AgentService.AgentListStatistics statistics = agentService.calculateAgentListStatistics(agents);
                modelAndView.addObject("totalAgents", statistics.getTotalAgents());
                modelAndView.addObject("agentsWithProperties", statistics.getAgentsWithProperties());
                modelAndView.addObject("averageExperience", statistics.getAverageExperience());
                
                // Note: Agent properties are loaded separately on agent detail page
                // Properties are managed by Property Service microservice
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
                                try {
                                    List<PropertyDto> agentProperties = propertyUtilityService.getPropertiesByAgent(agentId);
                                    modelAndView.addObject("agentProperties", agentProperties);
                                } catch (Exception e) {
                                    log.error("Error loading agent properties from property-service", e);
                                    modelAndView.addObject("agentProperties", List.of());
                                }
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
            // Get property count from property-service
            long totalProperties = propertyUtilityService.getAllProperties().size();
            
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
