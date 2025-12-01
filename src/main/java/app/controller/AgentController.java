package app.controller;


import app.client.PropertyServiceClient;
import app.dto.AgentRegistrationDto;
import app.dto.InquiryUpdateDto;
import app.dto.PropertyDto;
import app.dto.PropertyUpdateDto;
import app.entity.Agent;
import app.entity.City;
import app.entity.Inquiry;
import app.entity.InquiryStatus;
import app.entity.PropertyType;
import app.entity.User;
import app.exception.AgentNotFoundException;
import app.exception.ApplicationException;
import app.exception.InquiryNotFoundException;
import app.exception.PropertyNotFoundException;
import app.exception.UserNotFoundException;
import app.service.*;
import feign.FeignException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/agent")
@RequiredArgsConstructor
@Slf4j
public class AgentController {

    private final AgentRegistrationService agentRegistrationService;
    private final AgentService agentService;
    private final UserService userService;
    private final PropertyServiceClient propertyServiceClient;
    private final PropertyUtilityService propertyUtilityService;
    private final CityService cityService;
    private final PropertyTypeService propertyTypeService;
    private final FileUploadService fileUploadService;
    private final InquiryService inquiryService;

    /**
     * Show agent registration form
     */
    @GetMapping("/register")
    public ModelAndView showAgentRegistrationForm() {
        log.debug("Showing agent registration form");
        ModelAndView modelAndView = new ModelAndView("auth/agent-register");
        modelAndView.addObject("agentRegistrationDto", new AgentRegistrationDto());
        return modelAndView;
    }


    @PostMapping("/register")
    public String registerAgent(@Valid @ModelAttribute AgentRegistrationDto registrationDto,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes) {
        log.info("Processing agent registration for email: {}", registrationDto.getEmail());

        // Validate password confirmation
        if (!registrationDto.isPasswordMatching()) {
            bindingResult.rejectValue("confirmPassword", "error.confirmPassword", "Passwords do not match");
        }

        if (bindingResult.hasErrors()) {
            log.warn("Agent registration validation errors for email: {}", registrationDto.getEmail());
            return "auth/agent-register";
        }

        try {
            // Handle profile picture upload if provided
            String profilePictureUrl = null;
            if (registrationDto.getProfilePicture() != null && !registrationDto.getProfilePicture().isEmpty()) {
                try {
                    List<String> uploadedFiles = fileUploadService.uploadFiles(List.of(registrationDto.getProfilePicture()));
                    if (!uploadedFiles.isEmpty()) {
                        profilePictureUrl = uploadedFiles.get(0);
                        log.info("Profile picture uploaded successfully: {}", profilePictureUrl);
                    }
                } catch (Exception e) {
                    log.warn("Failed to upload profile picture: {}", e.getMessage());
                    // Continue registration without profile picture
                }
            }

            Agent agent = agentRegistrationService.createAgentWithProfile(registrationDto, profilePictureUrl);
            log.info("Agent registered successfully with ID: {}", agent.getId());

            redirectAttributes.addFlashAttribute("successMessage", 
                    "Agent registration successful! Please login with your credentials.");
            return "redirect:/auth/login";

        } catch (Exception e) {
            log.error("Error during agent registration", e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                    "Registration failed: " + e.getMessage());
            return "redirect:/agent/register";
        }
    }


