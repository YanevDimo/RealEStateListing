package app.service;

import app.entity.User;
import app.entity.UserRole;
import app.exception.UserNotFoundException;
import app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .name("Test User")
                .role(UserRole.USER)
                .isActive(true)
                .build();
    }

    @Test
    void testFindAllUsers() {
        // Given
        List<User> users = Collections.singletonList(testUser);
        when(userRepository.findAll()).thenReturn(users);

        // When
        List<User> result = userService.findAllUsers();

        // Then
        assertEquals(1, result.size());
        assertEquals("test@example.com", result.get(0).getEmail());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void testFindUserById_Found() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userService.findUserById(userId);

        // Then
        assertTrue(result.isPresent());
        assertEquals("test@example.com", result.get().getEmail());
    }

    @Test
    void testFindUserById_NotFound() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.findUserById(userId);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void testFindUserByEmail() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userService.findUserByEmail("test@example.com");

        // Then
        assertTrue(result.isPresent());
        assertEquals("test@example.com", result.get().getEmail());
    }

    @Test
    void testUserExistsByEmail() {
        // Given
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        // When
        boolean result = userService.userExistsByEmail("test@example.com");

        // Then
        assertTrue(result);
    }

    @Test
    void testCreateUser_Success() {
        // Given
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.createUser("new@example.com", "password", "New User", UserRole.USER);

        // Then
        assertNotNull(result);
        verify(userRepository).existsByEmail("new@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testCreateUser_DuplicateEmail_ThrowsException() {
        // Given
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                userService.createUser("test@example.com", "password", "User", UserRole.USER));

        assertTrue(exception.getMessage().contains("already exists"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testActivateUser() {
        // Given
        User inactiveUser = User.builder().id(userId).isActive(false).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(inactiveUser));
        when(userRepository.save(any(User.class))).thenReturn(inactiveUser);

        // When
        userService.activateUser(userId);

        // Then
        assertTrue(inactiveUser.getIsActive());
        verify(userRepository).save(inactiveUser);
    }

    @Test
    void testDeactivateUser() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.deactivateUser(userId);

        // Then
        assertFalse(testUser.getIsActive());
        verify(userRepository).save(testUser);
    }

    @Test
    void testChangeUserRole() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.changeUserRole(userId, UserRole.ADMIN);

        // Then
        assertEquals(UserRole.ADMIN, testUser.getRole());
        verify(userRepository).save(testUser);
    }

    @Test
    void testUpdateUserPassword() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.updateUserPassword(userId, "newPasswordHash");

        // Then
        assertEquals("newPasswordHash", result.getPasswordHash());
        verify(userRepository).save(testUser);
    }

    @Test
    void testUpdateUserProfile() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.updateUserProfile(userId, "New Name", "123456789");

        // Then
        assertEquals("New Name", result.getName());
        assertEquals("123456789", result.getPhone());
        verify(userRepository).save(testUser);
    }

    @Test
    void testValidateUserCredentials_Valid() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        boolean result = userService.validateUserCredentials("test@example.com", "hashedPassword");

        // Then
        assertTrue(result);
    }

    @Test
    void testValidateUserCredentials_InvalidPassword() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        boolean result = userService.validateUserCredentials("test@example.com", "wrongPassword");

        // Then
        assertFalse(result);
    }

    @Test
    void testValidateUserCredentials_InactiveUser() {
        // Given
        User inactiveUser = User.builder().email("test@example.com").passwordHash("hashedPassword").isActive(false).build();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(inactiveUser));

        // When
        boolean result = userService.validateUserCredentials("test@example.com", "hashedPassword");

        // Then
        assertFalse(result);
    }

    @Test
    void testFindUsersByRole() {
        // Given
        List<User> users = Collections.singletonList(testUser);
        when(userRepository.findByRole(UserRole.USER)).thenReturn(users);

        // When
        List<User> result = userService.findUsersByRole(UserRole.USER);

        // Then
        assertEquals(1, result.size());
        verify(userRepository, times(1)).findByRole(UserRole.USER);
    }

    @Test
    void testFindUsersByRole_WithPagination() {
        // Given
        List<User> users = Collections.singletonList(testUser);
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findByRole(UserRole.USER)).thenReturn(users);

        // When
        Page<User> result = userService.findUsersByRole(UserRole.USER, pageable);

        // Then
        assertEquals(1, result.getContent().size());
    }

    @Test
    void testSearchUsersByName() {
        // Given
        User user1 = User.builder().name("John Doe").email("john@example.com").build();
        User user2 = User.builder().name("Jane Smith").email("jane@example.com").build();
        List<User> allUsers = Arrays.asList(user1, user2);
        when(userRepository.findAll()).thenReturn(allUsers);

        // When
        List<User> result = userService.searchUsersByName("john");

        // Then
        assertEquals(1, result.size());
        assertEquals("John Doe", result.get(0).getName());
    }

    @Test
    void testGetUserStatistics() {
        // Given
        when(userRepository.count()).thenReturn(10L);
        when(userRepository.findByRole(UserRole.USER)).thenReturn(Collections.singletonList(testUser));
        when(userRepository.findByRole(UserRole.AGENT)).thenReturn(List.of());
        when(userRepository.findByRole(UserRole.ADMIN)).thenReturn(List.of());

        // When
        UserService.UserStatistics stats = userService.getUserStatistics();

        // Then
        assertNotNull(stats);
        assertEquals(10L, stats.getTotalUsers());
    }

    @Test
    void testActivateUser_UserNotFound_ThrowsException() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UserNotFoundException.class, () ->
                userService.activateUser(userId));
    }
}