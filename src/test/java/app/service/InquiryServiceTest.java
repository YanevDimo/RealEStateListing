package app.service;

import app.dto.InquiryDto;
import app.entity.Inquiry;
import app.entity.InquiryStatus;
import app.entity.User;
import app.exception.InquiryNotFoundException;
import app.repository.InquiryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InquiryServiceTest {

    @Mock
    private InquiryRepository inquiryRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private InquiryService inquiryService;

    private Inquiry testInquiry;
    private User testUser;
    private UUID inquiryId;
    private UUID userId;
    private UUID propertyId;

    @BeforeEach
    void setUp() {
        inquiryId = UUID.randomUUID();
        userId = UUID.randomUUID();
        propertyId = UUID.randomUUID();

        testUser = User.builder()
                .id(userId)
                .email("user@example.com")
                .name("Test User")
                .build();

        testInquiry = Inquiry.builder()
                .id(inquiryId)
                .propertyId(propertyId)
                .user(testUser)
                .contactName("Felix Brown")
                .contactEmail("felix@example.com")
                .contactPhone("123456789")
                .message("I'm interested in this property")
                .status(InquiryStatus.NEW)
                .build();
    }

    @Test
    void testCreateInquiry_WithAuthenticatedUser() {
        // Given
        InquiryDto inquiryDto = InquiryDto.builder()
                .contactName("Felix Brown")
                .contactEmail("felix@example.com")
                .contactPhone("123456789")
                .message("I'm interested")
                .build();

        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("user@example.com");
        when(userService.findUserByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        when(inquiryRepository.save(any(Inquiry.class))).thenReturn(testInquiry);

        // When
        inquiryService.createInquiry(inquiryDto, propertyId, authentication);

        // Then
        verify(userService).findUserByEmail("user@example.com");
        verify(inquiryRepository).save(any(Inquiry.class));
    }

    @Test
    void testCreateInquiry_WithoutAuthentication() {
        // Given
        InquiryDto inquiryDto = InquiryDto.builder()
                .contactName("Felix Brown")
                .contactEmail("felix@example.com")
                .contactPhone("123456789")
                .message("I'm interested")
                .build();

        when(inquiryRepository.save(any(Inquiry.class))).thenReturn(testInquiry);

        // When
        inquiryService.createInquiry(inquiryDto, propertyId, null);

        // Then
        verify(userService, never()).findUserByEmail(any());
        verify(inquiryRepository).save(any(Inquiry.class));
    }

    @Test
    void testCreateInquiry_UserNotFound() {
        // Given
        InquiryDto inquiryDto = InquiryDto.builder()
                .contactName("Felix Brown")
                .contactEmail("felix@example.com")
                .message("I'm interested")
                .build();

        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("felix@example.com");
        when(userService.findUserByEmail("felix@example.com")).thenReturn(Optional.empty());
        when(inquiryRepository.save(any(Inquiry.class))).thenReturn(testInquiry);

        // When
        inquiryService.createInquiry(inquiryDto, propertyId, authentication);

        // Then - should still create inquiry without user
        verify(inquiryRepository).save(any(Inquiry.class));
    }

    @Test
    void testFindInquiryById_Found() {
        // Given
        when(inquiryRepository.findById(inquiryId)).thenReturn(Optional.of(testInquiry));

        // When
        Optional<Inquiry> result = inquiryService.findInquiryById(inquiryId);

        // Then
        assertTrue(result.isPresent());
        assertEquals("Felix Brown", result.get().getContactName());
        assertEquals(InquiryStatus.NEW, result.get().getStatus());
    }

    @Test
    void testFindInquiryById_NotFound() {
        // Given
        when(inquiryRepository.findById(inquiryId)).thenReturn(Optional.empty());

        // When
        Optional<Inquiry> result = inquiryService.findInquiryById(inquiryId);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void testFindInquiriesByPropertyId() {
        // Given
        List<Inquiry> inquiries = Collections.singletonList(testInquiry);
        when(inquiryRepository.findByPropertyId(propertyId)).thenReturn(inquiries);

        // When
        List<Inquiry> result = inquiryService.findInquiriesByPropertyId(propertyId);

        // Then
        assertEquals(1, result.size());
        assertEquals(propertyId, result.get(0).getPropertyId());
        verify(inquiryRepository).findByPropertyId(propertyId);
    }

    @Test
    void testFindInquiriesByUserId() {
        // Given
        List<Inquiry> inquiries = Collections.singletonList(testInquiry);
        when(inquiryRepository.findByUserId(userId)).thenReturn(inquiries);

        // When
        List<Inquiry> result = inquiryService.findInquiriesByUserId(userId);

        // Then
        assertEquals(1, result.size());
        verify(inquiryRepository).findByUserId(userId);
    }

    @Test
    void testFindInquiriesByStatus() {
        // Given
        List<Inquiry> inquiries = Collections.singletonList(testInquiry);
        when(inquiryRepository.findByStatus(InquiryStatus.NEW)).thenReturn(inquiries);

        // When
        List<Inquiry> result = inquiryService.findInquiriesByStatus(InquiryStatus.NEW);

        // Then
        assertEquals(1, result.size());
        assertEquals(InquiryStatus.NEW, result.get(0).getStatus());
    }

    @Test
    void testUpdateInquiryStatus() {
        // Given
        when(inquiryRepository.findById(inquiryId)).thenReturn(Optional.of(testInquiry));
        when(inquiryRepository.save(any(Inquiry.class))).thenReturn(testInquiry);

        // When
        Inquiry result = inquiryService.updateInquiryStatus(inquiryId, InquiryStatus.CONTACTED);

        // Then
        assertEquals(InquiryStatus.CONTACTED, result.getStatus());
        verify(inquiryRepository).save(testInquiry);
    }

    @Test
    void testUpdateInquiryStatus_NotFound_ThrowsException() {
        // Given
        when(inquiryRepository.findById(inquiryId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(InquiryNotFoundException.class, () ->
                inquiryService.updateInquiryStatus(inquiryId, InquiryStatus.CONTACTED));

        verify(inquiryRepository, never()).save(any());
    }

    @Test
    void testAddResponse_NewInquiry_UpdatesStatus() {
        // Given
        when(inquiryRepository.findById(inquiryId)).thenReturn(Optional.of(testInquiry));
        when(inquiryRepository.save(any(Inquiry.class))).thenReturn(testInquiry);

        // When
        Inquiry result = inquiryService.addResponse(inquiryId, "Thank you for your inquiry");

        // Then
        assertEquals("Thank you for your inquiry", result.getResponse());
        assertEquals(InquiryStatus.CONTACTED, result.getStatus());
        verify(inquiryRepository).save(testInquiry);
    }

    @Test
    void testAddResponse_NonNewInquiry_DoesNotChangeStatus() {
        // Given
        testInquiry.setStatus(InquiryStatus.CONTACTED);
        when(inquiryRepository.findById(inquiryId)).thenReturn(Optional.of(testInquiry));
        when(inquiryRepository.save(any(Inquiry.class))).thenReturn(testInquiry);

        // When
        Inquiry result = inquiryService.addResponse(inquiryId, "Response message");

        // Then
        assertEquals(InquiryStatus.CONTACTED, result.getStatus()); // Status unchanged
        assertEquals("Response message", result.getResponse());
    }

    @Test
    void testAddResponse_NotFound_ThrowsException() {
        // Given
        when(inquiryRepository.findById(inquiryId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(InquiryNotFoundException.class, () ->
                inquiryService.addResponse(inquiryId, "Response"));

        verify(inquiryRepository, never()).save(any());
    }

    @Test
    void testCountInquiriesByPropertyId() {
        // Given
        List<Inquiry> inquiries = Arrays.asList(testInquiry, testInquiry);
        when(inquiryRepository.findByPropertyId(propertyId)).thenReturn(inquiries);

        // When
        long result = inquiryService.countInquiriesByPropertyId(propertyId);

        // Then
        assertEquals(2, result);
    }

    @Test
    void testFindAllInquiries() {
        // Given
        List<Inquiry> inquiries = Collections.singletonList(testInquiry);
        when(inquiryRepository.findAll()).thenReturn(inquiries);

        // When
        List<Inquiry> result = inquiryService.findAllInquiries();

        // Then
        assertEquals(1, result.size());
        verify(inquiryRepository).findAll();
    }

    @Test
    void testFindInquiriesByPropertyIds() {
        // Given
        UUID propertyId2 = UUID.randomUUID();
        Inquiry inquiry2 = Inquiry.builder()
                .id(UUID.randomUUID())
                .propertyId(propertyId2)
                .contactName("Jane Doe")
                .contactEmail("jane@example.com")
                .message("Message")
                .build();

        List<Inquiry> allInquiries = Arrays.asList(testInquiry, inquiry2);
        when(inquiryRepository.findAll()).thenReturn(allInquiries);

        // When
        List<Inquiry> result = inquiryService.findInquiriesByPropertyIds(
                Arrays.asList(propertyId, propertyId2));

        // Then
        assertEquals(2, result.size());
    }

    @Test
    void testUpdateInquiry_WithStatusAndResponse() {
        // Given
        when(inquiryRepository.findById(inquiryId)).thenReturn(Optional.of(testInquiry));
        when(inquiryRepository.save(any(Inquiry.class))).thenReturn(testInquiry);

        // When
        inquiryService.updateInquiry(inquiryId, InquiryStatus.CLOSED, "Property sold");

        // Then
        assertEquals(InquiryStatus.CLOSED, testInquiry.getStatus());
        assertEquals("Property sold", testInquiry.getResponse());
        verify(inquiryRepository).save(testInquiry);
    }

    @Test
    void testUpdateInquiry_OnlyStatus() {
        // Given
        when(inquiryRepository.findById(inquiryId)).thenReturn(Optional.of(testInquiry));
        when(inquiryRepository.save(any(Inquiry.class))).thenReturn(testInquiry);

        // When
        inquiryService.updateInquiry(inquiryId, InquiryStatus.CONTACTED, null);

        // Then
        assertEquals(InquiryStatus.CONTACTED, testInquiry.getStatus());
        assertNull(testInquiry.getResponse());
    }

    @Test
    void testUpdateInquiry_OnlyResponse_UpdatesStatusIfNew() {
        // Given
        when(inquiryRepository.findById(inquiryId)).thenReturn(Optional.of(testInquiry));
        when(inquiryRepository.save(any(Inquiry.class))).thenReturn(testInquiry);

        // When
        inquiryService.updateInquiry(inquiryId, null, "Response text");

        // Then
        assertEquals(InquiryStatus.CONTACTED, testInquiry.getStatus()); // Auto-updated
        assertEquals("Response text", testInquiry.getResponse());
    }

    @Test
    void testUpdateInquiry_EmptyResponse_DoesNotUpdate() {
        // Given
        when(inquiryRepository.findById(inquiryId)).thenReturn(Optional.of(testInquiry));
        when(inquiryRepository.save(any(Inquiry.class))).thenReturn(testInquiry);

        // When
        inquiryService.updateInquiry(inquiryId, null, "   "); // Whitespace only

        // Then
        assertNull(testInquiry.getResponse());
    }

    @Test
    void testUpdateInquiry_NotFound_ThrowsException() {
        // Given
        when(inquiryRepository.findById(inquiryId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(InquiryNotFoundException.class, () ->
                inquiryService.updateInquiry(inquiryId, InquiryStatus.CONTACTED, "Response"));

        verify(inquiryRepository, never()).save(any());
    }
}