    @GetMapping("/dashboard")
    public ModelAndView showAgentDashboard(Authentication authentication) {
        log.debug("Showing agent dashboard");
        ModelAndView modelAndView = new ModelAndView("dashboard/agent-dashboard");

        try {
            // Get current agent using helper method
            Agent agent = getCurrentAgent(authentication);

            // Get agent's properties from property-service (with fallback handling)
            List<PropertyDto> agentProperties = new ArrayList<>(); // Initialize to empty list
            try {
                agentProperties = propertyUtilityService.getPropertiesByAgent(agent.getId());
                if (agentProperties == null || agentProperties.isEmpty()) {
                    log.debug("No properties found for agent {}", agent.getId());
                }
            } catch (Exception e) {
                log.error("Error loading agent properties from property-service: {}", e.getMessage(), e);
                agentProperties = new ArrayList<>(); // Empty list as fallback
                modelAndView.addObject("serviceWarning", 
                    "Error loading properties. Please try again later.");
            }

            // Get statistics
            long totalProperties = agentProperties.size();
            long activeProperties = propertyUtilityService.filterActiveProperties(agentProperties).size();

            modelAndView.addObject("agent", agent);
            modelAndView.addObject("properties", agentProperties);
            modelAndView.addObject("totalProperties", totalProperties);
            modelAndView.addObject("activeProperties", activeProperties);
            modelAndView.addObject("totalListings", agent.getTotalListings());
            modelAndView.addObject("rating", agent.getRating());

        } catch (ApplicationException e) {
            log.error("Error loading agent dashboard: {}", e.getMessage(), e);
            modelAndView.addObject("error", "Error loading dashboard: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error loading agent dashboard", e);
            modelAndView.addObject("error", "An unexpected error occurred while loading the dashboard.");
        }

        return modelAndView;
    }


    @GetMapping("/properties/add")
    public ModelAndView showAddPropertyForm() {
        log.debug("Showing add property form");
        ModelAndView modelAndView = new ModelAndView("agent/add-property");
        
        modelAndView.addObject("propertyDto", new PropertyDto());
        // Cities and propertyTypes are automatically added by ModelAttribute
        
        return modelAndView;
    }


     // Process add property

    @PostMapping("/properties/add")
    public ModelAndView addProperty(@Valid @ModelAttribute PropertyDto propertyDto,
                            BindingResult bindingResult,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        log.info("Processing add property: {}", propertyDto.getTitle());
        log.info("Property DTO details - Title: {}, Price: {}, PropertyTypeId: {}, CityId: {}", 
                propertyDto.getTitle(), propertyDto.getPrice(), propertyDto.getPropertyTypeId(), propertyDto.getCityId());

        if (bindingResult.hasErrors()) {
            log.warn("Property validation errors: {}", bindingResult.getAllErrors());
            ModelAndView modelAndView = new ModelAndView("agent/add-property");
            modelAndView.addObject("propertyDto", propertyDto);
            // Cities and propertyTypes are automatically added by ModelAttribute
            return modelAndView;
        }

        try {
            // Get current agent using helper method
            Agent agent = getCurrentAgent(authentication);

            // Upload images if provided
            List<String> imageUrls = new ArrayList<>();
            if (propertyDto.getImages() != null && !propertyDto.getImages().isEmpty()) {
                try {
                    imageUrls = fileUploadService.uploadFiles(propertyDto.getImages());
                    log.info("Uploaded {} images for property", imageUrls.size());
                } catch (Exception e) {
                    log.error("Error uploading images", e);
                    return createAddPropertyErrorModel(propertyDto, "Failed to upload images: " + e.getMessage());
                }
            }

            // Add image URLs to PropertyDto (if any)
            if (!imageUrls.isEmpty()) {
                propertyDto.setImageUrls(imageUrls);
                log.info("✅ Set {} image URLs on PropertyDto: {}", imageUrls.size(), imageUrls);
            } else {
                log.warn("⚠️ No image URLs to set! imageUrls list is empty. Uploaded files count: {}", 
                        propertyDto.getImages() != null ? propertyDto.getImages().size() : 0);
            }
            
            // Verify imageUrls are set before sending
            log.info("PropertyDto imageUrls before sending: {}", propertyDto.getImageUrls());
            
            // Create property via property-service
            log.info("Attempting to create property for agent: {} via Property Service", agent.getId());
            PropertyDto property;
            try {
                property = agentRegistrationService.createPropertyForAgent(agent.getId(), propertyDto);
                
                if (property == null || property.getId() == null) {
                    log.error("Property creation returned null or property without ID");
                    throw new ApplicationException("Property Service failed to create property. Received null or invalid response.");
                }
                
                log.info("Property created successfully via Property Service with ID: {} and {} images", 
                        property.getId(), imageUrls.size());
                
                redirectAttributes.addFlashAttribute("successMessage", 
                        "Property added successfully! Property ID: " + property.getId());
                return new ModelAndView("redirect:/agent/dashboard");
            } catch (FeignException e) {
                // Re-throw FeignException to be handled by outer catch block (no duplicate logging)
                throw e;
            } catch (ApplicationException e) {
                // Re-throw ApplicationException to be handled by outer catch block
                throw e;
            } catch (Exception e) {
                log.error("Unexpected error creating property via Property Service: {}", e.getMessage(), e);
                throw e; // Re-throw to be caught by outer catch block
            }

        } catch (FeignException e) {
            // Log connection issues at WARN level (infrastructure issue, not application error)
            if (e instanceof feign.RetryableException) {
                log.warn("Property Service unavailable (connection refused). User cannot add property. Service URL: http://localhost:8083");
            } else {
                log.error("Property Service communication error: {}", e.getMessage(), e);
            }
            
            // Provide user-friendly error message based on exception type
            String errorMessage;
            if (e instanceof feign.RetryableException) {
                errorMessage = "Property Service is currently unavailable. Please ensure the property service is running on port 8083 and try again.";
            } else {
                errorMessage = "Failed to communicate with Property Service. Please try again later.";
            }
            return createAddPropertyErrorModel(propertyDto, errorMessage);
        } catch (ApplicationException e) {
            log.error("Error adding property: {}", e.getMessage(), e);
            return createAddPropertyErrorModel(propertyDto, "Failed to add property: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error adding property: {}", e.getMessage(), e);
            return createAddPropertyErrorModel(propertyDto, "An unexpected error occurred while adding the property. Please try again.");
        }
    }


