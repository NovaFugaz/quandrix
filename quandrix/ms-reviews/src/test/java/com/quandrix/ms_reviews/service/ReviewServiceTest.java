package com.quandrix.ms_reviews.service;

import com.quandrix.ms_reviews.client.NotificationClient;
import com.quandrix.ms_reviews.client.TransactionClient;
import com.quandrix.ms_reviews.dto.ReviewRequest;
import com.quandrix.ms_reviews.dto.ReviewResponse;
import com.quandrix.ms_reviews.dto.SellerRatingResponse;
import com.quandrix.ms_reviews.exception.ReviewAlreadyExistsException;
import com.quandrix.ms_reviews.exception.ReviewNotAllowedException;
import com.quandrix.ms_reviews.exception.ReviewNotFoundException;
import com.quandrix.ms_reviews.model.Review;
import com.quandrix.ms_reviews.repository.ReviewRepository;
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
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private TransactionClient transactionClient;

    @Mock
    private NotificationClient notificationClient;

    @InjectMocks
    private ReviewService reviewService;

    @Test
    void create_conDatosValidos_persisteYRetornaResena() {
        // ARRANGE
        ReviewRequest request = new ReviewRequest();
        request.setReviewerId(1L);
        request.setSellerId(2L);
        request.setRating(5);
        request.setComment("Excelente");

        when(transactionClient.existsCompletedTransaction(1L, 2L)).thenReturn(true);
        when(reviewRepository.existsByReviewerIdAndSellerId(1L, 2L)).thenReturn(false);

        Review guardada = new Review();
        guardada.setId(1L);
        guardada.setReviewerId(1L);
        guardada.setSellerId(2L);
        guardada.setRating(5);
        guardada.setComment("Excelente");
        when(reviewRepository.save(any(Review.class))).thenReturn(guardada);

        // ACT
        ReviewResponse response = reviewService.create(request);

        // ASSERT
        assertThat(response.getRating()).isEqualTo(5);
        assertThat(response.getSellerId()).isEqualTo(2L);

        // VERIFY: confirmamos que se notificó al vendedor.
        verify(notificationClient, times(1)).send(any());
    }


// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: notificationClient.send(...) se invoca con userId
// igual al SELLER (quien recibe la reseña), no al reviewer
// Se obtuvo: la notificación se envía al reviewer por error,
// avisándole a la persona equivocada sobre una reseña que ella misma
// escribió
// Esto podría pasar si alguien confunde request.getSellerId() con
// request.getReviewerId() al construir el NotificationRequest dentro
// de ReviewService.create().

    @Test
    void create_conRatingFueraDeRango_lanzaIllegalArgumentException() {
        // ARRANGE: rating=10, fuera del rango 1-5. Como estamos invocando
        // ReviewService.create() DIRECTAMENTE (sin pasar por el
        // controller ni por Bean Validation), este es el único punto
        // donde realmente podemos disparar la validación manual del
        // código y confirmar su comportamiento real.
        ReviewRequest request = new ReviewRequest();
        request.setReviewerId(1L);
        request.setSellerId(2L);
        request.setRating(10); // fuera de rango
        request.setComment("Buena carta");

        // ACT + ASSERT: el código real lanza IllegalArgumentException,
        // NO una excepción de negocio propia de este dominio (como
        // ReviewNotAllowedException). Esto es importante documentarlo,
        // porque GlobalExceptionHandler no tiene un @ExceptionHandler
        // específico para IllegalArgumentException — en un escenario
        // donde este código SÍ se alcanzara vía HTTP (lo cual no ocurre
        // normalmente, como confirmamos en el controller), terminaría
        // en un 500 Internal Server Error, no en un 400 Bad Request
        // como uno esperaría para un error de validación.
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> reviewService.create(request)
        );
        assertThat(ex.getMessage()).isEqualTo("Rating debe estar entre 1 y 5");

        // VERIFY: al fallar esta validación, ningún cliente externo
        // debió ser invocado.
        verify(transactionClient, never()).existsCompletedTransaction(any(), any());
        verify(reviewRepository, never()).save(any());
    }

