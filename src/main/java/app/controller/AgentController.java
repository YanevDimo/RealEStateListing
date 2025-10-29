package app.controller;


import app.dto.AgentRegistrationDto;
import app.dto.PropertyDto;
import app.entity.Agent;
import app.entity.Property;
import app.entity.PropertyImage;
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
    private final PropertyService propertyService;
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
        ModelAndView modelAndView = new ModelAndView("dashboard/agent-dashboard-basic");

        try {
            // Get current user
            String email = authentication.getName();
            User user = userService.findUserByEmail(email)
                    .orElseThrow(() -> new UserNotFoundException("User not found"));

            // Get agent profile
            Agent agent = agentService.findAgentByUserId(user.getId())
                    .orElseThrow(() -> new AgentNotFoundException("Agent profile not found"));

            // Get agent's properties
            List<Property> agentProperties = propertyService.findPropertiesByAgent(agent.getId());

            // Get statistics
            long totalProperties = agentProperties.size();
            long activeProperties = agentProperties.stream()
                    .filter(p -> p.getStatus().name().equals("ACTIVE"))
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

            // Create property
            Property property = agentRegistrationService.createPropertyForAgent(agent.getId(), propertyDto);
            
            // Add property images
            if (!imageUrls.isEmpty()) {
                for (int i = 0; i < imageUrls.size(); i++) {
                    PropertyImage propertyImage = PropertyImage.builder()
                            .property(property)
                            .imageUrl(imageUrls.get(i))
                            .isPrimary(i == 0) // First image is primary
                            .sortOrder(i)
                            .build();
                    propertyService.savePropertyImage(propertyImage);
                }
            }
            
            log.info("Property created successfully with ID: {} and {} images", 
                    property.getId(), imageUrls.size());
            
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Property added successfully with " + imageUrls.size() + " images!");
            return new ModelAndView("redirect:/agent/dashboard");

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

            Property property = propertyService.findPropertyById(id)
                    .orElseThrow(() -> new PropertyNotFoundException("Property not found"));

            // Check if agent owns this property
            if (!property.getAgent().getId().equals(agent.getId())) {
                throw new ApplicationException("You don't have permission to edit this property");
            }

            // Convert to DTO
            PropertyDto propertyDto = PropertyDto.builder()
                    .title(property.getTitle())
                    .description(property.getDescription())
                    .propertyTypeId(property.getPropertyType().getId())
                    .cityId(property.getCity().getId())
                    .address(property.getAddress())
                    .price(property.getPrice())
                    .beds(property.getBeds())
                    .baths(property.getBaths())
                    .areaSqm(property.getAreaSqm())
                    .yearBuilt(property.getYearBuilt())
                    .latitude(property.getLatitude())
                    .longitude(property.getLongitude())
                    .featured(property.getFeatured())
                    .build();

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

            Property property = propertyService.findPropertyById(id)
                    .orElseThrow(() -> new PropertyNotFoundException("Property not found"));

            // Check if agent owns this property
            if (!property.getAgent().getId().equals(agent.getId())) {
                throw new ApplicationException("You don't have permission to edit this property");
            }

            // Update property
            property.setTitle(propertyDto.getTitle());
            property.setDescription(propertyDto.getDescription());
            property.setAddress(propertyDto.getAddress());
            property.setPrice(propertyDto.getPrice());
            property.setBeds(propertyDto.getBeds() != null ? propertyDto.getBeds() : 0);
            property.setBaths(propertyDto.getBaths() != null ? propertyDto.getBaths() : 0);
            property.setAreaSqm(propertyDto.getAreaSqm());
            property.setYearBuilt(propertyDto.getYearBuilt());
            property.setLatitude(propertyDto.getLatitude());
            property.setLongitude(propertyDto.getLongitude());
            property.setFeatured(propertyDto.getFeatured() != null ? propertyDto.getFeatured() : false);

            // Update property type and city if changed
            if (!property.getPropertyType().getId().equals(propertyDto.getPropertyTypeId())) {
                property.setPropertyType(propertyTypeService.findPropertyTypeById(propertyDto.getPropertyTypeId())
                        .orElseThrow(() -> new PropertyTypeNotFoundException("Property type not found")));
            }

            if (!property.getCity().getId().equals(propertyDto.getCityId())) {
                property.setCity(cityService.findCityById(propertyDto.getCityId())
                        .orElseThrow(() -> new CityNotFoundException("City not found")));
            }

            propertyService.updateProperty(property);
            
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

            Property property = propertyService.findPropertyById(id)
                    .orElseThrow(() -> new PropertyNotFoundException("Property not found"));

            // Check if agent owns this property
            if (!property.getAgent().getId().equals(agent.getId())) {
                throw new ApplicationException("You don't have permission to delete this property");
            }

            // Delete property
            propertyService.deleteProperty(id);
            
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
