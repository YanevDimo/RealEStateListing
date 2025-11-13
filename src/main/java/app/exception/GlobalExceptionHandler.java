package app.exception;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the application.
 * Handles exceptions and returns ResponseEntity with error details.
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle custom application exceptions
     */
    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<Map<String, Object>> handleApplicationException(
            ApplicationException ex, WebRequest request) {
        log.error("Application exception occurred: {}", ex.getMessage(), ex);
        
        Map<String, Object> errorResponse = buildErrorResponse(
            ex.getMessage(),
            HttpStatus.BAD_REQUEST,
            request
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handle specific not found exceptions
     */
    @ExceptionHandler({
        UserNotFoundException.class,
        AgentNotFoundException.class,
        PropertyNotFoundException.class,
        CityNotFoundException.class,
        PropertyTypeNotFoundException.class
    })
    public ResponseEntity<Map<String, Object>> handleNotFoundException(
            ApplicationException ex, WebRequest request) {
        log.error("Resource not found: {}", ex.getMessage(), ex);
        
        Map<String, Object> errorResponse = buildErrorResponse(
            ex.getMessage(),
            HttpStatus.NOT_FOUND,
            request
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handle validation exceptions (duplicate data, invalid input)
     */
    @ExceptionHandler({
        DuplicateEmailException.class,
        DuplicateLicenseNumberException.class,
        InvalidRoleException.class
    })
    public ResponseEntity<Map<String, Object>> handleValidationException(
            ApplicationException ex, WebRequest request) {
        log.error("Validation exception: {}", ex.getMessage(), ex);
        
        Map<String, Object> errorResponse = buildErrorResponse(
            ex.getMessage(),
            HttpStatus.CONFLICT,
            request
        );
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Handle IllegalArgumentException (built-in Java exception)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        log.error("Illegal argument exception: {}", ex.getMessage(), ex);
        
        Map<String, Object> errorResponse = buildErrorResponse(
            "Invalid input: " + ex.getMessage(),
            HttpStatus.BAD_REQUEST,
            request
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handle Feign client exceptions (service communication errors)
     */
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<Map<String, Object>> handleFeignException(
            FeignException ex, WebRequest request) {
        log.error("Feign client exception: Status {} - {}", ex.status(), ex.getMessage(), ex);
        
        HttpStatus status = HttpStatus.resolve(ex.status());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        
        Map<String, Object> errorResponse = buildErrorResponse(
            "Service communication error: " + ex.getMessage(),
            status,
            request
        );
        
        return ResponseEntity.status(status).body(errorResponse);
    }

    /**
     * Handle NullPointerException
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<Map<String, Object>> handleNullPointerException(
            NullPointerException ex, WebRequest request) {
        log.error("Null pointer exception occurred", ex);
        
        Map<String, Object> errorResponse = buildErrorResponse(
            "A required value was null. Please check your request.",
            HttpStatus.BAD_REQUEST,
            request
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handle NoResourceFoundException (missing static resources like CSS, JS, images)
     * This is common and not a critical error, so we log at debug level
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFoundException(
            NoResourceFoundException ex, WebRequest request) {
        // Log at debug level since missing static resources are common and not critical
        log.debug("Static resource not found: {}", ex.getResourcePath());
        
        Map<String, Object> errorResponse = buildErrorResponse(
            "Resource not found: " + ex.getResourcePath(),
            HttpStatus.NOT_FOUND,
            request
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handle generic exceptions (catch-all)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex, WebRequest request) {
        log.error("Unexpected exception occurred", ex);
        
        Map<String, Object> errorResponse = buildErrorResponse(
            "An unexpected error occurred. Please try again later.",
            HttpStatus.INTERNAL_SERVER_ERROR,
            request
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Build error response map
     */
    private Map<String, Object> buildErrorResponse(
            String message, HttpStatus status, WebRequest request) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", status.value());
        errorResponse.put("error", status.getReasonPhrase());
        errorResponse.put("message", message);
        errorResponse.put("path", request.getDescription(false).replace("uri=", ""));
        return errorResponse;
    }
}
