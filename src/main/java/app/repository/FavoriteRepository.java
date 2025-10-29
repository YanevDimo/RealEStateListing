package app.repository;


import app.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, UUID> {
    

    Optional<Favorite> findByUserIdAndPropertyId(UUID userId, UUID propertyId);
    boolean existsByUserIdAndPropertyId(UUID userId, UUID propertyId);
    List<Favorite> findByUserId(UUID userId);
    List<Favorite> findByPropertyId(UUID propertyId);
}




