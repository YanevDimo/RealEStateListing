package app.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InquiryDtoTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testValidInquiryDto() {
        InquiryDto dto = InquiryDto.builder()
                .propertyId(UUID.randomUUID())
                .contactName("John Doe")
                .contactEmail("john@example.com")
                .contactPhone("1234567890")
                .message("I'm interested in this property")
                .build();

        Set<ConstraintViolation<InquiryDto>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testContactName_NotBlank() {
        InquiryDto dto = InquiryDto.builder()
                .contactName("")
                .contactEmail("john@example.com")
                .message("Message")
                .build();

        Set<ConstraintViolation<InquiryDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testContactEmail_NotBlank() {
        InquiryDto dto = InquiryDto.builder()
                .contactName("John Doe")
                .contactEmail("")
                .message("Message")
                .build();

        Set<ConstraintViolation<InquiryDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testContactEmail_InvalidFormat() {
        InquiryDto dto = InquiryDto.builder()
                .contactName("John Doe")
                .contactEmail("invalid-email")
                .message("Message")
                .build();

        Set<ConstraintViolation<InquiryDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testMessage_NotBlank() {
        InquiryDto dto = InquiryDto.builder()
                .contactName("John Doe")
                .contactEmail("john@example.com")
                .message("")
                .build();

        Set<ConstraintViolation<InquiryDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testMessage_SizeMax() {
        String longMessage = "a".repeat(2001);
        InquiryDto dto = InquiryDto.builder()
                .contactName("John Doe")
                .contactEmail("john@example.com")
                .message(longMessage)
                .build();

        Set<ConstraintViolation<InquiryDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }
}






