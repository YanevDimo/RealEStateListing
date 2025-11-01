package app.service;


import app.entity.Agent;
import app.entity.User;
import app.repository.AgentRepository;
import app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AgentService {

    private final AgentRepository agentRepository;
    private final UserRepository userRepository;


    public List<Agent> findAllAgents() {
        log.debug("Finding all agents");
        return agentRepository.findAll();
    }

    public long countAllAgents() {
        log.debug("Counting all agents");
        return agentRepository.count();
    }

    @Transactional
    public Agent saveAgent(Agent agent) {
        log.debug("Saving agent: {}", agent.getAgentName());
        return agentRepository.save(agent);
    }

    @Transactional
    public void incrementAgentListings(UUID agentId) {
        log.debug("Incrementing listings for agent ID: {}", agentId);
        agentRepository.findById(agentId).ifPresent(agent -> {
            agent.setTotalListings(agent.getTotalListings() + 1);
            agentRepository.save(agent);
        });
    }



    public Optional<Agent> findAgentById(UUID id) {
        log.debug("Finding agent by ID: {}", id);
        return agentRepository.findById(id);
    }


    public Optional<Agent> findAgentByUserId(UUID userId) {
        log.debug("Finding agent by user ID: {}", userId);
        return agentRepository.findByUserId(userId);
    }


    public Optional<Agent> findAgentByLicenseNumber(String licenseNumber) {
        log.debug("Finding agent by license number: {}", licenseNumber);
        return agentRepository.findByLicenseNumber(licenseNumber);
    }


    public boolean licenseNumberExists(String licenseNumber) {
        log.debug("Checking if license number exists: {}", licenseNumber);
        return agentRepository.existsByLicenseNumber(licenseNumber);
    }


    public List<Agent> findAgentsByExperience(Integer experienceYears) {
        log.debug("Finding agents by experience: {} years", experienceYears);
        return agentRepository.findAll().stream()
                .filter(a -> a.getExperienceYears() >= experienceYears)
                .toList();
    }


    public List<Agent> findAgentsByRating(BigDecimal rating) {
        log.debug("Finding agents by rating: {}", rating);
        return agentRepository.findAll().stream()
                .filter(a -> a.getRating().compareTo(rating) >= 0)
                .toList();
    }


    public List<Agent> findTopRatedAgents(Pageable pageable) {
        log.debug("Finding top-rated agents with pagination: {}", pageable);
        // Sort agents by rating in descending order and apply pagination
        List<Agent> allAgents = agentRepository.findAll().stream()
                .sorted((a, b) -> b.getRating().compareTo(a.getRating()))
                .toList();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allAgents.size());
        return allAgents.subList(start, end);
    }


    public List<Agent> findAgentsByExperienceRange(Integer minExperience, Integer maxExperience) {
        log.debug("Finding agents by experience range: {} - {} years", minExperience, maxExperience);
        return agentRepository.findAll().stream()
                .filter(a -> a.getExperienceYears() >= minExperience && a.getExperienceYears() <= maxExperience)
                .toList();
    }


    public List<Agent> findAgentsByRatingRange(BigDecimal minRating, BigDecimal maxRating) {
        log.debug("Finding agents by rating range: {} - {}", minRating, maxRating);
        return agentRepository.findAll().stream()
                .filter(a -> a.getRating().compareTo(minRating) >= 0 && a.getRating().compareTo(maxRating) <= 0)
                .toList();
    }


    public List<Agent> findAgentsByMostListings(Pageable pageable) {
        log.debug("Finding agents by most listings with pagination: {}", pageable);
        // Sort agents by total listings in descending order and apply pagination
        List<Agent> allAgents = agentRepository.findAll().stream()
                .sorted((a, b) -> Integer.compare(b.getTotalListings(), a.getTotalListings()))
                .toList();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allAgents.size());
        return allAgents.subList(start, end);
    }


    public List<Agent> findAgentsBySpecialization(String specialization) {
        log.debug("Finding agents by specialization: {}", specialization);
        return agentRepository.findAll().stream()
                .filter(a -> a.getSpecializations() != null && a.getSpecializations().contains(specialization))
                .toList();
    }


    public List<Agent> searchAgentsByName(String name) {
        log.debug("Searching agents by name: {}", name);
        return agentRepository.findAll().stream()
                .filter(a -> a.getUser() != null && a.getUser().getName().toLowerCase().contains(name.toLowerCase()))
                .toList();
    }


    public List<Agent> searchAgentsByEmail(String email) {
        log.debug("Searching agents by email: {}", email);
        return agentRepository.findAll().stream()
                .filter(a -> a.getUser() != null && a.getUser().getEmail().toLowerCase().contains(email.toLowerCase()))
                .toList();
    }


    public List<Agent> findAgentsWithActiveProperties() {
        log.debug("Finding agents with active properties");
        return agentRepository.findAll().stream()
                .filter(a -> a.getProperties() != null && !a.getProperties().isEmpty())
                .toList();
    }


    public List<Agent> findAgentsWithBio() {
        log.debug("Finding agents with bio");
        return agentRepository.findAll().stream()
                .filter(a -> a.getBio() != null && !a.getBio().trim().isEmpty())
                .toList();
    }


    public List<Agent> findAgentsCreatedAfter(LocalDateTime date) {
        log.debug("Finding agents created after: {}", date);
        return agentRepository.findAll().stream()
                .filter(a -> a.getCreatedAt().isAfter(date))
                .toList();
    }


    public long countAgentsByExperience(Integer experienceYears) {
        log.debug("Counting agents by experience: {} years", experienceYears);
        return agentRepository.findAll().stream()
                .filter(a -> a.getExperienceYears() >= experienceYears)
                .count();
    }


    public long countAgentsByRating(BigDecimal rating) {
        log.debug("Counting agents by rating: {}", rating);
        return agentRepository.findAll().stream()
                .filter(a -> a.getRating().compareTo(rating) >= 0)
                .count();
    }


    public long countActivePropertiesByAgent(UUID agentId) {
        log.debug("Counting active properties by agent: {}", agentId);
        return agentRepository.findById(agentId)
                .map(agent -> agent.getProperties() != null ? agent.getProperties().size() : 0)
                .orElse(0);
    }


    public BigDecimal getAverageRating() {
        log.debug("Getting average rating of all agents");
        return agentRepository.findAll().stream()
                .map(Agent::getRating)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(agentRepository.count()), 2, RoundingMode.HALF_UP);
    }


    public Long getTotalListings() {
        log.debug("Getting total number of listings by all agents");
        return agentRepository.findAll().stream()
                .mapToLong(Agent::getTotalListings)
                .sum();
    }


    @Transactional
    public Agent updateAgent(Agent agent) {
        log.debug("Updating agent: {}", agent.getAgentName());
        return agentRepository.save(agent);
    }


    @Transactional
    public void deleteAgent(UUID id) {
        log.debug("Deleting agent with ID: {}", id);
        agentRepository.deleteById(id);
    }


    @Transactional
    public Agent createAgent(UUID userId, String licenseNumber, String bio, 
                           Integer experienceYears, String specializations) {
        log.debug("Creating new agent for user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        if (agentRepository.existsByUserId(userId)) {
            throw new RuntimeException("Agent already exists for user: " + userId);
        }

        if (licenseNumber != null && agentRepository.existsByLicenseNumber(licenseNumber)) {
            throw new RuntimeException("License number already exists: " + licenseNumber);
        }

        Agent agent = Agent.builder()
                .user(user)
                .licenseNumber(licenseNumber)
                .bio(bio)
                .experienceYears(experienceYears != null ? experienceYears : 0)
                .specializations(specializations)
                .rating(BigDecimal.ZERO)
                .totalListings(0)
                .build();

        return agentRepository.save(agent);
    }


    @Transactional
    public Agent updateAgentRating(UUID agentId, BigDecimal rating) {
        log.debug("Updating agent rating: {} to {}", agentId, rating);
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent not found with ID: " + agentId));
        agent.setRating(rating);
        return agentRepository.save(agent);
    }


    @Transactional
    public Agent updateAgentExperience(UUID agentId, Integer experienceYears) {
        log.debug("Updating agent experience: {} to {} years", agentId, experienceYears);
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent not found with ID: " + agentId));
        agent.setExperienceYears(experienceYears);
        return agentRepository.save(agent);
    }


    @Transactional
    public Agent updateAgentBio(UUID agentId, String bio) {
        log.debug("Updating agent bio: {}", agentId);
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent not found with ID: " + agentId));
        agent.setBio(bio);
        return agentRepository.save(agent);
    }


    @Transactional
    public Agent updateAgentSpecializations(UUID agentId, String specializations) {
        log.debug("Updating agent specializations: {}", agentId);
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent not found with ID: " + agentId));
        agent.setSpecializations(specializations);
        return agentRepository.save(agent);
    }


    @Transactional
    public Agent decrementAgentListings(UUID agentId) {
        log.debug("Decrementing agent listings count: {}", agentId);
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent not found with ID: " + agentId));
        agent.setTotalListings(Math.max(0, agent.getTotalListings() - 1));
        return agentRepository.save(agent);
    }


    public AgentStatistics getAgentStatistics() {
        log.debug("Getting agent statistics");
        long totalAgents = agentRepository.count();
        BigDecimal averageRating = getAverageRating();
        Long totalListings = getTotalListings();
        
        return new AgentStatistics(totalAgents, averageRating, totalListings);
    }



