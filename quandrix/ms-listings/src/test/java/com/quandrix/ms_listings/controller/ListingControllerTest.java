package com.quandrix.ms_listings.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quandrix.ms_listings.dto.ListingRequest;
import com.quandrix.ms_listings.dto.ListingResponse;
import com.quandrix.ms_listings.exception.InvalidListingException;
import com.quandrix.ms_listings.exception.ListingNotFoundException;
import com.quandrix.ms_listings.model.CardCondition;
import com.quandrix.ms_listings.model.ListingStatus;
import com.quandrix.ms_listings.service.ListingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ListingController.class)
class ListingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListingService listingService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void create_conDatosValidos_retorna201ConListingCreado() throws Exception {
        // ARRANGE: preparamos el request válido y la respuesta simulada
        // que debería devolver el service tras crear el listing.
        ListingRequest request = new ListingRequest();
        request.setSellerId(1L);
        request.setCardName("Black Lotus");
        request.setSetCode("LEA");
        request.setCondition("MINT");
        request.setPrice(5000L);
        request.setQuantity(3);

        ListingResponse fakeResponse = new ListingResponse(
                1L, 1L, "bd8fa8c8-7e1c-4f5d-a6d3-123456789abc",
                CardCondition.MINT, 5000L, 3,
                ListingStatus.ACTIVE, LocalDateTime.of(2026, 6, 12, 14, 30)
        );

        when(listingService.create(any(ListingRequest.class))).thenReturn(fakeResponse);

        // ACT: ejecutamos el endpoint POST /listings con MockMvc.
        // ASSERT: verificamos 201 Created y los datos esperados en el body.
        mockMvc.perform(post("/listings")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.sellerId").value(1))
                .andExpect(jsonPath("$.scryfallId").value("bd8fa8c8-7e1c-4f5d-a6d3-123456789abc"))
                .andExpect(jsonPath("$.condition").value("MINT"))
                .andExpect(jsonPath("$.price").value(5000))
                .andExpect(jsonPath("$.quantity").value(3))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // VERIFY: confirmamos que el controller delegó la creación
        // al service exactamente una vez.
        verify(listingService, times(1)).create(any(ListingRequest.class));
    }


// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 201 Created con el campo "status": "ACTIVE"
// Se obtuvo: HTTP 201 Created con "status": null
// Esto podría pasar si alguien cambia el orden de los argumentos en
// el constructor de ListingResponse (que usa @AllArgsConstructor
// posicional de Lombok), por ejemplo intercambiando "quantity" y
// "status" sin que el compilador lo detecte, ya que ambos podrían
// ser tipos compatibles en algún escenario de refactor descuidado.

