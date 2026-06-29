package com.quandrix.ms_transactions.service;

import com.quandrix.ms_transactions.dto.TransactionRequest;
import com.quandrix.ms_transactions.dto.TransactionResponse;
import com.quandrix.ms_transactions.exception.DuplicateTransactionException;
import com.quandrix.ms_transactions.exception.TransactionNotFoundException;
import com.quandrix.ms_transactions.model.Transaction;
import com.quandrix.ms_transactions.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void register_conOrderIdNuevo_persisteYRetornaTransaccion() {
        // ARRANGE
        TransactionRequest request = new TransactionRequest();
        request.setOrderId(1L);
        request.setBuyerId(2L);
        request.setSellerId(3L);
        request.setScryfallId("id-bl");
        request.setAmount(5000L);

        when(transactionRepository.findByOrderId(1L)).thenReturn(Optional.empty());

        Transaction guardada = new Transaction();
        guardada.setId(1L);
        guardada.setOrderId(1L);
        guardada.setBuyerId(2L);
        guardada.setSellerId(3L);
        guardada.setScryfallId("id-bl");
        guardada.setAmount(5000L);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(guardada);

        // ACT
        TransactionResponse response = transactionService.register(request);

        // ASSERT
        assertThat(response.getOrderId()).isEqualTo(1L);
        assertThat(response.getAmount()).isEqualTo(5000L);
    }

    @Test
    void register_conOrderIdDuplicado_lanzaDuplicateTransactionException() {
        // ARRANGE: ya existe una transacción para ese orderId.
        TransactionRequest request = new TransactionRequest();
        request.setOrderId(1L);
        request.setBuyerId(2L);
        request.setSellerId(3L);
        request.setScryfallId("id-bl");
        request.setAmount(5000L);

        Transaction existente = new Transaction();
        existente.setId(99L);
        existente.setOrderId(1L);
        when(transactionRepository.findByOrderId(1L)).thenReturn(Optional.of(existente));

        // ACT + ASSERT
        DuplicateTransactionException ex = assertThrows(
                DuplicateTransactionException.class,
                () -> transactionService.register(request)
        );
        assertThat(ex.getMessage()).isEqualTo("Ya existe una transacción para la orden: 1");

        // VERIFY: al detectarse el duplicado, nunca se debió guardar
        // una segunda transacción.
        verify(transactionRepository, never()).save(any());
    }

// CASOS HIPOTÉTICOS DE FALLA (para QA):
// Test 1 - Se esperaba: response.getAmount() == 5000, mapeado
// correctamente desde la entidad guardada
// Se obtuvo: response.getAmount() == null o incorrecto
// Esto podría pasar si toResponse() rompe el mapeo de algún campo.
//
// Test 2 - Se esperaba: DuplicateTransactionException cuando ya
// existe una transacción para ese orderId (reforzado también por
// @Column(unique = true) a nivel de base de datos)
// Se obtuvo: se intenta guardar una segunda transacción para la
// misma orden, lo que en un entorno real fallaría con una excepción
// de violación de constraint de base de datos en vez de un error
// de negocio claro — esto sería especialmente grave porque
// ms-orders podría reintentar registrar la transacción si su
// llamada anterior falló por timeout sin saber si realmente se
// había completado, y esta protección evita que eso genere un
// registro duplicado.

@Test
void getByOrderId_conOrderIdExistente_retornaTransaccion() {
    // ARRANGE
    Long orderId = 1L;
    Transaction transaction = new Transaction();
    transaction.setId(1L);
    transaction.setOrderId(orderId);
    transaction.setBuyerId(2L);
    transaction.setSellerId(3L);
    transaction.setAmount(5000L);

    when(transactionRepository.findByOrderId(orderId)).thenReturn(Optional.of(transaction));

    // ACT
    TransactionResponse response = transactionService.getByOrderId(orderId);

    // ASSERT
    assertThat(response.getOrderId()).isEqualTo(orderId);
    assertThat(response.getAmount()).isEqualTo(5000L);
}

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: response.getAmount() refleja exactamente el monto
// real de la transacción almacenada
// Se obtuvo: response.getAmount() == null
// Esto podría pasar si alguien modifica el metodo privado toResponse()
// y olvida mapear el campo amount al construir el TransactionResponse.


