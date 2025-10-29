package app.repository;


import app.entity.PropertyImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PropertyImageRepository extends JpaRepository<PropertyImage, UUID> {
    
    List<PropertyImage> findByPropertyId(UUID propertyId);
    
    List<PropertyImage> findByPropertyIdOrderBySortOrder(UUID propertyId);
    
    PropertyImage findByPropertyIdAndIsPrimaryTrue(UUID propertyId);
    
    void deleteByPropertyId(UUID propertyId);
}











