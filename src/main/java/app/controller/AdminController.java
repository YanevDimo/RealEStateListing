package app.controller;

import app.entity.Inquiry;
import app.entity.InquiryStatus;
import app.entity.UserRole;
import app.service.InquiryService;
import app.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final UserService userService;
    private final InquiryService inquiryService;

    @GetMapping("/dashboard")
    public ModelAndView adminDashboard(Authentication authentication) {
        log.debug("Loading admin dashboard");
        ModelAndView modelAndView = new ModelAndView("admin/dashboard");
        
        try {
            UserService.UserStatistics statistics = userService.getUserStatistics();
            modelAndView.addObject("statistics", statistics);
            modelAndView.addObject("totalUsers", statistics.getTotalUsers());
            modelAndView.addObject("activeUsers", statistics.getActiveUsers());
            modelAndView.addObject("totalAgents", statistics.getTotalAgents());
            modelAndView.addObject("totalAdmins", statistics.getTotalAdmins());
        } catch (Exception e) {
            log.error("Error loading admin dashboard", e);
            modelAndView.addObject("error", "Error loading dashboard statistics");
        }
        
        return modelAndView;
    }

    @GetMapping("/users")
    public ModelAndView listUsers() {
        log.debug("Loading users list for admin");
        ModelAndView modelAndView = new ModelAndView("admin/users");
        
        try {
            modelAndView.addObject("users", userService.findAllUsers());
            modelAndView.addObject("roles", UserRole.values());
        } catch (Exception e) {
            log.error("Error loading users list", e);
            modelAndView.addObject("error", "Error loading users");
        }
        
        return modelAndView;
    }

    @PostMapping("/users/{id}/role")
    public String changeUserRole(@PathVariable UUID id,
                                @RequestParam String role,
                                RedirectAttributes redirectAttributes) {
        log.info("Changing user role: {} to {}", id, role);
        
        try {
            UserRole userRole = UserRole.valueOf(role.toUpperCase());
            userService.changeUserRole(id, userRole);
            log.info("User role changed successfully: {} to {}", id, role);
            redirectAttributes.addFlashAttribute("successMessage", 
                    "User role changed successfully to " + role);
        } catch (IllegalArgumentException e) {
            log.error("Invalid role: {}", role, e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                    "Invalid role: " + role);
        } catch (Exception e) {
            log.error("Error changing user role: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                    "Failed to change user role: " + e.getMessage());
        }
        
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/activate")
    public String activateUser(@PathVariable UUID id,
                              RedirectAttributes redirectAttributes) {
        log.info("Activating user: {}", id);
        
        try {
            userService.activateUser(id);
            log.info("User activated successfully: {}", id);
            redirectAttributes.addFlashAttribute("successMessage", 
                    "User activated successfully");
        } catch (Exception e) {
            log.error("Error activating user: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                    "Failed to activate user: " + e.getMessage());
        }
        
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/deactivate")
    public String deactivateUser(@PathVariable UUID id,
                                RedirectAttributes redirectAttributes) {
        log.info("Deactivating user: {}", id);
        
        try {
            userService.deactivateUser(id);
            log.info("User deactivated successfully: {}", id);
            redirectAttributes.addFlashAttribute("successMessage", 
                    "User deactivated successfully");
        } catch (Exception e) {
            log.error("Error deactivating user: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                    "Failed to deactivate user: " + e.getMessage());
        }
        
        return "redirect:/admin/users";
    }

    @GetMapping("/inquiries")
    public ModelAndView listInquiries() {
        log.debug("Loading inquiries list for admin");
        ModelAndView modelAndView = new ModelAndView("admin/inquiries");
        
        try {
            modelAndView.addObject("inquiries", inquiryService.findAllInquiries());
            modelAndView.addObject("statuses", InquiryStatus.values());
            modelAndView.addObject("totalInquiries", inquiryService.findAllInquiries().size());
            modelAndView.addObject("newInquiries", inquiryService.findInquiriesByStatus(InquiryStatus.NEW).size());
        } catch (Exception e) {
            log.error("Error loading inquiries list", e);
            modelAndView.addObject("error", "Error loading inquiries");
        }
        
        return modelAndView;
    }

    @GetMapping("/inquiries/{id}")
    public ModelAndView viewInquiry(@PathVariable java.util.UUID id) {
        log.debug("Loading inquiry detail: {}", id);
        ModelAndView modelAndView = new ModelAndView("admin/inquiry-detail");
        
        try {
            Inquiry inquiry = inquiryService.findInquiryById(id)
                    .orElseThrow(() -> new RuntimeException("Inquiry not found"));
            
            modelAndView.addObject("inquiry", inquiry);
            modelAndView.addObject("statuses", InquiryStatus.values());
        } catch (Exception e) {
            log.error("Error loading inquiry detail", e);
            modelAndView.addObject("error", "Error loading inquiry: " + e.getMessage());
        }
        
        return modelAndView;
    }
}

