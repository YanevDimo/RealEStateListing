package app.controller;


import app.dto.UserLoginDto;
import app.dto.UserRegistrationDto;
import app.entity.User;
import app.entity.UserRole;
import app.exception.InvalidRoleException;
import app.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;


    @GetMapping("/login")
    public ModelAndView showLoginPage() {
        log.debug("Showing login page");
        ModelAndView modelAndView = new ModelAndView("auth/login");
        modelAndView.addObject("userLoginDto", new UserLoginDto());
        return modelAndView;
    }


    @GetMapping("/register")
    public ModelAndView showRegisterPage() {
        log.debug("Showing registration page");
        ModelAndView modelAndView = new ModelAndView("auth/register");
        modelAndView.addObject("userRegistrationDto", new UserRegistrationDto());
        return modelAndView;
    }


    @PostMapping("/register")
    public String registerUser(@Valid UserRegistrationDto registrationDto,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes) {
        log.debug("Processing user registration for email: {}", registrationDto.getEmail());

        if (!registrationDto.isPasswordMatching()) {
            bindingResult.rejectValue("confirmPassword", "error.confirmPassword", "Passwords do not match");
        }

        if (userService.userExistsByEmail(registrationDto.getEmail())) {
            bindingResult.rejectValue("email", "error.email", "Email already exists");
        }

        if (bindingResult.hasErrors()) {
            log.warn("Registration validation errors for email: {}", registrationDto.getEmail());
            return "auth/register";
        }

        try {
            // Validate role
            UserRole role;
            try {
                String roleStr = registrationDto.getRole();
                if (roleStr == null || roleStr.trim().isEmpty()) {
                    throw new InvalidRoleException("Role cannot be empty");
                }
                role = UserRole.valueOf(roleStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                bindingResult.rejectValue("role", "error.role", "Invalid role. Valid roles are: USER, AGENT, ADMIN");
                return "auth/register";
            }

            // Create new user
            User newUser = User.builder()
                    .name(registrationDto.getName())
                    .email(registrationDto.getEmail())
                    .passwordHash(passwordEncoder.encode(registrationDto.getPassword()))
                    .phone(registrationDto.getPhone())
                    .role(role)
                    .isActive(true)
                    .build();

            User savedUser = userService.saveUser(newUser);
            log.info("User registered successfully with ID: {}", savedUser.getId());

            redirectAttributes.addFlashAttribute("successMessage", 
                    "Registration successful! Please login with your credentials.");
            return "redirect:/auth/login";

        } catch (Exception e) {
            log.error("Error during user registration", e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                    "Registration failed. Please try again.");
            return "redirect:/auth/register";
        }
    }

}
