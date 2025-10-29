package app.repository;


import app.entity.Inquiry;
import app.entity.InquiryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InquiryRepository extends JpaRepository<Inquiry, UUID> {

    List<Inquiry> findByUserId(UUID userId);
    List<Inquiry> findByPropertyId(UUID propertyId);
    List<Inquiry> findByStatus(InquiryStatus status);
}




