package com.quandrix.ms_notifications.controller;

import com.quandrix.ms_notifications.dto.NotificationRequest;
import com.quandrix.ms_notifications.dto.NotificationResponse;
import com.quandrix.ms_notifications.service.NotificationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    public ResponseEntity<NotificationResponse> create(@Valid @RequestBody NotificationRequest request) {
        log.info("POST /notifications userId={}", request.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationService.create(request));
    }

    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<Map<String, Long>> getUnread(@PathVariable Long userId) {
        log.info("GET /notifications/user/{}/unread/count", userId);
        return ResponseEntity.ok(Map.of("unread", notificationService.countUnread(userId)));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<NotificationResponse>> getByUser(@PathVariable Long userId) {
        log.info("GET /notifications/user/{}", userId);
        return ResponseEntity.ok(notificationService.getByUser(userId));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable Long id) {
        log.info("PATCH /notifications/{}/read", id);
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("DELETE /notifications/{}", id);
        notificationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
