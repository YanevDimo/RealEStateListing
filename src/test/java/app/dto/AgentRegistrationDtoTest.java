package app.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AgentRegistrationDtoTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testValidAgentRegistrationDto() {
        AgentRegistrationDto dto = AgentRegistrationDto.builder()
                .name("John Doe")
                .email("john@example.com")
                .password("password123")
                .confirmPassword("password123")
                .licenseNumber("LIC123")
                .build();

        Set<ConstraintViolation<AgentRegistrationDto>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testName_NotBlank() {
        AgentRegistrationDto dto = AgentRegistrationDto.builder()
                .name("")
                .email("john@example.com")
                .password("password123")
                .confirmPassword("password123")
                .licenseNumber("LIC123")
                .build();

        Set<ConstraintViolation<AgentRegistrationDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));
    }

    @Test
    void testName_SizeMin() {
        AgentRegistrationDto dto = AgentRegistrationDto.builder()
                .name("J")
                .email("john@example.com")
                .password("password123")
                .confirmPassword("password123")
                .licenseNumber("LIC123")
                .build();

        Set<ConstraintViolation<AgentRegistrationDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testEmail_NotBlank() {
        AgentRegistrationDto dto = AgentRegistrationDto.builder()
                .name("John Doe")
                .email("")
                .password("password123")
                .confirmPassword("password123")
                .licenseNumber("LIC123")
                .build();

        Set<ConstraintViolation<AgentRegistrationDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testEmail_InvalidFormat() {
        AgentRegistrationDto dto = AgentRegistrationDto.builder()
                .name("John Doe")
                .email("invalid-email")
                .password("password123")
                .confirmPassword("password123")
                .licenseNumber("LIC123")
                .build();

        Set<ConstraintViolation<AgentRegistrationDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testPassword_NotBlank() {
        AgentRegistrationDto dto = AgentRegistrationDto.builder()
                .name("John Doe")
                .email("john@example.com")
                .password("")
                .confirmPassword("password123")
                .licenseNumber("LIC123")
                .build();

        Set<ConstraintViolation<AgentRegistrationDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testPassword_SizeMin() {
        AgentRegistrationDto dto = AgentRegistrationDto.builder()
                .name("John Doe")
                .email("john@example.com")
                .password("12345")
                .confirmPassword("12345")
                .licenseNumber("LIC123")
                .build();

        Set<ConstraintViolation<AgentRegistrationDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testLicenseNumber_NotBlank() {
        AgentRegistrationDto dto = AgentRegistrationDto.builder()
                .name("John Doe")
                .email("john@example.com")
                .password("password123")
                .confirmPassword("password123")
                .licenseNumber("")
                .build();

        Set<ConstraintViolation<AgentRegistrationDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testExperienceYears_Min() {
        AgentRegistrationDto dto = AgentRegistrationDto.builder()
                .name("John Doe")
                .email("john@example.com")
                .password("password123")
                .confirmPassword("password123")
                .licenseNumber("LIC123")
                .experienceYears(-1)
                .build();

        Set<ConstraintViolation<AgentRegistrationDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testExperienceYears_Max() {
        AgentRegistrationDto dto = AgentRegistrationDto.builder()
                .name("John Doe")
                .email("john@example.com")
                .password("password123")
                .confirmPassword("password123")
                .licenseNumber("LIC123")
                .experienceYears(51)
                .build();

        Set<ConstraintViolation<AgentRegistrationDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testIsPasswordMatching_True() {
        AgentRegistrationDto dto = AgentRegistrationDto.builder()
                .password("password123")
                .confirmPassword("password123")
                .build();

        assertTrue(dto.isPasswordMatching());
    }

    @Test
    void testIsPasswordMatching_False() {
        AgentRegistrationDto dto = AgentRegistrationDto.builder()
                .password("password123")
                .confirmPassword("different")
                .build();

        assertFalse(dto.isPasswordMatching());
    }
}






