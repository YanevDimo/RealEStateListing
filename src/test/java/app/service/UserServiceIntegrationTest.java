package app.service;

import app.entity.User;
import app.entity.UserRole;
import app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("h2")
class UserServiceIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
        
        userRepository.deleteAll();
        
        testUser = User.builder()
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .name("Test User")
                .phone("1234567890")
                .role(UserRole.USER)
                .isActive(true)
                .build();
    }

    @Test
    void testSaveAndFindUser() {
        User saved = userService.saveUser(testUser);

        assertNotNull(saved.getId());
        assertEquals("test@example.com", saved.getEmail());
        assertEquals("Test User", saved.getName());

        Optional<User> found = userService.findUserById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals("test@example.com", found.get().getEmail());
    }

    @Test
    void testFindUserByEmail() {
        User saved = userService.saveUser(testUser);

        Optional<User> found = userService.findUserByEmail("test@example.com");

        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
    }

    @Test
    void testUserExistsByEmail() {
        userService.saveUser(testUser);

        boolean exists = userService.userExistsByEmail("test@example.com");

        assertTrue(exists);
    }

    @Test
    void testUserExistsByEmail_NotExists() {
        boolean exists = userService.userExistsByEmail("nonexistent@example.com");

        assertFalse(exists);
    }

    @Test
    void testFindUsersByRole() {
        userService.saveUser(User.builder().email("user1@example.com").passwordHash("hash").name("User 1").role(UserRole.USER).isActive(true).build());
        userService.saveUser(User.builder().email("agent1@example.com").passwordHash("hash").name("Agent 1").role(UserRole.AGENT).isActive(true).build());
        userService.saveUser(User.builder().email("user2@example.com").passwordHash("hash").name("User 2").role(UserRole.USER).isActive(true).build());

        List<User> users = userService.findUsersByRole(UserRole.USER);

        assertEquals(2, users.size());
        assertTrue(users.stream().allMatch(u -> u.getRole() == UserRole.USER));
    }

    @Test
    void testFindActiveUsersByRole() {
        userService.saveUser(User.builder().email("active@example.com").passwordHash("hash").name("Active").role(UserRole.USER).isActive(true).build());
        userService.saveUser(User.builder().email("inactive@example.com").passwordHash("hash").name("Inactive").role(UserRole.USER).isActive(false).build());

        List<User> activeUsers = userService.findActiveUsersByRole(UserRole.USER);

        assertEquals(1, activeUsers.size());
        assertEquals("active@example.com", activeUsers.get(0).getEmail());
    }

    @Test
    void testSearchUsersByName() {
        userService.saveUser(User.builder().email("domi@example.com").passwordHash("hash").name("Domi Kirev").role(UserRole.USER).isActive(true).build());
        userService.saveUser(User.builder().email("ivan@example.com").passwordHash("hash").name("Ivan Georgiev").role(UserRole.USER).isActive(true).build());

        List<User> results = userService.searchUsersByName("domi");

        assertEquals(1, results.size());
        assertEquals("Domi Kirev", results.get(0).getName());
    }

    @Test
    void testFindUsersByEmailDomain() {
        userService.saveUser(User.builder().email("user1@gmail.com").passwordHash("hash").name("User 1").role(UserRole.USER).isActive(true).build());
        userService.saveUser(User.builder().email("user2@yahoo.com").passwordHash("hash").name("User 2").role(UserRole.USER).isActive(true).build());
        userService.saveUser(User.builder().email("user3@gmail.com").passwordHash("hash").name("User 3").role(UserRole.USER).isActive(true).build());

        List<User> gmailUsers = userService.findUsersByEmailDomain("@gmail.com");

        assertEquals(2, gmailUsers.size());
        assertTrue(gmailUsers.stream().allMatch(u -> u.getEmail().endsWith("@gmail.com")));
    }

    @Test
    void testFindUsersWithPhone() {
        userService.saveUser(User.builder().email("withphone@example.com").passwordHash("hash").name("With Phone").phone("1234567890").role(UserRole.USER).isActive(true).build());
        userService.saveUser(User.builder().email("nophone@example.com").passwordHash("hash").name("No Phone").role(UserRole.USER).isActive(true).build());

        List<User> usersWithPhone = userService.findUsersWithPhone();

        assertEquals(1, usersWithPhone.size());
        assertEquals("withphone@example.com", usersWithPhone.get(0).getEmail());
    }

    @Test
    void testFindUsersCreatedAfter() {
        LocalDateTime pastTime = LocalDateTime.now().minusDays(1);
        
        userService.saveUser(User.builder().email("old@example.com").passwordHash("hash").name("Old User").role(UserRole.USER).isActive(true).build());
        userService.saveUser(User.builder().email("new@example.com").passwordHash("hash").name("New User").role(UserRole.USER).isActive(true).build());

        List<User> newUsers = userService.findUsersCreatedAfter(pastTime);

        assertEquals(2, newUsers.size());
    }

    @Test
    void testCreateUser_Success() {
        User created = userService.createUser("newuser@example.com", "hashedPassword", "New User", UserRole.USER);

        assertNotNull(created.getId());
        assertEquals("newuser@example.com", created.getEmail());
        assertEquals("New User", created.getName());
        assertEquals(UserRole.USER, created.getRole());
        assertTrue(created.getIsActive());

        Optional<User> found = userRepository.findById(created.getId());
        assertTrue(found.isPresent());
    }

    @Test
    void testCreateUser_DuplicateEmail_ThrowsException() {
        userService.saveUser(testUser);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            userService.createUser("test@example.com", "hash", "Duplicate", UserRole.USER));

        assertTrue(exception.getMessage().contains("already exists"));
    }

    @Test
    void testUpdateUser() {
        User saved = userService.saveUser(testUser);
        UUID userId = saved.getId();

        saved.setName("Updated Name");
        userService.updateUser(saved);

        Optional<User> found = userRepository.findById(userId);
        assertTrue(found.isPresent());
        assertEquals("Updated Name", found.get().getName());
    }

    @Test
    void testUpdateUserPassword() {
        User saved = userService.saveUser(testUser);
        UUID userId = saved.getId();

        User updated = userService.updateUserPassword(userId, "newHashedPassword");

        assertEquals("newHashedPassword", updated.getPasswordHash());

        Optional<User> found = userRepository.findById(userId);
        assertTrue(found.isPresent());
        assertEquals("newHashedPassword", found.get().getPasswordHash());
    }

    @Test
    void testUpdateUserProfile() {
        User saved = userService.saveUser(testUser);
        UUID userId = saved.getId();

        User updated = userService.updateUserProfile(userId, "Updated Name", "9876543210");

        assertEquals("Updated Name", updated.getName());
        assertEquals("9876543210", updated.getPhone());

        Optional<User> found = userRepository.findById(userId);
        assertTrue(found.isPresent());
        assertEquals("Updated Name", found.get().getName());
        assertEquals("9876543210", found.get().getPhone());
    }

    @Test
    void testActivateUser() {
        User inactiveUser = User.builder()
                .email("inactive@example.com")
                .passwordHash("hash")
                .name("Inactive")
                .role(UserRole.USER)
                .isActive(false)
                .build();
        User saved = userService.saveUser(inactiveUser);
        UUID userId = saved.getId();

        userService.activateUser(userId);

        Optional<User> found = userRepository.findById(userId);
        assertTrue(found.isPresent());
        assertTrue(found.get().getIsActive());
    }

    @Test
    void testDeactivateUser() {
        User saved = userService.saveUser(testUser);
        UUID userId = saved.getId();

        userService.deactivateUser(userId);

        Optional<User> found = userRepository.findById(userId);
        assertTrue(found.isPresent());
        assertFalse(found.get().getIsActive());
    }

    @Test
    void testChangeUserRole() {
        User saved = userService.saveUser(testUser);
        UUID userId = saved.getId();

        userService.changeUserRole(userId, UserRole.ADMIN);

        Optional<User> found = userRepository.findById(userId);
        assertTrue(found.isPresent());
        assertEquals(UserRole.ADMIN, found.get().getRole());
    }

    @Test
    void testDeleteUser() {
        User saved = userService.saveUser(testUser);
        UUID userId = saved.getId();

        userService.deleteUser(userId);

        Optional<User> found = userRepository.findById(userId);
        assertFalse(found.isPresent());
    }

    @Test
    void testFindAllUsers() {
        userService.saveUser(User.builder().email("user1@example.com").passwordHash("hash").name("User 1").role(UserRole.USER).isActive(true).build());
        userService.saveUser(User.builder().email("user2@example.com").passwordHash("hash").name("User 2").role(UserRole.USER).isActive(true).build());

        List<User> allUsers = userService.findAllUsers();

        assertEquals(2, allUsers.size());
    }

    @Test
    void testFindAllActiveUsers() {
        userService.saveUser(User.builder().email("active1@example.com").passwordHash("hash").name("Active 1").role(UserRole.USER).isActive(true).build());
        userService.saveUser(User.builder().email("inactive@example.com").passwordHash("hash").name("Inactive").role(UserRole.USER).isActive(false).build());
        userService.saveUser(User.builder().email("active2@example.com").passwordHash("hash").name("Active 2").role(UserRole.USER).isActive(true).build());

        List<User> activeUsers = userService.findAllActiveUsers();

        assertEquals(2, activeUsers.size());
        assertTrue(activeUsers.stream().allMatch(User::getIsActive));
    }

    @Test
    void testCountAllUsers() {
        userService.saveUser(User.builder().email("user1@example.com").passwordHash("hash").name("User 1").role(UserRole.USER).isActive(true).build());
        userService.saveUser(User.builder().email("user2@example.com").passwordHash("hash").name("User 2").role(UserRole.USER).isActive(true).build());

        long count = userService.countAllUsers();

        assertEquals(2, count);
    }

    @Test
    void testCountUsersByRole() {
        userService.saveUser(User.builder().email("user1@example.com").passwordHash("hash").name("User 1").role(UserRole.USER).isActive(true).build());
        userService.saveUser(User.builder().email("agent1@example.com").passwordHash("hash").name("Agent 1").role(UserRole.AGENT).isActive(true).build());
        userService.saveUser(User.builder().email("user2@example.com").passwordHash("hash").name("User 2").role(UserRole.USER).isActive(true).build());

        long count = userService.countUsersByRole(UserRole.USER);

        assertEquals(2, count);
    }

    @Test
    void testCountActiveUsersByRole() {
        userService.saveUser(User.builder().email("active@example.com").passwordHash("hash").name("Active").role(UserRole.USER).isActive(true).build());
        userService.saveUser(User.builder().email("inactive@example.com").passwordHash("hash").name("Inactive").role(UserRole.USER).isActive(false).build());

        long count = userService.countActiveUsersByRole(UserRole.USER);

        assertEquals(1, count);
    }

    @Test
    void testValidateUserCredentials_Valid() {
        User saved = userService.saveUser(testUser);

        boolean isValid = userService.validateUserCredentials("test@example.com", "hashedPassword");

        assertTrue(isValid);
    }

    @Test
    void testValidateUserCredentials_InvalidPassword() {
        userService.saveUser(testUser);

        boolean isValid = userService.validateUserCredentials("test@example.com", "wrongPassword");

        assertFalse(isValid);
    }

    @Test
    void testValidateUserCredentials_InactiveUser() {
        User inactiveUser = User.builder()
                .email("inactive@example.com")
                .passwordHash("hashedPassword")
                .name("Inactive")
                .role(UserRole.USER)
                .isActive(false)
                .build();
        userService.saveUser(inactiveUser);

        boolean isValid = userService.validateUserCredentials("inactive@example.com", "hashedPassword");

        assertFalse(isValid);
    }

    @Test
    void testValidateUserCredentials_NonExistentUser() {
        boolean isValid = userService.validateUserCredentials("nonexistent@example.com", "password");

        assertFalse(isValid);
    }

    @Test
    void testGetUserStatistics() {
        userService.saveUser(User.builder().email("user1@example.com").passwordHash("hash").name("User 1").role(UserRole.USER).isActive(true).build());
        userService.saveUser(User.builder().email("agent1@example.com").passwordHash("hash").name("Agent 1").role(UserRole.AGENT).isActive(true).build());
        userService.saveUser(User.builder().email("admin1@example.com").passwordHash("hash").name("Admin 1").role(UserRole.ADMIN).isActive(true).build());

        UserService.UserStatistics stats = userService.getUserStatistics();

        assertEquals(3, stats.getTotalUsers());
        assertEquals(1, stats.getTotalAgents());
        assertEquals(1, stats.getTotalAdmins());
    }
}

