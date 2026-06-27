package com.quandrix.ms_notifications.service;

import com.quandrix.ms_notifications.dto.NotificationRequest;
import com.quandrix.ms_notifications.dto.NotificationResponse;
import com.quandrix.ms_notifications.exception.InvalidNotificationTypeException;
import com.quandrix.ms_notifications.exception.NotificationNotFoundException;
import com.quandrix.ms_notifications.model.Notification;
import com.quandrix.ms_notifications.model.NotificationType;
import com.quandrix.ms_notifications.repository.NotificationRepository;

import jakarta.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private static final Logger log =
            LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public NotificationResponse create(NotificationRequest request) {

        log.info("Creando notificación para userId: {} type: {}",
                request.getUserId(), request.getType());

        NotificationType type;

        try {
            String normalizedType = request.getType()
                    .trim()
                    .replace("-", "_")
                    .replace(" ", "_")
                    .toUpperCase();

            type = NotificationType.valueOf(normalizedType);

        } catch (IllegalArgumentException e) {

            log.warn("Tipo de notificación inválido: {}",
                    request.getType());

            throw new InvalidNotificationTypeException(
                    request.getType());
        }

        Notification notification = new Notification();

        notification.setUserId(request.getUserId());
        notification.setType(type);
        notification.setMessage(request.getMessage());

        Notification saved = notificationRepository.save(notification);

        log.info("Notificación creada id: {} para userId: {}",
                saved.getId(),
                saved.getUserId());

        return toResponse(saved);
    }

    public List<NotificationResponse> getByUser(Long userId) {

        log.info("Obteniendo todas las notificaciones de userId: {}",
                userId);

        return notificationRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<NotificationResponse> getUnreadByUser(Long userId) {

        log.info("Obteniendo notificaciones no leídas de userId: {}",
                userId);

        return notificationRepository
                .findByUserIdAndReadFlag(userId, false)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public long countUnread(Long userId) {

        long count =
                notificationRepository.countByUserIdAndReadFlag(
                        userId,
                        false
                );

        log.info("Notificaciones no leídas para userId: {}: {}",
                userId,
                count);

        return count;
    }

    /**
     * Marca una notificación como leída.
     * Si ya estaba marcada, no hace nada.
     */
    @Transactional
    public NotificationResponse markAsRead(Long id) {

        log.info("Marcando notificación id: {} como leída", id);

        Notification notification =
                notificationRepository.findById(id)
                        .orElseThrow(() -> {

                            log.warn(
                                    "Notificación no encontrada: {}",
                                    id
                            );

                            return new NotificationNotFoundException(id);
                        });

        if (notification.isReadFlag()) {

            log.info(
                    "Notificación id: {} ya estaba marcada como leída",
                    id
            );

            return toResponse(notification);
        }

        notification.setReadFlag(true);

        Notification updated =
                notificationRepository.save(notification);

        log.info("Notificación id: {} marcada como leída", id);

        return toResponse(updated);
    }

    /**
     * Marca todas las notificaciones no leídas
     * de un usuario como leídas.
     */
    @Transactional
    public void markAllAsRead(Long userId) {

        log.info(
                "Marcando todas las notificaciones de userId: {} como leídas",
                userId
        );

        List<Notification> unread =
                notificationRepository
                        .findByUserIdAndReadFlag(userId, false);

        if (unread.isEmpty()) {

            log.info(
                    "No hay notificaciones pendientes para userId: {}",
                    userId
            );

            return;
        }

        unread.forEach(notification ->
                notification.setReadFlag(true));

        notificationRepository.saveAll(unread);

        log.info(
                "Marcadas {} notificaciones como leídas para userId: {}",
                unread.size(),
                userId
        );
    }

    public void delete(Long id) {

        log.info("Eliminando notificación id: {}", id);

        Notification notification =
                notificationRepository.findById(id)
                        .orElseThrow(() -> {

                            log.warn(
                                    "Notificación no encontrada: {}",
                                    id
                            );

                            return new NotificationNotFoundException(id);
                        });

        notificationRepository.delete(notification);

        log.info("Notificación id: {} eliminada", id);
    }

    private NotificationResponse toResponse(Notification n) {

        return new NotificationResponse(
                n.getId(),
                n.getUserId(),
                n.getType(),
                n.getMessage(),
                n.isReadFlag(),
                n.getCreatedAt()
        );
    }
}