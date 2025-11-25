package app.service;

import app.entity.Inquiry;
import app.entity.InquiryStatus;
import app.entity.User;
import app.entity.UserRole;
import app.repository.InquiryRepository;
import app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("h2")
class InquiryServiceIntegrationTest {

    @Autowired
    private InquiryRepository inquiryRepository;

    @Autowired
    private UserRepository userRepository;

    private InquiryService inquiryService;
    private UserService userService;

    private User testUser;
    private UUID testPropertyId;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
        inquiryService = new InquiryService(inquiryRepository, userService);
        
        inquiryRepository.deleteAll();
        userRepository.deleteAll();
        
        testUser = User.builder()
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .name("Test User")
                .role(UserRole.USER)
                .isActive(true)
                .build();
        testUser = userRepository.save(testUser);
        
        testPropertyId = UUID.randomUUID();
    }

    @Test
    void testSaveAndFindInquiry() {
        Inquiry inquiry = Inquiry.builder()
                .propertyId(testPropertyId)
                .user(testUser)
                .contactName("Domi Kirev")
                .contactEmail("domi@example.com")
                .contactPhone("1234567890")
                .message("I'm interested in this property")
                .status(InquiryStatus.NEW)
                .build();

        Inquiry saved = inquiryRepository.save(inquiry);

        assertNotNull(saved.getId());
        assertEquals(testPropertyId, saved.getPropertyId());
        assertEquals("Domi Kirev", saved.getContactName());

        Optional<Inquiry> found = inquiryService.findInquiryById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals("Domi Kirev", found.get().getContactName());
    }

    @Test
    void testFindInquiriesByPropertyId() {
        UUID propertyId1 = UUID.randomUUID();
        UUID propertyId2 = UUID.randomUUID();

        Inquiry inquiry1 = Inquiry.builder()
                .propertyId(propertyId1)
                .user(testUser)
                .contactName("User 1")
                .contactEmail("user1@example.com")
                .message("Message 1")
                .status(InquiryStatus.NEW)
                .build();

        Inquiry inquiry2 = Inquiry.builder()
                .propertyId(propertyId1)
                .user(testUser)
                .contactName("User 2")
                .contactEmail("user2@example.com")
                .message("Message 2")
                .status(InquiryStatus.NEW)
                .build();

        Inquiry inquiry3 = Inquiry.builder()
                .propertyId(propertyId2)
                .user(testUser)
                .contactName("User 3")
                .contactEmail("user3@example.com")
                .message("Message 3")
                .status(InquiryStatus.NEW)
                .build();

        inquiryRepository.save(inquiry1);
        inquiryRepository.save(inquiry2);
        inquiryRepository.save(inquiry3);

        List<Inquiry> inquiries = inquiryService.findInquiriesByPropertyId(propertyId1);

        assertEquals(2, inquiries.size());
        assertTrue(inquiries.stream().allMatch(i -> i.getPropertyId().equals(propertyId1)));
    }

    @Test
    void testFindInquiriesByUserId() {
        User user2 = User.builder()
                .email("user2@example.com")
                .passwordHash("hash")
                .name("User 2")
                .role(UserRole.USER)
                .isActive(true)
                .build();
        user2 = userRepository.save(user2);

        Inquiry inquiry1 = Inquiry.builder()
                .propertyId(testPropertyId)
                .user(testUser)
                .contactName("User 1")
                .contactEmail("user1@example.com")
                .message("Message 1")
                .status(InquiryStatus.NEW)
                .build();

        Inquiry inquiry2 = Inquiry.builder()
                .propertyId(testPropertyId)
                .user(user2)
                .contactName("User 2")
                .contactEmail("user2@example.com")
                .message("Message 2")
                .status(InquiryStatus.NEW)
                .build();

        inquiryRepository.save(inquiry1);
        inquiryRepository.save(inquiry2);

        List<Inquiry> inquiries = inquiryService.findInquiriesByUserId(testUser.getId());

        assertEquals(1, inquiries.size());
        assertEquals(testUser.getId(), inquiries.get(0).getUser().getId());
    }

    @Test
    void testFindInquiriesByStatus() {
        Inquiry inquiry1 = Inquiry.builder()
                .propertyId(testPropertyId)
                .user(testUser)
                .contactName("User 1")
                .contactEmail("user1@example.com")
                .message("Message 1")
                .status(InquiryStatus.NEW)
                .build();

        Inquiry inquiry2 = Inquiry.builder()
                .propertyId(testPropertyId)
                .user(testUser)
                .contactName("User 2")
                .contactEmail("user2@example.com")
                .message("Message 2")
                .status(InquiryStatus.CONTACTED)
                .build();

        Inquiry inquiry3 = Inquiry.builder()
                .propertyId(testPropertyId)
                .user(testUser)
                .contactName("User 3")
                .contactEmail("user3@example.com")
                .message("Message 3")
                .status(InquiryStatus.NEW)
                .build();

        inquiryRepository.save(inquiry1);
        inquiryRepository.save(inquiry2);
        inquiryRepository.save(inquiry3);

        List<Inquiry> newInquiries = inquiryService.findInquiriesByStatus(InquiryStatus.NEW);

        assertEquals(2, newInquiries.size());
        assertTrue(newInquiries.stream().allMatch(i -> i.getStatus() == InquiryStatus.NEW));
    }

    @Test
    void testUpdateInquiryStatus() {
        Inquiry inquiry = Inquiry.builder()
                .propertyId(testPropertyId)
                .user(testUser)
                .contactName("Domi Kirev")
                .contactEmail("domi@example.com")
                .message("I'm interested")
                .status(InquiryStatus.NEW)
                .build();
        Inquiry saved = inquiryRepository.save(inquiry);
        UUID inquiryId = saved.getId();

        Inquiry updated = inquiryService.updateInquiryStatus(inquiryId, InquiryStatus.CONTACTED);

        assertEquals(InquiryStatus.CONTACTED, updated.getStatus());

        Optional<Inquiry> found = inquiryRepository.findById(inquiryId);
        assertTrue(found.isPresent());
        assertEquals(InquiryStatus.CONTACTED, found.get().getStatus());
    }

    @Test
    void testAddResponse() {
        Inquiry inquiry = Inquiry.builder()
                .propertyId(testPropertyId)
                .user(testUser)
                .contactName("Domi Kirev")
                .contactEmail("domi@example.com")
                .message("I'm interested")
                .status(InquiryStatus.NEW)
                .build();
        Inquiry saved = inquiryRepository.save(inquiry);
        UUID inquiryId = saved.getId();

        Inquiry updated = inquiryService.addResponse(inquiryId, "Thank you for your interest. We'll contact you soon.");

        assertEquals("Thank you for your interest. We'll contact you soon.", updated.getResponse());
        assertEquals(InquiryStatus.CONTACTED, updated.getStatus());

        Optional<Inquiry> found = inquiryRepository.findById(inquiryId);
        assertTrue(found.isPresent());
        assertEquals("Thank you for your interest. We'll contact you soon.", found.get().getResponse());
        assertEquals(InquiryStatus.CONTACTED, found.get().getStatus());
    }

    @Test
    void testAddResponse_AlreadyContacted() {
        Inquiry inquiry = Inquiry.builder()
                .propertyId(testPropertyId)
                .user(testUser)
                .contactName("Domi Kirev")
                .contactEmail("domi@example.com")
                .message("I'm interested")
                .status(InquiryStatus.CONTACTED)
                .build();
        Inquiry saved = inquiryRepository.save(inquiry);
        UUID inquiryId = saved.getId();

        Inquiry updated = inquiryService.addResponse(inquiryId, "Additional response");

        assertEquals("Additional response", updated.getResponse());
        assertEquals(InquiryStatus.CONTACTED, updated.getStatus());
    }

    @Test
    void testUpdateInquiry() {
        Inquiry inquiry = Inquiry.builder()
                .propertyId(testPropertyId)
                .user(testUser)
                .contactName("Domi Kirev")
                .contactEmail("domi@example.com")
                .message("I'm interested")
                .status(InquiryStatus.NEW)
                .build();
        Inquiry saved = inquiryRepository.save(inquiry);
        UUID inquiryId = saved.getId();

        inquiryService.updateInquiry(inquiryId, InquiryStatus.CONTACTED, "We'll contact you soon.");

        Optional<Inquiry> found = inquiryRepository.findById(inquiryId);
        assertTrue(found.isPresent());
        assertEquals(InquiryStatus.CONTACTED, found.get().getStatus());
        assertEquals("We'll contact you soon.", found.get().getResponse());
    }

    @Test
    void testUpdateInquiry_OnlyStatus() {
        Inquiry inquiry = Inquiry.builder()
                .propertyId(testPropertyId)
                .user(testUser)
                .contactName("Domi Kirev")
                .contactEmail("domi@example.com")
                .message("I'm interested")
                .status(InquiryStatus.NEW)
                .build();
        Inquiry saved = inquiryRepository.save(inquiry);
        UUID inquiryId = saved.getId();

        inquiryService.updateInquiry(inquiryId, InquiryStatus.CLOSED, null);

        Optional<Inquiry> found = inquiryRepository.findById(inquiryId);
        assertTrue(found.isPresent());
        assertEquals(InquiryStatus.CLOSED, found.get().getStatus());
        assertNull(found.get().getResponse());
    }

    @Test
    void testUpdateInquiry_OnlyResponse() {
        Inquiry inquiry = Inquiry.builder()
                .propertyId(testPropertyId)
                .user(testUser)
                .contactName("Domi Kirev")
                .contactEmail("domi@example.com")
                .message("I'm interested")
                .status(InquiryStatus.NEW)
                .build();
        Inquiry saved = inquiryRepository.save(inquiry);
        UUID inquiryId = saved.getId();

        inquiryService.updateInquiry(inquiryId, null, "Response text");

        Optional<Inquiry> found = inquiryRepository.findById(inquiryId);
        assertTrue(found.isPresent());
        assertEquals("Response text", found.get().getResponse());
        assertEquals(InquiryStatus.CONTACTED, found.get().getStatus());
    }

    @Test
    void testCountInquiriesByPropertyId() {
        UUID propertyId1 = UUID.randomUUID();
        UUID propertyId2 = UUID.randomUUID();

        Inquiry inquiry1 = Inquiry.builder()
                .propertyId(propertyId1)
                .user(testUser)
                .contactName("User 1")
                .contactEmail("user1@example.com")
                .message("Message 1")
                .status(InquiryStatus.NEW)
                .build();

        Inquiry inquiry2 = Inquiry.builder()
                .propertyId(propertyId1)
                .user(testUser)
                .contactName("User 2")
                .contactEmail("user2@example.com")
                .message("Message 2")
                .status(InquiryStatus.NEW)
                .build();

        Inquiry inquiry3 = Inquiry.builder()
                .propertyId(propertyId2)
                .user(testUser)
                .contactName("User 3")
                .contactEmail("user3@example.com")
                .message("Message 3")
                .status(InquiryStatus.NEW)
                .build();

        inquiryRepository.save(inquiry1);
        inquiryRepository.save(inquiry2);
        inquiryRepository.save(inquiry3);

        long count = inquiryService.countInquiriesByPropertyId(propertyId1);

        assertEquals(2, count);
    }

    @Test
    void testFindAllInquiries() {
        Inquiry inquiry1 = Inquiry.builder()
                .propertyId(testPropertyId)
                .user(testUser)
                .contactName("User 1")
                .contactEmail("user1@example.com")
                .message("Message 1")
                .status(InquiryStatus.NEW)
                .build();

        Inquiry inquiry2 = Inquiry.builder()
                .propertyId(testPropertyId)
                .user(testUser)
                .contactName("User 2")
                .contactEmail("user2@example.com")
                .message("Message 2")
                .status(InquiryStatus.NEW)
                .build();

        inquiryRepository.save(inquiry1);
        inquiryRepository.save(inquiry2);

        List<Inquiry> allInquiries = inquiryService.findAllInquiries();

        assertEquals(2, allInquiries.size());
    }

    @Test
    void testFindInquiriesByPropertyIds() {
        UUID propertyId1 = UUID.randomUUID();
        UUID propertyId2 = UUID.randomUUID();
        UUID propertyId3 = UUID.randomUUID();

        Inquiry inquiry1 = Inquiry.builder()
                .propertyId(propertyId1)
                .user(testUser)
                .contactName("User 1")
                .contactEmail("user1@example.com")
                .message("Message 1")
                .status(InquiryStatus.NEW)
                .build();

        Inquiry inquiry2 = Inquiry.builder()
                .propertyId(propertyId2)
                .user(testUser)
                .contactName("User 2")
                .contactEmail("user2@example.com")
                .message("Message 2")
                .status(InquiryStatus.NEW)
                .build();

        Inquiry inquiry3 = Inquiry.builder()
                .propertyId(propertyId3)
                .user(testUser)
                .contactName("User 3")
                .contactEmail("user3@example.com")
                .message("Message 3")
                .status(InquiryStatus.NEW)
                .build();

        inquiryRepository.save(inquiry1);
        inquiryRepository.save(inquiry2);
        inquiryRepository.save(inquiry3);

        List<Inquiry> inquiries = inquiryService.findInquiriesByPropertyIds(List.of(propertyId1, propertyId2));

        assertEquals(2, inquiries.size());
        assertTrue(inquiries.stream().anyMatch(i -> i.getPropertyId().equals(propertyId1)));
        assertTrue(inquiries.stream().anyMatch(i -> i.getPropertyId().equals(propertyId2)));
    }
}