//      Recalculate agent ratings based on recent activity.
//      Called by scheduled job.

    @Transactional
    public int recalculateAgentRatings() {
        log.debug("Recalculating agent ratings");
        List<Agent> agents = agentRepository.findAll();
        int updatedCount = 0;
        
        for (Agent agent : agents) {
            // Simple rating calculation based on total listings and experience
           
            if (agent.getTotalListings() != null && agent.getTotalListings() > 0) {
                // Base rating on listings and experience
                BigDecimal newRating = BigDecimal.valueOf(
                    Math.min(5.0, 
                        3.0 + (agent.getTotalListings() * 0.1) + 
                        (agent.getExperienceYears() != null ? agent.getExperienceYears() * 0.05 : 0)
                    )
                );
                
                if (agent.getRating() == null || !agent.getRating().equals(newRating)) {
                    agent.setRating(newRating);
                    agentRepository.save(agent);
                    updatedCount++;
                }
            }
        }
        
        log.info("Recalculated ratings for {} agents", updatedCount);
        return updatedCount;
    }

    public static class AgentStatistics {
        private final long totalAgents;
        private final BigDecimal averageRating;
        private final Long totalListings;

        public AgentStatistics(long totalAgents, BigDecimal averageRating, Long totalListings) {
            this.totalAgents = totalAgents;
            this.averageRating = averageRating;
            this.totalListings = totalListings;
        }

        public long getTotalAgents() { return totalAgents; }
        public BigDecimal getAverageRating() { return averageRating; }
        public Long getTotalListings() { return totalListings; }
    }
}