    @GetMapping("/properties/edit/{id}")
    public ModelAndView showEditPropertyForm(@PathVariable UUID id, Authentication authentication) {
        log.debug("Showing edit property form for ID: {}", id);
        ModelAndView modelAndView = new ModelAndView("agent/edit-property");

        try {
            // Get current agent using helper method
            Agent agent = getCurrentAgent(authentication);

            // Verify agent owns this property using helper method
            PropertyDto property = verifyPropertyOwnership(id, agent.getId(), "edit");

            // PropertyDto already has all the data we need
            PropertyDto propertyDto = property;

            modelAndView.addObject("propertyDto", propertyDto);
            modelAndView.addObject("propertyId", id);
            // Cities and propertyTypes are automatically added by @ModelAttribute

        } catch (Exception e) {
            log.error("Error loading edit property form", e);
            modelAndView.addObject("error", "Error loading property: " + e.getMessage());
        }

        return modelAndView;
    }

    /**
     * Process edit property
     */
    @PostMapping("/properties/edit/{id}")
    public String editProperty(@PathVariable UUID id,
                             @Valid @ModelAttribute PropertyDto propertyDto,
                             BindingResult bindingResult,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        log.info("Processing edit property for ID: {}", id);

        if (bindingResult.hasErrors()) {
            log.warn("Property validation errors");
            return "agent/edit-property";
        }

        try {
            // Get current agent using helper method
            Agent agent = getCurrentAgent(authentication);

            // Verify agent owns this property using helper method
            PropertyDto property = verifyPropertyOwnership(id, agent.getId(), "edit");

            // Convert PropertyDto to PropertyUpdateDto using service
            PropertyUpdateDto updateDto = agentRegistrationService.buildPropertyUpdateDto(propertyDto, property);

            // Update property via property-service
            propertyServiceClient.updateProperty(id, updateDto);
            
            // Evict allProperties cache to reflect the updated property
            propertyUtilityService.evictAllPropertiesCache();
            
            log.info("Property updated successfully with ID: {}", id);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Property updated successfully!");
            return "redirect:/agent/dashboard";

        } catch (Exception e) {
            log.error("Error updating property", e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                    "Failed to update property: " + e.getMessage());
            return "redirect:/agent/properties/edit/" + id;
        }
    }

