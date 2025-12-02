package app.service;

import app.client.PropertyServiceClient;
import app.dto.InquiryDto;
import app.dto.PropertyDto;
import app.entity.Agent;
import app.entity.Inquiry;
import app.entity.InquiryStatus;
import app.entity.User;
import app.event.InquiryCreatedEvent;
import app.exception.InquiryNotFoundException;
import app.repository.InquiryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
    private final PropertyServiceClient propertyServiceClient;
    private final AgentService agentService;
    private final ApplicationEventPublisher eventPublisher;

    private Inquiry findInquiryByIdOrThrow(UUID id) {
        return inquiryRepository.findById(id)
                .orElseThrow(() -> new InquiryNotFoundException(id));
    }

    @Transactional
    public void createInquiry(InquiryDto inquiryDto, UUID propertyId, Authentication authentication) {
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
        
        try {
            PropertyDto property = propertyServiceClient.getPropertyById(propertyId);
            String propertyTitle = property != null ? property.getTitle() : "Unknown Property";
            UUID agentId = property != null ? property.getAgentId() : null;
            String agentEmail = "unknown@example.com";
            
            if (agentId != null) {
                try {
                    Optional<Agent> agent = agentService.findAgentById(agentId);
                    if (agent.isPresent() && agent.get().getUser() != null) {
                        agentEmail = agent.get().getUser().getEmail();
                    }
                } catch (Exception e) {
                    log.warn("Could not fetch agent email for agent ID: {}", agentId, e);
                }
            }
            
            eventPublisher.publishEvent(new InquiryCreatedEvent(
                this,
                savedInquiry,
                propertyId,
                propertyTitle,
                agentId,
                agentEmail
            ));
            log.info("📢 Published InquiryCreatedEvent for inquiry: {}", savedInquiry.getId());
        } catch (Exception e) {
            log.error("Error publishing InquiryCreatedEvent for inquiry: {}", savedInquiry.getId(), e);
        }
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
        Inquiry inquiry = findInquiryByIdOrThrow(id);
        inquiry.setStatus(status);
        return inquiryRepository.save(inquiry);
    }

    @Transactional
    public Inquiry addResponse(UUID id, String response) {
        log.info("Adding response to inquiry: {}", id);
        Inquiry inquiry = findInquiryByIdOrThrow(id);
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
    public void updateInquiry(UUID id, InquiryStatus status, String response) {
        log.info("Updating inquiry {} with status: {} and response", id, status);
        Inquiry inquiry = findInquiryByIdOrThrow(id);
        
        if (status != null) {
            inquiry.setStatus(status);
        }
        if (response != null && !response.trim().isEmpty()) {
            inquiry.setResponse(response);
            if (inquiry.getStatus() == InquiryStatus.NEW) {
                inquiry.setStatus(InquiryStatus.CONTACTED);
            }
        }

        inquiryRepository.save(inquiry);
    }
}

