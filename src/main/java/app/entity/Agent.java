package app.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "agents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"user"})
public class Agent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Size(max = 100)
    @Column(name = "license_number")
    private String licenseNumber;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "experience_years")
    @Builder.Default
    private Integer experienceYears = 0;

    @Column(columnDefinition = "JSON")
    private String specializations; // JSON array of specializations

    @Column(precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal rating = BigDecimal.ZERO;

    @Column(name = "total_listings")
    @Builder.Default
    private Integer totalListings = 0;

    @Size(max = 500)
    @Column(name = "profile_picture_url")
    private String profilePictureUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Note: Properties are now managed in property-service microservice
    // Properties are referenced via UUID (agentId) in property-service

    // Helper methods
    public String getAgentName() {
        return user != null ? user.getName() : null;
    }

    public String getAgentEmail() {
        return user != null ? user.getEmail() : null;
    }

    public String getAgentPhone() {
        return user != null ? user.getPhone() : null;
    }

    /**
     * Get specializations as a readable string
     */
    public String getSpecializationsAsString() {
        if (specializations == null || specializations.trim().isEmpty() || specializations.equals("[]")) {
            return "";
        }
        
        try {
            // Remove brackets and quotes, then split by comma
            String cleaned = specializations.replaceAll("[\\[\\]\"]", "");
            return cleaned.replaceAll(",\\s*", ", ");
        } catch (Exception e) {
            return specializations; // Return as-is if parsing fails
        }
    }

}