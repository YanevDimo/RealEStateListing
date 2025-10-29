package app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRegistrationDto {

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

    @Builder.Default
    private String role = "USER";
    public boolean isPasswordMatching() {
        return password != null && password.equals(confirmPassword);
    }
}
