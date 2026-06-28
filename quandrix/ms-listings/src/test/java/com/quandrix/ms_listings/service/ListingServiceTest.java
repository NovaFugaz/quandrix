package com.quandrix.ms_listings.service;

import com.quandrix.ms_listings.client.CatalogClient;
import com.quandrix.ms_listings.client.UserClient;
import com.quandrix.ms_listings.dto.CardResponse;
import com.quandrix.ms_listings.dto.ListingRequest;
import com.quandrix.ms_listings.dto.ListingResponse;
import com.quandrix.ms_listings.dto.UserResponse;
import com.quandrix.ms_listings.exception.InvalidListingException;
import com.quandrix.ms_listings.exception.ListingNotAvailableException;
import com.quandrix.ms_listings.exception.ListingNotFoundException;
import com.quandrix.ms_listings.model.CardCondition;
import com.quandrix.ms_listings.model.Listing;
import com.quandrix.ms_listings.model.ListingStatus;
import com.quandrix.ms_listings.repository.ListingRepository;
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
class ListingServiceTest {

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private CatalogClient catalogClient;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private ListingService listingService;

    @Test
    void create_conDatosValidos_persisteYRetornaListing() {
        // ARRANGE
        ListingRequest request = new ListingRequest();
        request.setSellerId(1L);
        request.setCardName("Black Lotus");
        request.setSetCode("LEA");
        request.setCondition("MINT");
        request.setPrice(5000L);
        request.setQuantity(3);

        // El service solo necesita que esta llamada NO lance excepción;
        // no usa ningún campo del UserResponse retornado.
        when(userClient.getUser(1L)).thenReturn(new UserResponse());

        // CardResponse en ms-listings NO tiene constructor con argumentos
        // (es distinto al de ms-catalog) — se construye con setters.
        CardResponse card = new CardResponse();
        card.setScryfallId("id-bl");
        card.setName("Black Lotus");
        card.setRarity("RARE");
        when(catalogClient.findByNameAndSet("Black Lotus", "LEA")).thenReturn(card);

        when(listingRepository.findBySellerIdAndScryfallIdAndCardConditionAndStatus(
                1L, "id-bl", CardCondition.MINT, ListingStatus.ACTIVE))
                .thenReturn(List.of());

        Listing listingGuardado = new Listing();
        listingGuardado.setId(1L);
        listingGuardado.setSellerId(1L);
        listingGuardado.setScryfallId("id-bl");
        listingGuardado.setCardCondition(CardCondition.MINT);
        listingGuardado.setPrice(5000L);
        listingGuardado.setQuantity(3);
        listingGuardado.setStatus(ListingStatus.ACTIVE);
        when(listingRepository.save(any(Listing.class))).thenReturn(listingGuardado);

        // ACT
        ListingResponse response = listingService.create(request);

        // ASSERT
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getScryfallId()).isEqualTo("id-bl");
        assertThat(response.getCondition()).isEqualTo(CardCondition.MINT);
        assertThat(response.getStatus()).isEqualTo(ListingStatus.ACTIVE);