    /**
     * Delete property
     */
    @PostMapping("/properties/delete/{id}")
    public String deleteProperty(@PathVariable UUID id,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        log.info("Processing delete property for ID: {}", id);

        try {
            // Get current agent using helper method
            Agent agent = getCurrentAgent(authentication);

            // Verify agent owns this property using helper method
            PropertyDto property = verifyPropertyOwnership(id, agent.getId(), "delete");

            // Delete property via property-service
            propertyServiceClient.deleteProperty(id);
            
            // Evict allProperties cache to reflect the deleted property
            propertyUtilityService.evictAllPropertiesCache();
            
            // Decrement agent's listing count
            agentService.decrementAgentListings(agent.getId());
            
            log.info("Property deleted successfully with ID: {}", id);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Property deleted successfully!");

        } catch (Exception e) {
            log.error("Error deleting property", e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                    "Failed to delete property: " + e.getMessage());
        }

        return "redirect:/agent/dashboard";
    }

    /**
     * Show agent profile edit form
     */
    @GetMapping("/profile/edit")
    public ModelAndView showEditProfileForm(Authentication authentication) {
        log.debug("Showing edit profile form");
        ModelAndView modelAndView = new ModelAndView("agent/edit-profile");

        try {
            // Get current agent using helper method
            Agent agent = getCurrentAgent(authentication);

            // Get user from agent
            User user = agent.getUser();
            if (user == null) {
                throw new UserNotFoundException("User not found for agent");
            }

            AgentRegistrationDto profileDto = AgentRegistrationDto.builder()
                    .name(user.getName() != null ? user.getName() : "")
                    .email(user.getEmail() != null ? user.getEmail() : "")
                    .phone(user.getPhone() != null ? user.getPhone() : "")
                    .licenseNumber(agent.getLicenseNumber() != null ? agent.getLicenseNumber() : "")
                    .bio(agent.getBio() != null ? agent.getBio() : "")
                    .experienceYears(agent.getExperienceYears() != null ? agent.getExperienceYears() : 0)
                    .specializations(agentService.parseSpecializationsFromJson(agent.getSpecializations()))
                    .build();

            modelAndView.addObject("profileDto", profileDto);

        } catch (Exception e) {
            log.error("Error loading profile form", e);
            // Always provide a profileDto even on error to prevent Thymeleaf parsing errors
            AgentRegistrationDto profileDto = AgentRegistrationDto.builder()
                    .name("")
                    .email("")
                    .phone("")
                    .licenseNumber("")
                    .bio("")
                    .experienceYears(0)
                    .specializations("")
                    .build();
            modelAndView.addObject("profileDto", profileDto);
            modelAndView.addObject("error", "Error loading profile: " + e.getMessage());
        }

        return modelAndView;
    }

