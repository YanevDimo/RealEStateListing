package app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InquiryDto {

    private UUID propertyId;

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String contactName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String contactEmail;

    @Size(max = 50, message = "Phone number must not exceed 50 characters")
    private String contactPhone;

    @NotBlank(message = "Message is required")
    @Size(max = 2000, message = "Message must not exceed 2000 characters")
    private String message;
}