@Test
void getByOrderId_conOrderIdSinTransaccion_lanzaTransactionNotFoundException() {
    // ARRANGE
    Long orderIdSinTransaccion = 999L;
    when(transactionRepository.findByOrderId(orderIdSinTransaccion))
            .thenReturn(Optional.empty());

    // ACT + ASSERT
    TransactionNotFoundException ex = assertThrows(
            TransactionNotFoundException.class,
            () -> transactionService.getByOrderId(orderIdSinTransaccion)
    );
    assertThat(ex.getMessage()).isEqualTo("Transacción no encontrada: orderId: 999");


// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: TransactionNotFoundException cuando el repositorio
// no encuentra la transacción (Optional vacío)
// Se obtuvo: NoSuchElementException sin capturar (HTTP 500 en vez
// de 404), mismo patrón de riesgo ya documentado repetidamente en
// todos los microservicios de Quandrix al reemplazar
// .orElseThrow(...) por un .get() directo.

    }

    @Test
    void getByBuyer_conTransaccionesExistentes_retornaListaCorrecta() {
        // ARRANGE
        Long buyerId = 2L;
        Transaction tx1 = new Transaction();
        tx1.setId(1L);
        tx1.setOrderId(1L);
        tx1.setBuyerId(buyerId);
        tx1.setSellerId(3L);
        tx1.setAmount(5000L);

        when(transactionRepository.findByBuyerId(buyerId)).thenReturn(List.of(tx1));

        // ACT
        List<TransactionResponse> response = transactionService.getByBuyer(buyerId);

        // ASSERT
        assertThat(response).hasSize(1);
        assertThat(response.getFirst().getBuyerId()).isEqualTo(buyerId);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: solo transacciones del buyerId solicitado
// Se obtuvo: transacciones de otros compradores (findAll() en vez
// de findByBuyerId(buyerId)) — problema de privacidad.


    @Test
    void getBySeller_conTransaccionesExistentes_retornaListaCorrecta() {
        // ARRANGE
        Long sellerId = 3L;
        Transaction tx1 = new Transaction();
        tx1.setId(1L);
        tx1.setOrderId(1L);
        tx1.setBuyerId(2L);
        tx1.setSellerId(sellerId);
        tx1.setAmount(5000L);

        when(transactionRepository.findBySellerId(sellerId)).thenReturn(List.of(tx1));

        // ACT
        List<TransactionResponse> response = transactionService.getBySeller(sellerId);

        // ASSERT
        assertThat(response).hasSize(1);
        assertThat(response.getFirst().getSellerId()).isEqualTo(sellerId);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: solo transacciones del sellerId solicitado
// Se obtuvo: transacciones de otros vendedores mezcladas — mismo
// riesgo de privacidad ya documentado para getByBuyer().


    @Test
    void getByDateRange_conTransaccionesEnElRango_retornaListaCorrecta() {
        // ARRANGE
        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 6, 13, 23, 59, 59);

        Transaction tx1 = new Transaction();
        tx1.setId(1L);
        tx1.setOrderId(1L);
        tx1.setAmount(5000L);

        when(transactionRepository.findByCompletedAtBetween(from, to)).thenReturn(List.of(tx1));

        // ACT
        List<TransactionResponse> response = transactionService.getByDateRange(from, to);

        // ASSERT
        assertThat(response).hasSize(1);

        // VERIFY: confirmamos que se consultó con los parámetros
        // exactos recibidos, sin alterarlos.
        verify(transactionRepository, times(1)).findByCompletedAtBetween(from, to);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: el repositorio se consulta con el from/to EXACTOS
// recibidos como parámetros, sin modificarlos
// Se obtuvo: se consulta con fechas distintas a las solicitadas
// (por ejemplo, si alguien intercambia from y to por error al
// llamar a findByCompletedAtBetween), rompiendo los cálculos de
// ms-reports que dependen de este rango exacto.


    @Test
    void getAll_conTransaccionesExistentes_retornaListaCompleta() {
        // ARRANGE
        Transaction tx1 = new Transaction();
        tx1.setId(1L);
        tx1.setOrderId(1L);
        tx1.setAmount(5000L);

        Transaction tx2 = new Transaction();
        tx2.setId(2L);
        tx2.setOrderId(2L);
        tx2.setAmount(3000L);

        when(transactionRepository.findAll()).thenReturn(List.of(tx1, tx2));

        // ACT
        List<TransactionResponse> response = transactionService.getAll();

        // ASSERT
        assertThat(response).hasSize(2);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: getAll() retorna TODAS las transacciones del sistema
// sin ningún filtro (es el único metodo que intencionalmente no
// filtra por usuario, ya que está pensado para uso administrativo)
// Se obtuvo: la lista viene filtrada o incompleta por error
// Esto podría pasar si alguien modifica por error
// transactionRepository.findAll() y lo reemplaza con un metodo
// que aplique algún filtro no documentado.

    @Test
    void existsCompletedTransaction_conTransaccionExistente_retornaTrue() {
        // ARRANGE
        Long buyerId = 1L;
        Long sellerId = 2L;

        when(transactionRepository.existsByBuyerIdAndSellerId(buyerId, sellerId))
                .thenReturn(true);

        // ACT
        boolean exists = transactionService.existsCompletedTransaction(buyerId, sellerId);

        // ASSERT
        assertThat(exists).isTrue();

        // VERIFY: confirmamos que se consultó con el orden correcto de
        // argumentos (buyerId primero, sellerId segundo) — importante,
        // ya que este metodo es la validación central que usa
        // ms-reviews antes de permitir cualquier reseña.
        verify(transactionRepository, times(1)).existsByBuyerIdAndSellerId(buyerId, sellerId);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: true cuando existe una transacción completada entre
// ese comprador y vendedor específicos, en ese orden exacto
// Se obtuvo: false a pesar de existir la transacción, porque
// alguien invirtió los argumentos al llamar a
// existsByBuyerIdAndSellerId(sellerId, buyerId) en vez de
// (buyerId, sellerId) — esto rompería silenciosamente TODA la
// funcionalidad de reseñas del marketplace, ya que ms-reviews
// confía completamente en la respuesta de este metodo para decidir
// si permite o rechaza cada intento de reseña.


    @Test
    void existsCompletedTransaction_sinTransaccion_retornaFalse() {
        // ARRANGE
        Long buyerId = 1L;
        Long sellerId = 99L;

        when(transactionRepository.existsByBuyerIdAndSellerId(buyerId, sellerId))
                .thenReturn(false);

        // ACT
        boolean exists = transactionService.existsCompletedTransaction(buyerId, sellerId);

        // ASSERT
        assertThat(exists).isFalse();
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: false cuando NO existe ninguna transacción entre
// ese comprador y vendedor (caso normal, no un error)
// Se obtuvo: una excepción inesperada en vez de simplemente false
// Este metodo retorna un boolean simple desde una query "exists",
// que en Spring Data JPA nunca lanza excepción ni retorna null —
// siempre true o false, sin ambigüedad posible.
}