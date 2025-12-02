package app.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserRegistrationDtoTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testValidUserRegistrationDto() {
        UserRegistrationDto dto = UserRegistrationDto.builder()
                .name("John Doe")
                .email("john@example.com")
                .password("password123")
                .confirmPassword("password123")
                .build();

        Set<ConstraintViolation<UserRegistrationDto>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testName_NotBlank() {
        UserRegistrationDto dto = UserRegistrationDto.builder()
                .name("")
                .email("john@example.com")
                .password("password123")
                .confirmPassword("password123")
                .build();

        Set<ConstraintViolation<UserRegistrationDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testEmail_InvalidFormat() {
        UserRegistrationDto dto = UserRegistrationDto.builder()
                .name("John Doe")
                .email("invalid-email")
                .password("password123")
                .confirmPassword("password123")
                .build();

        Set<ConstraintViolation<UserRegistrationDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testPassword_SizeMin() {
        UserRegistrationDto dto = UserRegistrationDto.builder()
                .name("John Doe")
                .email("john@example.com")
                .password("12345")
                .confirmPassword("12345")
                .build();

        Set<ConstraintViolation<UserRegistrationDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testIsPasswordMatching_True() {
        UserRegistrationDto dto = UserRegistrationDto.builder()
                .password("password123")
                .confirmPassword("password123")
                .build();

        assertTrue(dto.isPasswordMatching());
    }

    @Test
    void testIsPasswordMatching_False() {
        UserRegistrationDto dto = UserRegistrationDto.builder()
                .password("password123")
                .confirmPassword("different")
                .build();

        assertFalse(dto.isPasswordMatching());
    }
}




