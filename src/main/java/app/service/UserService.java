package app.service;


import app.entity.User;
import app.entity.UserRole;
import app.repository.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Data
@Slf4j
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;


    public List<User> findAllUsers() {
        log.debug("Finding all users");
        return userRepository.findAll();
    }

    public long countAllUsers() {
        log.debug("Counting all users");
        return userRepository.count();
    }

    @Transactional
    public User saveUser(User user) {
        log.debug("Saving user: {}", user.getEmail());
        return userRepository.save(user);
    }


    public List<User> findAllActiveUsers() {
        log.debug("Finding all active users");
        return userRepository.findByIsActiveTrue();
    }


    public Optional<User> findUserById(UUID id) {
        log.debug("Finding user by ID: {}", id);
        return userRepository.findById(id);
    }


    public Optional<User> findUserByEmail(String email) {
        log.debug("Finding user by email: {}", email);
        return userRepository.findByEmail(email);
    }


    public boolean userExistsByEmail(String email) {
        log.debug("Checking if user exists by email: {}", email);
        return userRepository.existsByEmail(email);
    }


    public List<User> findUsersByRole(UserRole role) {
        log.debug("Finding users by role: {}", role);
        return userRepository.findByRole(role);
    }


    public List<User> findActiveUsersByRole(UserRole role) {
        log.debug("Finding active users by role: {}", role);
        return userRepository.findByRole(role).stream().filter(User::getIsActive).toList();
    }


    public Page<User> findUsersByRole(UserRole role, Pageable pageable) {
        log.debug("Finding users by role: {} with pagination: {}", role, pageable);
        List<User> allUsers = userRepository.findByRole(role);
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allUsers.size());
        List<User> pageContent = allUsers.subList(start, end);
        return new org.springframework.data.domain.PageImpl<>(pageContent, pageable, allUsers.size());
    }


    public List<User> searchUsersByName(String name) {
        log.debug("Searching users by name: {}", name);
        return userRepository.findAll().stream().filter(u -> u.getName().toLowerCase().contains(name.toLowerCase())).toList();
    }


    public List<User> findUsersByEmailDomain(String domain) {
        log.debug("Finding users by email domain: {}", domain);
        return userRepository.findAll().stream().filter(u -> u.getEmail().toLowerCase().endsWith(domain.toLowerCase())).toList();
    }


    public List<User> findUsersWithPhone() {
        log.debug("Finding users with phone number");
        return userRepository.findAll().stream().filter(u -> u.getPhone() != null && !u.getPhone().trim().isEmpty()).toList();
    }


    public List<User> findUsersCreatedAfter(LocalDateTime date) {
        log.debug("Finding users created after: {}", date);
        return userRepository.findAll().stream().filter(u -> u.getCreatedAt().isAfter(date)).toList();
    }


    public long countUsersByRole(UserRole role) {
        log.debug("Counting users by role: {}", role);
        return userRepository.findByRole(role).size();
    }


    public long countActiveUsersByRole(UserRole role) {
        log.debug("Counting active users by role: {}", role);
        return findActiveUsersByRole(role).size();
    }


    @Transactional
    public User updateUser(User user) {
        log.debug("Updating user: {}", user.getEmail());
        return userRepository.save(user);
    }


    @Transactional
    public void deleteUser(UUID id) {
        log.debug("Deleting user with ID: {}", id);
        userRepository.deleteById(id);
    }


    @Transactional
    public User activateUser(UUID id) {
        log.debug("Activating user: {}", id);
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found with ID: " + id));
        user.setIsActive(true);
        return userRepository.save(user);
    }


    @Transactional
    public User deactivateUser(UUID id) {
        log.debug("Deactivating user: {}", id);
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found with ID: " + id));
        user.setIsActive(false);
        return userRepository.save(user);
    }


    @Transactional
    public User changeUserRole(UUID id, UserRole role) {
        log.debug("Changing user role: {} to {}", id, role);
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found with ID: " + id));
        user.setRole(role);
        return userRepository.save(user);
    }


    @Transactional
    public User updateUserPassword(UUID id, String passwordHash) {
        log.debug("Updating user password: {}", id);
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found with ID: " + id));
        user.setPasswordHash(passwordHash);
        return userRepository.save(user);
    }


    @Transactional
    public User updateUserProfile(UUID id, String name, String phone) {
        log.debug("Updating user profile: {}", id);
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found with ID: " + id));
        user.setName(name);
        user.setPhone(phone);
        return userRepository.save(user);
    }


    @Transactional
    public User createUser(String email, String passwordHash, String name, UserRole role) {
        log.debug("Creating new user: {}", email);

        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("User with email " + email + " already exists");
        }

        User user = User.builder().email(email).passwordHash(passwordHash).name(name).role(role).isActive(true).build();

        return userRepository.save(user);
    }

    public boolean validateUserCredentials(String email, String passwordHash) {
        log.debug("Validating user credentials for: {}", email);
        Optional<User> user = userRepository.findByEmail(email);
        return user.isPresent() && user.get().getPasswordHash().equals(passwordHash) && user.get().getIsActive();
    }


    public UserStatistics getUserStatistics() {
        log.debug("Getting user statistics");
        long totalUsers = userRepository.count();
        long activeUsers = countActiveUsersByRole(UserRole.USER);
        long totalAgents = countActiveUsersByRole(UserRole.AGENT);
        long totalAdmins = countActiveUsersByRole(UserRole.ADMIN);

        return new UserStatistics(totalUsers, activeUsers, totalAgents, totalAdmins);
    }


    public static class UserStatistics {
        private final long totalUsers;
        private final long activeUsers;
        private final long totalAgents;
        private final long totalAdmins;

        public UserStatistics(long totalUsers, long activeUsers, long totalAgents, long totalAdmins) {
            this.totalUsers = totalUsers;
            this.activeUsers = activeUsers;
            this.totalAgents = totalAgents;
            this.totalAdmins = totalAdmins;
        }

        // Getters
        public long getTotalUsers() { return totalUsers; }
        public long getActiveUsers() { return activeUsers; }
        public long getTotalAgents() { return totalAgents; }
        public long getTotalAdmins() { return totalAdmins; }
    }
}




