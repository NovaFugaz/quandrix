package com.quandrix.ms_notifications.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quandrix.ms_notifications.dto.NotificationRequest;
import com.quandrix.ms_notifications.dto.NotificationResponse;
import com.quandrix.ms_notifications.exception.InvalidNotificationTypeException;
import com.quandrix.ms_notifications.exception.NotificationNotFoundException;
import com.quandrix.ms_notifications.model.NotificationType;
import com.quandrix.ms_notifications.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void create_conDatosValidos_retorna201ConNotificacionCreada() throws Exception {
        // ARRANGE
        NotificationRequest request = new NotificationRequest(
                1L, "LISTING_SOLD", "Su publicación ha sido vendida");

        NotificationResponse fakeResponse = new NotificationResponse(
                1L, 1L, NotificationType.LISTING_SOLD,
                "Su publicación ha sido vendida", false,
                LocalDateTime.of(2026, 6, 12, 14, 30));

        when(notificationService.create(any(NotificationRequest.class))).thenReturn(fakeResponse);

        // ACT + ASSERT
        mockMvc.perform(post("/notifications")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.type").value("LISTING_SOLD"))
                .andExpect(jsonPath("$.message").value("Su publicación ha sido vendida"))
                .andExpect(jsonPath("$.readFlag").value(false));

        // VERIFY
        verify(notificationService, times(1)).create(any(NotificationRequest.class));
    }


// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 201 Created con "readFlag": false (toda notificación
// nueva debe nacer como no leída)
// Se obtuvo: HTTP 201 Created con "readFlag": true
// Esto podría pasar si alguien modifica el @PrePersist de Notification
// y cambia "this.readFlag = false" por "this.readFlag = true" por error,
// haciendo que las notificaciones nuevas aparezcan como ya leídas,
// ocultándolas silenciosamente del conteo de no leídas que ve el usuario.

    @Test
    void create_conTipoInvalido_retorna400ConMensajeDeError() throws Exception {
        // ARRANGE
        NotificationRequest request = new NotificationRequest(
                1L, "TIPO_INEXISTENTE", "Algún mensaje");

        when(notificationService.create(any(NotificationRequest.class)))
                .thenThrow(new InvalidNotificationTypeException("TIPO_INEXISTENTE"));

        // ACT + ASSERT: verificamos el mensaje EXACTO tal como lo genera
        // la excepción real (incluyendo los nombres en español que, como
        // ya notamos, no coinciden con los valores reales del enum en
        // inglés — esto documenta el comportamiento actual, no lo corrige).
        mockMvc.perform(post("/notifications")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        "Tipo de notificación invalido: TIPO_INEXISTENTE. " +
                                "Valores válidos: NUEVA_ORDEN, PAGO_CONFIRMADO, " +
                                "PUBLICACION_VENDIDA, RESEÑA_RECIBIDA, ORDEN_CANCELADA"));

        // VERIFY
        verify(notificationService, times(1)).create(any(NotificationRequest.class));
    }

