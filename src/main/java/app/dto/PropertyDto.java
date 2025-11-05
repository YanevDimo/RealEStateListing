package app.dto;

import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * PropertyDto for receiving data from property-service microservice.
 * Compatible with property-service PropertyDto structure.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyDto {

    private UUID id;
    private String title;
    private String description;
    private BigDecimal price;
    private UUID agentId;
    private UUID cityId;
    private UUID propertyTypeId;
    private String status; // PropertyStatus as String (enum from property-service)
    private Integer bedrooms;
    private Integer bathrooms;
    private Integer squareFeet;
    private String address;
    private Boolean isFeatured;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<String> imageUrls;
    private List<String> features;
    
    // Legacy fields for template compatibility and form submission
    @Transient
    private List<MultipartFile> images; // For form uploads only
    
    @Transient
    private Integer yearBuilt; // Optional field for form display (not sent to Property Service)
    
    // Helper methods for template compatibility (getters and setters for form binding)
    public Integer getBeds() {
        return bedrooms;
    }
    
    public void setBeds(Integer beds) {
        this.bedrooms = beds;
    }
    
    public Integer getBaths() {
        return bathrooms;
    }
    
    public void setBaths(Integer baths) {
        this.bathrooms = baths;
    }
    
    public BigDecimal getAreaSqm() {
        return squareFeet != null ? new BigDecimal(squareFeet) : null;
    }
    
    public void setAreaSqm(BigDecimal areaSqm) {
        this.squareFeet = areaSqm != null ? areaSqm.intValue() : null;
    }
    
    public Boolean getFeatured() {
        return isFeatured;
    }
    
    public void setFeatured(Boolean featured) {
        this.isFeatured = featured;
    }
    
    // Helper methods for template display
    public String getPrimaryImageUrl() {
        if (imageUrls != null && !imageUrls.isEmpty()) {
            String firstImage = imageUrls.get(0);
            if (firstImage != null && !firstImage.trim().isEmpty()) {
                return firstImage;
            }
        }
        // Use a better placeholder image from Unsplash (real property image)
        return "https://images.unsplash.com/photo-1568605114967-8130f3a94e52?w=400&h=280&fit=crop&q=80";
    }
    
    // Transient fields for template enrichment
    @Transient
    private String cityName; // Enriched from cityId
    
    @Transient
    private String agentName; // Enriched from agentId
    
    @Transient
    private String agentEmail; // Enriched from agentId
    
    @Transient
    private String agentProfilePictureUrl; // Enriched from agentId
    
    @Transient
    private java.math.BigDecimal agentRating; // Enriched from agentId
    
    @Transient
    private Integer agentTotalListings; // Enriched from agentId
}