    /**
     * Process profile update
     */
    @PostMapping("/profile/edit")
    public ModelAndView updateProfile(@ModelAttribute AgentRegistrationDto profileDto,
                              BindingResult bindingResult,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        log.info("Processing profile update");

        // Validate only non-password fields for profile update
        // Password fields are optional during profile update
        if (profileDto.getName() == null || profileDto.getName().trim().isEmpty()) {
            bindingResult.rejectValue("name", "error.name", "Name is required");
        }
        if (profileDto.getLicenseNumber() == null || profileDto.getLicenseNumber().trim().isEmpty()) {
            bindingResult.rejectValue("licenseNumber", "error.licenseNumber", "License number is required");
        }

        if (bindingResult.hasErrors()) {
            log.warn("Profile validation errors: {}", bindingResult.getAllErrors());
            ModelAndView modelAndView = new ModelAndView("agent/edit-profile");
            modelAndView.addObject("profileDto", profileDto);
            modelAndView.addObject("errorMessage", "Please correct the errors below");
            return modelAndView;
        }

        try {
            // Get current agent using helper method
            Agent agent = getCurrentAgent(authentication);

            agentRegistrationService.updateAgentProfile(agent.getId(), profileDto);
            
            log.info("Profile updated successfully");
            
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Profile updated successfully!");
            
            return new ModelAndView("redirect:/agent/dashboard");

        } catch (Exception e) {
            log.error("Error updating profile", e);
            ModelAndView modelAndView = new ModelAndView("agent/edit-profile");
            modelAndView.addObject("profileDto", profileDto);
            modelAndView.addObject("errorMessage", "Failed to update profile: " + e.getMessage());
            return modelAndView;
        }
    }


    /**
     * Show inquiries for agent's properties
     */
    @GetMapping("/inquiries")
    public ModelAndView showInquiries(Authentication authentication) {
        log.debug("Showing inquiries for agent");
        ModelAndView modelAndView = new ModelAndView("agent/inquiries");

        try {
            // Get current agent using helper method
            Agent agent = getCurrentAgent(authentication);

            // Get agent's properties (with fallback handling)
            List<PropertyDto> agentProperties = propertyUtilityService.getPropertiesByAgent(agent.getId());
            List<UUID> propertyIds = agentProperties.stream()
                    .map(PropertyDto::getId)
                    .toList();

            // Get inquiries for agent's properties
            List<Inquiry> inquiries = inquiryService.findInquiriesByPropertyIds(propertyIds);

            // Create a map of property ID to property title for easy lookup in template
            Map<UUID, String> propertyTitleMap = agentProperties.stream()
                    .collect(java.util.stream.Collectors.toMap(
                            PropertyDto::getId,
                            PropertyDto::getTitle,
                            (existing, replacement) -> existing
                    ));

            // Enrich inquiries with property information
            modelAndView.addObject("inquiries", inquiries);
            modelAndView.addObject("properties", agentProperties);
            modelAndView.addObject("propertyTitleMap", propertyTitleMap);
            modelAndView.addObject("statuses", InquiryStatus.values());
            modelAndView.addObject("totalInquiries", inquiries.size());
            modelAndView.addObject("newInquiries", inquiries.stream()
                    .filter(i -> i.getStatus() == InquiryStatus.NEW)
                    .count());

        } catch (Exception e) {
            log.error("Error loading inquiries", e);
            modelAndView.addObject("error", "Error loading inquiries: " + e.getMessage());
            // Provide default values to prevent template errors
            modelAndView.addObject("inquiries", List.of());
            modelAndView.addObject("properties", List.of());
            modelAndView.addObject("propertyTitleMap", Map.of());
            modelAndView.addObject("statuses", InquiryStatus.values());
            modelAndView.addObject("totalInquiries", 0);
            modelAndView.addObject("newInquiries", 0);
        }

        return modelAndView;
    }

