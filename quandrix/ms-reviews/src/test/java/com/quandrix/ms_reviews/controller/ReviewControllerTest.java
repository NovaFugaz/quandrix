package com.quandrix.ms_reviews.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quandrix.ms_reviews.dto.ReviewRequest;
import com.quandrix.ms_reviews.dto.ReviewResponse;
import com.quandrix.ms_reviews.dto.SellerRatingResponse;
import com.quandrix.ms_reviews.exception.ReviewAlreadyExistsException;
import com.quandrix.ms_reviews.exception.ReviewNotAllowedException;
import com.quandrix.ms_reviews.exception.ReviewNotFoundException;
import com.quandrix.ms_reviews.service.ReviewService;
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

@WebMvcTest(ReviewController.class)
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReviewService reviewService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void create_conDatosValidos_retorna201ConResenaCreada() throws Exception {
        // ARRANGE
        ReviewRequest request = new ReviewRequest();
        request.setReviewerId(1L);
        request.setSellerId(2L);
        request.setRating(5);
        request.setComment("Excelente vendedor, carta en perfecto estado");

        ReviewResponse fakeResponse = new ReviewResponse(
                1L, 1L, 2L, 5, "Excelente vendedor, carta en perfecto estado",
                LocalDateTime.of(2026, 6, 12, 14, 30));

        when(reviewService.create(any(ReviewRequest.class))).thenReturn(fakeResponse);

        // ACT + ASSERT
        mockMvc.perform(post("/reviews")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.comment").value("Excelente vendedor, carta en perfecto estado"));

        // VERIFY
        verify(reviewService, times(1)).create(any(ReviewRequest.class));
    }


// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 201 Created con rating=5 reflejado exactamente
// como se envió
// Se obtuvo: HTTP 201 Created con rating distinto al enviado
// Esto podría pasar si alguien modifica el orden de los argumentos
// en el constructor de ReviewResponse (@AllArgsConstructor posicional
// de Lombok), por ejemplo intercambiando "sellerId" y "rating" sin
// que el compilador lo detecte, ya que ambos son tipos numéricos
// compatibles en una refactorización descuidada.

    @Test
    void create_sinTransaccionCompletada_retorna403ConMensaje() throws Exception {
        // ARRANGE
        ReviewRequest request = new ReviewRequest();
        request.setReviewerId(1L);
        request.setSellerId(2L);
        request.setRating(5);
        request.setComment("Buena carta");

        when(reviewService.create(any(ReviewRequest.class)))
                .thenThrow(new ReviewNotAllowedException());

        // ACT + ASSERT: 403 Forbidden — primera vez que usamos este
        // código en Quandrix.
        mockMvc.perform(post("/reviews")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value(
                        "No puedes reseñar a un vendedor sin haber completado la compra."));

        // VERIFY
        verify(reviewService, times(1)).create(any(ReviewRequest.class));
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 403 Forbidden cuando no existe una transacción
// completada entre reviewer y seller
// Se obtuvo: HTTP 500 Internal Server Error
// Esto podría pasar si alguien elimina el @ExceptionHandler de
// ReviewNotAllowedException en GlobalExceptionHandler.


    @Test
    void create_conResenaDuplicada_retorna409ConMensaje() throws Exception {
        // ARRANGE
        ReviewRequest request = new ReviewRequest();
        request.setReviewerId(1L);
        request.setSellerId(2L);
        request.setRating(5);
        request.setComment("Buena carta");

        when(reviewService.create(any(ReviewRequest.class)))
                .thenThrow(new ReviewAlreadyExistsException(1L, 2L));

        // ACT + ASSERT
        mockMvc.perform(post("/reviews")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(
                        "Ya reseñaste a este vendedor. reviewerId=1 sellerId=2"));

        // VERIFY
        verify(reviewService, times(1)).create(any(ReviewRequest.class));
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 409 Conflict cuando el reviewer ya reseñó a ese
// seller anteriormente
// Se obtuvo: HTTP 500 Internal Server Error
// Esto podría pasar si alguien elimina el @ExceptionHandler de
// ReviewAlreadyExistsException.


    @Test
    void create_conRatingNulo_retorna400ConErrorDeValidacion() throws Exception {
        // ARRANGE: omitimos rating (obligatorio según @NotNull).
        ReviewRequest request = new ReviewRequest();
        request.setReviewerId(1L);
        request.setSellerId(2L);
        request.setRating(null);
        request.setComment("Buena carta");

        // ACT + ASSERT
        mockMvc.perform(post("/reviews")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.rating").value("El rating es obligatorio"));

        // VERIFY: la validación de Bean Validation actúa ANTES de que
        // el service se ejecute.
        verify(reviewService, never()).create(any());
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: reviewService.create(...) NUNCA se invoca cuando el
// rating es nulo
// Se obtuvo: reviewService.create(...) SÍ se invoca, posiblemente
// causando un NullPointerException dentro del service al intentar
// comparar request.getRating() < 1 con un valor null (Integer
// autoboxing fallaría con NPE al intentar el unboxing a int)
// Esto podría pasar si alguien elimina @Valid del @RequestBody en
// ReviewController.create().


    @Test
    void create_conRatingFueraDeRangoViaBeanValidation_retorna400() throws Exception {
        // ARRANGE: rating=10, violando @Max(5) — confirmamos que Bean
        // Validation intercepta esto ANTES de que el código manual
        // (con el bug de IllegalArgumentException -> 500) tenga
        // oportunidad de ejecutarse, en el flujo real vía HTTP.
        ReviewRequest request = new ReviewRequest();
        request.setReviewerId(1L);
        request.setSellerId(2L);
        request.setRating(10);
        request.setComment("Buena carta");

        // ACT + ASSERT: 400, NO 500 — porque Bean Validation actúa primero.
        mockMvc.perform(post("/reviews")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.rating").value("El raiting su máximo es 5"));

        // VERIFY: el service nunca llega a ejecutarse, así que el bug
        // del IllegalArgumentException -> 500 nunca se dispara en un
        // flujo HTTP real.
        verify(reviewService, never()).create(any());
    }

// NOTA PARA QA (aclaración de contrato, no un caso de falla):
// Aunque ReviewService.create() tiene una validación manual de
// rating que, si se alcanzara, terminaría en un HTTP 500 (bug
// documentado y probado directamente en ReviewServiceTest), ese
// código es en la práctica INALCANZABLE a través de la API HTTP
// normal, porque @Min(1)/@Max(5) en ReviewRequest ya bloquea el
// request con 400 antes de que el controller invoque al service.
// Este test confirma esa primera línea de defensa. El bug del
// service solo se manifestaría si alguien invocara
// ReviewService.create() desde otro punto de entrada que no pase
// por Bean Validation (ej. un futuro listener de eventos), o si
// se eliminaran por error las anotaciones @Min/@Max del DTO.

    @Test
    void getBySeller_conResenasExistentes_retorna200ConLista() throws Exception {
        // ARRANGE
        Long sellerId = 2L;
        ReviewResponse review1 = new ReviewResponse(
                1L, 1L, sellerId, 5, "Excelente", LocalDateTime.of(2026, 6, 12, 14, 30));
        ReviewResponse review2 = new ReviewResponse(
                2L, 3L, sellerId, 4, "Muy bueno", LocalDateTime.of(2026, 6, 11, 10, 0));

        when(reviewService.getBySeller(sellerId)).thenReturn(List.of(review1, review2));

        // ACT + ASSERT
        mockMvc.perform(get("/reviews/seller/{sellerId}", sellerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].rating").value(5))
                .andExpect(jsonPath("$[1].rating").value(4));

        // VERIFY
        verify(reviewService, times(1)).getBySeller(sellerId);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: solo reseñas del sellerId solicitado
// Se obtuvo: reseñas de OTROS vendedores mezcladas en la respuesta
// Esto podría pasar si alguien modifica el repositorio y usa
// findAll() en vez de findBySellerId(sellerId).


    @Test
    void getBySeller_sinResenas_retorna200ConListaVacia() throws Exception {
        // ARRANGE: mismo criterio ya aplicado en microservicios
        // anteriores — el Swagger documenta un posible 404, pero el
        // comportamiento real es 200 OK + lista vacía.
        Long sellerId = 99L;
        when(reviewService.getBySeller(sellerId)).thenReturn(List.of());

        // ACT + ASSERT
        mockMvc.perform(get("/reviews/seller/{sellerId}", sellerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // VERIFY
        verify(reviewService, times(1)).getBySeller(sellerId);
    }

// NOTA PARA QA (no es un caso de falla, es una aclaración de contrato):
// 200 OK + lista vacía representa "el vendedor no tiene reseñas",
// no un error del sistema.


    @Test
    void getSellerRating_conResenasExistentes_retorna200ConPromedio() throws Exception {
        // ARRANGE
        Long sellerId = 2L;
        SellerRatingResponse fakeResponse = new SellerRatingResponse(sellerId, 4.5, 20L);

        when(reviewService.getSellerRating(sellerId)).thenReturn(fakeResponse);

        // ACT + ASSERT
        mockMvc.perform(get("/reviews/seller/{sellerId}/rating", sellerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sellerId").value(2))
                .andExpect(jsonPath("$.averageRating").value(4.5))
                .andExpect(jsonPath("$.totalReviews").value(20));

        // VERIFY
        verify(reviewService, times(1)).getSellerRating(sellerId);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: averageRating refleja el promedio REAL calculado
// desde las reseñas almacenadas
// Se obtuvo: averageRating siempre en 0.0 o un valor que no
// corresponde al promedio real
// Esto podría pasar si alguien modifica la query JPQL
// "SELECT AVG(r.rating) FROM Review r WHERE r.sellerId = :sellerId"
// en ReviewRepository, rompiendo el cálculo del promedio.

    @Test
    void getSellerRating_sinResenas_retorna200ConPromedioCero() throws Exception {
        // ARRANGE: el vendedor existe, pero no tiene ninguna reseña aún.
        // NOTA: mismo criterio de contrato ya aplicado en otros
        // microservicios — el Swagger documenta un posible 404, pero el
        // comportamiento real es 200 OK con averageRating=0.0 y
        // totalReviews=0, no un error.
        Long sellerId = 99L;
        SellerRatingResponse fakeResponse = new SellerRatingResponse(sellerId, 0.0, 0L);

        when(reviewService.getSellerRating(sellerId)).thenReturn(fakeResponse);

        // ACT + ASSERT
        mockMvc.perform(get("/reviews/seller/{sellerId}/rating", sellerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").value(0.0))
                .andExpect(jsonPath("$.totalReviews").value(0));

        // VERIFY
        verify(reviewService, times(1)).getSellerRating(sellerId);
    }

// NOTA PARA QA (no es un caso de falla, es una aclaración de contrato):
// 200 OK + averageRating=0.0 representa "el vendedor no tiene
// reseñas todavía" (un vendedor nuevo en el marketplace), no un error.


    @Test
    void getById_conIdExistente_retorna200ConResena() throws Exception {
        // ARRANGE
        Long id = 1L;
        ReviewResponse fakeResponse = new ReviewResponse(
                id, 1L, 2L, 5, "Excelente vendedor",
                LocalDateTime.of(2026, 6, 12, 14, 30));

        when(reviewService.getById(id)).thenReturn(fakeResponse);

        // ACT + ASSERT
        mockMvc.perform(get("/reviews/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.rating").value(5));

        // VERIFY
        verify(reviewService, times(1)).getById(id);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 200 OK al consultar una reseña existente
// Se obtuvo: HTTP 404 Not Found a pesar de que el id existe
// Esto podría pasar si alguien cambia por error el tipo del
// @PathVariable de Long a String en ReviewController.getById().


    @Test
    void getById_conIdInexistente_retorna404NotFound() throws Exception {
        // ARRANGE
        Long idInexistente = 999L;

        when(reviewService.getById(idInexistente))
                .thenThrow(new ReviewNotFoundException(idInexistente));

        // ACT + ASSERT
        mockMvc.perform(get("/reviews/{id}", idInexistente))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Reseña no encontrada con id: " + idInexistente));

        // VERIFY
        verify(reviewService, times(1)).getById(idInexistente);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 404 Not Found al consultar una reseña inexistente
// Se obtuvo: HTTP 500 Internal Server Error
// Esto podría pasar si alguien elimina el @ExceptionHandler de
// ReviewNotFoundException.


    @Test
    void delete_conIdExistente_retorna204NoContent() throws Exception {
        // ARRANGE
        Long id = 1L;
        doNothing().when(reviewService).delete(id);

        // ACT + ASSERT
        mockMvc.perform(delete("/reviews/{id}", id))
                .andExpect(status().isNoContent());

        // VERIFY
        verify(reviewService, times(1)).delete(id);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 204 No Content al eliminar una reseña existente
// Se obtuvo: HTTP 404 Not Found a pesar de que el id sí existe
// Esto podría pasar si alguien invierte por error la lógica de
// existencia en ReviewService.delete().


    @Test
    void delete_conIdInexistente_retorna404NotFound() throws Exception {
        // ARRANGE
        Long idInexistente = 999L;
        doThrow(new ReviewNotFoundException(idInexistente))
                .when(reviewService).delete(idInexistente);

        // ACT + ASSERT
        mockMvc.perform(delete("/reviews/{id}", idInexistente))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Reseña no encontrada con id: " + idInexistente));

        // VERIFY
        verify(reviewService, times(1)).delete(idInexistente);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 404 Not Found al eliminar una reseña inexistente
// Se obtuvo: HTTP 204 No Content (silenciosamente "exitoso")
// Esto podría pasar si alguien reemplaza la validación de existencia
// por un deleteById(id) directo de Spring Data JPA — mismo riesgo
// ya documentado repetidamente en otros microservicios.
}