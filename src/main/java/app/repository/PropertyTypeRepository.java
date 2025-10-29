package app.repository;


import app.entity.PropertyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PropertyTypeRepository extends JpaRepository<PropertyType, UUID> {
    

    Optional<PropertyType> findByName(String name);
    boolean existsByName(String name);
}