        // VERIFY
        verify(listingRepository, times(1)).save(any(Listing.class));
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: el listing guardado tiene scryfallId == "id-bl" (el
// resuelto vía CatalogClient a partir del nombre y set enviados)
// Se obtuvo: el listing guardado tiene scryfallId == null o vacío
// Esto podría pasar si alguien modifica ListingService.create() y
// olvida llamar a listing.setScryfallId(card.getScryfallId()) antes
// de guardar, dejando el campo sin asignar a pesar de haber resuelto
// correctamente la carta vía CatalogClient.


    @Test
    void create_conVendedorInexistente_lanzaInvalidListingException() {
        // ARRANGE: simulamos que userClient.getUser(...) lanza cualquier
        // excepción (el código real captura "Exception" genérico, así que
        // no importa el tipo exacto que lance Feign en producción).
        ListingRequest request = new ListingRequest();
        request.setSellerId(999L);
        request.setCardName("Black Lotus");
        request.setSetCode("LEA");
        request.setCondition("MINT");
        request.setPrice(5000L);
        request.setQuantity(3);

        when(userClient.getUser(999L)).thenThrow(new RuntimeException("404 Not Found"));

        // ACT + ASSERT
        InvalidListingException ex = assertThrows(
                InvalidListingException.class,
                () -> listingService.create(request)
        );
        assertThat(ex.getMessage()).isEqualTo("El vendedor con id 999 no existe");

        // VERIFY: al fallar la primera validación, ninguna de las
        // siguientes llamadas debió ejecutarse.
        verify(catalogClient, never()).findByNameAndSet(any(), any());
        verify(listingRepository, never()).save(any());
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: catalogClient.findByNameAndSet(...) NUNCA se invoca
// si el vendedor no existe (la validación se detiene en el primer paso)
// Se obtuvo: catalogClient.findByNameAndSet(...) SÍ se invoca a pesar
// del vendedor inexistente, haciendo una llamada Feign innecesaria
// a otro microservicio antes de fallar.
// Esto podría pasar si alguien reordena el código de ListingService
// .create() y mueve la resolución de la carta ANTES de la validación
// del vendedor, gastando una llamada de red innecesaria a ms-catalog
// cuando ya se sabe que la solicitud completa va a fallar.

    @Test
    void create_conCartaInexistente_lanzaInvalidListingException() {
        // ARRANGE: el vendedor existe, pero CatalogClient no encuentra
        // la carta solicitada.
        ListingRequest request = new ListingRequest();
        request.setSellerId(1L);
        request.setCardName("Carta Inexistente");
        request.setSetCode("XXX");
        request.setCondition("MINT");
        request.setPrice(5000L);
        request.setQuantity(3);

        when(userClient.getUser(1L)).thenReturn(new UserResponse());
        when(catalogClient.findByNameAndSet("Carta Inexistente", "XXX"))
                .thenThrow(new RuntimeException("404 Not Found"));

        // ACT + ASSERT
        InvalidListingException ex = assertThrows(
                InvalidListingException.class,
                () -> listingService.create(request)
        );
        assertThat(ex.getMessage()).isEqualTo(
                "No se encontró la carta 'Carta Inexistente' en el set XXX");

        // VERIFY
        verify(listingRepository, never()).save(any());
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: el mensaje incluye " en el set XXX" cuando setCode
// SÍ fue proporcionado en el request
// Se obtuvo: el mensaje omite la parte del set incluso habiéndolo
// enviado, mostrando solo "No se encontró la carta 'Carta Inexistente'"
// Esto podría pasar si alguien rompe la condición ternaria
// "(request.getSetCode() != null ? ... : "")" en ListingService.create(),
// por ejemplo invirtiendo la condición a "== null", mostrando la
// información del set exactamente al revés de lo esperado (la
// mostraría cuando NO se envió, y la ocultaría cuando SÍ se envió).

    @Test
    void create_conCondicionInvalida_lanzaInvalidListingException() {
        // ARRANGE: vendedor y carta válidos, pero la condición enviada
        // no corresponde a ningún valor del enum CardCondition.
        ListingRequest request = new ListingRequest();
        request.setSellerId(1L);
        request.setCardName("Black Lotus");
        request.setSetCode("LEA");
        request.setCondition("SUPER_MINT"); // no existe en CardCondition
        request.setPrice(5000L);
        request.setQuantity(3);

        when(userClient.getUser(1L)).thenReturn(new UserResponse());

        CardResponse card = new CardResponse();
        card.setScryfallId("id-bl");
        card.setName("Black Lotus");
        when(catalogClient.findByNameAndSet("Black Lotus", "LEA")).thenReturn(card);

        // ACT + ASSERT
        InvalidListingException ex = assertThrows(
                InvalidListingException.class,
                () -> listingService.create(request)
        );
        assertThat(ex.getMessage()).isEqualTo("Condición inválida: SUPER_MINT");

        // VERIFY: el flujo se detiene aquí, antes de llegar a validar
        // duplicados o persistir.
        verify(listingRepository, never())
                .findBySellerIdAndScryfallIdAndCardConditionAndStatus(any(), any(), any(), any());
        verify(listingRepository, never()).save(any());
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: InvalidListingException con condición inválida,
// ANTES de validar precio/cantidad/duplicados
// Se obtuvo: una IllegalArgumentException sin capturar, propagándose
// como HTTP 500 en vez de un error controlado de 400
// Esto podría pasar si alguien elimina por error el try/catch alrededor
// de CardCondition.valueOf(...) en ListingService.create(), dejando
// que la excepción nativa del enum suba sin traducirse.

    @Test
    void create_conPrecioFueraDeRango_lanzaInvalidListingException() {
        // ARRANGE: precio por debajo del mínimo permitido (100).
        ListingRequest request = new ListingRequest();
        request.setSellerId(1L);
        request.setCardName("Black Lotus");
        request.setSetCode("LEA");
        request.setCondition("MINT");
        request.setPrice(50L); // menor a 100
        request.setQuantity(3);

        when(userClient.getUser(1L)).thenReturn(new UserResponse());

        CardResponse card = new CardResponse();
        card.setScryfallId("id-bl");
        card.setName("Black Lotus");
        when(catalogClient.findByNameAndSet("Black Lotus", "LEA")).thenReturn(card);

        // ACT + ASSERT
        InvalidListingException ex = assertThrows(
                InvalidListingException.class,
                () -> listingService.create(request)
        );
        assertThat(ex.getMessage()).isEqualTo("Precio fuera de rango permitido");

        // VERIFY
        verify(listingRepository, never()).save(any());
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: InvalidListingException con precio=50 (menor al
// mínimo de 100)
// Se obtuvo: el listing se crea exitosamente con precio=50
// Esto podría pasar si alguien invierte por error el operador de la
// condición "request.getPrice() < 100" a "request.getPrice() > 100"
// en ListingService.create(), permitiendo precios absurdamente bajos
// (o incluso negativos, si la validación de Bean Validation @Positive
// también fallara) y bloqueando precios normales en su lugar.

    @Test
    void create_conCantidadFueraDeRango_lanzaInvalidListingException() {
        // ARRANGE: cantidad por encima del máximo permitido (100).
        ListingRequest request = new ListingRequest();
        request.setSellerId(1L);
        request.setCardName("Black Lotus");
        request.setSetCode("LEA");
        request.setCondition("MINT");
        request.setPrice(5000L);
        request.setQuantity(150); // mayor a 100

        when(userClient.getUser(1L)).thenReturn(new UserResponse());

        CardResponse card = new CardResponse();
        card.setScryfallId("id-bl");
        card.setName("Black Lotus");
        when(catalogClient.findByNameAndSet("Black Lotus", "LEA")).thenReturn(card);

        // ACT + ASSERT
        InvalidListingException ex = assertThrows(
                InvalidListingException.class,
                () -> listingService.create(request)
        );
        assertThat(ex.getMessage()).isEqualTo("Cantidad debe estar entre 1 y 100");

        // VERIFY
        verify(listingRepository, never()).save(any());
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: InvalidListingException con cantidad=150 (mayor al
// máximo de 100)
// Se obtuvo: el listing se crea con quantity=150, permitiendo a un
// vendedor publicar una cantidad irrealmente alta de copias de una
// misma carta en una sola publicación
// Esto podría pasar si alguien cambia por error el límite superior
// "request.getQuantity() > 100" a un valor mucho más alto sin
// coordinar ese cambio con el límite real de inventario físico que
// el negocio del marketplace puede manejar razonablemente.

    @Test
    void create_conListingDuplicado_lanzaInvalidListingException() {
        // ARRANGE: vendedor y carta válidos, precio/cantidad válidos,
        // pero el vendedor YA tiene un listing activo de esta misma
        // carta y condición.
        ListingRequest request = new ListingRequest();
        request.setSellerId(1L);
        request.setCardName("Black Lotus");
        request.setSetCode("LEA");
        request.setCondition("MINT");
        request.setPrice(5000L);
        request.setQuantity(3);

        when(userClient.getUser(1L)).thenReturn(new UserResponse());

        CardResponse card = new CardResponse();
        card.setScryfallId("id-bl");
        card.setName("Black Lotus");
        when(catalogClient.findByNameAndSet("Black Lotus", "LEA")).thenReturn(card);

        Listing listingExistente = new Listing();
        listingExistente.setId(5L);
        listingExistente.setSellerId(1L);
        listingExistente.setScryfallId("id-bl");
        listingExistente.setCardCondition(CardCondition.MINT);
        listingExistente.setStatus(ListingStatus.ACTIVE);

        when(listingRepository.findBySellerIdAndScryfallIdAndCardConditionAndStatus(
                1L, "id-bl", CardCondition.MINT, ListingStatus.ACTIVE))
                .thenReturn(List.of(listingExistente)); // YA existe uno

        // ACT + ASSERT
        InvalidListingException ex = assertThrows(
                InvalidListingException.class,
                () -> listingService.create(request)
        );
        assertThat(ex.getMessage()).isEqualTo("Ya tienes un listing activo de esta carta");

        // VERIFY: al detectarse el duplicado, nunca se debió persistir
        // un segundo listing.
        verify(listingRepository, never()).save(any());
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: InvalidListingException cuando ya existe un listing
// ACTIVO del mismo vendedor, misma carta (scryfallId) y misma condición
// Se obtuvo: se permite crear un segundo listing duplicado, fragmentando
// el inventario del vendedor en múltiples publicaciones idénticas
// (lo cual confunde a los compradores, que verían la misma carta en
// la misma condición publicada dos veces por el mismo vendedor)
// Esto podría pasar si alguien invierte por error la negación en
// "boolean isDuplicate = !listingRepository.find...().isEmpty()"
// (quitando el "!"), invirtiendo completamente la lógica: marcaría
// como duplicado cuando NO hay coincidencias, y como NO duplicado
// cuando SÍ las hay.

    @Test
    void getById_conIdExistente_retornaListing() {
        // ARRANGE
        Long id = 1L;
        Listing listing = new Listing();
        listing.setId(id);
        listing.setSellerId(1L);
        listing.setScryfallId("id-bl");
        listing.setCardCondition(CardCondition.MINT);
        listing.setPrice(5000L);
        listing.setQuantity(3);
        listing.setStatus(ListingStatus.ACTIVE);

        when(listingRepository.findById(id)).thenReturn(Optional.of(listing));

        // ACT
        ListingResponse response = listingService.getById(id);

        // ASSERT
        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getScryfallId()).isEqualTo("id-bl");
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: response.getStatus() == ListingStatus.ACTIVE
// Se obtuvo: response.getStatus() == null
// Esto podría pasar si alguien modifica el metodo privado toResponse()
// y olvida mapear el campo status al construir el ListingResponse,
// dejando el campo sin asignar a pesar de que el Listing original sí
// lo tenía correctamente seteado.

    @Test
    void getById_conIdInexistente_lanzaListingNotFoundException() {
        // ARRANGE
        Long idInexistente = 999L;

        when(listingRepository.findById(idInexistente)).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThrows(
                ListingNotFoundException.class,
                () -> listingService.getById(idInexistente)
        );
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: ListingNotFoundException cuando el repositorio no
// encuentra el listing (Optional vacío)
// Se obtuvo: una NoSuchElementException sin capturar, propagándose
// como HTTP 500 en vez de un error controlado de 404
// Esto podría pasar si alguien reemplaza el .orElseThrow(() -> new
// ListingNotFoundException(id)) por un .get() directo sobre el
// Optional, dejando que el Optional vacío lance su excepción nativa
// de Java en vez de la excepción de negocio esperada.

    @Test
    void update_conDatosValidos_actualizaYRetornaListing() {
        // ARRANGE: el listing existe, pertenece al mismo vendedor que
        // solicita la actualización, y los nuevos datos son válidos.
        Long id = 1L;
        Listing listingExistente = new Listing();
        listingExistente.setId(id);
        listingExistente.setSellerId(1L);
        listingExistente.setScryfallId("id-bl");
        listingExistente.setCardCondition(CardCondition.MINT);
        listingExistente.setPrice(5000L);
        listingExistente.setQuantity(3);
        listingExistente.setStatus(ListingStatus.ACTIVE);

        when(listingRepository.findById(id)).thenReturn(Optional.of(listingExistente));

        ListingRequest request = new ListingRequest();
        request.setSellerId(1L); // mismo vendedor, no se cambia
        request.setCondition("NEAR_MINT");
        request.setPrice(6000L);
        request.setQuantity(2);

        when(listingRepository.save(any(Listing.class))).thenReturn(listingExistente);

        // ACT
        ListingResponse response = listingService.update(id, request);

        // ASSERT: el objeto mockeado (listingExistente) es el mismo que
        // se modifica en memoria y luego se "guarda" — por eso podemos
        // verificar sus campos directamente después del ACT.
        assertThat(listingExistente.getPrice()).isEqualTo(6000L);
        assertThat(listingExistente.getQuantity()).isEqualTo(2);
        assertThat(listingExistente.getCardCondition()).isEqualTo(CardCondition.NEAR_MINT);

        // VERIFY
        verify(listingRepository, times(1)).save(listingExistente);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: el precio y cantidad del listing existente se
// actualizan a los nuevos valores enviados en el request
// Se obtuvo: el listing conserva sus valores originales (5000, 3)
// a pesar de haber enviado un request con valores distintos
// Esto podría pasar si alguien olvida llamar a los setters
// (listing.setPrice(...), listing.setQuantity(...)) antes de
// listingRepository.save(listing) en ListingService.update().


    @Test
    void update_conIdInexistente_lanzaListingNotFoundException() {
        // ARRANGE
        Long idInexistente = 999L;
        when(listingRepository.findById(idInexistente)).thenReturn(Optional.empty());

        ListingRequest request = new ListingRequest();
        request.setSellerId(1L);
        request.setCondition("MINT");
        request.setPrice(5000L);
        request.setQuantity(3);

        // ACT + ASSERT
        assertThrows(
                ListingNotFoundException.class,
                () -> listingService.update(idInexistente, request)
        );
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: ListingNotFoundException al actualizar un id inexistente
// Se obtuvo: NoSuchElementException sin capturar (HTTP 500 en vez de 404)
// Mismo patrón de riesgo que ya vimos en getById(): reemplazar
// .orElseThrow(...) por un .get() directo sobre el Optional.


    @Test
    void update_conSellerIdDistinto_lanzaInvalidListingException() {
        // ARRANGE: el listing pertenece al vendedor 1, pero alguien con
        // sellerId=2 intenta actualizarlo.
        Long id = 1L;
        Listing listingExistente = new Listing();
        listingExistente.setId(id);
        listingExistente.setSellerId(1L); // dueño real
        listingExistente.setCardCondition(CardCondition.MINT);
        listingExistente.setPrice(5000L);
        listingExistente.setQuantity(3);

        when(listingRepository.findById(id)).thenReturn(Optional.of(listingExistente));

        ListingRequest request = new ListingRequest();
        request.setSellerId(2L); // intruso
        request.setCondition("MINT");
        request.setPrice(6000L);
        request.setQuantity(2);

        // ACT + ASSERT
        InvalidListingException ex = assertThrows(
                InvalidListingException.class,
                () -> listingService.update(id, request)
        );
        assertThat(ex.getMessage()).isEqualTo("No puedes cambiar el vendedor");

        // VERIFY: al detectarse el intento de suplantación, nunca se
        // debió persistir ningún cambio.
        verify(listingRepository, never()).save(any());
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: InvalidListingException cuando el sellerId del request
// no coincide con el dueño real del listing (vulnerabilidad de
// autorización: "IDOR" — Insecure Direct Object Reference)
// Se obtuvo: el listing se actualiza exitosamente, permitiendo que
// CUALQUIER usuario modifique publicaciones ajenas simplemente
// conociendo su id numérico
// Esto podría pasar si alguien elimina por error la validación
// "if (!listing.getSellerId().equals(request.getSellerId()))" en
// ListingService.update() — una falla de seguridad crítica, ya que
// permitiría a cualquier vendedor modificar precio/cantidad de
// publicaciones que no le pertenecen.


    @Test
    void update_conPrecioFueraDeRango_lanzaInvalidListingException() {
        // ARRANGE
        Long id = 1L;
        Listing listingExistente = new Listing();
        listingExistente.setId(id);
        listingExistente.setSellerId(1L);
        listingExistente.setCardCondition(CardCondition.MINT);
        listingExistente.setPrice(5000L);
        listingExistente.setQuantity(3);

        when(listingRepository.findById(id)).thenReturn(Optional.of(listingExistente));

        ListingRequest request = new ListingRequest();
        request.setSellerId(1L);
        request.setCondition("MINT");
        request.setPrice(20_000_000L); // mayor a 10_000_000
        request.setQuantity(3);

        // ACT + ASSERT
        InvalidListingException ex = assertThrows(
                InvalidListingException.class,
                () -> listingService.update(id, request)
        );
        assertThat(ex.getMessage()).isEqualTo("Precio fuera de rango permitido");

        // VERIFY
        verify(listingRepository, never()).save(any());
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: InvalidListingException con precio=20_000_000 (mayor
// al máximo de 10_000_000)
// Se obtuvo: el listing se actualiza con ese precio absurdamente alto
// Mismo riesgo que en create(): invertir el operador de comparación
// del límite superior permitiría precios fuera de cualquier rango
// razonable para el marketplace.


    @Test
    void update_conCantidadFueraDeRango_lanzaInvalidListingException() {
        // ARRANGE
        Long id = 1L;
        Listing listingExistente = new Listing();
        listingExistente.setId(id);
        listingExistente.setSellerId(1L);
        listingExistente.setCardCondition(CardCondition.MINT);
        listingExistente.setPrice(5000L);
        listingExistente.setQuantity(3);

        when(listingRepository.findById(id)).thenReturn(Optional.of(listingExistente));

        ListingRequest request = new ListingRequest();
        request.setSellerId(1L);
        request.setCondition("MINT");
        request.setPrice(5000L);
        request.setQuantity(0); // menor a 1

        // ACT + ASSERT
        InvalidListingException ex = assertThrows(
                InvalidListingException.class,
                () -> listingService.update(id, request)
        );
        assertThat(ex.getMessage()).isEqualTo("Cantidad fuera de rango permitido");

        // VERIFY
        verify(listingRepository, never()).save(any());
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: InvalidListingException con cantidad=0 (menor al
// mínimo de 1) — un listing con 0 unidades no tiene sentido en el
// marketplace, ya que representaría stock inexistente
// Se obtuvo: el listing se actualiza con quantity=0, quedando
// "activo" pero sin ninguna unidad real disponible para comprar
// Esto podría pasar si alguien cambia el límite inferior de
// "request.getQuantity() < 1" a "request.getQuantity() < 0",
// permitiendo erróneamente que 0 sea un valor válido.

    @Test
    void withdraw_conDatosValidos_marcaComoWithdrawn() {
        // ARRANGE: el listing existe, está ACTIVE, y pertenece al
        // vendedor que solicita el retiro.
        Long id = 1L;
        Long sellerId = 1L;

        Listing listing = new Listing();
        listing.setId(id);
        listing.setSellerId(sellerId);
        listing.setStatus(ListingStatus.ACTIVE);

        when(listingRepository.findById(id)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any(Listing.class))).thenReturn(listing);

        // ACT
        ListingResponse response = listingService.withdraw(id, sellerId);

        // ASSERT
        assertThat(listing.getStatus()).isEqualTo(ListingStatus.WITHDRAWN);
        assertThat(response.getStatus()).isEqualTo(ListingStatus.WITHDRAWN);

        // VERIFY
        verify(listingRepository, times(1)).save(listing);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: listing.getStatus() == WITHDRAWN después de retirarlo
// Se obtuvo: listing.getStatus() == ACTIVE (sin cambios)
// Esto podría pasar si alguien olvida la línea
// listing.setStatus(ListingStatus.WITHDRAWN) antes de
// listingRepository.save(listing) en ListingService.withdraw().


    @Test
    void withdraw_conIdInexistente_lanzaListingNotFoundException() {
        // ARRANGE
        Long idInexistente = 999L;
        when(listingRepository.findById(idInexistente)).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThrows(
                ListingNotFoundException.class,
                () -> listingService.withdraw(idInexistente, 1L)
        );
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: ListingNotFoundException al retirar un id inexistente
// Se obtuvo: NoSuchElementException sin capturar (HTTP 500 en vez de 404)


    @Test
    void withdraw_conSellerIdDistinto_lanzaInvalidListingException() {
        // ARRANGE: el listing pertenece al vendedor 1, pero alguien con
        // sellerId=2 intenta retirarlo.
        Long id = 1L;
        Listing listing = new Listing();
        listing.setId(id);
        listing.setSellerId(1L); // dueño real
        listing.setStatus(ListingStatus.ACTIVE);

        when(listingRepository.findById(id)).thenReturn(Optional.of(listing));

        // ACT + ASSERT
        InvalidListingException ex = assertThrows(
                InvalidListingException.class,
                () -> listingService.withdraw(id, 2L) // intruso
        );
        assertThat(ex.getMessage()).isEqualTo("No puedes retirar un listing que no es tuyo");

        // VERIFY
        verify(listingRepository, never()).save(any());
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: InvalidListingException cuando el sellerId no coincide
// con el dueño real (misma vulnerabilidad IDOR que vimos en update())
// Se obtuvo: cualquier usuario podría retirar publicaciones ajenas
// del marketplace simplemente conociendo su id numérico
// Esto podría pasar si alguien elimina por error la validación
// "if (!listing.getSellerId().equals(sellerId))" en
// ListingService.withdraw() — falla de seguridad crítica.


    @Test
    void withdraw_conListingYaNoActivo_lanzaListingNotAvailableException() {
        // ARRANGE: el listing pertenece al vendedor correcto, pero ya
        // fue vendido previamente (no está ACTIVE).
        Long id = 1L;
        Long sellerId = 1L;

        Listing listing = new Listing();
        listing.setId(id);
        listing.setSellerId(sellerId);
        listing.setStatus(ListingStatus.SOLD); // ya no está activo

        when(listingRepository.findById(id)).thenReturn(Optional.of(listing));

        // ACT + ASSERT
        assertThrows(
                ListingNotAvailableException.class,
                () -> listingService.withdraw(id, sellerId)
        );

        // VERIFY
        verify(listingRepository, never()).save(any());
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: ListingNotAvailableException al intentar retirar un
// listing que ya fue vendido (status == SOLD)
// Se obtuvo: el listing SOLD se marca como WITHDRAWN, sobrescribiendo
// silenciosamente el estado real de "vendido" — esto podría causar
// inconsistencias graves si ms-orders ya procesó una venta basada en
// ese listing, ya que el historial de transacciones quedaría
// desincronizado con el estado real de la publicación
// Esto podría pasar si alguien elimina por error la validación
// "if (listing.getStatus() != ListingStatus.ACTIVE)" en
// ListingService.withdraw().

    @Test
    void markAsSold_conMasDeUnaUnidad_descuentaStockYMantieneActivo() {
        // ARRANGE: el listing tiene 3 unidades en stock — al venderse 1,
        // debe quedar con 2, SIN cambiar de estado (sigue ACTIVE).
        Long id = 1L;
        Listing listing = new Listing();
        listing.setId(id);
        listing.setStatus(ListingStatus.ACTIVE);
        listing.setQuantity(3);

        when(listingRepository.findById(id)).thenReturn(Optional.of(listing));

        // ACT
        listingService.markAsSold(id);

        // ASSERT: verificamos el estado del objeto en memoria después
        // de la operación (markAsSold es void, no retorna nada).
        assertThat(listing.getQuantity()).isEqualTo(2);
        assertThat(listing.getStatus()).isEqualTo(ListingStatus.ACTIVE);

        // VERIFY
        verify(listingRepository, times(1)).save(listing);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: con quantity=3, al venderse 1 unidad, queda quantity=2
// y status sigue ACTIVE (aún hay stock disponible)
// Se obtuvo: el listing se marca como SOLD a pesar de quedar stock
// disponible (quantity=2), ocultando 2 unidades vendibles del catálogo
// Esto podría pasar si alguien invierte por error la condición
// "if (listing.getQuantity() > 1)" a "if (listing.getQuantity() < 1)"
// en ListingService.markAsSold(), entrando siempre a la rama de
// "última unidad" sin importar el stock real disponible.


    @Test
    void markAsSold_conUltimaUnidad_marcaComoSold() {
        // ARRANGE: el listing tiene exactamente 1 unidad — al venderse,
        // debe pasar a SOLD con quantity=0.
        Long id = 1L;
        Listing listing = new Listing();
        listing.setId(id);
        listing.setStatus(ListingStatus.ACTIVE);
        listing.setQuantity(1);

        when(listingRepository.findById(id)).thenReturn(Optional.of(listing));

        // ACT
        listingService.markAsSold(id);

        // ASSERT
        assertThat(listing.getQuantity()).isEqualTo(0);
        assertThat(listing.getStatus()).isEqualTo(ListingStatus.SOLD);

        // VERIFY
        verify(listingRepository, times(1)).save(listing);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: con quantity=1, al venderse la última unidad, el
// listing pasa a status=SOLD y quantity=0
// Se obtuvo: el listing queda con quantity=0 pero status=ACTIVE,
// mostrando una publicación "activa" sin ninguna unidad disponible
// para comprar — un estado inconsistente que confundiría a los
// compradores del marketplace
// Esto podría pasar si alguien olvida la línea
// listing.setStatus(ListingStatus.SOLD) en la rama "else" de
// ListingService.markAsSold(), descontando la cantidad pero sin
// actualizar el estado correspondiente.


    @Test
    void markAsSold_conIdInexistente_lanzaListingNotFoundException() {
        // ARRANGE
        Long idInexistente = 999L;
        when(listingRepository.findById(idInexistente)).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThrows(
                ListingNotFoundException.class,
                () -> listingService.markAsSold(idInexistente)
        );
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: ListingNotFoundException al marcar como vendido un
// id inexistente (por ejemplo, si ms-orders envía un id incorrecto)
// Se obtuvo: NoSuchElementException sin capturar (HTTP 500 en vez de 404)


    @Test
    void markAsSold_conListingYaNoActivo_lanzaListingNotAvailableException() {
        // ARRANGE: el listing ya fue retirado por su vendedor, pero
        // ms-orders intenta marcarlo como vendido igual (posible
        // condición de carrera entre un retiro y una compra simultánea).
        Long id = 1L;
        Listing listing = new Listing();
        listing.setId(id);
        listing.setStatus(ListingStatus.WITHDRAWN);
        listing.setQuantity(3);

        when(listingRepository.findById(id)).thenReturn(Optional.of(listing));

        // ACT + ASSERT
        assertThrows(
                ListingNotAvailableException.class,
                () -> listingService.markAsSold(id)
        );

        // VERIFY
        verify(listingRepository, never()).save(any());
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: ListingNotAvailableException si ms-orders intenta
// marcar como vendido un listing que ya fue retirado por su vendedor
// (escenario de condición de carrera: el comprador inició el checkout
// justo antes de que el vendedor retirara la publicación)
// Se obtuvo: el listing WITHDRAWN se marca como vendido igual,
// generando una venta de un producto que el vendedor ya había
// retirado intencionalmente del marketplace — un problema serio de
// integridad de datos entre ms-listings y ms-orders
// Esto podría pasar si alguien elimina por error la validación
// "if (listing.getStatus() != ListingStatus.ACTIVE)" en
// ListingService.markAsSold().

    @Test
    void delete_conIdExistente_eliminaElListing() {
        // ARRANGE
        Long id = 1L;
        Listing listing = new Listing();
        listing.setId(id);

        when(listingRepository.findById(id)).thenReturn(Optional.of(listing));

        // ACT
        listingService.delete(id);

        // VERIFY: confirmamos que se llamó a delete() con el objeto
        // Listing real (no con el id directamente — la firma del
        // repositorio recibe la entidad completa, no el id).
        verify(listingRepository, times(1)).delete(listing);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: listingRepository.delete(listing) se invoca con el
// objeto Listing real obtenido de findById(...)
// Se obtuvo: listingRepository.deleteById(id) se invoca en su lugar,
// sin haber verificado primero que el listing realmente existe
// Esto podría pasar si alguien "simplifica" por error
// ListingService.delete(), reemplazando el patrón
// findById(...).orElseThrow(...) + delete(listing) por un
// deleteById(id) directo — como ya vimos en el test del controller,
// deleteById() de Spring Data JPA NO lanza excepción si el id no
// existe, perdiendo la validación de existencia que el metodo actual
// sí garantiza.


    @Test
    void delete_conIdInexistente_lanzaListingNotFoundException() {
        // ARRANGE
        Long idInexistente = 999L;
        when(listingRepository.findById(idInexistente)).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThrows(
                ListingNotFoundException.class,
                () -> listingService.delete(idInexistente)
        );

        // VERIFY: al no existir, nunca se debió invocar delete().
        verify(listingRepository, never()).delete(any());
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: ListingNotFoundException al eliminar un id inexistente,
// sin invocar delete() en absoluto
// Se obtuvo: NoSuchElementException sin capturar (HTTP 500 en vez de 404)
// Mismo patrón de riesgo ya visto en getById(), update(), withdraw()
// y markAsSold(): reemplazar .orElseThrow(...) por un .get() directo.
}