// CASO HIPOTÉTICO DE FALLA (para QA) — BUG REAL YA IDENTIFICADO:
// Se esperaba (para ser útil al usuario de la API): un mensaje de error
// que liste los valores REALES y válidos del enum NotificationType
// (NEW_ORDER, PAYMENT_CONFIRMED, LISTING_SOLD, REVIEW_RECEIVED,
// ORDER_CANCELLED)
// Se obtuvo: un mensaje con nombres en español (NUEVA_ORDEN,
// PAGO_CONFIRMADO, PUBLICACION_VENDIDA, RESEÑA_RECIBIDA, ORDEN_CANCELADA)
// que NO corresponden a ningún valor aceptado realmente por
// NotificationType.valueOf(...) — un desarrollador que reciba este
// error e intente corregir su request usando exactamente esos nombres
// en español seguiría recibiendo el mismo error 400, ya que esos
// valores tampoco existen en el enum real.
// Esto es un defecto real en InvalidNotificationTypeException que
// vale la pena reportar y corregir (actualizar el mensaje para que
// liste los valores reales del enum en inglés), independientemente
// de que el test documente el comportamiento actual mientras no se
// corrija.

    @Test
    void getByUser_conUserIdExistente_retorna200ConSusNotificaciones() throws Exception {
        // ARRANGE
        Long userId = 1L;
        NotificationResponse notif1 = new NotificationResponse(
                1L, userId, NotificationType.LISTING_SOLD,
                "Su publicación ha sido vendida", false,
                LocalDateTime.of(2026, 6, 12, 14, 30));
        NotificationResponse notif2 = new NotificationResponse(
                2L, userId, NotificationType.PAYMENT_CONFIRMED,
                "Su pago ha sido confirmado", true,
                LocalDateTime.of(2026, 6, 11, 9, 0));

        when(notificationService.getByUser(userId)).thenReturn(List.of(notif1, notif2));

        // ACT + ASSERT
        mockMvc.perform(get("/notifications/user/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value("LISTING_SOLD"))
                .andExpect(jsonPath("$[1].type").value("PAYMENT_CONFIRMED"));

        // VERIFY
        verify(notificationService, times(1)).getByUser(userId);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: una lista con TODAS las notificaciones del usuario
// (leídas y no leídas mezcladas, ya que este endpoint no filtra por
// readFlag, a diferencia de /unread)
// Se obtuvo: una lista que excluye las notificaciones ya leídas
// Esto podría pasar si alguien modifica NotificationService.getByUser()
// y reemplaza notificationRepository.findByUserId(userId) por el
// metodo findByUserIdAndReadFlag(userId, false) que usa getUnreadByUser(),
// mezclando por error la lógica de ambos metodos.

    @Test
    void getByUser_sinNotificaciones_retorna200ConListaVacia() throws Exception {
        // ARRANGE: el usuario existe, pero no tiene ninguna notificación.
        // NOTA: el Swagger documenta un posible 404, pero el comportamiento
        // acordado (mismo criterio aplicado en ms-listings) es 200 OK +
        // lista vacía = "0 notificaciones", no un error.
        Long userId = 99L;

        when(notificationService.getByUser(userId)).thenReturn(List.of());

        // ACT + ASSERT
        mockMvc.perform(get("/notifications/user/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // VERIFY
        verify(notificationService, times(1)).getByUser(userId);
    }

// NOTA PARA QA (no es un caso de falla, es una aclaración de contrato):
// 200 OK + lista vacía representa "el usuario no tiene notificaciones",
// no un error del sistema. Si el equipo decide alinear esto con el
// Swagger (404), este test debe actualizarse junto con ese cambio.

    @Test
    void getUnread_conNotificacionesNoLeidas_retorna200ConLista() throws Exception {
        // ARRANGE
        Long userId = 1L;
        NotificationResponse notifNoLeida = new NotificationResponse(
                1L, userId, NotificationType.LISTING_SOLD,
                "Su publicación ha sido vendida", false,
                LocalDateTime.of(2026, 6, 12, 14, 30));

        when(notificationService.getUnreadByUser(userId)).thenReturn(List.of(notifNoLeida));

        // ACT + ASSERT
        mockMvc.perform(get("/notifications/user/{userId}/unread", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].readFlag").value(false));

        // VERIFY
        verify(notificationService, times(1)).getUnreadByUser(userId);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: solo notificaciones con readFlag=false
// Se obtuvo: la lista incluye también notificaciones con readFlag=true
// Esto podría pasar si alguien modifica el repositorio y usa
// findByUserId(...) en vez de findByUserIdAndReadFlag(userId, false),
// mostrando notificaciones ya leídas en una sección que el usuario
// espera ver solo como "pendientes".


    @Test
    void getUnread_sinNotificacionesNoLeidas_retorna200ConListaVacia() throws Exception {
        // ARRANGE: el usuario ya leyó todas sus notificaciones.
        // NOTA: mismo criterio de contrato que getByUser() — 200 OK +
        // lista vacía, no 404, a pesar de lo que sugiere el Swagger.
        Long userId = 1L;

        when(notificationService.getUnreadByUser(userId)).thenReturn(List.of());

        // ACT + ASSERT
        mockMvc.perform(get("/notifications/user/{userId}/unread", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // VERIFY
        verify(notificationService, times(1)).getUnreadByUser(userId);
    }

// NOTA PARA QA (no es un caso de falla, es una aclaración de contrato):
// 200 OK + lista vacía representa "el usuario no tiene notificaciones
// pendientes (las leyó todas, o nunca tuvo)", no un error del sistema.

    @Test
    void countUnread_conNotificacionesPendientes_retorna200ConElConteo() throws Exception {
        // ARRANGE
        Long userId = 1L;
        when(notificationService.countUnread(userId)).thenReturn(5L);

        // ACT + ASSERT: el body es un objeto simple {"unread": 5},
        // no un array ni un DTO con varios campos.
        mockMvc.perform(get("/notifications/user/{userId}/unread/count", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unread").value(5));

        // VERIFY
        verify(notificationService, times(1)).countUnread(userId);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: "unread": 5 cuando hay exactamente 5 notificaciones
// con readFlag=false para ese usuario
// Se obtuvo: "unread": 0 a pesar de haber notificaciones pendientes
// Esto podría pasar si alguien modifica NotificationService.countUnread()
// y cambia el segundo argumento de countByUserIdAndReadFlag(userId, false)
// a countByUserIdAndReadFlag(userId, true) por error, contando las
// notificaciones YA LEÍDAS en vez de las pendientes — el usuario
// vería "0 notificaciones nuevas" en su badge de la app a pesar de
// tener notificaciones reales sin revisar.


    @Test
    void countUnread_sinNotificacionesPendientes_retorna200ConConteoCero() throws Exception {
        // ARRANGE: a diferencia de getByUser/getUnread (donde "vacío" es
        // una lista vacía), aquí "sin pendientes" es simplemente el
        // número 0 — no hay ninguna discrepancia de Swagger que documentar,
        // porque un conteo de 0 es un valor numérico válido, no la ausencia
        // de un recurso.
        Long userId = 1L;
        when(notificationService.countUnread(userId)).thenReturn(0L);

        // ACT + ASSERT
        mockMvc.perform(get("/notifications/user/{userId}/unread/count", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unread").value(0));

        // VERIFY
        verify(notificationService, times(1)).countUnread(userId);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 200 OK con "unread": 0 (un conteo de cero es un
// resultado perfectamente válido, no un error)
// Se obtuvo: HTTP 404 Not Found cuando el conteo es cero
// Esto podría pasar si alguien agrega por error una validación del
// tipo "if (count == 0) throw new NotificationNotFoundException(...)"
// en NotificationService.countUnread(), confundiendo "no hay
// notificaciones pendientes" (un dato legítimo) con "el usuario no
// existe" (un error real) — son conceptos completamente distintos
// que no deberían compartir el mismo código de error.

    @Test
    void markAsRead_conIdExistente_retorna200ConNotificacionLeida() throws Exception {
        // ARRANGE
        Long id = 1L;
        NotificationResponse fakeResponse = new NotificationResponse(
                id, 1L, NotificationType.LISTING_SOLD,
                "Su publicación ha sido vendida", true, // ya leída
                LocalDateTime.of(2026, 6, 12, 14, 30));

        when(notificationService.markAsRead(id)).thenReturn(fakeResponse);

        // ACT + ASSERT
        mockMvc.perform(patch("/notifications/{id}/read", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readFlag").value(true));

        // VERIFY
        verify(notificationService, times(1)).markAsRead(id);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 200 OK con "readFlag": true después de marcar
// una notificación como leída
// Se obtuvo: HTTP 200 OK con "readFlag": false (sin cambios reales)
// Esto podría pasar si alguien olvida la línea
// notification.setReadFlag(true) antes de
// notificationRepository.save(notification) en
// NotificationService.markAsRead(), retornando la notificación
// "actualizada" pero sin que el cambio realmente se haya aplicado.

    @Test
    void markAsRead_conIdInexistente_retorna404NotFound() throws Exception {
        // ARRANGE
        Long idInexistente = 999L;

        when(notificationService.markAsRead(idInexistente))
                .thenThrow(new NotificationNotFoundException(idInexistente));

        // ACT + ASSERT
        mockMvc.perform(patch("/notifications/{id}/read", idInexistente))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(
                        "Notificación no encontrada con id: " + idInexistente));

        // VERIFY
        verify(notificationService, times(1)).markAsRead(idInexistente);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 404 Not Found al marcar como leída una
// notificación que no existe
// Se obtuvo: HTTP 500 Internal Server Error
// Esto podría pasar si alguien elimina el @ExceptionHandler de
// NotificationNotFoundException en GlobalExceptionHandler, dejando
// que caiga en el handler genérico de Exception.class.

    @Test
    void markAllAsRead_conUserIdValido_retorna204NoContent() throws Exception {
        // ARRANGE: markAllAsRead() es void — solo necesitamos que no
        // lance ninguna excepción.
        Long userId = 1L;

        doNothing().when(notificationService).markAllAsRead(userId);

        // ACT + ASSERT
        mockMvc.perform(patch("/notifications/user/{userId}/read-all", userId))
                .andExpect(status().isNoContent());

        // VERIFY
        verify(notificationService, times(1)).markAllAsRead(userId);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 204 No Content tanto si había notificaciones
// pendientes como si no (el comportamiento es el mismo en ambos casos,
// según el código real de NotificationService.markAllAsRead())
// Se obtuvo: HTTP 500 Internal Server Error cuando el usuario no
// tenía ninguna notificación pendiente
// Esto podría pasar si alguien agrega por error una validación que
// asuma que "lista vacía" es un caso anómalo (por ejemplo, lanzando
// una excepción si unread.isEmpty()), cuando en realidad el código
// actual maneja ese caso de forma silenciosa y exitosa (simplemente
// hace un "return" temprano sin hacer nada más).

    @Test
    void delete_conIdExistente_retorna204NoContent() throws Exception {
        // ARRANGE
        Long id = 1L;
        doNothing().when(notificationService).delete(id);

        // ACT + ASSERT
        mockMvc.perform(delete("/notifications/{id}", id))
                .andExpect(status().isNoContent());

        // VERIFY
        verify(notificationService, times(1)).delete(id);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 204 No Content al eliminar una notificación existente
// Se obtuvo: HTTP 404 Not Found a pesar de que el id sí existe
// Esto podría pasar si alguien invierte por error la lógica de
// existencia en NotificationService.delete(), lanzando
// NotificationNotFoundException precisamente cuando la notificación
// SÍ existe (mismo tipo de riesgo ya documentado en ms-listings).


    @Test
    void delete_conIdInexistente_retorna404NotFound() throws Exception {
        // ARRANGE
        Long idInexistente = 999L;
        doThrow(new NotificationNotFoundException(idInexistente))
                .when(notificationService).delete(idInexistente);

        // ACT + ASSERT
        mockMvc.perform(delete("/notifications/{id}", idInexistente))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(
                        "Notificación no encontrada con id: " + idInexistente));

        // VERIFY
        verify(notificationService, times(1)).delete(idInexistente);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 404 Not Found al eliminar una notificación inexistente
// Se obtuvo: HTTP 204 No Content (silenciosamente "exitoso")
// Esto podría pasar si alguien reemplaza la validación de existencia
// por un deleteById(id) directo de Spring Data JPA, que no lanza
// excepción si el id no existe — mismo riesgo ya documentado en
// ms-listings y ms-auth.

}