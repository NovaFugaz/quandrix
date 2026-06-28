package com.quandrix.ms_orders.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quandrix.ms_orders.dto.OrderRequest;
import com.quandrix.ms_orders.dto.OrderResponse;
import com.quandrix.ms_orders.exception.InvalidOrderException;
import com.quandrix.ms_orders.exception.OrderNotCancellableException;
import com.quandrix.ms_orders.exception.OrderNotFoundException;
import com.quandrix.ms_orders.model.OrderStatus;
import com.quandrix.ms_orders.service.OrderService;
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

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void create_conDatosValidos_retorna201ConOrdenCreada() throws Exception {
        // ARRANGE
        OrderRequest request = new OrderRequest();
        request.setBuyerId(1L);
        request.setListingId(10L);
        request.setPaymentMethod("CREDIT_CARD");

        OrderResponse fakeResponse = new OrderResponse(
                1L, 1L, 10L, 2L, 5000L, OrderStatus.COMPLETED, "CREDIT_CARD",
                LocalDateTime.of(2026, 6, 12, 14, 30),
                LocalDateTime.of(2026, 6, 12, 14, 31));

        when(orderService.create(any(OrderRequest.class))).thenReturn(fakeResponse);

        // ACT + ASSERT
        mockMvc.perform(post("/orders")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.amount").value(5000));

        // VERIFY
        verify(orderService, times(1)).create(any(OrderRequest.class));
    }


// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 201 Created con "status": "COMPLETED" cuando
// el flujo completo de creación de orden (validación, pago,
// notificaciones) terminó exitosamente
// Se obtuvo: HTTP 201 Created con "status": "PENDING" o "CONFIRMED",
// sugiriendo que el flujo se interrumpió a medias sin que el
// controller lo refleje como un error
// Esto podría pasar si alguien modifica OrderService.create() y
// omite el paso final "order.setStatus(OrderStatus.COMPLETED)",
// dejando que el controller responda 201 con una orden que en
// realidad no completó su flujo de procesamiento.

    @Test
    void create_conListingInexistente_retorna400ConMensajeDeNegocio() throws Exception {
        // ARRANGE
        OrderRequest request = new OrderRequest();
        request.setBuyerId(1L);
        request.setListingId(999L);
        request.setPaymentMethod("CREDIT_CARD");

        when(orderService.create(any(OrderRequest.class)))
                .thenThrow(new InvalidOrderException("El listing 999 no existe"));

        // ACT + ASSERT
        mockMvc.perform(post("/orders")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El listing 999 no existe"));

        // VERIFY
        verify(orderService, times(1)).create(any(OrderRequest.class));
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 400 Bad Request cuando el service rechaza la
// creación por una regla de negocio (listing inexistente, pago
// rechazado, comprador = vendedor, etc.)
// Se obtuvo: HTTP 500 Internal Server Error
// Esto podría pasar si alguien elimina el @ExceptionHandler de
// InvalidOrderException en GlobalExceptionHandler, dejando que caiga
// en el handler genérico de Exception.class — perdiendo la
// distinción entre "el comprador hizo algo inválido" (400) y "el
// servidor falló" (500).


    @Test
    void create_conBuyerIdNulo_retorna400ConErrorDeValidacion() throws Exception {
        // ARRANGE: omitimos buyerId (obligatorio según @NotNull).
        OrderRequest request = new OrderRequest();
        request.setBuyerId(null); // viola @NotNull
        request.setListingId(10L);
        request.setPaymentMethod("CREDIT_CARD");

        // ACT + ASSERT
        mockMvc.perform(post("/orders")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.buyerId").value("El buyerId es obligatorio"));

        // VERIFY: la validación de Bean Validation actúa ANTES de que
        // el metodo del controller se ejecute — el service nunca debió
        // ser invocado.
        verify(orderService, never()).create(any());
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: orderService.create(...) NUNCA se invoca cuando el
// request no pasa las validaciones de Bean Validation
// Se obtuvo: orderService.create(...) SÍ se invoca con un buyerId
// nulo, posiblemente causando un NullPointerException más adelante
// dentro del service (por ejemplo, al comparar
// listing.getSellerId().equals(request.getBuyerId()) con un
// buyerId null) en vez de fallar limpiamente con 400 en el borde
// de la API
// Esto podría pasar si alguien elimina por error la anotación @Valid
// del parámetro @RequestBody en OrderController.create().

    @Test
    void getById_conIdExistente_retorna200ConOrden() throws Exception {
        // ARRANGE
        Long id = 1L;
        OrderResponse fakeResponse = new OrderResponse(
                id, 1L, 10L, 2L, 5000L, OrderStatus.COMPLETED, "CREDIT_CARD",
                LocalDateTime.of(2026, 6, 12, 14, 30),
                LocalDateTime.of(2026, 6, 12, 14, 31));

        when(orderService.getById(id)).thenReturn(fakeResponse);

        // ACT + ASSERT
        mockMvc.perform(get("/orders/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // VERIFY
        verify(orderService, times(1)).getById(id);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 200 OK al consultar una orden existente
// Se obtuvo: HTTP 404 Not Found a pesar de que el id existe
// Esto podría pasar si alguien cambia por error el tipo del
// @PathVariable de Long a String en OrderController.getById(),
// causando un error de conversión antes de invocar al service.


    @Test
    void getById_conIdInexistente_retorna404NotFound() throws Exception {
        // ARRANGE
        Long idInexistente = 999L;

        when(orderService.getById(idInexistente))
                .thenThrow(new OrderNotFoundException(idInexistente));

        // ACT + ASSERT
        mockMvc.perform(get("/orders/{id}", idInexistente))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Orden no encontrada con id: " + idInexistente));

        // VERIFY
        verify(orderService, times(1)).getById(idInexistente);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 404 Not Found al consultar una orden inexistente
// Se obtuvo: HTTP 500 Internal Server Error
// Esto podría pasar si alguien elimina el @ExceptionHandler de
// OrderNotFoundException en GlobalExceptionHandler.

    @Test
    void getByBuyer_conOrdenesExistentes_retorna200ConLista() throws Exception {
        // ARRANGE
        Long buyerId = 1L;
        OrderResponse order1 = new OrderResponse(
                1L, buyerId, 10L, 2L, 5000L, OrderStatus.COMPLETED, "CREDIT_CARD",
                LocalDateTime.of(2026, 6, 12, 14, 30), LocalDateTime.of(2026, 6, 12, 14, 31));

        when(orderService.getByBuyer(buyerId)).thenReturn(List.of(order1));

        // ACT + ASSERT
        mockMvc.perform(get("/orders/buyer/{buyerId}", buyerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].buyerId").value(1));

        // VERIFY
        verify(orderService, times(1)).getByBuyer(buyerId);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: solo órdenes del buyerId solicitado
// Se obtuvo: órdenes de OTROS compradores mezcladas en la respuesta
// Esto podría pasar si alguien modifica el repositorio y usa
// findAll() en vez de findByBuyerId(buyerId) — un problema serio de
// privacidad, ya que un comprador podría ver órdenes ajenas.


    @Test
    void getByBuyer_sinOrdenes_retorna200ConListaVacia() throws Exception {
        // ARRANGE: mismo criterio ya aplicado en otros microservicios —
        // el Swagger documenta un posible 404, pero el comportamiento
        // real y acordado es 200 OK + lista vacía.
        Long buyerId = 99L;
        when(orderService.getByBuyer(buyerId)).thenReturn(List.of());

        // ACT + ASSERT
        mockMvc.perform(get("/orders/buyer/{buyerId}", buyerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // VERIFY
        verify(orderService, times(1)).getByBuyer(buyerId);
    }

// NOTA PARA QA (no es un caso de falla, es una aclaración de contrato):
// 200 OK + lista vacía representa "el comprador no tiene órdenes",
// no un error del sistema.


    @Test
    void getBySeller_conOrdenesExistentes_retorna200ConLista() throws Exception {
        // ARRANGE
        Long sellerId = 2L;
        OrderResponse order1 = new OrderResponse(
                1L, 1L, 10L, sellerId, 5000L, OrderStatus.COMPLETED, "CREDIT_CARD",
                LocalDateTime.of(2026, 6, 12, 14, 30), LocalDateTime.of(2026, 6, 12, 14, 31));

        when(orderService.getBySeller(sellerId)).thenReturn(List.of(order1));

        // ACT + ASSERT
        mockMvc.perform(get("/orders/seller/{sellerId}", sellerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].sellerId").value(2));

        // VERIFY
        verify(orderService, times(1)).getBySeller(sellerId);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: solo órdenes del sellerId solicitado
// Se obtuvo: órdenes de OTROS vendedores mezcladas en la respuesta
// Mismo riesgo de privacidad ya documentado para getByBuyer().


    @Test
    void getBySeller_sinOrdenes_retorna200ConListaVacia() throws Exception {
        // ARRANGE
        Long sellerId = 99L;
        when(orderService.getBySeller(sellerId)).thenReturn(List.of());

        // ACT + ASSERT
        mockMvc.perform(get("/orders/seller/{sellerId}", sellerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // VERIFY
        verify(orderService, times(1)).getBySeller(sellerId);
    }

// NOTA PARA QA (no es un caso de falla, es una aclaración de contrato):
// 200 OK + lista vacía representa "el vendedor no tiene órdenes",
// no un error del sistema.

    @Test
    void cancel_conDatosValidos_retorna200ConOrdenCancelada() throws Exception {
        // ARRANGE
        Long id = 1L;
        Long buyerId = 1L;

        OrderResponse fakeResponse = new OrderResponse(
                id, buyerId, 10L, 2L, 5000L, OrderStatus.CANCELLED, "CREDIT_CARD",
                LocalDateTime.of(2026, 6, 12, 14, 30), LocalDateTime.of(2026, 6, 12, 14, 35));

        when(orderService.cancel(id, buyerId)).thenReturn(fakeResponse);

        // ACT + ASSERT
        mockMvc.perform(patch("/orders/{id}/cancel", id)
                        .param("buyerId", String.valueOf(buyerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // VERIFY
        verify(orderService, times(1)).cancel(id, buyerId);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 200 OK con "status": "CANCELLED"
// Se obtuvo: HTTP 200 OK con "status": "PENDING" (sin cambios reales)
// Esto podría pasar si alguien olvida la línea
// order.setStatus(OrderStatus.CANCELLED) antes de
// orderRepository.save(order) en OrderService.cancel().


    @Test
    void cancel_conIdInexistente_retorna404NotFound() throws Exception {
        // ARRANGE
        Long idInexistente = 999L;
        Long buyerId = 1L;

        when(orderService.cancel(idInexistente, buyerId))
                .thenThrow(new OrderNotFoundException(idInexistente));

        // ACT + ASSERT
        mockMvc.perform(patch("/orders/{id}/cancel", idInexistente)
                        .param("buyerId", String.valueOf(buyerId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Orden no encontrada con id: " + idInexistente));

        // VERIFY
        verify(orderService, times(1)).cancel(idInexistente, buyerId);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 404 Not Found al cancelar una orden inexistente
// Se obtuvo: HTTP 500 Internal Server Error
// Esto podría pasar si alguien elimina el @ExceptionHandler de
// OrderNotFoundException.


    @Test
    void cancel_conBuyerIdDistinto_retorna400ConMensajeDeNegocio() throws Exception {
        // ARRANGE: la orden pertenece a otro comprador.
        Long id = 1L;
        Long buyerIdIncorrecto = 2L;

        when(orderService.cancel(id, buyerIdIncorrecto))
                .thenThrow(new InvalidOrderException("No puedes cancelar una orden que no es tuya"));

        // ACT + ASSERT
        mockMvc.perform(patch("/orders/{id}/cancel", id)
                        .param("buyerId", String.valueOf(buyerIdIncorrecto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("No puedes cancelar una orden que no es tuya"));

        // VERIFY
        verify(orderService, times(1)).cancel(id, buyerIdIncorrecto);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 400 Bad Request cuando alguien intenta cancelar
// una orden que no le pertenece (vulnerabilidad IDOR, mismo patrón
// ya documentado en ms-listings para update()/withdraw())
// Se obtuvo: la orden se cancela exitosamente sin validar el dueño
// real, permitiendo que cualquier comprador cancele órdenes ajenas
// conociendo solo su id numérico.


    @Test
    void cancel_conOrdenNoCancelable_retorna409Conflict() throws Exception {
        // ARRANGE: la orden existe y pertenece al comprador correcto,
        // pero ya no está en estado PENDING (por ejemplo, ya fue COMPLETED).
        Long id = 1L;
        Long buyerId = 1L;

        when(orderService.cancel(id, buyerId))
                .thenThrow(new OrderNotCancellableException(id));

        // ACT + ASSERT: a diferencia de los demás microservicios, este
        // caso usa específicamente 409 CONFLICT, no 400 ni 404 — porque
        // semánticamente el problema no es que el request esté mal
        // formado ni que la orden no exista, sino que el ESTADO ACTUAL
        // del recurso entra en conflicto con la operación solicitada.
        mockMvc.perform(patch("/orders/{id}/cancel", id)
                        .param("buyerId", String.valueOf(buyerId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(
                        "La orden " + id + " no puede cancelarse en su estado actual"));

        // VERIFY
        verify(orderService, times(1)).cancel(id, buyerId);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 409 Conflict al intentar cancelar una orden que
// ya no está en estado PENDING (por ejemplo, ya fue COMPLETED o
// CANCELLED previamente)
// Se obtuvo: HTTP 500 Internal Server Error, o peor, HTTP 200 OK
// permitiendo "cancelar" una orden que ya estaba completada,
// generando un estado inconsistente entre ms-orders y los demás
// microservicios que ya procesaron esa venta como definitiva
// (ms-listings ya marcó el listing como SOLD, ms-transactions ya
// registró la transacción, etc.)
// Esto podría pasar si alguien elimina el @ExceptionHandler de
// OrderNotCancellableException en GlobalExceptionHandler.
}