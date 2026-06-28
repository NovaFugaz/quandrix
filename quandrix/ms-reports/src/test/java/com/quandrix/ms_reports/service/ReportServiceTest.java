package com.quandrix.ms_reports.service;

import com.quandrix.ms_reports.client.TransactionClient;
import com.quandrix.ms_reports.dto.SalesReportResponse;
import com.quandrix.ms_reports.dto.TopCardResponse;
import com.quandrix.ms_reports.dto.TopSellerResponse;
import com.quandrix.ms_reports.dto.TransactionResponse;
import com.quandrix.ms_reports.exception.ReportGenerationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private TransactionClient transactionClient;

    @InjectMocks
    private ReportService reportService;

    @Test
    void getSalesReport_conTransacciones_calculaTotalesCorrectamente() {
        // ARRANGE: 3 transacciones con montos conocidos, para poder
        // verificar a mano el total y el promedio esperados.
        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 1, 31, 23, 59, 59);

        TransactionResponse tx1 = new TransactionResponse(
                1L, 1L, 1L, 1L, "id-1", 1000L, LocalDateTime.of(2026, 1, 5, 10, 0));
        TransactionResponse tx2 = new TransactionResponse(
                2L, 2L, 2L, 2L, "id-2", 2000L, LocalDateTime.of(2026, 1, 10, 10, 0));
        TransactionResponse tx3 = new TransactionResponse(
                3L, 3L, 3L, 3L, "id-3", 3000L, LocalDateTime.of(2026, 1, 15, 10, 0));

        when(transactionClient.getByDateRange(from, to))
                .thenReturn(List.of(tx1, tx2, tx3));

        // ACT
        SalesReportResponse response = reportService.getSalesReport(from, to);

        // ASSERT: total = 1000+2000+3000 = 6000; promedio = 6000/3 = 2000
        assertThat(response.getTotalTransactions()).isEqualTo(3);
        assertThat(response.getTotalAmount()).isEqualTo(6000L);
        assertThat(response.getAverageAmount()).isEqualTo(2000L);
        assertThat(response.getFrom()).isEqualTo(from);
        assertThat(response.getTo()).isEqualTo(to);
    }


// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: totalAmount == 6000 (suma exacta de 1000+2000+3000)
// Se obtuvo: totalAmount == 6 (si alguien confunde el metodo
// .mapToLong(TransactionResponse::getAmount) con un .count() en vez
// de un .sum(), contando transacciones en vez de sumar sus montos)
// Esto representaría un error catastrófico para un reporte financiero,
// mostrando un monto total drásticamente distinto al real.

    @Test
    void getSalesReport_sinTransaccionesEnElRango_retornaCeros() {
        // ARRANGE
        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 1, 31, 23, 59, 59);

        when(transactionClient.getByDateRange(from, to)).thenReturn(List.of());

        // ACT
        SalesReportResponse response = reportService.getSalesReport(from, to);

        // ASSERT: el reporte debe conservar el from/to solicitado, pero
        // con todos los totales en cero — no debe intentar dividir por
        // cero al calcular el promedio.
        assertThat(response.getFrom()).isEqualTo(from);
        assertThat(response.getTo()).isEqualTo(to);
        assertThat(response.getTotalTransactions()).isEqualTo(0);
        assertThat(response.getTotalAmount()).isEqualTo(0L);
        assertThat(response.getAverageAmount()).isEqualTo(0L);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: averageAmount == 0 cuando no hay transacciones,
