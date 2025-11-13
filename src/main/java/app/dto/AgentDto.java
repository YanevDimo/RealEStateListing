package app.dto;

import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentDto {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    
    // Legacy fields for internal use (not sent to property-service)
   @Transient
    private UUID userId;
    @Transient
    private String licenseNumber;
    @Transient
    private String bio;
    @Transient
    private Integer experienceYears;
    @Transient
    private String specializations;
    @Transient
    private BigDecimal rating;
    @Transient
    private Integer totalListings;
    @Transient
    private String profilePictureUrl;
}







