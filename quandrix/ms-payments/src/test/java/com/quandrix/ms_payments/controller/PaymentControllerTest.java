package com.quandrix.ms_payments.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quandrix.ms_payments.dto.PaymentRequest;
import com.quandrix.ms_payments.dto.PaymentResponse;
import com.quandrix.ms_payments.exception.PaymentNotFoundException;
import com.quandrix.ms_payments.exception.PaymentProcessingException;
import com.quandrix.ms_payments.model.PaymentMethod;
import com.quandrix.ms_payments.model.PaymentStatus;
import com.quandrix.ms_payments.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void process_conDatosValidos_retorna201ConPagoAprobado() throws Exception {
        // ARRANGE: PaymentRequest tiene @Builder, así que lo usamos
        // directamente (no necesitamos forceFailure aquí, el default
        // del builder ya es false).
        PaymentRequest request = PaymentRequest.builder()
                .orderId(1L)
                .amount(5000L)
                .method("CREDIT_CARD")
                .build();

        PaymentResponse fakeResponse = new PaymentResponse(
                1L, 1L, 5000L, PaymentMethod.CREDIT_CARD, PaymentStatus.APPROVED,
                LocalDateTime.of(2026, 6, 12, 14, 30),
                LocalDateTime.of(2026, 6, 12, 14, 30));

        when(paymentService.process(any(PaymentRequest.class))).thenReturn(fakeResponse);

        // ACT + ASSERT
        mockMvc.perform(post("/payments/process")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.amount").value(5000))
                .andExpect(jsonPath("$.method").value("CREDIT_CARD"));

        // VERIFY
        verify(paymentService, times(1)).process(any(PaymentRequest.class));
    }


// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 201 Created con "status": "APPROVED" y
// "processedAt" con una fecha asignada (no null)
// Se obtuvo: HTTP 201 Created con "processedAt": null a pesar de
// que el pago fue aprobado
// Esto podría pasar si alguien modifica PaymentService.process() y
// rompe la condición "if (payment.getStatus() == PaymentStatus.APPROVED)"
// que asigna processedAt, dejando ese campo sin completar incluso
// en pagos exitosos.

    @Test
    void process_conForceFailureTrue_retorna201ConPagoRechazado() throws Exception {
        // ARRANGE: usamos forceFailure=true para simular un rechazo de
        // forma determinística, tal como indica el comentario del propio
        // código ("Opción para los tests de defensa").
        PaymentRequest request = PaymentRequest.builder()
                .orderId(2L)
                .amount(3000L)
                .method("DEBIT_CARD")
                .forceFailure(true)
                .build();

        PaymentResponse fakeResponse = new PaymentResponse(
                2L, 2L, 3000L, PaymentMethod.DEBIT_CARD, PaymentStatus.REJECTED,
                null, // processedAt es null porque el pago fue rechazado
                LocalDateTime.of(2026, 6, 12, 14, 30));

        when(paymentService.process(any(PaymentRequest.class))).thenReturn(fakeResponse);

        // ACT + ASSERT: nota que sigue siendo 201 Created, no un error —
        // un pago "rechazado" es un resultado de negocio válido, no una
        // falla técnica del sistema.
        mockMvc.perform(post("/payments/process")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.processedAt").doesNotExist());

        // VERIFY
        verify(paymentService, times(1)).process(any(PaymentRequest.class));
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 201 Created (no un error) cuando el pago es
// rechazado — un rechazo es una respuesta de negocio normal, el
// sistema "funcionó correctamente" al procesar la solicitud y
// determinar que no se aprobaba
// Se obtuvo: HTTP 422 o algún código de error cuando el pago es
// rechazado, confundiendo "el pago no fue aprobado" (resultado
// válido) con "la solicitud de pago estaba mal formada" (error real)
// Esto podría pasar si alguien modifica PaymentController.process()
// y agrega lógica condicional que devuelva un código de error distinto
// según el status del PaymentResponse, en vez de siempre responder
// 201 Created y dejar que el cliente de la API inspeccione el campo
// "status" del body para saber si fue aprobado o rechazado.

    @Test
    void process_conPagoDuplicado_retorna422ConMensajeDeNegocio() throws Exception {
        // ARRANGE
        PaymentRequest request = PaymentRequest.builder()
                .orderId(1L)
                .amount(5000L)
                .method("CREDIT_CARD")
                .build();

        when(paymentService.process(any(PaymentRequest.class)))
                .thenThrow(new PaymentProcessingException(
                        "Ya existe un pago registrado para la orden 1"));

        // ACT + ASSERT: el código real usa 422 (Unprocessable Entity),
        // no 400 como sugiere el Swagger — confirmamos el comportamiento
        // real, documentado explícitamente.
        mockMvc.perform(post("/payments/process")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error").value(
                        "Ya existe un pago registrado para la orden 1"));

        // VERIFY
        verify(paymentService, times(1)).process(any(PaymentRequest.class));
    }

// CASO HIPOTÉTICO DE FALLA (para QA) — DISCREPANCIA DE DOCUMENTACIÓN:
// Se esperaba (según el código real): HTTP 422 Unprocessable Entity
// Se obtuvo (según lo que sugiere el Swagger del controller): alguien
// podría asumir erróneamente que este endpoint responde 400, y
// escribir código de cliente (frontend, otro microservicio) que
// solo maneje 400/404/500, sin reconocer el 422 — un consumidor de
// la API que no revise el código real (solo el Swagger) podría
// fallar al manejar este caso correctamente
// Vale la pena reportar esta discrepancia para actualizar el Swagger
// de PaymentController (cambiar la documentación de 400 a 422),
// un fix de bajo riesgo y alto valor para la claridad de la API.


    @Test
    void process_conMontoInvalido_retorna422ConMensajeDeNegocio() throws Exception {
        // ARRANGE: aquí usamos un monto que pasa la validación de Bean
        // Validation (@Min(1)) pero que técnicamente podría llegar a la
        // validación interna del service como <= 0 si, por ejemplo, esa
        // regla cambia en el futuro — el request mockeado refleja
        // directamente lo que el SERVICE retorna, no necesitamos que el
        // monto real viole @Min aquí, ya que estamos mockeando el service.
        PaymentRequest request = PaymentRequest.builder()
                .orderId(1L)
                .amount(1L) // válido para Bean Validation, pero forzamos
                // el escenario vía el mock del service
                .method("CREDIT_CARD")
                .build();

        when(paymentService.process(any(PaymentRequest.class)))
                .thenThrow(new PaymentProcessingException("Monto debe ser positivo"));

        // ACT + ASSERT
        mockMvc.perform(post("/payments/process")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error").value("Monto debe ser positivo"));

        // VERIFY
        verify(paymentService, times(1)).process(any(PaymentRequest.class));
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 422 cuando el service determina que el monto no
// es válido según SU PROPIA regla de negocio (independiente de la
// validación de Bean Validation del controller)
// Se obtuvo: HTTP 500, perdiendo la distinción entre un error de
// negocio claro y un error interno del servidor
// Esto podría pasar si alguien elimina el @ExceptionHandler de
// PaymentProcessingException.


    @Test
    void process_conMetodoInvalido_retorna422ConMensajeDeNegocio() throws Exception {
        // ARRANGE
        PaymentRequest request = PaymentRequest.builder()
                .orderId(1L)
                .amount(5000L)
                .method("BITCOIN") // no existe en el enum PaymentMethod
                .build();

        when(paymentService.process(any(PaymentRequest.class)))
                .thenThrow(new PaymentProcessingException(
                        "Método de pago inválido: BITCOIN. Valores válidos: " +
                                "CREDIT_CARD, DEBIT_CARD, BANK_TRANSFER, CASH"));

        // ACT + ASSERT
        mockMvc.perform(post("/payments/process")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error").value(
                        "Método de pago inválido: BITCOIN. Valores válidos: " +
                                "CREDIT_CARD, DEBIT_CARD, BANK_TRANSFER, CASH"));

        // VERIFY
        verify(paymentService, times(1)).process(any(PaymentRequest.class));
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 422 con un mensaje que liste los valores REALES
// del enum PaymentMethod (a diferencia del bug que documentamos en
// ms-notifications, aquí el mensaje SÍ corresponde exactamente a los
// valores reales del enum)
// Se obtuvo: HTTP 500, perdiendo el mensaje útil que ayuda al cliente
// de la API a corregir su solicitud
// Esto podría pasar si alguien elimina el @ExceptionHandler de
// PaymentProcessingException.

    @Test
    void getByOrderId_conOrderIdExistente_retorna200ConPago() throws Exception {
        // ARRANGE
        Long orderId = 1L;
        PaymentResponse fakeResponse = new PaymentResponse(
                1L, orderId, 5000L, PaymentMethod.CREDIT_CARD, PaymentStatus.APPROVED,
                LocalDateTime.of(2026, 6, 12, 14, 30),
                LocalDateTime.of(2026, 6, 12, 14, 30));

        when(paymentService.getByOrderId(orderId)).thenReturn(fakeResponse);

        // ACT + ASSERT
        mockMvc.perform(get("/payments/order/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // VERIFY
        verify(paymentService, times(1)).getByOrderId(orderId);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 200 OK al consultar el pago de una orden existente
// Se obtuvo: HTTP 404 Not Found a pesar de que el pago existe
// Esto podría pasar si alguien cambia por error el tipo del
// @PathVariable de Long a String en PaymentController.getByOrderId(),
// causando un error de conversión.


    @Test
    void getByOrderId_conOrderIdSinPago_retorna404NotFound() throws Exception {
        // ARRANGE
        Long orderIdSinPago = 999L;

        when(paymentService.getByOrderId(orderIdSinPago))
                .thenThrow(new PaymentNotFoundException(orderIdSinPago));

        // ACT + ASSERT
        mockMvc.perform(get("/payments/order/{orderId}", orderIdSinPago))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(
                        "Pago no encontrado para orderId: " + orderIdSinPago));

        // VERIFY
        verify(paymentService, times(1)).getByOrderId(orderIdSinPago);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 404 Not Found cuando no existe ningún pago
// registrado para ese orderId
// Se obtuvo: HTTP 500 Internal Server Error
// Esto podría pasar si alguien elimina el @ExceptionHandler de
// PaymentNotFoundException.
}