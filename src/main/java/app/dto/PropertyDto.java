package app.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyDto {

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @NotNull(message = "Property type is required")
    private UUID propertyTypeId;

    @NotNull(message = "City is required")
    private UUID cityId;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be 0 or greater")
    private BigDecimal price;

    @Min(value = 0, message = "Number of beds must be 0 or greater")
    private Integer beds;

    @Min(value = 0, message = "Number of baths must be 0 or greater")
    private Integer baths;

    private BigDecimal areaSqm;

    private Integer yearBuilt;

    private BigDecimal latitude;
    private BigDecimal longitude;

    @Builder.Default
    private Boolean featured = false;

    // Image upload fields
    private List<MultipartFile> images;
    private List<String> imageUrls;
}