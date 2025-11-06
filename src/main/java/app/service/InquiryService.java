package app.service;

import app.dto.InquiryDto;
import app.entity.Inquiry;
import app.entity.InquiryStatus;
import app.entity.User;
import app.repository.InquiryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final UserService userService;

    @Transactional
    public Inquiry createInquiry(InquiryDto inquiryDto, UUID propertyId, Authentication authentication) {
        log.info("Creating inquiry for property: {}", propertyId);
        
        User user = null;
        if (authentication != null && authentication.isAuthenticated()) {
            try {
                Optional<User> optionalUser = userService.findUserByEmail(authentication.getName());
                user = optionalUser.orElse(null);
            } catch (Exception e) {
                log.debug("User not found or not authenticated, creating inquiry without user association");
            }
        }

        Inquiry inquiry = Inquiry.builder()
                .propertyId(propertyId)
                .user(user)
                .contactName(inquiryDto.getContactName())
                .contactEmail(inquiryDto.getContactEmail())
                .contactPhone(inquiryDto.getContactPhone())
                .message(inquiryDto.getMessage())
                .status(InquiryStatus.NEW)
                .build();

        Inquiry savedInquiry = inquiryRepository.save(inquiry);
        log.info("Inquiry created successfully with ID: {}", savedInquiry.getId());
        return savedInquiry;
    }

    public List<Inquiry> findInquiriesByPropertyId(UUID propertyId) {
        log.debug("Finding inquiries for property: {}", propertyId);
        return inquiryRepository.findByPropertyId(propertyId);
    }

    public List<Inquiry> findInquiriesByUserId(UUID userId) {
        log.debug("Finding inquiries for user: {}", userId);
        return inquiryRepository.findByUserId(userId);
    }

    public List<Inquiry> findInquiriesByStatus(InquiryStatus status) {
        log.debug("Finding inquiries with status: {}", status);
        return inquiryRepository.findByStatus(status);
    }

    public Optional<Inquiry> findInquiryById(UUID id) {
        log.debug("Finding inquiry by ID: {}", id);
        return inquiryRepository.findById(id);
    }

    @Transactional
    public Inquiry updateInquiryStatus(UUID id, InquiryStatus status) {
        log.info("Updating inquiry {} status to {}", id, status);
        Inquiry inquiry = inquiryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inquiry not found with ID: " + id));
        inquiry.setStatus(status);
        return inquiryRepository.save(inquiry);
    }

    @Transactional
    public Inquiry addResponse(UUID id, String response) {
        log.info("Adding response to inquiry: {}", id);
        Inquiry inquiry = inquiryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inquiry not found with ID: " + id));
        inquiry.setResponse(response);
        if (inquiry.getStatus() == InquiryStatus.NEW) {
            inquiry.setStatus(InquiryStatus.CONTACTED);
        }
        return inquiryRepository.save(inquiry);
    }

    public long countInquiriesByPropertyId(UUID propertyId) {
        log.debug("Counting inquiries for property: {}", propertyId);
        return inquiryRepository.findByPropertyId(propertyId).size();
    }

    public List<Inquiry> findAllInquiries() {
        log.debug("Finding all inquiries");
        return inquiryRepository.findAll();
    }

    public List<Inquiry> findInquiriesByPropertyIds(List<UUID> propertyIds) {
        log.debug("Finding inquiries for {} properties", propertyIds.size());
        return inquiryRepository.findAll().stream()
                .filter(inquiry -> propertyIds.contains(inquiry.getPropertyId()))
                .toList();
    }

    @Transactional
    public Inquiry updateInquiry(UUID id, InquiryStatus status, String response) {
        log.info("Updating inquiry {} with status: {} and response", id, status);
        Inquiry inquiry = inquiryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inquiry not found with ID: " + id));
        
        if (status != null) {
            inquiry.setStatus(status);
        }
        if (response != null && !response.trim().isEmpty()) {
            inquiry.setResponse(response);
            if (inquiry.getStatus() == InquiryStatus.NEW) {
                inquiry.setStatus(InquiryStatus.CONTACTED);
            }
        }
        
        return inquiryRepository.save(inquiry);
    }
}

