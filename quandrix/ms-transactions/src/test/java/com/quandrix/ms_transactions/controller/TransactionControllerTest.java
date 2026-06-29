package com.quandrix.ms_transactions.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quandrix.ms_transactions.dto.TransactionRequest;
import com.quandrix.ms_transactions.dto.TransactionResponse;
import com.quandrix.ms_transactions.exception.DuplicateTransactionException;
import com.quandrix.ms_transactions.exception.TransactionNotFoundException;
import com.quandrix.ms_transactions.service.TransactionService;
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

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void register_conDatosValidos_retorna201ConTransaccionCreada() throws Exception {
        // ARRANGE
        TransactionRequest request = new TransactionRequest();
        request.setOrderId(1L);
        request.setBuyerId(2L);
        request.setSellerId(3L);
        request.setScryfallId("id-bl");
        request.setAmount(5000L);

        TransactionResponse fakeResponse = new TransactionResponse(
                1L, 1L, 2L, 3L, "id-bl", 5000L,
                LocalDateTime.of(2026, 6, 12, 14, 30));

        when(transactionService.register(any(TransactionRequest.class))).thenReturn(fakeResponse);

        // ACT + ASSERT
        mockMvc.perform(post("/transactions")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.amount").value(5000));

        // VERIFY
        verify(transactionService, times(1)).register(any(TransactionRequest.class));
    }


// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 201 Created con todos los campos mapeados
// correctamente desde el TransactionRequest original
// Se obtuvo: HTTP 201 Created con "amount" distinto al enviado
// Esto podría pasar si alguien modifica el orden de los argumentos
// en el constructor de TransactionResponse (@AllArgsConstructor
// posicional), por ejemplo intercambiando "sellerId" y "amount" sin
// que el compilador lo detecte.


    @Test
    void register_conOrderIdDuplicado_retorna409Conflict() throws Exception {
        // ARRANGE
        TransactionRequest request = new TransactionRequest();
        request.setOrderId(1L);
        request.setBuyerId(2L);
        request.setSellerId(3L);
        request.setScryfallId("id-bl");
        request.setAmount(5000L);

        when(transactionService.register(any(TransactionRequest.class)))
                .thenThrow(new DuplicateTransactionException(1L));

        // ACT + ASSERT
        mockMvc.perform(post("/transactions")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Ya existe una transacción para la orden: 1"));

        // VERIFY
        verify(transactionService, times(1)).register(any(TransactionRequest.class));


// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 409 Conflict cuando ya existe una transacción
// para ese orderId (reforzado también por @Column(unique = true)
// en la entidad Transaction)
// Se obtuvo: HTTP 500 Internal Server Error
// Esto podría pasar si alguien elimina el @ExceptionHandler de
// DuplicateTransactionException.

    }

    @Test
    void getByOrderId_conOrderIdExistente_retorna200ConTransaccion() throws Exception {
        // ARRANGE
        Long orderId = 1L;
        TransactionResponse fakeResponse = new TransactionResponse(
                1L, orderId, 2L, 3L, "id-bl", 5000L,
                LocalDateTime.of(2026, 6, 12, 14, 30));

        when(transactionService.getByOrderId(orderId)).thenReturn(fakeResponse);

        // ACT + ASSERT
        mockMvc.perform(get("/transactions/order/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.amount").value(5000));

        // VERIFY
        verify(transactionService, times(1)).getByOrderId(orderId);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 200 OK al consultar la transacción de una orden
// existente
// Se obtuvo: HTTP 404 Not Found a pesar de que la transacción existe
// Esto podría pasar si alguien cambia por error el tipo del
// @PathVariable de Long a String en TransactionController
// .getByOrderId(), causando un error de conversión.


    @Test
    void getByOrderId_conOrderIdSinTransaccion_retorna404NotFound() throws Exception {
        // ARRANGE
        Long orderIdSinTransaccion = 999L;

        when(transactionService.getByOrderId(orderIdSinTransaccion))
                .thenThrow(new TransactionNotFoundException("orderId: " + orderIdSinTransaccion));

        // ACT + ASSERT
        mockMvc.perform(get("/transactions/order/{orderId}", orderIdSinTransaccion))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(
                        "Transacción no encontrada: orderId: " + orderIdSinTransaccion));

        // VERIFY
        verify(transactionService, times(1)).getByOrderId(orderIdSinTransaccion);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 404 Not Found cuando no existe ninguna
// transacción registrada para ese orderId
// Se obtuvo: HTTP 500 Internal Server Error
// Esto podría pasar si alguien elimina el @ExceptionHandler de
// TransactionNotFoundException.

    @Test
    void getByBuyer_conTransaccionesExistentes_retorna200ConLista() throws Exception {
        // ARRANGE
        Long buyerId = 2L;
        TransactionResponse tx1 = new TransactionResponse(
                1L, 1L, buyerId, 3L, "id-bl", 5000L,
                LocalDateTime.of(2026, 6, 12, 14, 30));

        when(transactionService.getByBuyer(buyerId)).thenReturn(List.of(tx1));

        // ACT + ASSERT
        mockMvc.perform(get("/transactions/buyer/{buyerId}", buyerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].buyerId").value(2));

        // VERIFY
        verify(transactionService, times(1)).getByBuyer(buyerId);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: solo transacciones del buyerId solicitado
// Se obtuvo: transacciones de OTROS compradores mezcladas —
// problema de privacidad ya documentado repetidamente en Quandrix
// (ms-orders, ms-listings) si se usa findAll() en vez de
// findByBuyerId(buyerId).


    @Test
    void getByBuyer_sinTransacciones_retorna200ConListaVacia() throws Exception {
        // ARRANGE
        Long buyerId = 99L;
        when(transactionService.getByBuyer(buyerId)).thenReturn(List.of());

        // ACT + ASSERT
        mockMvc.perform(get("/transactions/buyer/{buyerId}", buyerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // VERIFY
        verify(transactionService, times(1)).getByBuyer(buyerId);
    }

// NOTA PARA QA (no es un caso de falla, es una aclaración de contrato):
// 200 OK + lista vacía representa "el comprador no tiene
// transacciones", no un error del sistema.


    @Test
    void getBySeller_conTransaccionesExistentes_retorna200ConLista() throws Exception {
        // ARRANGE
        Long sellerId = 3L;
        TransactionResponse tx1 = new TransactionResponse(
                1L, 1L, 2L, sellerId, "id-bl", 5000L,
                LocalDateTime.of(2026, 6, 12, 14, 30));

        when(transactionService.getBySeller(sellerId)).thenReturn(List.of(tx1));

        // ACT + ASSERT
        mockMvc.perform(get("/transactions/seller/{sellerId}", sellerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].sellerId").value(3));

        // VERIFY
        verify(transactionService, times(1)).getBySeller(sellerId);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: solo transacciones del sellerId solicitado
// Se obtuvo: transacciones de OTROS vendedores mezcladas — mismo
// riesgo de privacidad ya documentado para getByBuyer().


    @Test
    void getBySeller_sinTransacciones_retorna200ConListaVacia() throws Exception {
        // ARRANGE
        Long sellerId = 99L;
        when(transactionService.getBySeller(sellerId)).thenReturn(List.of());

        // ACT + ASSERT
        mockMvc.perform(get("/transactions/seller/{sellerId}", sellerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // VERIFY
        verify(transactionService, times(1)).getBySeller(sellerId);
    }

// NOTA PARA QA (no es un caso de falla, es una aclaración de contrato):
// 200 OK + lista vacía representa "el vendedor no tiene
// transacciones", no un error del sistema.

    @Test
    void getByDateRange_conTransaccionesEnElRango_retorna200ConLista() throws Exception {
        // ARRANGE
        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 6, 13, 23, 59, 59);

        TransactionResponse tx1 = new TransactionResponse(
                1L, 1L, 2L, 3L, "id-bl", 5000L,
                LocalDateTime.of(2026, 3, 15, 10, 0));

        when(transactionService.getByDateRange(from, to)).thenReturn(List.of(tx1));

        // ACT + ASSERT: las fechas se envían como query params en
        // formato ISO, igual que ya hicimos en ms-reports.
        mockMvc.perform(get("/transactions/range")
                        .param("from", "2026-01-01T00:00:00")
                        .param("to", "2026-06-13T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1));

        // VERIFY
        verify(transactionService, times(1)).getByDateRange(from, to);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: solo transacciones cuyo completedAt esté DENTRO del
// rango [from, to] solicitado
// Se obtuvo: transacciones fuera del rango incluidas en la respuesta
// (por ejemplo, si alguien usa findAll() en vez de
// findByCompletedAtBetween(from, to)), rompiendo los reportes de
// ms-reports que dependen de este endpoint para calcular totales
// por período exacto.


    @Test
    void getByDateRange_sinTransaccionesEnElRango_retorna200ConListaVacia() throws Exception {
        // ARRANGE: mismo criterio de contrato ya aplicado — sin
        // resultados en el rango es un resultado válido, no un error.
        LocalDateTime from = LocalDateTime.of(2020, 1, 1, 0, 0, 0);
        LocalDateTime to = LocalDateTime.of(2020, 12, 31, 23, 59, 59);

        when(transactionService.getByDateRange(from, to)).thenReturn(List.of());

        // ACT + ASSERT
        mockMvc.perform(get("/transactions/range")
                        .param("from", "2020-01-01T00:00:00")
                        .param("to", "2020-12-31T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // VERIFY
        verify(transactionService, times(1)).getByDateRange(from, to);
    }

// NOTA PARA QA (no es un caso de falla, es una aclaración de contrato):
// 200 OK + lista vacía representa "no hubo transacciones en ese
// período" (por ejemplo, un rango anterior al lanzamiento del
// marketplace), no un error. Igual que en ms-reports, este
// endpoint tampoco valida "from <= to" — el Swagger documenta un
// posible 400 ("Parámetros de las fechas inválidas") que en la
// práctica nunca se dispara con el código actual.

    @Test
    void getAll_conTransaccionesExistentes_retorna200ConLista() throws Exception {
        // ARRANGE: este endpoint NO tiene seguridad real implementada
        // (SecurityConfig usa permitAll() para todas las rutas), así que
        // no probamos ningún escenario de "acceso denegado" — el 403
        // documentado en el Swagger nunca ocurre con el código actual.
        TransactionResponse tx1 = new TransactionResponse(
                1L, 1L, 2L, 3L, "id-bl", 5000L,
                LocalDateTime.of(2026, 6, 12, 14, 30));
        TransactionResponse tx2 = new TransactionResponse(
                2L, 4L, 5L, 6L, "id-mox", 3000L,
                LocalDateTime.of(2026, 6, 13, 10, 0));

        when(transactionService.getAll()).thenReturn(List.of(tx1, tx2));

        // ACT + ASSERT
        mockMvc.perform(get("/transactions/admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // VERIFY
        verify(transactionService, times(1)).getAll();
    }

// CASO HIPOTÉTICO DE FALLA (para QA) — HALLAZGO DE SEGURIDAD REAL:
// Se esperaba (según el Swagger y el comentario "// Solo ADMIN" en
// el código): que este endpoint requiera un rol de administrador,
// retornando 403 Forbidden para cualquier otro usuario
// Se obtuvo: SecurityConfig.filterChain() usa
// ".anyRequest().permitAll()", permitiendo que CUALQUIER persona,
// incluso sin autenticar, consulte TODAS las transacciones del
// marketplace (montos, compradores, vendedores) a través de este
// endpoint — una exposición de datos sensibles real y verificable
// en el código actual, no solo un caso hipotético
// Esto es consistente con lo observado en otros microservicios de
// Quandrix (la seguridad real probablemente se centraliza en el
// API Gateway), pero vale la pena que el equipo confirme
// explícitamente que ese filtro de Gateway realmente bloquea el
// acceso no autorizado a esta ruta específica antes de considerar
// este hallazgo resuelto — si el Gateway no lo cubre, esto sería
// un hallazgo de seguridad crítico para reportar de inmediato.


    @Test
    void existsCompletedTransaction_conTransaccionExistente_retorna200ConTrue() throws Exception {
        // ARRANGE
        Long buyerId = 1L;
        Long sellerId = 2L;

        when(transactionService.existsCompletedTransaction(buyerId, sellerId)).thenReturn(true);

        // ACT + ASSERT
        mockMvc.perform(get("/transactions/exists")
                        .param("buyerId", String.valueOf(buyerId))
                        .param("sellerId", String.valueOf(sellerId)))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        // VERIFY
        verify(transactionService, times(1)).existsCompletedTransaction(buyerId, sellerId);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: "true" cuando SÍ existe una transacción entre ese
// comprador y vendedor — este endpoint es consumido directamente
// por ms-reviews para decidir si permite crear una reseña
// Se obtuvo: "false" a pesar de existir una transacción real
// Esto podría pasar si alguien invierte por error los argumentos
// de existsByBuyerIdAndSellerId(buyerId, sellerId) a
// existsByBuyerIdAndSellerId(sellerId, buyerId) en el repositorio,
// rompiendo silenciosamente la validación central de ms-reviews
// para los usuarios del sistema.


    @Test
    void existsCompletedTransaction_sinTransaccion_retorna200ConFalse() throws Exception {
        // ARRANGE
        Long buyerId = 1L;
        Long sellerId = 99L;

        when(transactionService.existsCompletedTransaction(buyerId, sellerId)).thenReturn(false);

        // ACT + ASSERT
        mockMvc.perform(get("/transactions/exists")
                        .param("buyerId", String.valueOf(buyerId))
                        .param("sellerId", String.valueOf(sellerId)))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));

        // VERIFY
        verify(transactionService, times(1)).existsCompletedTransaction(buyerId, sellerId);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: "false" cuando NO existe ninguna transacción entre
// ese comprador y vendedor (caso "normal", no un error)
// Se obtuvo: una excepción inesperada en vez de simplemente "false"
// Este endpoint retorna un boolean simple — no hay ninguna razón
// para que el caso "no existe" lance una excepción, a diferencia de
// getByOrderId() donde "no existe" sí amerita un 404. La distinción
// es clara: aquí se está respondiendo una PREGUNTA de sí/no, no
// solicitando un RECURSO específico.
}