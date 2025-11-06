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
public class CityDto {
    private UUID id;
    private String name;
    private String state; // For property-service compatibility
    private String country;
    
    // Legacy fields for internal use (not sent to property-service)
    @Transient
    private BigDecimal latitude;
    @Transient
    private BigDecimal longitude;
}