// sin lanzar ninguna excepción de división por cero
// Se obtuvo: una ArithmeticException al intentar calcular
// totalAmount/transactions.size() con size()=0
// Esto podría pasar si alguien elimina el "if (transactions.isEmpty())"
// que retorna el reporte en ceros ANTES de llegar al cálculo del
// promedio, dejando que el código intente dividir por cero directamente.


    @Test
    void getSalesReport_conErrorDeFeign_lanzaReportGenerationException() {
        // ARRANGE: simulamos que TransactionClient lanza cualquier
        // excepción (el código real captura "Exception" genérico).
        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 1, 31, 23, 59, 59);

        when(transactionClient.getByDateRange(from, to))
                .thenThrow(new RuntimeException("Connection timed out"));

        // ACT + ASSERT
        ReportGenerationException ex = assertThrows(
                ReportGenerationException.class,
                () -> reportService.getSalesReport(from, to)
        );
        assertThat(ex.getMessage()).isEqualTo("Error al generar reporte: Connection timed out");
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: ReportGenerationException envolviendo el mensaje
// original del error de Feign ("Connection timed out"), preservando
// el detalle para facilitar el debugging
// Se obtuvo: una RuntimeException sin capturar, propagándose
// directamente sin traducirse a la excepción de negocio esperada
// Esto podría pasar si alguien elimina el try/catch alrededor de
// "transactionClient.getByDateRange(from, to)" en
// ReportService.getSalesReport(), dejando que cualquier falla de
// red o de Feign se propague sin control hacia las capas superiores.

    @Test
    void getTopSellers_conMultiplesTransacciones_agrupaOrdenaYLimita() {
        // ARRANGE: 2 transacciones del vendedor 1 (total 3000) y 1 del
        // vendedor 2 (total 1000) — el vendedor 1 debe quedar primero
        // en el ranking por tener mayor revenue total.
        TransactionResponse tx1 = new TransactionResponse(
                1L, 1L, 10L, 1L, "id-a", 1000L, LocalDateTime.now());
        TransactionResponse tx2 = new TransactionResponse(
                2L, 2L, 10L, 1L, "id-b", 2000L, LocalDateTime.now());
        TransactionResponse tx3 = new TransactionResponse(
                3L, 3L, 20L, 2L, "id-c", 1000L, LocalDateTime.now());

        when(transactionClient.getAll()).thenReturn(List.of(tx1, tx2, tx3));

        // ACT: limit=10, más que suficiente para no recortar nada.
        List<TopSellerResponse> response = reportService.getTopSellers(10);

        // ASSERT: 2 vendedores en el resultado (agrupados correctamente),
        // el vendedor 1 primero (3000 > 1000), con sus totales correctos.
        assertThat(response).hasSize(2);
        assertThat(response.getFirst().getSellerId()).isEqualTo(1L);
        assertThat(response.get(0).getTotalSales()).isEqualTo(2); // 2 transacciones
        assertThat(response.get(0).getTotalRevenue()).isEqualTo(3000L);
        assertThat(response.get(1).getSellerId()).isEqualTo(2L);
        assertThat(response.get(1).getTotalRevenue()).isEqualTo(1000L);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: el vendedor 1 (revenue=3000) aparece ANTES que el
// vendedor 2 (revenue=1000) en el ranking
// Se obtuvo: el orden es arbitrario o ascendente, mostrando al
// vendedor con MENOS ingresos primero
// Esto podría pasar si alguien elimina el ".reversed()" del
// Comparator en ReportService.getTopSellers() — ya documentado
// anteriormente, pero ahora confirmado directamente contra la
// lógica real del service, no solo a través del controller.


    @Test
    void getTopSellers_conLimitMenorQueLosResultados_respetaElLimite() {
        // ARRANGE: 3 vendedores distintos, pero limit=2 — solo deben
        // aparecer los 2 con mayor revenue.
        TransactionResponse tx1 = new TransactionResponse(
                1L, 1L, 10L, 1L, "id-a", 3000L, LocalDateTime.now());
        TransactionResponse tx2 = new TransactionResponse(
                2L, 2L, 20L, 2L, "id-b", 2000L, LocalDateTime.now());
        TransactionResponse tx3 = new TransactionResponse(
                3L, 3L, 30L, 3L, "id-c", 1000L, LocalDateTime.now());

        when(transactionClient.getAll()).thenReturn(List.of(tx1, tx2, tx3));

        // ACT
        List<TopSellerResponse> response = reportService.getTopSellers(2);

        // ASSERT: solo 2 elementos, a pesar de que había 3 vendedores
        // distintos con transacciones.
        assertThat(response).hasSize(2);
        assertThat(response.get(0).getSellerId()).isEqualTo(1L); // mayor revenue
        assertThat(response.get(1).getSellerId()).isEqualTo(2L); // segundo mayor
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: con limit=2, la respuesta contiene EXACTAMENTE 2
// vendedores (los de mayor revenue), aunque existan 3 en los datos
// Se obtuvo: la respuesta incluye los 3 vendedores, ignorando el
// límite solicitado por el usuario
// Esto podría pasar si alguien elimina por error la llamada
// ".limit(limit)" en la cadena de stream de
// ReportService.getTopSellers(), retornando siempre todos los
// resultados sin importar lo que el cliente de la API haya pedido —
// un problema de rendimiento real si hay miles de vendedores y el
// frontend solo esperaba mostrar el top 10.

    @Test
    void getTopSellers_conErrorDeFeign_lanzaReportGenerationException() {
        // ARRANGE
        when(transactionClient.getAll())
                .thenThrow(new RuntimeException("ms-transactions no responde"));

        // ACT + ASSERT
        ReportGenerationException ex = assertThrows(
                ReportGenerationException.class,
                () -> reportService.getTopSellers(10)
        );
        assertThat(ex.getMessage()).isEqualTo("Error al generar reporte: ms-transactions no responde");
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: ReportGenerationException envolviendo el error de Feign
// Se obtuvo: una RuntimeException sin capturar
// Mismo patrón de riesgo ya documentado en getSalesReport(): eliminar
// el try/catch alrededor de transactionClient.getAll().


    @Test
    void getTopCards_conMultiplesTransacciones_agrupaOrdenaYLimita() {
        // ARRANGE: 2 transacciones de la carta "id-a" y 1 de "id-b" —
        // "id-a" debe quedar primera por tener más ventas (conteo, no monto).
        TransactionResponse tx1 = new TransactionResponse(
                1L, 1L, 10L, 100L, "id-a", 1000L, LocalDateTime.now());
        TransactionResponse tx2 = new TransactionResponse(
                2L, 2L, 20L, 200L, "id-a", 5000L, LocalDateTime.now());
        TransactionResponse tx3 = new TransactionResponse(
                3L, 3L, 30L, 300L, "id-b", 2000L, LocalDateTime.now());

        when(transactionClient.getAll()).thenReturn(List.of(tx1, tx2, tx3));

        // ACT
        List<TopCardResponse> response = reportService.getTopCards(10);

        // ASSERT: "id-a" tiene 2 ventas (sin importar el monto, ya que
        // este metodo cuenta transacciones, no suma montos — a diferencia
        // de getTopSellers()), "id-b" tiene 1.
        assertThat(response).hasSize(2);
        assertThat(response.get(0).getScryfallId()).isEqualTo("id-a");
        assertThat(response.get(0).getTotalSales()).isEqualTo(2L);
        assertThat(response.get(1).getScryfallId()).isEqualTo("id-b");
        assertThat(response.get(1).getTotalSales()).isEqualTo(1L);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: el ranking de cartas se basa en CANTIDAD de
// transacciones (Collectors.counting()), no en el monto total vendido
// — por eso "id-a" gana con 2 ventas a pesar de que individualmente
// cada venta pudiera valer menos que la de "id-b"
// Se obtuvo: el ranking se basa por error en la suma de montos en
// vez de en el conteo, confundiendo la métrica de "carta más popular"
// (más transada) con "carta que generó más dinero"
// Esto podría pasar si alguien modifica
// "Collectors.groupingBy(TransactionResponse::getScryfallId,
// Collectors.counting())" en ReportService.getTopCards() y lo
// reemplaza por Collectors.summingLong(TransactionResponse::getAmount),
// cambiando silenciosamente el significado del reporte sin que el
// nombre del metodo ("topCards" = más transadas) lo refleje.


    @Test
    void getTopCards_conLimitMenorQueLosResultados_respetaElLimite() {
        // ARRANGE: 3 cartas distintas, pero limit=2.
        TransactionResponse tx1 = new TransactionResponse(
                1L, 1L, 10L, 100L, "id-a", 1000L, LocalDateTime.now());
        TransactionResponse tx2 = new TransactionResponse(
                2L, 2L, 10L, 100L, "id-a", 1000L, LocalDateTime.now());
        TransactionResponse tx3 = new TransactionResponse(
                3L, 3L, 20L, 200L, "id-b", 1000L, LocalDateTime.now());
        TransactionResponse tx4 = new TransactionResponse(
                4L, 4L, 30L, 300L, "id-c", 1000L, LocalDateTime.now());

        when(transactionClient.getAll()).thenReturn(List.of(tx1, tx2, tx3, tx4));

        // ACT
        List<TopCardResponse> response = reportService.getTopCards(2);

        // ASSERT: solo 2 cartas, a pesar de haber 3 distintas en los datos.
        assertThat(response).hasSize(2);
        assertThat(response.getFirst().getScryfallId()).isEqualTo("id-a"); // 2 ventas
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: con limit=2, la respuesta contiene exactamente 2 cartas
// Se obtuvo: la respuesta incluye las 3 cartas, ignorando el límite
// Mismo riesgo ya documentado para getTopSellers() sin límite respetado.


    @Test
    void getTopCards_conErrorDeFeign_lanzaReportGenerationException() {
        // ARRANGE
        when(transactionClient.getAll())
                .thenThrow(new RuntimeException("ms-transactions no responde"));

        // ACT + ASSERT
        ReportGenerationException ex = assertThrows(
                ReportGenerationException.class,
                () -> reportService.getTopCards(10)
        );
        assertThat(ex.getMessage()).isEqualTo("Error al generar reporte: ms-transactions no responde");
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: ReportGenerationException envolviendo el error de Feign
// Se obtuvo: una RuntimeException sin capturar
// Mismo patrón de riesgo ya documentado repetidamente para los
// otros 3 métodos del service.

    @Test
    void getGeneralSummary_conTransacciones_calculaOldestYNewestCorrectamente() {
        // ARRANGE: 3 transacciones con fechas distintas, no en orden
        // cronológico en la lista — para confirmar que el cálculo de
        // oldest/newest realmente encuentra los extremos correctos, no
        // simplemente toma el primer o último elemento de la lista.
        LocalDateTime fechaIntermedia = LocalDateTime.of(2026, 3, 15, 12, 0);
        LocalDateTime fechaMasAntigua = LocalDateTime.of(2025, 1, 10, 8, 0);
        LocalDateTime fechaMasReciente = LocalDateTime.of(2026, 6, 20, 18, 0);

        TransactionResponse tx1 = new TransactionResponse(
                1L, 1L, 10L, 100L, "id-a", 1000L, fechaIntermedia);
        TransactionResponse tx2 = new TransactionResponse(
                2L, 2L, 20L, 200L, "id-b", 2000L, fechaMasAntigua);
        TransactionResponse tx3 = new TransactionResponse(
                3L, 3L, 30L, 300L, "id-c", 3000L, fechaMasReciente);

        when(transactionClient.getAll()).thenReturn(List.of(tx1, tx2, tx3));

        // ACT
        SalesReportResponse response = reportService.getGeneralSummary();

        // ASSERT: oldest debe ser la fecha de tx2 (la más antigua de las 3,
        // aunque esté en el medio de la lista), newest la de tx3.
        assertThat(response.getFrom()).isEqualTo(fechaMasAntigua);
        assertThat(response.getTo()).isEqualTo(fechaMasReciente);
        assertThat(response.getTotalTransactions()).isEqualTo(3);
        assertThat(response.getTotalAmount()).isEqualTo(6000L);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: "from" == fecha de tx2 (10 enero 2025, la más antigua
// de las 3, sin importar su posición en la lista)
// Se obtuvo: "from" == fecha de tx1 (simplemente la primera transacción
// de la lista, sin haber comparado realmente las 3 fechas)
// Esto podría pasar si alguien reemplaza el ".min(Comparator
// .naturalOrder())" por algo que solo tome el primer elemento del
// stream (como ".findFirst()"), perdiendo la comparación real entre
// todas las fechas y dando un resultado dependiente del orden
// (posiblemente aleatorio) en que Feign devuelve las transacciones.


    @Test
    void getGeneralSummary_sinTransacciones_retornaTodoEnCeroYFechasNulas() {
        // ARRANGE: el sistema no tiene ninguna transacción registrada aún.
        when(transactionClient.getAll()).thenReturn(List.of());

        // ACT
        SalesReportResponse response = reportService.getGeneralSummary();

        // ASSERT: a diferencia de getSalesReport() (donde from/to
        // conservan los parámetros que el usuario envió), aquí no hay
        // ningún parámetro de entrada — por eso from/to deben ser null,
        // no una fecha por defecto ni un valor inventado.
        assertThat(response.getFrom()).isNull();
        assertThat(response.getTo()).isNull();
        assertThat(response.getTotalTransactions()).isEqualTo(0);
        assertThat(response.getTotalAmount()).isEqualTo(0L);
        assertThat(response.getAverageAmount()).isEqualTo(0L);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: from/to son null cuando no hay ninguna transacción
// (no hay datos de los cuales calcular un rango real)
// Se obtuvo: una NoSuchElementException sin capturar, al usar
// ".min(...).get()" en vez de ".min(...).orElse(null)" sobre un
// stream vacío — mismo riesgo ya documentado desde el test del
// controller, ahora confirmado directamente contra el service.


    @Test
    void getGeneralSummary_conErrorDeFeign_lanzaReportGenerationException() {
        // ARRANGE
        when(transactionClient.getAll())
                .thenThrow(new RuntimeException("ms-transactions no responde"));

        // ACT + ASSERT
        ReportGenerationException ex = assertThrows(
                ReportGenerationException.class,
                () -> reportService.getGeneralSummary()
        );
        assertThat(ex.getMessage()).isEqualTo("Error al generar reporte: ms-transactions no responde");
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: ReportGenerationException envolviendo el error de Feign
// Se obtuvo: una RuntimeException sin capturar
// Mismo patrón de riesgo ya documentado para los otros 3 métodos —
// los 4 métodos del service comparten exactamente la misma estructura
// de manejo de errores, así que es consistente que comparta también
// el mismo tipo de vulnerabilidad si el try/catch se elimina por error.
}