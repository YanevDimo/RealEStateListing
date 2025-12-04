package app.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserLoginDtoTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testValidUserLoginDto() {
        UserLoginDto dto = UserLoginDto.builder()
                .email("user@example.com")
                .password("password123")
                .build();

        Set<ConstraintViolation<UserLoginDto>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testEmail_NotBlank() {
        UserLoginDto dto = UserLoginDto.builder()
                .email("")
                .password("password123")
                .build();

        Set<ConstraintViolation<UserLoginDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testEmail_InvalidFormat() {
        UserLoginDto dto = UserLoginDto.builder()
                .email("invalid-email")
                .password("password123")
                .build();

        Set<ConstraintViolation<UserLoginDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testPassword_NotBlank() {
        UserLoginDto dto = UserLoginDto.builder()
                .email("user@example.com")
                .password("")
                .build();

        Set<ConstraintViolation<UserLoginDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }
}






