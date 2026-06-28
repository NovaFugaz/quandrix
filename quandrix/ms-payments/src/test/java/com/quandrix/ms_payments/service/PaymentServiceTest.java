package com.quandrix.ms_payments.service;

import com.quandrix.ms_payments.dto.PaymentRequest;
import com.quandrix.ms_payments.dto.PaymentResponse;
import com.quandrix.ms_payments.exception.PaymentNotFoundException;
import com.quandrix.ms_payments.exception.PaymentProcessingException;
import com.quandrix.ms_payments.model.Payment;
import com.quandrix.ms_payments.model.PaymentMethod;
import com.quandrix.ms_payments.model.PaymentStatus;
import com.quandrix.ms_payments.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void process_conDatosValidos_mapeaOrderIdAmountYMethodCorrectamente() {
        // ARRANGE: NO afirmamos nada sobre status/processedAt aquí,
        // porque esos dependen del 10% aleatorio del código real
        // (System.currentTimeMillis() % 10 == 0) y no podemos
        // controlarlos sin forceFailure. Este test se enfoca
        // exclusivamente en lo que SÍ es 100% determinístico:
        // el mapeo de los datos del request hacia la entidad Payment.
        PaymentRequest request = PaymentRequest.builder()
                .orderId(1L)
                .amount(5000L)
                .method("CREDIT_CARD")
                .forceFailure(false)
                .build();

        when(paymentRepository.existsByOrderId(1L)).thenReturn(false);
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // ACT
        PaymentResponse response = paymentService.process(request);

        // ASSERT: estos 3 campos se asignan ANTES de la rama aleatoria,
        // así que su valor es siempre el mismo sin importar el azar.
        assertThat(response.getOrderId()).isEqualTo(1L);
        assertThat(response.getAmount()).isEqualTo(5000L);
        assertThat(response.getMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);

        // VERIFY: confirmamos que efectivamente se intentó persistir
        // un Payment, capturando el objeto real para validar también
        // que el status final sea uno de los dos válidos (sin afirmar
        // cuál específicamente, ya que ambos son resultados legítimos
        // de este camino no forzado).
        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getStatus())
                .isIn(PaymentStatus.APPROVED, PaymentStatus.REJECTED);
    }


// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: orderId, amount y method del Payment guardado
// coinciden EXACTAMENTE con los valores del PaymentRequest original
// Se obtuvo: alguno de estos 3 campos no se mapea correctamente
// (por ejemplo, si alguien confunde el orden de los setters al
// construir el objeto Payment dentro de PaymentService.process())
// Esto representaría un error grave: un pago registrado con el
// monto o metodo incorrecto, independientemente de si fue aprobado
// o rechazado por el azar.

    @Test
    void process_conForceFailureTrue_rechazaElPagoSinAsignarProcessedAt() {
        // ARRANGE: forceFailure=true garantiza el resultado, sin
        // depender de ninguna aleatoriedad.
        PaymentRequest request = PaymentRequest.builder()
                .orderId(2L)
                .amount(3000L)
                .method("DEBIT_CARD")
                .forceFailure(true)
                .build();

        when(paymentRepository.existsByOrderId(2L)).thenReturn(false);
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // ACT
        PaymentResponse response = paymentService.process(request);

        // ASSERT: ahora SÍ podemos afirmar el resultado exacto, sin
        // ambigüedad ni dependencia del azar.
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.REJECTED);
        assertThat(response.getProcessedAt()).isNull();
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: con forceFailure=true, el resultado es SIEMPRE
// REJECTED con processedAt=null, sin excepción
// Se obtuvo: a pesar de forceFailure=true, el pago se aprueba
// (status=APPROVED) en algunos casos, sugiriendo que el chequeo de
// forceFailure no tiene prioridad real sobre el azar
// Esto podría pasar si alguien reordena el código de
// PaymentService.process() y mueve la evaluación aleatoria ANTES
// del chequeo de "Boolean.TRUE.equals(request.getForceFailure())",
// dejando que el azar sobrescriba el resultado forzado en vez de
// que el forceFailure tenga prioridad absoluta como está diseñado.


    @Test
    void process_conPagoDuplicado_lanzaPaymentProcessingException() {
        // ARRANGE: ya existe un pago registrado para ese orderId.
        PaymentRequest request = PaymentRequest.builder()
                .orderId(1L)
                .amount(5000L)
                .method("CREDIT_CARD")
                .build();

        when(paymentRepository.existsByOrderId(1L)).thenReturn(true);

        // ACT + ASSERT
        PaymentProcessingException ex = assertThrows(
                PaymentProcessingException.class,
                () -> paymentService.process(request)
        );
        assertThat(ex.getMessage()).isEqualTo(
                "Ya existe un pago registrado para la orden 1");

        // VERIFY: al detectarse el duplicado, nunca se debió persistir
        // un segundo pago para la misma orden.
        verify(paymentRepository, never()).save(any());
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: PaymentProcessingException cuando ya existe un pago
// para el mismo orderId (la columna orderId es @Column(unique = true)
// en la entidad Payment, así que esta validación previene también
// una excepción real de violación de constraint en la base de datos)
// Se obtuvo: se intenta guardar un segundo pago para la misma orden,
// lo que en un entorno real fallaría con una excepción de base de
// datos (constraint violation) en vez de un error de negocio claro
// Esto podría pasar si alguien elimina la validación
// "if (paymentRepository.existsByOrderId(request.getOrderId()))".


    @Test
    void process_conMontoInvalido_lanzaPaymentProcessingException() {
        // ARRANGE: simulamos un monto <= 0. Nota: @Min(1) en
        // PaymentRequest debería bloquear esto antes de llegar al
        // service en un flujo real vía HTTP, pero como estamos probando
        // el service en aislamiento (sin el controller ni Bean
        // Validation), confirmamos que el service TAMBIÉN protege contra
        // esto de forma independiente — una buena práctica de "defensa
        // en profundidad", no una validación redundante e innecesaria.
        PaymentRequest request = PaymentRequest.builder()
                .orderId(1L)
                .amount(0L)
                .method("CREDIT_CARD")
                .build();

        when(paymentRepository.existsByOrderId(1L)).thenReturn(false);

        // ACT + ASSERT
        PaymentProcessingException ex = assertThrows(
                PaymentProcessingException.class,
                () -> paymentService.process(request)
        );
        assertThat(ex.getMessage()).isEqualTo("Monto debe ser positivo");

        // VERIFY
        verify(paymentRepository, never()).save(any());
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: PaymentProcessingException cuando amount <= 0,
// incluso si por alguna razón este request llegara al service sin
// haber pasado por la validación @Min(1) del controller (por ejemplo,
// si en el futuro se agrega otro punto de entrada al service que no
// use HTTP/Bean Validation, como un listener de eventos o un job)
// Se obtuvo: se procesa un pago con monto 0 o negativo, generando un
// registro financiero sin sentido en la base de datos
// Esto podría pasar si alguien elimina la validación interna
// "if (request.getAmount() <= 0)" confiando ÚNICAMENTE en que Bean
// Validation ya lo bloqueó — una mala práctica si el service llega
// a ser invocado desde otro lugar que no sea el controller HTTP.


    @Test
    void process_conMetodoInvalido_lanzaPaymentProcessingException() {
        // ARRANGE
        PaymentRequest request = PaymentRequest.builder()
                .orderId(1L)
                .amount(5000L)
                .method("BITCOIN")
                .build();

        when(paymentRepository.existsByOrderId(1L)).thenReturn(false);

        // ACT + ASSERT
        PaymentProcessingException ex = assertThrows(
                PaymentProcessingException.class,
                () -> paymentService.process(request)
        );
        assertThat(ex.getMessage()).isEqualTo(
                "Método de pago inválido: BITCOIN. Valores válidos: " +
                        "CREDIT_CARD, DEBIT_CARD, BANK_TRANSFER, CASH");

        // VERIFY
        verify(paymentRepository, never()).save(any());
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: PaymentProcessingException con el mensaje exacto
// listando los 4 métodos válidos del enum real
// Se obtuvo: una IllegalArgumentException sin capturar, propagándose
// como HTTP 500 en vez de 422
// Esto podría pasar si alguien elimina el try/catch alrededor de
// PaymentMethod.valueOf(...) en PaymentService.process().

    @Test
    void getByOrderId_conOrderIdExistente_retornaPago() {
        // ARRANGE
        Long orderId = 1L;
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setOrderId(orderId);
        payment.setAmount(5000L);
        payment.setMethod(PaymentMethod.CREDIT_CARD);
        payment.setStatus(PaymentStatus.APPROVED);

        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

        // ACT
        PaymentResponse response = paymentService.getByOrderId(orderId);

        // ASSERT
        assertThat(response.getOrderId()).isEqualTo(orderId);
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.APPROVED);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: response.getStatus() refleja exactamente el status
// real del pago almacenado
// Se obtuvo: response.getStatus() == null
// Esto podría pasar si alguien modifica el metodo privado toResponse()
// y olvida mapear el campo status al construir el PaymentResponse.


    @Test
    void getByOrderId_conOrderIdSinPago_lanzaPaymentNotFoundException() {
        // ARRANGE
        Long orderIdSinPago = 999L;
        when(paymentRepository.findByOrderId(orderIdSinPago)).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThrows(
                PaymentNotFoundException.class,
                () -> paymentService.getByOrderId(orderIdSinPago)
        );
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: PaymentNotFoundException cuando el repositorio no
// encuentra un pago para ese orderId (Optional vacío)
// Se obtuvo: NoSuchElementException sin capturar (HTTP 500 en vez
// de 404), mismo patrón de riesgo ya documentado en todos los demás
// microservicios al reemplazar .orElseThrow(...) por un .get() directo.
}