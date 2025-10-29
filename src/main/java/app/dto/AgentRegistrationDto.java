package app.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentRegistrationDto {

    // User fields
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String password;

    @NotBlank(message = "Password confirmation is required")
    @Size(min = 6, max = 100, message = "Password confirmation must be between 6 and 100 characters")
    private String confirmPassword;

    @Size(max = 50, message = "Phone number must not exceed 50 characters")
    private String phone;

    // Agent-specific fields
    @NotBlank(message = "License number is required")
    @Size(max = 100, message = "License number must not exceed 100 characters")
    private String licenseNumber;

    @Size(max = 1000, message = "Bio must not exceed 1000 characters")
    private String bio;

    @Min(value = 0, message = "Experience years must be 0 or greater")
    @Max(value = 50, message = "Experience years must be 50 or less")
    private Integer experienceYears;

    @Size(max = 500, message = "Specializations must not exceed 500 characters")
    private String specializations;

    // Profile picture
    private MultipartFile profilePicture;

    // Helper method
    public boolean isPasswordMatching() {
        return password != null && password.equals(confirmPassword);
    }
}










