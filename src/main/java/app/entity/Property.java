package app.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "properties")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"images", "features", "inquiries", "favorites"})
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_type_id", nullable = false)
    private PropertyType propertyType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    @Column(columnDefinition = "TEXT")
    private String address;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal price;

    @Min(0)
    @Column(nullable = false)
    @Builder.Default
    private Integer beds = 0;

    @Min(0)
    @Column(nullable = false)
    @Builder.Default
    private Integer baths = 0;

    @DecimalMin(value = "0.0", inclusive = false)
    @Column(name = "area_sqm", precision = 10, scale = 2)
    private BigDecimal areaSqm;

    @Min(1800)
    @Max(2030)
    @Column(name = "year_built")
    private Integer yearBuilt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private Agent agent;

    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PropertyStatus status = PropertyStatus.ACTIVE;

    @Column(nullable = false)
    @Builder.Default
    private Boolean featured = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationships
    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PropertyImage> images;

    /**
     * Set images list (needed for template population)
     */
    public void setImages(List<PropertyImage> images) {
        this.images = images;
    }

    /**
     * Get the primary image URL for display purposes
     */
    public String getPrimaryImageUrl() {
        if (images == null || images.isEmpty()) {
            return "https://cdn.pixabay.com/photo/2018/02/13/11/09/home-3150500_1280.jpg"; // Default image
        }
        
        // Find primary image first
        return images.stream()
                .filter(img -> img.getIsPrimary() != null && img.getIsPrimary())
                .findFirst()
                .map(PropertyImage::getImageUrl)
                .orElse(images.get(0).getImageUrl()); // Fallback to first image
    }

    @ManyToMany
    @JoinTable(
        name = "property_feature_assignments",
        joinColumns = @JoinColumn(name = "property_id"),
        inverseJoinColumns = @JoinColumn(name = "feature_id")
    )
    private List<PropertyFeature> features;

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Inquiry> inquiries;

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Favorite> favorites;

}