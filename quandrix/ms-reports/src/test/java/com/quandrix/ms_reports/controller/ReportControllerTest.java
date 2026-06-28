package com.quandrix.ms_reports.controller;

import com.quandrix.ms_reports.dto.SalesReportResponse;
import com.quandrix.ms_reports.dto.TopCardResponse;
import com.quandrix.ms_reports.dto.TopSellerResponse;
import com.quandrix.ms_reports.exception.ReportGenerationException;
import com.quandrix.ms_reports.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReportController.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    @Test
    void getSalesReport_conRangoValido_retorna200ConReporte() throws Exception {
        // ARRANGE
        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 6, 13, 23, 59, 59);

        SalesReportResponse fakeResponse = new SalesReportResponse(
                from, to, 150, 75000L, 500L);

        when(reportService.getSalesReport(from, to)).thenReturn(fakeResponse);

        // ACT + ASSERT: las fechas se envían como query params en
        // formato ISO (igual que las recibiría un cliente HTTP real).
        mockMvc.perform(get("/reports/sales")
                        .param("from", "2026-01-01T00:00:00")
                        .param("to", "2026-06-13T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTransactions").value(150))
                .andExpect(jsonPath("$.totalAmount").value(75000))
                .andExpect(jsonPath("$.averageAmount").value(500));

        // VERIFY
        verify(reportService, times(1)).getSalesReport(from, to);
    }


// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: averageAmount == 500, calculado como totalAmount/totalTransactions
// Se obtuvo: averageAmount == 0 o un valor incorrecto
// Esto podría pasar si alguien modifica ReportService.getSalesReport()
// y rompe el cálculo de Math.round((double) totalAmount / transactions.size()),
// por ejemplo haciendo la división con enteros en vez de forzar el cast
// a double primero, truncando el resultado en vez de redondearlo
// correctamente (un error clásico de división entera en Java).


    @Test
    void getSalesReport_sinTransaccionesEnElRango_retorna200ConCeros() throws Exception {
        // ARRANGE
        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 1, 31, 23, 59, 59);

        SalesReportResponse fakeResponse = new SalesReportResponse(from, to, 0, 0L, 0L);

        when(reportService.getSalesReport(from, to)).thenReturn(fakeResponse);

        // ACT + ASSERT
        mockMvc.perform(get("/reports/sales")
                        .param("from", "2026-01-01T00:00:00")
                        .param("to", "2026-01-31T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTransactions").value(0))
                .andExpect(jsonPath("$.totalAmount").value(0))
                .andExpect(jsonPath("$.averageAmount").value(0));

        // VERIFY
        verify(reportService, times(1)).getSalesReport(from, to);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 200 OK con ceros cuando no hay transacciones en
