package com.quandrix.ms_notifications.service;

import com.quandrix.ms_notifications.dto.NotificationRequest;
import com.quandrix.ms_notifications.dto.NotificationResponse;
import com.quandrix.ms_notifications.exception.InvalidNotificationTypeException;
import com.quandrix.ms_notifications.exception.NotificationNotFoundException;
import com.quandrix.ms_notifications.model.Notification;
import com.quandrix.ms_notifications.model.NotificationType;
import com.quandrix.ms_notifications.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void create_conTipoValidoDirecto_persisteYRetornaNotificacion() {
        // ARRANGE: el tipo viene exactamente como el enum lo espera,
        // sin necesidad de normalización (mayúsculas, sin guiones/espacios).
        NotificationRequest request = new NotificationRequest(
                1L, "LISTING_SOLD", "Su publicación ha sido vendida");

        Notification guardada = new Notification();
        guardada.setId(1L);
        guardada.setUserId(1L);
        guardada.setType(NotificationType.LISTING_SOLD);
        guardada.setMessage("Su publicación ha sido vendida");
        guardada.setReadFlag(false);

        when(notificationRepository.save(any(Notification.class))).thenReturn(guardada);

        // ACT
        NotificationResponse response = notificationService.create(request);

        // ASSERT
        assertThat(response.getType()).isEqualTo(NotificationType.LISTING_SOLD);
        assertThat(response.isReadFlag()).isFalse();

        // VERIFY
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }


// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: el Notification guardado tiene type == LISTING_SOLD,
// correctamente parseado desde el String "LISTING_SOLD" del request
// Se obtuvo: una excepción inesperada al intentar parsear un tipo
// que en teoría es válido
// Esto podría pasar si alguien modifica la lógica de normalización
// en NotificationService.create() y rompe el caso "ya viene bien
// formado", por ejemplo aplicando una transformación que no debería
// afectar a un string que ya está en mayúsculas y sin separadores.


    @Test
    void create_conTipoConGuionesYMinusculas_normalizaYPersisteCorrectamente() {
        // ARRANGE: el tipo viene en minúsculas y con guiones, simulando
        // un cliente de la API que no respeta el formato exacto del enum
        // (por ejemplo, un frontend que envía "listing-sold" en vez de
        // "LISTING_SOLD").
        NotificationRequest request = new NotificationRequest(
                1L, "listing-sold", "Su publicación ha sido vendida");

        Notification guardada = new Notification();
        guardada.setId(1L);
        guardada.setUserId(1L);
        guardada.setType(NotificationType.LISTING_SOLD);
        guardada.setMessage("Su publicación ha sido vendida");
        guardada.setReadFlag(false);

        when(notificationRepository.save(any(Notification.class))).thenReturn(guardada);

        // ACT
        NotificationResponse response = notificationService.create(request);

        // ASSERT: a pesar de que el request decía "listing-sold" (minúsculas
        // y guion), el resultado debe ser el enum LISTING_SOLD correctamente
        // normalizado e interpretado.
        assertThat(response.getType()).isEqualTo(NotificationType.LISTING_SOLD);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: "listing-sold" se normaliza a "LISTING_SOLD" y se
// acepta correctamente (trim + replace("-", "_") + replace(" ", "_")
// + toUpperCase)
// Se obtuvo: InvalidNotificationTypeException, rechazando un tipo
// que en realidad debería ser válido tras la normalización
// Esto podría pasar si alguien modifica el orden de las operaciones
// de normalización en NotificationService.create() y, por ejemplo,
// aplica toUpperCase() ANTES de trim(), dejando espacios en blanco al
// inicio/final que rompan el parseo — o si elimina por error una de
// las líneas .replace(...), dejando guiones o espacios sin convertir
// a guion bajo antes de llegar a NotificationType.valueOf(...).

    @Test
    void create_conTipoConEspacios_normalizaYPersisteCorrectamente() {
        // ARRANGE: variante con espacios en vez de guiones, y mezcla de
        // mayúsculas/minúsculas — para confirmar que el trim()+replace()
        // +toUpperCase() cubre esta combinación también, no solo guiones.
        NotificationRequest request = new NotificationRequest(
                1L, "  Payment Confirmed  ", "Su pago ha sido confirmado");

        Notification guardada = new Notification();
        guardada.setId(1L);
        guardada.setUserId(1L);
        guardada.setType(NotificationType.PAYMENT_CONFIRMED);
        guardada.setMessage("Su pago ha sido confirmado");
        guardada.setReadFlag(false);

        when(notificationRepository.save(any(Notification.class))).thenReturn(guardada);

        // ACT
        NotificationResponse response = notificationService.create(request);

        // ASSERT
        assertThat(response.getType()).isEqualTo(NotificationType.PAYMENT_CONFIRMED);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: "  Payment Confirmed  " (con espacios al inicio/final
// Y espacio entre palabras, mezcla de mayúsculas/minúsculas) se
// normaliza correctamente a PAYMENT_CONFIRMED
// Se obtuvo: InvalidNotificationTypeException, fallando específicamente
// por los espacios sobrantes al inicio o final que el trim() debería
// haber eliminado
// Esto podría pasar si alguien elimina por error la llamada a
// .trim() en la cadena de normalización, dejando que
// " PAYMENT_CONFIRMED " (con espacios invisibles en los extremos)
// llegue a NotificationType.valueOf(...) y falle, ya que el enum
// no tolera espacios sobrantes a pesar de que el resto del string
// esté perfectamente formado.

    @Test
    void create_conTipoInvalido_lanzaInvalidNotificationTypeException() {
        // ARRANGE: un tipo que, ni siquiera después de la normalización,
        // corresponde a ningún valor del enum NotificationType.
        NotificationRequest request = new NotificationRequest(
                1L, "TIPO_QUE_NO_EXISTE", "Algún mensaje");

        // ACT + ASSERT
        InvalidNotificationTypeException ex = assertThrows(
                InvalidNotificationTypeException.class,
                () -> notificationService.create(request)
        );
        assertThat(ex.getMessage()).contains("TIPO_QUE_NO_EXISTE");

        // VERIFY: al fallar el parseo del tipo, nunca se debió intentar
        // persistir nada.
        verify(notificationRepository, never()).save(any());
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: InvalidNotificationTypeException cuando el tipo,
// incluso normalizado, no corresponde a ningún valor del enum
// Se obtuvo: notificationRepository.save(...) SÍ se invoca con un
// Notification que tiene type=null, persistiendo un registro corrupto
// en la base de datos (recordando que el campo type tiene
// "nullable = false" según el modelo Notification, así que esto
// fallaría además con una excepción de integridad de base de datos
// real, no solo un dato silenciosamente incorrecto)
// Esto podría pasar si alguien modifica ListingService.create() y
// mueve la construcción del objeto Notification ANTES del try/catch
// que valida el tipo, en vez de después.

    @Test
    void getByUser_conNotificacionesExistentes_retornaListaCorrecta() {
        // ARRANGE
        Long userId = 1L;
        Notification notif1 = new Notification();
        notif1.setId(1L);
        notif1.setUserId(userId);
        notif1.setType(NotificationType.LISTING_SOLD);
        notif1.setMessage("Su publicación ha sido vendida");
        notif1.setReadFlag(false);

        Notification notif2 = new Notification();
        notif2.setId(2L);
        notif2.setUserId(userId);
        notif2.setType(NotificationType.PAYMENT_CONFIRMED);
        notif2.setMessage("Su pago ha sido confirmado");
        notif2.setReadFlag(true);

        when(notificationRepository.findByUserId(userId)).thenReturn(List.of(notif1, notif2));

        // ACT
        List<NotificationResponse> response = notificationService.getByUser(userId);

        // ASSERT: confirma que incluye AMBAS, sin importar su readFlag
        // (este metodo no filtra por estado de lectura).
        assertThat(response).hasSize(2);
        assertThat(response.get(0).isReadFlag()).isFalse();
        assertThat(response.get(1).isReadFlag()).isTrue();
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: getByUser() retorna TODAS las notificaciones del
// usuario, sin filtrar por readFlag
// Se obtuvo: solo retorna las no leídas, excluyendo silenciosamente
// las que ya fueron leídas
// Esto podría pasar si alguien confunde este metodo con
// getUnreadByUser() y usa por error
// findByUserIdAndReadFlag(userId, false) en vez de findByUserId(userId).


    @Test
    void getByUser_sinNotificaciones_retornaListaVacia() {
        // ARRANGE
        Long userId = 99L;
        when(notificationRepository.findByUserId(userId)).thenReturn(List.of());

        // ACT
        List<NotificationResponse> response = notificationService.getByUser(userId);

        // ASSERT
        assertThat(response).isEmpty();
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: una lista vacía (no una excepción) cuando el usuario
// no tiene notificaciones
// Se obtuvo: una NullPointerException al intentar hacer
// .stream().map(...) sobre un resultado null
// Esto podría pasar si el repositorio retornara null en vez de una
// lista vacía en algún escenario edge — Spring Data JPA normalmente
// garantiza una lista vacía y no null para métodos que retornan
// List<T>, pero vale la pena que el test lo confirme explícitamente
// como comportamiento esperado del service.

    @Test
    void getUnreadByUser_conNotificacionesNoLeidas_retornaSoloEsas() {
        // ARRANGE
        Long userId = 1L;
        Notification notifNoLeida = new Notification();
        notifNoLeida.setId(1L);
        notifNoLeida.setUserId(userId);
        notifNoLeida.setType(NotificationType.LISTING_SOLD);
        notifNoLeida.setMessage("Su publicación ha sido vendida");
        notifNoLeida.setReadFlag(false);

        when(notificationRepository.findByUserIdAndReadFlag(userId, false))
                .thenReturn(List.of(notifNoLeida));

        // ACT
        List<NotificationResponse> response = notificationService.getUnreadByUser(userId);

        // ASSERT
        assertThat(response).hasSize(1);
        assertThat(response.getFirst().isReadFlag()).isFalse();
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: el repositorio se consulta específicamente con
// readFlag=false como segundo argumento
// Se obtuvo: se consulta con readFlag=true por error, retornando
// las notificaciones YA LEÍDAS en una sección que el usuario espera
// ver como "pendientes de revisar"
// Esto podría pasar si alguien invierte por error el valor booleano
// en findByUserIdAndReadFlag(userId, false) dentro de
// NotificationService.getUnreadByUser().


    @Test
    void getUnreadByUser_sinNotificacionesNoLeidas_retornaListaVacia() {
        // ARRANGE
        Long userId = 1L;
        when(notificationRepository.findByUserIdAndReadFlag(userId, false))
                .thenReturn(List.of());

        // ACT
        List<NotificationResponse> response = notificationService.getUnreadByUser(userId);

        // ASSERT
        assertThat(response).isEmpty();
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: una lista vacía cuando el usuario ya leyó todas sus
// notificaciones
// Se obtuvo: una excepción inesperada en vez de una lista vacía
// Mismo patrón de riesgo que getByUser() sin resultados.

    @Test
    void countUnread_retornaElConteoDelRepositorio() {
        // ARRANGE: este metodo no tiene lógica condicional propia —
        // simplemente delega el conteo al repositorio y lo retorna.
        Long userId = 1L;
        when(notificationRepository.countByUserIdAndReadFlag(userId, false)).thenReturn(5L);

        // ACT
        long count = notificationService.countUnread(userId);

        // ASSERT
        assertThat(count).isEqualTo(5L);

        // VERIFY: confirmamos que se consultó específicamente con
        // readFlag=false (no con true, que contaría las ya leídas).
        verify(notificationRepository, times(1)).countByUserIdAndReadFlag(userId, false);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: countUnread() retorna exactamente el valor que entrega
// el repositorio para countByUserIdAndReadFlag(userId, false)
// Se obtuvo: un valor distinto al que retorna el repositorio (por
// ejemplo, siempre 0, o el conteo de TODAS las notificaciones sin
// filtrar por readFlag)
// Esto podría pasar si alguien modifica countUnread() y cambia el
// segundo argumento a "true" por error, o si reemplaza la llamada
// por un count() genérico sobre todas las notificaciones del usuario
// sin filtrar por estado de lectura.

    @Test
    void markAsRead_conNotificacionNoLeida_laMarcaComoLeida() {
        // ARRANGE: la notificación existe y aún NO está leída.
        Long id = 1L;
        Notification notification = new Notification();
        notification.setId(id);
        notification.setUserId(1L);
        notification.setType(NotificationType.LISTING_SOLD);
        notification.setMessage("Su publicación ha sido vendida");
        notification.setReadFlag(false);

        when(notificationRepository.findById(id)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        // ACT
        NotificationResponse response = notificationService.markAsRead(id);

        // ASSERT
        assertThat(response.isReadFlag()).isTrue();

        // VERIFY: como NO estaba leída, SÍ se debió llamar a save().
        verify(notificationRepository, times(1)).save(notification);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: al marcar como leída una notificación que estaba en
// readFlag=false, el resultado final tiene readFlag=true
// Se obtuvo: el resultado sigue con readFlag=false a pesar de haber
// llamado al endpoint
// Esto podría pasar si alguien olvida la línea
// notification.setReadFlag(true) antes de save() en
// NotificationService.markAsRead(), guardando el objeto sin haber
// aplicado realmente el cambio de estado.


    @Test
    void markAsRead_conNotificacionYaLeida_esIdempotenteYNoLlamaASave() {
        // ARRANGE: la notificación YA estaba marcada como leída.
        // Este es el caso más interesante de este metodo: el código
        // real tiene un "if (notification.isReadFlag()) return toResponse(...)"
        // ANTES de llegar a setReadFlag()/save() — es decir, si ya estaba
        // leída, retorna inmediatamente sin tocar la base de datos.
        Long id = 1L;
        Notification notificationYaLeida = new Notification();
        notificationYaLeida.setId(id);
        notificationYaLeida.setUserId(1L);
        notificationYaLeida.setType(NotificationType.LISTING_SOLD);
        notificationYaLeida.setMessage("Su publicación ha sido vendida");
        notificationYaLeida.setReadFlag(true); // ya estaba leída

        when(notificationRepository.findById(id)).thenReturn(Optional.of(notificationYaLeida));

        // ACT
        NotificationResponse response = notificationService.markAsRead(id);

        // ASSERT: el resultado sigue siendo readFlag=true (sin cambios,
        // porque ya lo estaba).
        assertThat(response.isReadFlag()).isTrue();

        // VERIFY: lo más importante de este test — al ser idempotente,
        // NUNCA se debió llamar a save(), porque no había nada que
        // persistir de nuevo.
        verify(notificationRepository, never()).save(any());
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: markAsRead() sobre una notificación YA leída NO
// invoca save() (operación idempotente, sin escritura redundante
// en la base de datos)
// Se obtuvo: save() SÍ se invoca igual, haciendo una escritura
// innecesaria en la base de datos cada vez que alguien "re-marca"
// como leída una notificación que ya lo estaba
// Esto podría pasar si alguien elimina por error el "if
// (notification.isReadFlag()) return toResponse(notification)" en
// NotificationService.markAsRead(), perdiendo la optimización de
// idempotencia y generando escrituras redundantes innecesarias en
// la base de datos (un problema de rendimiento, no de corrección
// funcional, ya que el resultado visible para el usuario sería
// el mismo).


    @Test
    void markAsRead_conIdInexistente_lanzaNotificationNotFoundException() {
        // ARRANGE
        Long idInexistente = 999L;
        when(notificationRepository.findById(idInexistente)).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThrows(
                NotificationNotFoundException.class,
                () -> notificationService.markAsRead(idInexistente)
        );

        // VERIFY
        verify(notificationRepository, never()).save(any());
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: NotificationNotFoundException cuando el repositorio
// no encuentra la notificación (Optional vacío)
// Se obtuvo: NoSuchElementException sin capturar (HTTP 500 en vez
// de 404), mismo patrón de riesgo ya visto en todos los demás
// microservicios al reemplazar .orElseThrow(...) por un .get() directo.

    @Test
    void markAllAsRead_conNotificacionesPendientes_lasMarcaTodasComoLeidas() {
        // ARRANGE: el usuario tiene 2 notificaciones sin leer.
        Long userId = 1L;
        Notification notif1 = new Notification();
        notif1.setId(1L);
        notif1.setReadFlag(false);

        Notification notif2 = new Notification();
        notif2.setId(2L);
        notif2.setReadFlag(false);

        List<Notification> pendientes = List.of(notif1, notif2);
        when(notificationRepository.findByUserIdAndReadFlag(userId, false))
                .thenReturn(pendientes);

        // ACT
        notificationService.markAllAsRead(userId);

        // ASSERT: verificamos que AMBOS objetos en memoria fueron
        // modificados a readFlag=true antes de guardarlos.
        assertThat(notif1.isReadFlag()).isTrue();
        assertThat(notif2.isReadFlag()).isTrue();

        // VERIFY: se debió guardar la lista completa de una sola vez
        // (saveAll), no notificación por notificación.
        verify(notificationRepository, times(1)).saveAll(pendientes);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: TODAS las notificaciones pendientes del usuario
// terminan con readFlag=true después de llamar a este metodo
// Se obtuvo: solo ALGUNAS quedan marcadas como leídas (por ejemplo,
// si el forEach se interrumpe a medias, o si se aplica el cambio
// sobre una copia de la lista en vez de los objetos originales)
// Esto podría pasar si alguien modifica el
// "unread.forEach(notification -> notification.setReadFlag(true))"
// y lo reemplaza por una operación que no mute los objetos originales
// (por ejemplo, un .stream().map(...) que cree copias nuevas sin
// modificar las referencias que luego se pasan a saveAll()).


    @Test
    void markAllAsRead_sinNotificacionesPendientes_noLlamaASaveAll() {
        // ARRANGE: el usuario no tiene ninguna notificación sin leer.
        Long userId = 1L;
        when(notificationRepository.findByUserIdAndReadFlag(userId, false))
                .thenReturn(List.of());

        // ACT
        notificationService.markAllAsRead(userId);

        // VERIFY: al no haber nada que marcar, NUNCA se debió llamar a
        // saveAll() — el código real hace un "return" temprano apenas
        // detecta que la lista está vacía.
        verify(notificationRepository, never()).saveAll(any());
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: saveAll() NUNCA se invoca cuando no hay notificaciones
// pendientes (operación vacía, sin trabajo redundante)
// Se obtuvo: saveAll() SÍ se invoca igual con una lista vacía,
// generando una llamada innecesaria a la base de datos
// Esto podría pasar si alguien elimina por error el
// "if (unread.isEmpty()) { return; }" en
// NotificationService.markAllAsRead(), perdiendo esa pequeña
// optimización (aunque guardar una lista vacía con saveAll()
// generalmente no causaría un error real, sí sería una llamada
// completamente innecesaria a la base de datos).


    @Test
    void delete_conIdExistente_eliminaLaNotificacion() {
        // ARRANGE
        Long id = 1L;
        Notification notification = new Notification();
        notification.setId(id);

        when(notificationRepository.findById(id)).thenReturn(Optional.of(notification));

        // ACT
        notificationService.delete(id);

        // VERIFY
        verify(notificationRepository, times(1)).delete(notification);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: notificationRepository.delete(notification) se invoca
// con el objeto real obtenido de findById(...), confirmando primero
// que existe
// Se obtuvo: deleteById(id) se invoca directamente sin esa validación
// previa, perdiendo la garantía de existencia (mismo patrón de riesgo
// ya documentado en ms-listings: deleteById() de Spring Data JPA no
// lanza excepción si el id no existe).


    @Test
    void delete_conIdInexistente_lanzaNotificationNotFoundException() {
        // ARRANGE
        Long idInexistente = 999L;
        when(notificationRepository.findById(idInexistente)).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThrows(
                NotificationNotFoundException.class,
                () -> notificationService.delete(idInexistente)
        );

        // VERIFY
        verify(notificationRepository, never()).delete(any());
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: NotificationNotFoundException cuando el repositorio
// no encuentra la notificación a eliminar
// Se obtuvo: NoSuchElementException sin capturar (HTTP 500 en vez
// de 404)
// Mismo patrón de riesgo ya visto repetidamente en todos los
// microservicios: reemplazar .orElseThrow(...) por un .get() directo.
}