    /**
     * Show inquiry detail
     */
    @GetMapping("/inquiries/{id}")
    public ModelAndView showInquiryDetail(@PathVariable UUID id, Authentication authentication) {
        log.debug("Showing inquiry detail: {}", id);
        ModelAndView modelAndView = new ModelAndView("agent/inquiry-detail");

        try {
            // Get current agent
            // Get current agent using helper method
            Agent agent = getCurrentAgent(authentication);

            // Get inquiry
            Inquiry inquiry = inquiryService.findInquiryById(id)
                    .orElseThrow(() -> new InquiryNotFoundException(id));

            // Verify inquiry belongs to agent's property using our helper method
            if (verifyPropertyBelongsToAgent(agent.getId(), inquiry.getPropertyId())) {
                modelAndView.addObject("error", "You don't have permission to view this inquiry");
                return modelAndView;
            }

            // Get property details
            PropertyDto property = propertyServiceClient.getPropertyById(inquiry.getPropertyId());

            modelAndView.addObject("inquiry", inquiry);
            modelAndView.addObject("property", property);
            modelAndView.addObject("statuses", InquiryStatus.values());
            modelAndView.addObject("inquiryUpdateDto", InquiryUpdateDto.builder().build());

        } catch (Exception e) {
            log.error("Error loading inquiry detail", e);
            modelAndView.addObject("error", "Error loading inquiry: " + e.getMessage());
        }

        return modelAndView;
    }

    /**
     * Update inquiry status and response
     */
    @PostMapping("/inquiries/{id}/update")
    public String updateInquiry(@PathVariable UUID id,
                                @ModelAttribute InquiryUpdateDto updateDto,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        log.info("Updating inquiry: {}", id);

        try {
            // Get current agent
            // Get current agent using helper method
            Agent agent = getCurrentAgent(authentication);

            // Get inquiry
            Inquiry inquiry = inquiryService.findInquiryById(id)
                    .orElseThrow(() -> new InquiryNotFoundException(id));

            // Verify inquiry belongs to agent's property using our helper method
            if (verifyPropertyBelongsToAgent(agent.getId(), inquiry.getPropertyId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "You don't have permission to update this inquiry");
                return "redirect:/agent/inquiries";
            }

            // Update inquiry
            inquiryService.updateInquiry(id, updateDto.getStatus(), updateDto.getResponse());
            log.info("Inquiry updated successfully: {}", id);

            redirectAttributes.addFlashAttribute("successMessage", "Inquiry updated successfully");
            return "redirect:/agent/inquiries/" + id;

        } catch (Exception e) {
            log.error("Error updating inquiry: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update inquiry: " + e.getMessage());
            return "redirect:/agent/inquiries/" + id;
        }
    }


    @ModelAttribute("cities")
    public List<City> getCities() {
        return cityService.findAllCities();
    }


    @ModelAttribute("propertyTypes")
    public List<PropertyType> getPropertyTypes() {
        return propertyTypeService.findAllPropertyTypes();
    }

    private boolean verifyPropertyBelongsToAgent(UUID agentId, UUID propertyId) {
        // Get all properties owned by this agent
        List<PropertyDto> agentProperties = propertyUtilityService.getPropertiesByAgent(agentId);
        
        // Check if any of the agent's properties match the property ID we're looking for
        return agentProperties.stream()
                .noneMatch(p -> p.getId().equals(propertyId));
    }


    private Agent getCurrentAgent(Authentication authentication) {
        // Get the email from the authentication object
        String email = authentication.getName();
        
        // Find the user in the database using the email
        User user = userService.findUserByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        // Find the agent profile linked to this user
        return agentService.findAgentByUserId(user.getId())
                .orElseThrow(() -> new AgentNotFoundException("Agent profile not found"));
    }

    private ModelAndView createAddPropertyErrorModel(PropertyDto propertyDto, String errorMessage) {
        ModelAndView modelAndView = new ModelAndView("agent/add-property");
        modelAndView.addObject("propertyDto", propertyDto);
        // Cities and propertyTypes are automatically added by @ModelAttribute
        modelAndView.addObject("errorMessage", errorMessage);
        return modelAndView;
    }

    private PropertyDto verifyPropertyOwnership(UUID propertyId, UUID agentId, String action) {
        PropertyDto property = propertyServiceClient.getPropertyById(propertyId);
        if (property == null) {
            throw new PropertyNotFoundException("Property not found");
        }
        
        if (!property.getAgentId().equals(agentId)) {
            throw new ApplicationException(
                "You don't have permission to " + action + " this property");
        }
        
        return property;
    }
}