// el rango (un resultado legítimo, no un error)
// Se obtuvo: una ArithmeticException o un valor NaN al intentar
// calcular el promedio (totalAmount / transactions.size()) cuando
// transactions.size() es 0
// Esto podría pasar si alguien elimina por error el
// "if (transactions.isEmpty())" en ReportService.getSalesReport(),
// dejando que el cálculo de averageAmount intente dividir por cero
// directamente en vez de retornar el reporte vacío de forma controlada.

    @Test
    void getSalesReport_conErrorDeFeign_retorna500ConMensaje() throws Exception {
        // ARRANGE: simulamos que TransactionClient (vía el service) falla
        // por cualquier motivo — el código real captura "Exception"
        // genérico, así que no importa el tipo exacto.
        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 1, 31, 23, 59, 59);

        when(reportService.getSalesReport(from, to))
                .thenThrow(new ReportGenerationException("ms-transactions no disponible"));

        // ACT + ASSERT
        mockMvc.perform(get("/reports/sales")
                        .param("from", "2026-01-01T00:00:00")
                        .param("to", "2026-01-31T23:59:59"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value(
                        "Error al generar reporte: ms-transactions no disponible"));

        // VERIFY
        verify(reportService, times(1)).getSalesReport(from, to);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 500 con un mensaje que incluya el detalle del
// error subyacente ("ms-transactions no disponible")
// Se obtuvo: HTTP 500 con el mensaje genérico "Error interno del
// servidor", perdiendo el detalle específico de qué falló
// Esto podría pasar si alguien elimina por error el
// @ExceptionHandler específico de ReportGenerationException en
// GlobalExceptionHandler, dejando que caiga en el handler genérico
// de Exception.class, que oculta el mensaje real — un problema para
// debugging, ya que el equipo de soporte no sabría si el problema
// es ms-transactions caído, un timeout de red, u otra causa.

    @Test
    void getTopSellers_conLimitExplicito_retorna200ConLista() throws Exception {
        // ARRANGE
        TopSellerResponse seller1 = new TopSellerResponse(1L, 50, 25000L);
        TopSellerResponse seller2 = new TopSellerResponse(2L, 30, 15000L);

        when(reportService.getTopSellers(5)).thenReturn(List.of(seller1, seller2));

        // ACT + ASSERT
        mockMvc.perform(get("/reports/top-sellers").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].sellerId").value(1))
                .andExpect(jsonPath("$[0].totalSales").value(50))
                .andExpect(jsonPath("$[0].totalRevenue").value(25000))
                .andExpect(jsonPath("$[1].sellerId").value(2));

        // VERIFY
        verify(reportService, times(1)).getTopSellers(5);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: la lista respeta el orden DESCENDENTE por totalSales
// (seller1 con 50 ventas aparece antes que seller2 con 30)
// Se obtuvo: el orden no respeta el ranking esperado, mostrando
// vendedores en un orden arbitrario o ascendente
// Esto podría pasar si alguien elimina el ".reversed()" en
// "Comparator.comparingLong(TopSellerResponse::getTotalSales).reversed()"
// dentro de ReportService.getTopSellers(), invirtiendo el ranking y
// mostrando a los vendedores con MENOS ventas primero — justo lo
// opuesto a lo que un reporte de "top vendedores" debería mostrar.

    @Test
    void getTopSellers_sinLimitExplicito_usaElValorPorDefecto10() throws Exception {
        // ARRANGE: no enviamos el parámetro "limit" en absoluto.
        when(reportService.getTopSellers(10)).thenReturn(List.of());

        // ACT + ASSERT
        mockMvc.perform(get("/reports/top-sellers"))
                .andExpect(status().isOk());

        // VERIFY: confirmamos que, sin enviar el parámetro, el controller
        // efectivamente llamó al service con 10 (el defaultValue), no con
        // 0 ni con cualquier otro valor.
        verify(reportService, times(1)).getTopSellers(10);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: al omitir "limit" en la URL, el controller usa 10
// como valor por defecto (según @RequestParam(defaultValue = "10"))
// Se obtuvo: el controller llama al service con limit=0, mostrando
// una lista siempre vacía cuando el usuario no especifica un límite
// Esto podría pasar si alguien elimina por error el atributo
// "defaultValue = "10"" de la anotación @RequestParam en
// ReportController.getTopSellers(), dejando que Spring use el valor
// por defecto de un int primitivo (0) en su lugar, en vez de fallar
// con un error claro de parámetro faltante o usar el 10 documentado.

    @Test
    void getTopCards_conLimitExplicito_retorna200ConLista() throws Exception {
        // ARRANGE
        TopCardResponse card1 = new TopCardResponse("id-black-lotus", 150);
        TopCardResponse card2 = new TopCardResponse("id-mox-sapphire", 80);

        when(reportService.getTopCards(5)).thenReturn(List.of(card1, card2));

        // ACT + ASSERT
        mockMvc.perform(get("/reports/top-cards").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].scryfallId").value("id-black-lotus"))
                .andExpect(jsonPath("$[0].totalSales").value(150))
                .andExpect(jsonPath("$[1].scryfallId").value("id-mox-sapphire"));

        // VERIFY
        verify(reportService, times(1)).getTopCards(5);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: la lista respeta el orden DESCENDENTE por totalSales
// (card1 con 150 ventas aparece antes que card2 con 80)
// Se obtuvo: el orden está invertido o es arbitrario
// Esto podría pasar si alguien elimina el ".reversed()" en
// ReportService.getTopCards(), mismo riesgo ya documentado para
// getTopSellers() — ambos métodos comparten el mismo patrón de
// ordenamiento y son vulnerables al mismo tipo de error.


    @Test
    void getTopCards_sinLimitExplicito_usaElValorPorDefecto10() throws Exception {
        // ARRANGE
        when(reportService.getTopCards(10)).thenReturn(List.of());

        // ACT + ASSERT
        mockMvc.perform(get("/reports/top-cards"))
                .andExpect(status().isOk());

        // VERIFY
        verify(reportService, times(1)).getTopCards(10);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: al omitir "limit", se usa 10 como valor por defecto
// Se obtuvo: limit=0, mostrando siempre una lista vacía
// Mismo riesgo ya documentado para getTopSellers() sin límite explícito.

    @Test
    void getSummary_conTransacciones_retorna200ConResumenCompleto() throws Exception {
        // ARRANGE: a diferencia de getSalesReport(), aquí from/to NO son
        // parámetros del usuario — son calculados por el service a partir
        // de las fechas reales (oldest/newest) de las transacciones.
        LocalDateTime oldest = LocalDateTime.of(2025, 3, 1, 10, 0, 0);
        LocalDateTime newest = LocalDateTime.of(2026, 6, 13, 18, 30, 0);

        SalesReportResponse fakeResponse = new SalesReportResponse(
                oldest, newest, 500, 250000L, 500L);

        when(reportService.getGeneralSummary()).thenReturn(fakeResponse);

        // ACT + ASSERT: este endpoint no recibe ningún parámetro.
        mockMvc.perform(get("/reports/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTransactions").value(500))
                .andExpect(jsonPath("$.totalAmount").value(250000))
                .andExpect(jsonPath("$.from").value("2025-03-01T10:00:00"))
                .andExpect(jsonPath("$.to").value("2026-06-13T18:30:00"));

        // VERIFY
        verify(reportService, times(1)).getGeneralSummary();
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: "from" representa la fecha de la transacción MÁS
// ANTIGUA registrada en el sistema (no una fecha fija ni un
// parámetro enviado por el usuario, ya que este endpoint no acepta
// ninguno)
// Se obtuvo: "from" siempre es null, o muestra una fecha incorrecta
// que no corresponde a ninguna transacción real
// Esto podría pasar si alguien modifica ReportService.getGeneralSummary()
// y rompe el cálculo de "oldest" (por ejemplo, usando
// Comparator.reverseOrder() en vez de Comparator.naturalOrder() al
// buscar el mínimo), retornando la fecha más reciente en el campo
// que debería mostrar la más antigua.


    @Test
    void getSummary_sinTransacciones_retorna200ConCerosYFechasNulas() throws Exception {
        // ARRANGE: aún no hay ninguna transacción registrada en el
        // sistema — caso distinto a "sin transacciones EN EL RANGO" de
        // getSalesReport(), porque aquí no hay rango que filtrar, es
        // simplemente "el sistema está vacío".
        SalesReportResponse fakeResponse = new SalesReportResponse(
                null, null, 0, 0L, 0L);

        when(reportService.getGeneralSummary()).thenReturn(fakeResponse);

        // ACT + ASSERT: from/to deben ser null en el JSON (no una fecha
        // arbitraria ni un error), ya que no hay transacciones de las
        // cuales calcular oldest/newest.
        mockMvc.perform(get("/reports/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTransactions").value(0))
                .andExpect(jsonPath("$.from").doesNotExist())
                .andExpect(jsonPath("$.to").doesNotExist());

        // VERIFY
        verify(reportService, times(1)).getGeneralSummary();
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: "from"/"to" son null cuando no hay ninguna transacción
// en absoluto en el sistema (caso de un marketplace recién lanzado,
// sin ventas aún)
// Se obtuvo: una NoSuchElementException sin capturar al intentar
// calcular oldest/newest sobre un stream vacío
// Esto podría pasar si alguien reemplaza el ".min(...).orElse(null)"
// por un ".min(...).get()" directo en ReportService.getGeneralSummary(),
// rompiendo el manejo explícito del caso "lista de transacciones vacía"
// que el código actual maneja correctamente con el "if (transactions
// .isEmpty())" justo antes.
}