    @Test
    void create_conSellerIdNulo_retorna400ConErrorDeValidacion() throws Exception {
        // ARRANGE: preparamos un request que omite el sellerId (obligatorio
        // según @NotNull en ListingRequest).
        ListingRequest request = new ListingRequest();
        request.setSellerId(null); // viola @NotNull
        request.setCardName("Black Lotus");
        request.setSetCode("LEA");
        request.setCondition("MINT");
        request.setPrice(5000L);
        request.setQuantity(3);

        // ACT: ejecutamos el endpoint POST /listings con MockMvc.
        // ASSERT: verificamos 400 Bad Request y el mensaje de error
        // específico para el campo sellerId.
        mockMvc.perform(post("/listings")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.sellerId").value("El sellerId es obligatorio"));

        // VERIFY: lo más importante de este test — el controller NUNCA
        // debió llegar a invocar al service, porque la validación de
        // Bean Validation actúa ANTES de que el metodo se ejecute.
        verify(listingService, never()).create(any());
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: listingService.create(...) NUNCA se invoca cuando
// el request no pasa las validaciones de Bean Validation.
// Se obtuvo: listingService.create(...) SÍ se invoca con un sellerId
// nulo, posiblemente causando un NullPointerException más adelante
// dentro del service (por ejemplo, en listingRepository.findBySellerId...
// con un parámetro null) en vez de fallar limpiamente con 400 en el
// borde de la API.
// Esto podría pasar si alguien elimina por error la anotación @Valid
// del parámetro @RequestBody en ListingController.create(), dejando
// que Spring deserialice el JSON sin aplicar ninguna validación.


    @Test
    void create_conVendedorInexistente_retorna400ConMensajeDeNegocio() throws Exception {
        // ARRANGE: el request es válido a nivel de Bean Validation, pero
        // simulamos que el service rechaza la creación porque el vendedor
        // no existe (regla de negocio verificada vía UserClient).
        ListingRequest request = new ListingRequest();
        request.setSellerId(999L);
        request.setCardName("Black Lotus");
        request.setSetCode("LEA");
        request.setCondition("MINT");
        request.setPrice(5000L);
        request.setQuantity(3);

        when(listingService.create(any(ListingRequest.class)))
                .thenThrow(new InvalidListingException("El vendedor con id 999 no existe"));

        // ACT: ejecutamos el endpoint POST /listings con MockMvc.
        // ASSERT: verificamos 400 Bad Request y el mensaje específico
        // de la excepción de negocio.
        mockMvc.perform(post("/listings")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El vendedor con id 999 no existe"));

        // VERIFY: a diferencia del test anterior, aquí el service SÍ debió
        // ser invocado — la validación pasó la primera barrera (Bean
        // Validation) y llegó hasta la lógica de negocio, que fue la que
        // realmente rechazó la solicitud.
        verify(listingService, times(1)).create(any(ListingRequest.class));
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 400 Bad Request con el mensaje específico
// "El vendedor con id 999 no existe"
// Se obtuvo: HTTP 500 Internal Server Error con un mensaje genérico
// Esto podría pasar si alguien elimina por error el @ExceptionHandler
// de InvalidListingException en GlobalExceptionHandler, dejando que
// la excepción caiga en el handler genérico de Exception.class, que
// no distingue entre errores de negocio (400) y errores reales del
// servidor (500) — una pérdida de información valiosa para el cliente
// de la API, que ya no sabría qué corregir en su solicitud.

    @Test
    void getById_conIdExistente_retorna200ConListing() throws Exception {
        // ARRANGE
        Long id = 1L;
        ListingResponse fakeResponse = new ListingResponse(
                id, 1L, "bd8fa8c8-7e1c-4f5d-a6d3-123456789abc",
                CardCondition.MINT, 5000L, 3,
                ListingStatus.ACTIVE, LocalDateTime.of(2026, 6, 12, 14, 30)
        );

        when(listingService.getById(id)).thenReturn(fakeResponse);

        // ACT + ASSERT
        mockMvc.perform(get("/listings/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // VERIFY
        verify(listingService, times(1)).getById(id);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 200 OK al consultar un listing existente por id
// Se obtuvo: HTTP 404 Not Found a pesar de que el id existe
// Esto podría pasar si alguien cambia por error el tipo del
// @PathVariable de Long a String en ListingController.getById(),
// causando que Spring no pueda hacer el binding correcto del path
// variable numérico y termine devolviendo un error de conversión
// antes de llegar siquiera a invocar al service.

    @Test
    void getById_conIdInexistente_retorna404NotFound() throws Exception {
        // ARRANGE
        Long idInexistente = 999L;

        when(listingService.getById(idInexistente))
                .thenThrow(new ListingNotFoundException(idInexistente));

        // ACT + ASSERT
        mockMvc.perform(get("/listings/{id}", idInexistente))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Listing no encontrado con id: " + idInexistente));

        // VERIFY
        verify(listingService, times(1)).getById(idInexistente);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 404 Not Found con mensaje "Listing no encontrado
// con id: 999"
// Se obtuvo: HTTP 500 Internal Server Error
// Esto podría pasar si alguien elimina el @ExceptionHandler de
// ListingNotFoundException en GlobalExceptionHandler, dejando que la
// excepción caiga en el handler genérico de Exception.class.

    @Test
    void getAllActive_retorna200ConListaDeListingsActivos() throws Exception {
        // ARRANGE
        ListingResponse listing1 = new ListingResponse(
                1L, 1L, "id-1", CardCondition.MINT, 5000L, 3,
                ListingStatus.ACTIVE, LocalDateTime.of(2026, 6, 12, 14, 30));
        ListingResponse listing2 = new ListingResponse(
                2L, 2L, "id-2", CardCondition.NEAR_MINT, 3000L, 1,
                ListingStatus.ACTIVE, LocalDateTime.of(2026, 6, 13, 10, 0));

        when(listingService.getAllActive()).thenReturn(List.of(listing1, listing2));

        // ACT + ASSERT
        mockMvc.perform(get("/listings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));

        // VERIFY
        verify(listingService, times(1)).getAllActive();
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: una lista con SOLO listings en estado ACTIVE
// Se obtuvo: una lista que incluye también listings WITHDRAWN o SOLD
// Esto podría pasar si alguien modifica ListingService.getAllActive()
// y reemplaza listingRepository.findByStatus(ListingStatus.ACTIVE)
// por un findAll() sin filtro, mostrando publicaciones que ya no
// deberían estar disponibles para compra a los usuarios del marketplace.

    @Test
    void getBySeller_conSellerIdExistente_retorna200ConSusListings() throws Exception {
        // ARRANGE
        Long sellerId = 1L;
        ListingResponse listing1 = new ListingResponse(
                1L, sellerId, "id-1", CardCondition.MINT, 5000L, 3,
                ListingStatus.ACTIVE, LocalDateTime.of(2026, 6, 12, 14, 30));

        when(listingService.getBySeller(sellerId)).thenReturn(List.of(listing1));

        // ACT + ASSERT
        mockMvc.perform(get("/listings/seller/{sellerId}", sellerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].sellerId").value(1));

        // VERIFY
        verify(listingService, times(1)).getBySeller(sellerId);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: solo los listings del sellerId solicitado (id=1)
// Se obtuvo: listings de OTROS vendedores mezclados en la respuesta
// Esto podría pasar si alguien modifica el repositorio y usa
// findByStatus(...) en vez de findBySellerIdAndStatus(sellerId, ...),
// filtrando solo por estado y olvidando el filtro por vendedor —
// un problema serio de privacidad/seguridad, ya que un vendedor podría
// ver o gestionar publicaciones que no le pertenecen.

    @Test
    void getBySeller_sinListingsParaEseVendedor_retorna200ConListaVacia() throws Exception {
        // ARRANGE: el vendedor existe, pero no tiene ningún listing activo.
        // NOTA: el Swagger de este endpoint documenta un posible 404
        // ("No existen publicaciones de ese vendedor"), pero el código real
        // de ListingService.getBySeller() nunca lanza una excepción —
        // simplemente retorna una lista vacía, que se traduce en 200 OK.
        // Tras revisión del equipo, se decidió mantener este comportamiento
        // (lista vacía = "0 listings publicados", no un error), por lo que
        // este test documenta el comportamiento real y vigente.
        Long sellerId = 42L;

        when(listingService.getBySeller(sellerId)).thenReturn(List.of());

        // ACT + ASSERT: 200 OK con un array vacío, no 404.
        mockMvc.perform(get("/listings/seller/{sellerId}", sellerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // VERIFY
        verify(listingService, times(1)).getBySeller(sellerId);
    }

// NOTA PARA QA (no es un caso de falla, es una aclaración de contrato):
// Si en el futuro el equipo decide alinear el comportamiento con el
// Swagger actual (lanzar 404 en vez de retornar lista vacía), este
// test deberá actualizarse intencionalmente junto con ese cambio.
// Hasta entonces, este test protege el comportamiento acordado:
// 200 OK + lista vacía representa "el vendedor no tiene publicaciones",
// no un error del sistema.

    @Test
    void getAllActive_sinListingsActivos_retorna200ConListaVacia() throws Exception {
        // ARRANGE: no hay ningún listing en estado ACTIVE actualmente.
        // NOTA: igual que con getBySeller(), el Swagger documenta un
        // posible 404 ("No se encontraron publicaciones activas"), pero
        // el comportamiento acordado por el equipo es 200 OK + lista vacía.
        when(listingService.getAllActive()).thenReturn(List.of());

        // ACT + ASSERT
        mockMvc.perform(get("/listings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // VERIFY
        verify(listingService, times(1)).getAllActive();
    }

// NOTA PARA QA (no es un caso de falla, es una aclaración de contrato):
// 200 OK + lista vacía representa "no hay publicaciones activas en
// este momento", no un error del sistema. Si el equipo decide alinear
// esto con el Swagger (404), este test debe actualizarse junto con
// ese cambio de diseño.


    @Test
    void getByCard_sinListingsParaEsaCarta_retorna200ConListaVacia() throws Exception {
        // ARRANGE: la carta existe en el catálogo, pero nadie la tiene
        // publicada actualmente.
        String scryfallId = "id-sin-publicaciones";

        when(listingService.getByCard(scryfallId)).thenReturn(List.of());

        // ACT + ASSERT
        mockMvc.perform(get("/listings/card/{scryfallId}", scryfallId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // VERIFY
        verify(listingService, times(1)).getByCard(scryfallId);
    }

// NOTA PARA QA (no es un caso de falla, es una aclaración de contrato):
// Mismo criterio que getAllActive() y getBySeller(): 200 OK + lista
// vacía representa "nadie vende esta carta actualmente", no un error.

    @Test
    void getByCard_conPublicacionesExistentes_retorna200ConListings() throws Exception {
        // ARRANGE
        String scryfallId = "bd8fa8c8-7e1c-4f5d-a6d3-123456789abc";
        ListingResponse listing1 = new ListingResponse(
                1L, 1L, scryfallId, CardCondition.MINT, 5000L, 3,
                ListingStatus.ACTIVE, LocalDateTime.of(2026, 6, 12, 14, 30));
        ListingResponse listing2 = new ListingResponse(
                2L, 2L, scryfallId, CardCondition.NEAR_MINT, 4000L, 1,
                ListingStatus.ACTIVE, LocalDateTime.of(2026, 6, 13, 9, 0));

        when(listingService.getByCard(scryfallId)).thenReturn(List.of(listing1, listing2));

        // ACT + ASSERT
        mockMvc.perform(get("/listings/card/{scryfallId}", scryfallId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].scryfallId").value(scryfallId))
                .andExpect(jsonPath("$[1].scryfallId").value(scryfallId));

        // VERIFY
        verify(listingService, times(1)).getByCard(scryfallId);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: ambos listings retornados corresponden al MISMO
// scryfallId solicitado (distintos vendedores, misma carta)
// Se obtuvo: listings de OTRAS cartas mezclados en la respuesta
// Esto podría pasar si alguien modifica el repositorio y usa
// findByStatus(...) en vez de findByScryfallIdAndStatus(scryfallId, ...),
// olvidando el filtro por carta específica.

    @Test
    void update_conDatosValidos_retorna200ConListingActualizado() throws Exception {
        // ARRANGE
        Long id = 1L;
        ListingRequest request = new ListingRequest();
        request.setSellerId(1L);
        request.setCardName("Black Lotus");
        request.setSetCode("LEA");
        request.setCondition("MINT");
        request.setPrice(6000L); // precio actualizado
        request.setQuantity(2);  // cantidad actualizada

        ListingResponse fakeResponse = new ListingResponse(
                id, 1L, "id-1", CardCondition.MINT, 6000L, 2,
                ListingStatus.ACTIVE, LocalDateTime.of(2026, 6, 12, 14, 30));

        when(listingService.update(eq(id), any(ListingRequest.class))).thenReturn(fakeResponse);

        // ACT + ASSERT
        mockMvc.perform(put("/listings/{id}", id)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(6000))
                .andExpect(jsonPath("$.quantity").value(2));

        // VERIFY
        verify(listingService, times(1)).update(eq(id), any(ListingRequest.class));
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 200 OK con el nuevo precio/cantidad reflejados
// Se obtuvo: HTTP 200 OK pero con los valores ORIGINALES sin actualizar
// Esto podría pasar si alguien modifica ListingService.update() y
// olvida llamar a listingRepository.save(listing) después de aplicar
// los cambios en memoria, perdiendo la persistencia de la actualización
// aunque el objeto en memoria sí se haya modificado correctamente.

    @Test
    void update_conIdInexistente_retorna404NotFound() throws Exception {
        // ARRANGE
        Long idInexistente = 999L;
        ListingRequest request = new ListingRequest();
        request.setSellerId(1L);
        request.setCardName("Black Lotus");
        request.setSetCode("LEA");
        request.setCondition("MINT");
        request.setPrice(6000L);
        request.setQuantity(2);

        when(listingService.update(eq(idInexistente), any(ListingRequest.class)))
                .thenThrow(new ListingNotFoundException(idInexistente));

        // ACT + ASSERT
        mockMvc.perform(put("/listings/{id}", idInexistente)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Listing no encontrado con id: " + idInexistente));

        // VERIFY
        verify(listingService, times(1)).update(eq(idInexistente), any(ListingRequest.class));
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 404 Not Found al intentar actualizar un listing
// que no existe
// Se obtuvo: HTTP 500 Internal Server Error
// Esto podría pasar si alguien elimina el @ExceptionHandler de
// ListingNotFoundException, dejando que caiga en el handler genérico.

    @Test
    void withdraw_conIdYSellerIdValidos_retorna200ConListingRetirado() throws Exception {
        // ARRANGE
        Long id = 1L;
        Long sellerId = 1L;

        ListingResponse fakeResponse = new ListingResponse(
                id, sellerId, "id-1", CardCondition.MINT, 5000L, 3,
                ListingStatus.WITHDRAWN, LocalDateTime.of(2026, 6, 12, 14, 30));

        when(listingService.withdraw(id, sellerId)).thenReturn(fakeResponse);

        // ACT + ASSERT
        mockMvc.perform(patch("/listings/{id}/withdraw", id)
                        .param("sellerId", String.valueOf(sellerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WITHDRAWN"));

        // VERIFY
        verify(listingService, times(1)).withdraw(id, sellerId);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 200 OK con "status": "WITHDRAWN"
// Se obtuvo: HTTP 200 OK con "status": "ACTIVE" (sin cambios reales)
// Esto podría pasar si alguien modifica ListingService.withdraw() y
// olvida llamar a listing.setStatus(ListingStatus.WITHDRAWN) antes de
// guardar, retornando el listing original sin aplicar el cambio de
// estado que el endpoint promete realizar.

    @Test
    void markAsSold_conIdExistente_retorna204NoContent() throws Exception {
        // ARRANGE: markAsSold() es void en el service — no hay nada que
        // devolver, solo necesitamos que NO lance ninguna excepción.
        Long id = 1L;

        doNothing().when(listingService).markAsSold(id);

        // ACT + ASSERT: 204 No Content, sin body.
        mockMvc.perform(patch("/listings/{id}/sold", id))
                .andExpect(status().isNoContent());

        // VERIFY
        verify(listingService, times(1)).markAsSold(id);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 204 No Content al marcar un listing como vendido
// Se obtuvo: HTTP 200 OK con un body vacío "{}" en vez de 204
// Esto podría pasar si alguien cambia por error
// "ResponseEntity.noContent().build()" por "ResponseEntity.ok().build()"
// en ListingController.markAsSold(), un detalle sutil que rompería
// el contrato HTTP esperado por clientes que distinguen explícitamente
// entre "200 con posible body" y "204 sin body en absoluto".

    @Test
    void markAsSold_conIdInexistente_retorna404NotFound() throws Exception {
        // ARRANGE
        Long idInexistente = 999L;

        doThrow(new ListingNotFoundException(idInexistente))
                .when(listingService).markAsSold(idInexistente);

        // ACT + ASSERT
        mockMvc.perform(patch("/listings/{id}/sold", idInexistente))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Listing no encontrado con id: " + idInexistente));

        // VERIFY
        verify(listingService, times(1)).markAsSold(idInexistente);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 404 Not Found al intentar marcar como vendido
// un listing que no existe
// Se obtuvo: HTTP 500 Internal Server Error
// Esto podría pasar si alguien elimina el @ExceptionHandler de
// ListingNotFoundException, dejando que caiga en el handler genérico.

    @Test
    void delete_conIdExistente_retorna204NoContent() throws Exception {
        // ARRANGE: delete() es void — solo necesitamos que no lance excepción.
        Long id = 1L;

        doNothing().when(listingService).delete(id);

        // ACT + ASSERT
        mockMvc.perform(delete("/listings/{id}", id))
                .andExpect(status().isNoContent());

        // VERIFY
        verify(listingService, times(1)).delete(id);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 204 No Content al eliminar un listing existente
// Se obtuvo: HTTP 404 Not Found a pesar de que el id sí existe
// Esto podría pasar si alguien invierte por error la lógica de
// existencia en ListingService.delete() (por ejemplo, cambiando
// "if (!listingRepository.existsById(id))" a
// "if (listingRepository.existsById(id))"), lanzando
// ListingNotFoundException precisamente cuando el listing SÍ existe.

    @Test
    void delete_conIdInexistente_retorna404NotFound() throws Exception {
        // ARRANGE
        Long idInexistente = 999L;

        doThrow(new ListingNotFoundException(idInexistente))
                .when(listingService).delete(idInexistente);

        // ACT + ASSERT
        mockMvc.perform(delete("/listings/{id}", idInexistente))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Listing no encontrado con id: " + idInexistente));

        // VERIFY
        verify(listingService, times(1)).delete(idInexistente);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 404 Not Found al eliminar un listing inexistente
// Se obtuvo: HTTP 204 No Content (silenciosamente "exitoso")
// Esto podría pasar si alguien elimina la validación de existencia
// en ListingService.delete() y llama directamente a
// listingRepository.deleteById(id), que en JPA no lanza ninguna
// excepción si el id no existe — el delete "parecería" funcionar
// sin avisar que en realidad no había nada que borrar.
}