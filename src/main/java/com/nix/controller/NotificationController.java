package com.nix.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.nix.models.Notification;
import com.nix.models.User;
import com.nix.service.NotificationService;
import com.nix.service.UserService;

@RestController
public class NotificationController {
	@Autowired
    private NotificationService notificationService;
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/notifications")
    public ResponseEntity<List<Notification>> getUserNotifications(@RequestHeader("Authorization") String jwt) {
        User user = userService.findUserByJwt(jwt);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        List<Notification> notifications = notificationService.getUserNotifications(user);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get unread notifications for the authenticated user.
     */
    @GetMapping("/notifications/unread")
    public ResponseEntity<List<Notification>> getUnreadNotifications(@RequestHeader("Authorization") String jwt) {
        User user = userService.findUserByJwt(jwt);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        List<Notification> unreadNotifications = notificationService.getUnreadNotifications(user);
        return ResponseEntity.ok(unreadNotifications);
    }

    /**
     * Mark a notification as read.
     */
    @PutMapping("/notifications/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id, @RequestHeader("Authorization") String jwt) {
        User user = userService.findUserByJwt(jwt);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
                
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Endpoint for admins to create global announcements.
     */
    @PostMapping("/admin/notifications/announce")
    public ResponseEntity<Void> createGlobalAnnouncement(@RequestHeader("Authorization") String jwt,
                                                          @RequestBody String message) {
        User admin = userService.findUserByJwt(jwt);
        if (admin == null || !admin.getRole().getName().equals("ADMIN")) {
            return ResponseEntity.status(403).build();
        }
        
        notificationService.createGlobalAnnouncement(message);
        return ResponseEntity.ok().build();
    }
}
