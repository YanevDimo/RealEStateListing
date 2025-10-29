package app.repository;


import app.entity.Property;
import app.entity.PropertyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PropertyRepository extends JpaRepository<Property, UUID> {
    

    List<Property> findByStatus(PropertyStatus status);
    List<Property> findByFeaturedTrueAndStatus(PropertyStatus status);
    List<Property> findByCityIdAndStatus(UUID cityId, PropertyStatus status);
    List<Property> findByPropertyTypeIdAndStatus(UUID propertyTypeId, PropertyStatus status);
    List<Property> findByAgentIdAndStatus(UUID agentId, PropertyStatus status);
    
    long countByStatus(PropertyStatus status);
    
    @Query("SELECT AVG(p.price) FROM Property p WHERE p.price IS NOT NULL")
    Optional<Double> findAveragePrice();
}