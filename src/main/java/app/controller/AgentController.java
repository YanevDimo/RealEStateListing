package app.controller;


import app.client.PropertyServiceClient;
import app.dto.AgentRegistrationDto;
import app.dto.PropertyDto;
import app.dto.PropertyUpdateDto;
import app.entity.Agent;
import app.entity.User;
import app.exception.*;
import app.service.*;
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
    private final CityService cityService;
    private final PropertyTypeService propertyTypeService;
    private final FileUploadService fileUploadService;

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

    /**
     * Process agent registration
     */
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

    /**
     * Show agent dashboard
     */
    @GetMapping("/dashboard")
    public ModelAndView showAgentDashboard(Authentication authentication) {
        log.debug("Showing agent dashboard");
        ModelAndView modelAndView = new ModelAndView("dashboard/agent-dashboard");

        try {
            // Get current user
            String email = authentication.getName();
            User user = userService.findUserByEmail(email)
                    .orElseThrow(() -> new UserNotFoundException("User not found"));

            // Get agent profile
            Agent agent = agentService.findAgentByUserId(user.getId())
                    .orElseThrow(() -> new AgentNotFoundException("Agent profile not found"));

            // Get agent's properties from property-service
            List<PropertyDto> agentProperties = propertyServiceClient.getPropertiesByAgent(agent.getId());

            // Get statistics
            long totalProperties = agentProperties.size();
            long activeProperties = agentProperties.stream()
                    .filter(p -> p.getStatus() != null && p.getStatus().equals("ACTIVE"))
                    .count();

            modelAndView.addObject("agent", agent);
            modelAndView.addObject("properties", agentProperties);
            modelAndView.addObject("totalProperties", totalProperties);
            modelAndView.addObject("activeProperties", activeProperties);
            modelAndView.addObject("totalListings", agent.getTotalListings());
            modelAndView.addObject("rating", agent.getRating());

        } catch (Exception e) {
            log.error("Error loading agent dashboard", e);
            modelAndView.addObject("error", "Error loading dashboard: " + e.getMessage());
        }

        return modelAndView;
    }

    /**
     * Show add property form
     */
    @GetMapping("/properties/add")
    public ModelAndView showAddPropertyForm() {
        log.debug("Showing add property form");
        ModelAndView modelAndView = new ModelAndView("agent/add-property");
        
        modelAndView.addObject("propertyDto", new PropertyDto());
        modelAndView.addObject("cities", cityService.findAllCities());
        modelAndView.addObject("propertyTypes", propertyTypeService.findAllPropertyTypes());
        
        return modelAndView;
    }

    /**
     * Process add property
     */
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
            modelAndView.addObject("cities", cityService.findAllCities());
            modelAndView.addObject("propertyTypes", propertyTypeService.findAllPropertyTypes());
            return modelAndView;
        }

        try {
            // Get current agent
            String email = authentication.getName();
            User user = userService.findUserByEmail(email)
                    .orElseThrow(() -> new UserNotFoundException("User not found"));
            
            Agent agent = agentService.findAgentByUserId(user.getId())
                    .orElseThrow(() -> new AgentNotFoundException("Agent profile not found"));

            // Upload images if provided
            List<String> imageUrls = new ArrayList<>();
            if (propertyDto.getImages() != null && !propertyDto.getImages().isEmpty()) {
                try {
                    imageUrls = fileUploadService.uploadFiles(propertyDto.getImages());
                    log.info("Uploaded {} images for property", imageUrls.size());
                } catch (Exception e) {
                    log.error("Error uploading images", e);
                    ModelAndView errorModelAndView = new ModelAndView("agent/add-property");
                    errorModelAndView.addObject("propertyDto", propertyDto);
                    errorModelAndView.addObject("cities", cityService.findAllCities());
                    errorModelAndView.addObject("propertyTypes", propertyTypeService.findAllPropertyTypes());
                    errorModelAndView.addObject("errorMessage", "Failed to upload images: " + e.getMessage());
                    return errorModelAndView;
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
                    throw new RuntimeException("Property Service failed to create property. Received null or invalid response.");
                }
                
                log.info("Property created successfully via Property Service with ID: {} and {} images", 
                        property.getId(), imageUrls.size());
                
                redirectAttributes.addFlashAttribute("successMessage", 
                        "Property added successfully! Property ID: " + property.getId());
                return new ModelAndView("redirect:/agent/dashboard");
            } catch (Exception e) {
                log.error("Error creating property via Property Service: {}", e.getMessage(), e);
                throw e; // Re-throw to be caught by outer catch block
            }

        } catch (Exception e) {
            log.error("Error adding property", e);
            ModelAndView errorModelAndView = new ModelAndView("agent/add-property");
            errorModelAndView.addObject("propertyDto", propertyDto);
            errorModelAndView.addObject("cities", cityService.findAllCities());
            errorModelAndView.addObject("propertyTypes", propertyTypeService.findAllPropertyTypes());
            errorModelAndView.addObject("errorMessage", "Failed to add property: " + e.getMessage());
            return errorModelAndView;
        }
    }

    /**
     * Show edit property form
     */
    @GetMapping("/properties/edit/{id}")
    public ModelAndView showEditPropertyForm(@PathVariable UUID id, Authentication authentication) {
        log.debug("Showing edit property form for ID: {}", id);
        ModelAndView modelAndView = new ModelAndView("agent/edit-property");

        try {
            // Verify agent owns this property
            String email = authentication.getName();
            User user = userService.findUserByEmail(email)
                    .orElseThrow(() -> new UserNotFoundException("User not found"));
            
            Agent agent = agentService.findAgentByUserId(user.getId())
                    .orElseThrow(() -> new AgentNotFoundException("Agent profile not found"));

            PropertyDto property = propertyServiceClient.getPropertyById(id);
            if (property == null) {
                throw new PropertyNotFoundException("Property not found");
            }

            // Check if agent owns this property
            if (!property.getAgentId().equals(agent.getId())) {
                throw new ApplicationException("You don't have permission to edit this property");
            }

            // PropertyDto already has all the data we need
            PropertyDto propertyDto = property;

            modelAndView.addObject("propertyDto", propertyDto);
            modelAndView.addObject("propertyId", id);
            modelAndView.addObject("cities", cityService.findAllCities());
            modelAndView.addObject("propertyTypes", propertyTypeService.findAllPropertyTypes());

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
            // Verify agent owns this property
            String email = authentication.getName();
            User user = userService.findUserByEmail(email)
                    .orElseThrow(() -> new UserNotFoundException("User not found"));
            
            Agent agent = agentService.findAgentByUserId(user.getId())
                    .orElseThrow(() -> new AgentNotFoundException("Agent profile not found"));

            PropertyDto property = propertyServiceClient.getPropertyById(id);
            if (property == null) {
                throw new PropertyNotFoundException("Property not found");
            }

            // Check if agent owns this property
            if (!property.getAgentId().equals(agent.getId())) {
                throw new ApplicationException("You don't have permission to edit this property");
            }

            // Convert PropertyDto to PropertyUpdateDto
            PropertyUpdateDto updateDto = PropertyUpdateDto.builder()
                    .title(propertyDto.getTitle())
                    .description(propertyDto.getDescription())
                    .price(propertyDto.getPrice())
                    .agentId(property.getAgentId()) // Keep same agent
                    .cityId(propertyDto.getCityId())
                    .propertyTypeId(propertyDto.getPropertyTypeId())
                    .status(propertyDto.getStatus() != null ? propertyDto.getStatus() : property.getStatus())
                    .bedrooms(propertyDto.getBedrooms() != null ? propertyDto.getBedrooms() : 
                             (propertyDto.getBeds() != null ? propertyDto.getBeds() : property.getBedrooms()))
                    .bathrooms(propertyDto.getBathrooms() != null ? propertyDto.getBathrooms() : 
                              (propertyDto.getBaths() != null ? propertyDto.getBaths() : property.getBathrooms()))
                    .squareFeet(propertyDto.getSquareFeet() != null ? propertyDto.getSquareFeet() : 
                               (propertyDto.getAreaSqm() != null ? propertyDto.getAreaSqm().intValue() : property.getSquareFeet()))
                    .address(propertyDto.getAddress())
                    .features(propertyDto.getFeatures())
                    .build();

            // Update property via property-service
            propertyServiceClient.updateProperty(id, updateDto);
            
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
            // Verify agent owns this property
            String email = authentication.getName();
            User user = userService.findUserByEmail(email)
                    .orElseThrow(() -> new UserNotFoundException("User not found"));
            
            Agent agent = agentService.findAgentByUserId(user.getId())
                    .orElseThrow(() -> new AgentNotFoundException("Agent profile not found"));

            PropertyDto property = propertyServiceClient.getPropertyById(id);
            if (property == null) {
                throw new PropertyNotFoundException("Property not found");
            }

            // Check if agent owns this property
            if (!property.getAgentId().equals(agent.getId())) {
                throw new ApplicationException("You don't have permission to delete this property");
            }

            // Delete property via property-service
            propertyServiceClient.deleteProperty(id);
            
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
            String email = authentication.getName();
            User user = userService.findUserByEmail(email)
                    .orElseThrow(() -> new UserNotFoundException("User not found"));
            
            Agent agent = agentService.findAgentByUserId(user.getId())
                    .orElseThrow(() -> new AgentNotFoundException("Agent profile not found"));

            AgentRegistrationDto profileDto = AgentRegistrationDto.builder()
                    .name(user.getName())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .licenseNumber(agent.getLicenseNumber())
                    .bio(agent.getBio())
                    .experienceYears(agent.getExperienceYears())
                    .specializations(parseSpecializationsFromJson(agent.getSpecializations()))
                    .build();

            modelAndView.addObject("profileDto", profileDto);

        } catch (Exception e) {
            log.error("Error loading profile form", e);
            modelAndView.addObject("error", "Error loading profile: " + e.getMessage());
        }

        return modelAndView;
    }

    /**
     * Process profile update
     */
    @PostMapping("/profile/edit")
    public String updateProfile(@Valid @ModelAttribute AgentRegistrationDto profileDto,
                              BindingResult bindingResult,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        log.info("Processing profile update");

        if (bindingResult.hasErrors()) {
            log.warn("Profile validation errors");
            return "agent/edit-profile";
        }

        try {
            String email = authentication.getName();
            User user = userService.findUserByEmail(email)
                    .orElseThrow(() -> new UserNotFoundException("User not found"));
            
            Agent agent = agentService.findAgentByUserId(user.getId())
                    .orElseThrow(() -> new AgentNotFoundException("Agent profile not found"));

            agentRegistrationService.updateAgentProfile(agent.getId(), profileDto);
            
            log.info("Profile updated successfully");
            
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Profile updated successfully!");

        } catch (Exception e) {
            log.error("Error updating profile", e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                    "Failed to update profile: " + e.getMessage());
        }

        return "redirect:/agent/dashboard";
    }

    /**
     * Parse specializations JSON array back to comma-separated string
     */
    private String parseSpecializationsFromJson(String specializationsJson) {
        if (specializationsJson == null || specializationsJson.trim().isEmpty() || specializationsJson.equals("[]")) {
            return "";
        }
        
        try {
            // Remove brackets and quotes, then split by comma
            String cleaned = specializationsJson.replaceAll("[\\[\\]\"]", "");
            return cleaned.replaceAll(",\\s*", ", ");
        } catch (Exception e) {
            log.warn("Error parsing specializations JSON: {}", specializationsJson, e);
            return specializationsJson; // Return as-is if parsing fails
        }
    }
}