// CASO HIPOTÉTICO DE FALLA (para QA) — BUG REAL YA IDENTIFICADO:
// Se esperaba (para ser coherente con el resto del sistema): una
// excepción de negocio propia (similar a InvalidListingException en
// ms-listings, o InvalidOrderException en ms-orders) que
// GlobalExceptionHandler pueda traducir a HTTP 400 Bad Request
// Se obtuvo: IllegalArgumentException, un tipo de excepción nativo
// de Java que NO tiene @ExceptionHandler dedicado en
// GlobalExceptionHandler, y que por tanto caería en el handler
// genérico de Exception.class, resultando en HTTP 500 Internal
// Server Error con el mensaje genérico "Error interno del servidor"
// — perdiendo completamente el mensaje útil "Rating debe estar
// entre 1 y 5" que el código sí generó internamente
// NOTA IMPORTANTE: en la práctica, vía la API HTTP normal, este
// código es inalcanzable porque @Min(1)/@Max(5) en ReviewRequest ya
// bloquea el request con 400 antes de llegar al service (confirmado
// en ReviewControllerTest). Sin embargo, sigue siendo un defecto de
// diseño real: si alguien invoca el service desde otro punto de
// entrada (un futuro listener de eventos, un job batch, una llamada
// directa desde otro service), o si las anotaciones de Bean
// Validation se eliminan del DTO por error, este código fallaría
// de forma confusa con un 500 en vez de un error de negocio claro.
// Vale la pena reportarlo como mejora: reemplazar
// "throw new IllegalArgumentException(...)" por una excepción de
// negocio propia con su @ExceptionHandler correspondiente, para
// que el sistema sea robusto independientemente de la capa de
// entrada que lo invoque.


    @Test
    void create_conReviewerIgualASeller_lanzaReviewNotAllowedException() {
        // ARRANGE: el reviewer intenta reseñarse a sí mismo.
        ReviewRequest request = new ReviewRequest();
        request.setReviewerId(1L);
        request.setSellerId(1L); // mismo id
        request.setRating(5);
        request.setComment("Me reseño a mí mismo");

        // ACT + ASSERT
        assertThrows(
                ReviewNotAllowedException.class,
                () -> reviewService.create(request)
        );

        // VERIFY: esta validación ocurre ANTES de consultar
        // transactionClient — no tiene sentido verificar una transacción
        // entre una persona y ella misma.
        verify(transactionClient, never()).existsCompletedTransaction(any(), any());
        verify(reviewRepository, never()).save(any());
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: ReviewNotAllowedException cuando reviewerId == sellerId
// Se obtuvo: la reseña se crea exitosamente, permitiendo que un
// vendedor se autoreseñe con 5 estrellas para inflar artificialmente
// su calificación promedio (visible en getSellerRating()) —
// un problema de integridad de la plataforma, similar en espíritu
// al de "comprar tu propio listing" que documentamos en ms-orders
// Esto podría pasar si alguien elimina la validación
// "if (request.getReviewerId().equals(request.getSellerId()))".


    @Test
    void create_sinTransaccionCompletada_lanzaReviewNotAllowedException() {
        // ARRANGE: no hay transacción completada entre ambos usuarios.
        ReviewRequest request = new ReviewRequest();
        request.setReviewerId(1L);
        request.setSellerId(2L);
        request.setRating(5);
        request.setComment("Buena carta");

        when(transactionClient.existsCompletedTransaction(1L, 2L)).thenReturn(false);

        // ACT + ASSERT
        assertThrows(
                ReviewNotAllowedException.class,
                () -> reviewService.create(request)
        );

        // VERIFY
        verify(reviewRepository, never()).save(any());
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: ReviewNotAllowedException cuando no existe ninguna
// transacción completada entre reviewer y seller — esta es la
// validación central de el microservicio: previene reseñas
// falsas de personas que nunca compraron realmente
// Se obtuvo: la reseña se crea sin haber verificado ninguna compra
// real, abriendo la puerta a reseñas fraudulentas (positivas para
// inflar reputación, o negativas de competidores maliciosos)
// Esto podría pasar si alguien elimina la validación "if
// (!hasTransaction)".


    @Test
    void create_conErrorDeFeignAlVerificarTransaccion_lanzaReviewNotAllowedException() {
        // ARRANGE: transactionClient lanza una excepción (por ejemplo,
        // ms-transactions caído) — el código real traduce CUALQUIER
        // error de esta llamada a ReviewNotAllowedException, una
        // decisión de diseño "fail-safe": si no podemos confirmar que
        // la compra ocurrió, asumimos que NO se puede reseñar (en vez
        // de asumir lo contrario, que sería más permisivo pero también
        // más riesgoso).
        ReviewRequest request = new ReviewRequest();
        request.setReviewerId(1L);
        request.setSellerId(2L);
        request.setRating(5);
        request.setComment("Buena carta");

        when(transactionClient.existsCompletedTransaction(1L, 2L))
                .thenThrow(new RuntimeException("ms-transactions no disponible"));

        // ACT + ASSERT
        assertThrows(
                ReviewNotAllowedException.class,
                () -> reviewService.create(request)
        );

        // VERIFY
        verify(reviewRepository, never()).save(any());
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: cuando ms-transactions está caído o falla por
// cualquier motivo, el sistema rechaza la reseña con
// ReviewNotAllowedException (decisión fail-safe: ante la duda, no
// permitir la reseña)
// Se obtuvo: el error de Feign se propaga sin capturar como una
// excepción genérica, resultando en HTTP 500 en vez de un 403
// coherente con el resto del flujo de validación de negocio
// Esto podría pasar si alguien elimina el try/catch alrededor de
// "transactionClient.existsCompletedTransaction(...)" en
// ReviewService.create().


    @Test
    void create_conResenaDuplicada_lanzaReviewAlreadyExistsException() {
        // ARRANGE: el reviewer ya reseñó a este seller previamente.
        ReviewRequest request = new ReviewRequest();
        request.setReviewerId(1L);
        request.setSellerId(2L);
        request.setRating(5);
        request.setComment("Buena carta");

        when(transactionClient.existsCompletedTransaction(1L, 2L)).thenReturn(true);
        when(reviewRepository.existsByReviewerIdAndSellerId(1L, 2L)).thenReturn(true);

        // ACT + ASSERT
        ReviewAlreadyExistsException ex = assertThrows(
                ReviewAlreadyExistsException.class,
                () -> reviewService.create(request)
        );
        assertThat(ex.getMessage()).isEqualTo(
                "Ya reseñaste a este vendedor. reviewerId=1 sellerId=2");

        // VERIFY: al detectarse el duplicado, nunca se debió persistir
        // una segunda reseña ni notificar.
        verify(reviewRepository, never()).save(any());
        verify(notificationClient, never()).send(any());
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: ReviewAlreadyExistsException cuando el reviewer ya
// reseñó a ese seller (protección reforzada también por el
// @UniqueConstraint a nivel de base de datos en la entidad Review)
// Se obtuvo: se intenta guardar una segunda reseña para el mismo
// par reviewer+seller, lo que en un entorno real fallaría con una
// excepción de violación de constraint de base de datos en vez de
// un error de negocio claro
// Esto podría pasar si alguien elimina la validación
// "if (reviewRepository.existsByReviewerIdAndSellerId(...))".


    @Test
    void create_conErrorAlNotificar_continuaElFlujoYRetornaResenaCreada() {
        // ARRANGE: el flujo principal funciona, pero
        // notificationClient.send() lanza una excepción — este es un
        // paso "tolerante a fallos" (mismo patrón ya visto en
        // ms-notifications y ms-orders): el catch solo loguea el error
        // y continúa, sin interrumpir la creación de la reseña.
        ReviewRequest request = new ReviewRequest();
        request.setReviewerId(1L);
        request.setSellerId(2L);
        request.setRating(5);
        request.setComment("Excelente");

        when(transactionClient.existsCompletedTransaction(1L, 2L)).thenReturn(true);
        when(reviewRepository.existsByReviewerIdAndSellerId(1L, 2L)).thenReturn(false);

        Review guardada = new Review();
        guardada.setId(1L);
        guardada.setReviewerId(1L);
        guardada.setSellerId(2L);
        guardada.setRating(5);
        guardada.setComment("Excelente");
        when(reviewRepository.save(any(Review.class))).thenReturn(guardada);

        doThrow(new RuntimeException("ms-notifications no disponible"))
                .when(notificationClient).send(any());

        // ACT: a pesar del error al notificar, el metodo NO debe lanzar
        // ninguna excepción — la reseña ya fue persistida exitosamente
        // antes de llegar a este paso.
        ReviewResponse response = reviewService.create(request);

        // ASSERT: la reseña se creó igual, con todos sus datos correctos.
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getRating()).isEqualTo(5);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: un fallo al enviar la notificación al vendedor NO
// interrumpe la creación de la reseña — la reseña ya fue persistida
// en la base de datos ANTES del intento de notificación, así que
// el dato más importante (la reseña en sí) ya está seguro
// Se obtuvo: la excepción de notificationClient.send() se propaga
// sin capturar, y como el metodo es @Transactional, esto provocaría
// un ROLLBACK de la reseña recién guardada — perdiendo una reseña
// legítima solo porque ms-notifications estaba temporalmente caído,
// a pesar de que toda la validación de negocio (transacción
// verificada, sin duplicados) ya había pasado correctamente
// Esto podría pasar si alguien elimina el try/catch alrededor de
// "notificationClient.send(...)" en el paso final de
// ReviewService.create().

    @Test
    void getBySeller_conResenasExistentes_retornaListaCorrecta() {
        // ARRANGE
        Long sellerId = 2L;
        Review review1 = new Review();
        review1.setId(1L);
        review1.setReviewerId(1L);
        review1.setSellerId(sellerId);
        review1.setRating(5);

        when(reviewRepository.findBySellerId(sellerId)).thenReturn(List.of(review1));

        // ACT
        List<ReviewResponse> response = reviewService.getBySeller(sellerId);

        // ASSERT
        assertThat(response).hasSize(1);
        assertThat(response.getFirst().getSellerId()).isEqualTo(sellerId);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: solo reseñas del sellerId solicitado
// Se obtuvo: reseñas de TODOS los vendedores (findAll() en vez de
// findBySellerId(sellerId))


    @Test
    void getBySeller_sinResenas_retornaListaVacia() {
        // ARRANGE
        Long sellerId = 99L;
        when(reviewRepository.findBySellerId(sellerId)).thenReturn(List.of());

        // ACT
        List<ReviewResponse> response = reviewService.getBySeller(sellerId);

        // ASSERT
        assertThat(response).isEmpty();
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: una lista vacía cuando el vendedor no tiene reseñas
// Se obtuvo: una NullPointerException al intentar hacer
// .stream().map(...) sobre un resultado null


    @Test
    void getSellerRating_conResenasExistentes_calculaPromedioRedondeado() {
        // ARRANGE: el repositorio retorna un promedio con más de 2
        // decimales, para confirmar que el redondeo realmente funciona
        // (Math.round(average * 100.0) / 100.0).
        Long sellerId = 2L;
        when(reviewRepository.calculateAverageRating(sellerId))
                .thenReturn(Optional.of(4.3333333));
        when(reviewRepository.countBySellerId(sellerId)).thenReturn(3L);

        // ACT
        SellerRatingResponse response = reviewService.getSellerRating(sellerId);

        // ASSERT: 4.3333333 redondeado a 2 decimales = 4.33
        assertThat(response.getAverageRating()).isEqualTo(4.33);
        assertThat(response.getTotalReviews()).isEqualTo(3L);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: 4.3333333 se redondea a 4.33 (2 decimales exactos)
// Se obtuvo: 4.3333333 se mantiene sin redondear, o se redondea
// incorrectamente a un número distinto (por ejemplo, 4.3 con solo
// 1 decimal, o 4.34 con redondeo hacia arriba incorrecto)
// Esto podría pasar si alguien modifica la fórmula
// "Math.round(average * 100.0) / 100.0" en ReviewService
// .getSellerRating(), por ejemplo usando "Math.round(average) / 100.0"
// (olvidando multiplicar primero por 100), lo que redondearía el
// promedio a 0 decimales ANTES de dividir, perdiendo toda la
// precisión decimal esperada en el resultado final.


    @Test
    void getSellerRating_sinResenas_retornaPromedioCero() {
        // ARRANGE: calculateAverageRating() retorna Optional.empty()
        // cuando no hay reseñas (la consulta SQL AVG sobre cero filas
        // produce NULL, que JPA mapea a un Optional vacío).
        Long sellerId = 99L;
        when(reviewRepository.calculateAverageRating(sellerId))
                .thenReturn(Optional.empty());
        when(reviewRepository.countBySellerId(sellerId)).thenReturn(0L);

        // ACT
        SellerRatingResponse response = reviewService.getSellerRating(sellerId);

        // ASSERT: el .orElse(0.0) debe manejar el Optional vacío
        // correctamente, sin lanzar excepción.
        assertThat(response.getAverageRating()).isEqualTo(0.0);
        assertThat(response.getTotalReviews()).isEqualTo(0L);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: averageRating=0.0 cuando no hay ninguna reseña
// (Optional vacío manejado con .orElse(0.0))
// Se obtuvo: una NoSuchElementException al intentar hacer .get()
// directo sobre el Optional vacío en vez de usar .orElse(0.0)
// Esto podría pasar si alguien reemplaza
// "reviewRepository.calculateAverageRating(sellerId).orElse(0.0)"
// por un ".get()" directo, mismo patrón de riesgo ya documentado
// extensamente en Optional a lo largo de Quandrix.

    @Test
    void getById_conIdExistente_retornaResena() {
        // ARRANGE
        Long id = 1L;
        Review review = new Review();
        review.setId(id);
        review.setReviewerId(1L);
        review.setSellerId(2L);
        review.setRating(5);

        when(reviewRepository.findById(id)).thenReturn(Optional.of(review));

        // ACT
        ReviewResponse response = reviewService.getById(id);

        // ASSERT
        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getRating()).isEqualTo(5);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: response.getRating() refleja exactamente el rating
// real de la reseña almacenada
// Se obtuvo: response.getRating() == null
// Esto podría pasar si alguien modifica el metodo privado toResponse()
// y olvida mapear el campo rating al construir el ReviewResponse.


    @Test
    void getById_conIdInexistente_lanzaReviewNotFoundException() {
        // ARRANGE
        Long idInexistente = 999L;
        when(reviewRepository.findById(idInexistente)).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThrows(
                ReviewNotFoundException.class,
                () -> reviewService.getById(idInexistente)
        );
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: ReviewNotFoundException cuando el repositorio no
// encuentra la reseña (Optional vacío)
// Se obtuvo: NoSuchElementException sin capturar (HTTP 500 en vez
// de 404), mismo patrón de riesgo ya documentado en todos los demás
// microservicios.


    @Test
    void delete_conIdExistente_eliminaLaResena() {
        // ARRANGE
        Long id = 1L;
        Review review = new Review();
        review.setId(id);

        when(reviewRepository.findById(id)).thenReturn(Optional.of(review));

        // ACT
        reviewService.delete(id);

        // VERIFY: confirmamos que se llamó a delete() con el objeto
        // real obtenido de findById(...), no con deleteById(id) directo.
        verify(reviewRepository, times(1)).delete(review);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: reviewRepository.delete(review) se invoca con el
// objeto real obtenido de findById(...), confirmando primero que
// existe
// Se obtuvo: deleteById(id) se invoca directamente sin esa
// validación previa — mismo riesgo ya documentado repetidamente
// en otros microservicios (Spring Data JPA no lanza excepción si
// el id no existe).


    @Test
    void delete_conIdInexistente_lanzaReviewNotFoundException() {
        // ARRANGE
        Long idInexistente = 999L;
        when(reviewRepository.findById(idInexistente)).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThrows(
                ReviewNotFoundException.class,
                () -> reviewService.delete(idInexistente)
        );

        // VERIFY
        verify(reviewRepository, never()).delete(any());
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: ReviewNotFoundException cuando se intenta eliminar
// una reseña que no existe, sin invocar delete() en absoluto
// Se obtuvo: NoSuchElementException sin capturar (HTTP 500 en vez
// de 404)
}