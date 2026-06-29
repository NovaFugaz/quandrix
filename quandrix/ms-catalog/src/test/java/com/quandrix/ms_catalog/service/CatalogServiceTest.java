package com.quandrix.ms_catalog.service;

import com.quandrix.ms_catalog.client.ScryfallClient;
import com.quandrix.ms_catalog.dto.CardResponse;
import com.quandrix.ms_catalog.dto.ScryfallCardDto;
import com.quandrix.ms_catalog.exception.CardNotFoundException;
import com.quandrix.ms_catalog.model.Card;
import com.quandrix.ms_catalog.repository.CardRepository;
import com.quandrix.ms_catalog.repository.CardSetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private CardSetRepository cardSetRepository;

    @Mock
    private ScryfallClient scryfallClient;

    @InjectMocks
    private CatalogService catalogService;

    @Test
    void getCardById_conCartaExistenteLocalmente_retornaCartaSinLlamarScryfall() {
        // ARRANGE: simulamos que la carta YA existe en el repositorio local.
        String scryfallId = "id-local-123";
        Card cartaLocal = new Card();
        cartaLocal.setScryfallId(scryfallId);
        cartaLocal.setName("Black Lotus");
        cartaLocal.setSetCode("LEA");
        cartaLocal.setSetName("Limited Edition Alpha");
        cartaLocal.setImageUrl("https://cards.scryfall.io/normal/front/id-local-123.jpg");

        when(cardRepository.findById(scryfallId)).thenReturn(Optional.of(cartaLocal));

        // ACT: ejecutamos el metodo real del service.
        CardResponse response = catalogService.getCardById(scryfallId);

        // ASSERT: verificamos que los datos retornados correspondan
        // exactamente a la carta local.
        assertThat(response.getScryfallId()).isEqualTo(scryfallId);
        assertThat(response.getName()).isEqualTo("Black Lotus");
        assertThat(response.getSetCode()).isEqualTo("LEA");

        // VERIFY: lo más importante de este test — confirmar que,
        // al encontrarse localmente, NUNCA se llamó a Scryfall ni
        // se intentó guardar nada de nuevo (no hay trabajo redundante).
        verify(scryfallClient, never()).getCardById(any());
        verify(cardRepository, never()).save(any());
    }


// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: scryfallClient.getCardById(...) NUNCA se invoca cuando
// la carta ya existe localmente.
// Se obtuvo: scryfallClient.getCardById(...) SE INVOCA igual, a pesar
// de existir un resultado local válido.
// Esto podría pasar si alguien modifica el orElseGet(...) de
// CatalogService.getCardById() y lo convierte en una llamada incondicional
// (por ejemplo, usando .map(...).orElse(scryfallClient.getCardById(id))
// en vez de orElseGet con una lambda diferida), lo cual ejecutaría
// la llamada a Scryfall SIEMPRE, incluso cuando no es necesaria —
// un problema grave de rendimiento y de dependencia innecesaria de
// un servicio externo.

    @Test
    void getCardById_conCartaNoLocal_consultaYPersisteDesdeScryfall() {
        // ARRANGE: simulamos que la carta NO existe localmente, que
        // Scryfall sí la tiene, y que su set tampoco existe aún localmente.
        String scryfallId = "id-remoto-456";

        ScryfallCardDto dto = new ScryfallCardDto();
        dto.setId(scryfallId);
        dto.setName("Mox Sapphire");
        dto.setSetCode("LEA");
        dto.setSetName("Limited Edition Alpha");

        when(cardRepository.findById(scryfallId)).thenReturn(Optional.empty());
        when(scryfallClient.getCardById(scryfallId)).thenReturn(dto);
        when(cardSetRepository.existsById("LEA")).thenReturn(false);

        // Mockito necesita saber qué retornar cuando se llame a save(...),
        // ya que el metodo real usa el resultado (saved.getName() para el log,
        // y toResponse(saved) para construir la respuesta).
        Card cardGuardada = new Card();
        cardGuardada.setScryfallId(scryfallId);
        cardGuardada.setName("Mox Sapphire");
        cardGuardada.setSetCode("LEA");
        cardGuardada.setSetName("Limited Edition Alpha");
        when(cardRepository.save(any(Card.class))).thenReturn(cardGuardada);

        // ACT: ejecutamos el metodo real del service.
        CardResponse response = catalogService.getCardById(scryfallId);

        // ASSERT: verificamos que el response tenga los datos del DTO
        // de Scryfall, correctamente mapeados.
        assertThat(response.getScryfallId()).isEqualTo(scryfallId);
        assertThat(response.getName()).isEqualTo("Mox Sapphire");
        assertThat(response.getSetCode()).isEqualTo("LEA");

        // VERIFY: confirmamos que, al no existir localmente, SÍ se
        // consultó Scryfall, SÍ se persistió el set nuevo, y SÍ se
        // guardó la carta.
        verify(scryfallClient, times(1)).getCardById(scryfallId);
        verify(cardSetRepository, times(1)).save(any());
        verify(cardRepository, times(1)).save(any(Card.class));
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: el set "LEA" se persiste automáticamente antes de
// guardar la carta, ya que no existía localmente.
// Se obtuvo: una excepción de integridad referencial (foreign key
// constraint violation) al intentar guardar la carta.
// Esto podría pasar si alguien reordena el código en
// CatalogService.getCardById() y mueve cardRepository.save(toEntity(dto))
// ANTES de persistSetIfAbsent(dto), guardando la carta antes de que
// su set exista en la tabla card_sets — rompiendo la relación @ManyToOne
// que Card tiene hacia CardSet a nivel de base de datos real (aunque
// el mock no lo detecte, sí lo detectaría un test de integración o el
// ambiente de producción).

    @Test
    void searchByName_conResultadosLocales_retornaListaSinConsultarScryfall() {
        // ARRANGE: simulamos que la búsqueda local SÍ encuentra coincidencias.
        String name = "Black Lotus";

        Card carta1 = new Card();
        carta1.setScryfallId("id-1");
        carta1.setName("Black Lotus");
        carta1.setSetCode("LEA");

        Card carta2 = new Card();
        carta2.setScryfallId("id-2");
        carta2.setName("Black Lotus");
        carta2.setSetCode("LEB");

        when(cardRepository.searchByName(name)).thenReturn(List.of(carta1, carta2));

        // ACT: ejecutamos el metodo real del service.
        List<CardResponse> response = catalogService.searchByName(name);

        // ASSERT: verificamos que la lista tenga exactamente las 2 cartas
        // locales, con sus datos correctos.
        assertThat(response).hasSize(2);
        assertThat(response.get(0).getScryfallId()).isEqualTo("id-1");
        assertThat(response.get(1).getScryfallId()).isEqualTo("id-2");

        // VERIFY: lo más importante — confirmar que, al haber resultados
        // locales, NUNCA se consultó Scryfall.
        verify(scryfallClient, never()).searchByName(any());
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: scryfallClient.searchByName(...) NUNCA se invoca cuando
// ya hay resultados locales.
// Se obtuvo: se invoca igual, duplicando trabajo innecesariamente.
// Esto podría pasar si alguien elimina por error la condición
// "if (!local.isEmpty())" en CatalogService.searchByName(), dejando que
// el flujo siempre continúe hacia la consulta a Scryfall sin importar
// si ya había resultados — un problema de rendimiento y de dependencia
// innecesaria de un servicio externo, similar al que vimos en getCardById.

    @Test
    void searchByName_sinResultadosLocales_buscaYPersisteDesdeScryfall() {
        // ARRANGE: no hay resultados locales, pero Scryfall SÍ encuentra
        // una carta. Su set tampoco existe aún localmente.
        String name = "Mox Sapphire";

        ScryfallCardDto dto = new ScryfallCardDto();
        dto.setId("id-remoto-789");
        dto.setName("Mox Sapphire");
        dto.setSetCode("LEA");
        dto.setSetName("Limited Edition Alpha");

        when(cardRepository.searchByName(name)).thenReturn(List.of());
        when(scryfallClient.searchByName(name)).thenReturn(List.of(dto));
        when(cardSetRepository.existsById("LEA")).thenReturn(false);

        // Importante: dentro del .map(...), el código primero intenta
        // cardRepository.findById(dto.getId()) (por si ya se guardó en
        // otra búsqueda previa) y, si no existe, recién ahí llama a save(...).
        when(cardRepository.findById("id-remoto-789")).thenReturn(Optional.empty());

        Card cardGuardada = new Card();
        cardGuardada.setScryfallId("id-remoto-789");
        cardGuardada.setName("Mox Sapphire");
        cardGuardada.setSetCode("LEA");
        when(cardRepository.save(any(Card.class))).thenReturn(cardGuardada);

        // ACT: ejecutamos el metodo real del service.
        List<CardResponse> response = catalogService.searchByName(name);

        // ASSERT: verificamos que la lista tenga la carta obtenida
        // desde Scryfall, correctamente mapeada.
        assertThat(response).hasSize(1);
        assertThat(response.getFirst().getScryfallId()).isEqualTo("id-remoto-789");
        assertThat(response.getFirst().getName()).isEqualTo("Mox Sapphire");

        // VERIFY: confirmamos que, al no haber resultados locales, SÍ
        // se consultó Scryfall y SÍ se persistió la carta nueva.
        verify(scryfallClient, times(1)).searchByName(name);
        verify(cardRepository, times(1)).save(any(Card.class));
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: cardRepository.findById(dto.getId()) se consulta ANTES
// de guardar, evitando duplicar una carta que ya fue persistida por
// otra búsqueda concurrente o previa.
// Se obtuvo: cardRepository.save(...) se invoca directamente sin esa
// verificación previa, lo que podría generar un error de clave primaria
// duplicada si dos búsquedas distintas devuelven la misma carta desde
// Scryfall casi al mismo tiempo.
// Esto podría pasar si alguien simplifica por error el .orElseGet(...)
// en el .map(dto -> ...) de searchByName(), reemplazándolo por un
// cardRepository.save(toEntity(dto)) incondicional.

    @Test
    void searchByName_sinResultadosEnNingunLado_lanzaCardNotFoundException() {
        // ARRANGE: no hay resultados ni local ni en Scryfall.
        String name = "Carta Que No Existe En Ningún Lado";

        when(cardRepository.searchByName(name)).thenReturn(List.of());
        when(scryfallClient.searchByName(name)).thenReturn(List.of());

        // ACT + ASSERT: se debe lanzar CardNotFoundException con el
        // mensaje correcto.
        CardNotFoundException ex = org.junit.jupiter.api.Assertions.assertThrows(
                CardNotFoundException.class,
                () -> catalogService.searchByName(name)
        );
        assertThat(ex.getMessage()).isEqualTo("Carta no encontrada: " + name);

        // VERIFY: al no haber nada que persistir, save() nunca debió
        // invocarse ni en cards ni en sets.
        verify(cardRepository, never()).save(any());
        verify(cardSetRepository, never()).save(any());
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: CardNotFoundException cuando Scryfall retorna una
// lista vacía (dtos.isEmpty() == true)
// Se obtuvo: una excepción distinta (NullPointerException o similar)
// al intentar procesar una lista vacía como si tuviera elementos.
// Esto podría pasar si alguien elimina por error la validación
// "if (dtos.isEmpty())" en CatalogService.searchByName(), dejando que
// el código intente ejecutar el .stream().map(...) sobre una lista
// vacía sin lanzar la excepción de negocio esperada — el stream no
// fallaría con una lista vacía, pero el metodo retornaría una lista
// vacía en vez de un 404, rompiendo el contrato documentado en Swagger.

    @Test
    void getAllSets_retornaListaDesdeCardRepositoryFindAll() {
        // ARRANGE: simulamos un par de cartas en el repositorio.
        // NOTA: igual que documentamos en CatalogControllerTest, este
        // metodo usa cardRepository.findAll() en vez de cardSetRepository
        // .findAll(), a pesar de su nombre. El test refleja el
        // comportamiento real, no el que sugiere el nombre del metodo.
        Card carta1 = new Card();
        carta1.setScryfallId("id-1");
        carta1.setName("Black Lotus");

        Card carta2 = new Card();
        carta2.setScryfallId("id-2");
        carta2.setName("Mox Sapphire");

        when(cardRepository.findAll()).thenReturn(List.of(carta1, carta2));

        // ACT: ejecutamos el metodo real del service.
        List<CardResponse> response = catalogService.getAllSets();

        // ASSERT: verificamos que la lista tenga las 2 cartas esperadas.
        assertThat(response).hasSize(2);
        assertThat(response.get(0).getName()).isEqualTo("Black Lotus");
        assertThat(response.get(1).getName()).isEqualTo("Mox Sapphire");

        // VERIFY: confirmamos que se usó cardRepository, no cardSetRepository
        // (documentando explícitamente el bug a nivel de test).
        verify(cardRepository, times(1)).findAll();
        verify(cardSetRepository, never()).findAll();
    }

// CASO HIPOTÉTICO DE FALLA (para QA) — BUG YA CONOCIDO Y DOCUMENTADO:
// Se esperaba (según el nombre "getAllSets" y el contrato de Swagger):
// una lista de CardSet (sets/ediciones)
// Se obtuvo: una lista de Card (cartas individuales)
// Ver detalle completo en CatalogControllerTest.getAllSets_...— el
// fix correspondiente sería cambiar cardRepository.findAll() por
// cardSetRepository.findAll() y ajustar el tipo de retorno/mapeo.

    @Test
    void findByNameAndSet_conLocalYSetCodeCoincide_retornaCartaLocal() {
        // ARRANGE: hay cartas locales con ese nombre, y una de ellas
        // coincide exactamente con el setCode solicitado.
        String name = "Black Lotus";
        String setCode = "LEB";

        Card cartaLEA = new Card();
        cartaLEA.setScryfallId("id-lea");
        cartaLEA.setName(name);
        cartaLEA.setSetCode("LEA");

        Card cartaLEB = new Card();
        cartaLEB.setScryfallId("id-leb");
        cartaLEB.setName(name);
        cartaLEB.setSetCode("LEB");

        when(cardRepository.searchByName(name)).thenReturn(List.of(cartaLEA, cartaLEB));

        // ACT
        CardResponse response = catalogService.findByNameAndSet(name, setCode);

        // ASSERT: debe retornar específicamente la carta del set LEB,
        // no la primera de la lista (LEA).
        assertThat(response.getScryfallId()).isEqualTo("id-leb");
        assertThat(response.getSetCode()).isEqualTo("LEB");

        // VERIFY: al encontrarse localmente, nunca se debió consultar Scryfall.
        verify(scryfallClient, never()).searchByName(any());
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: con 2 cartas locales del mismo nombre (LEA y LEB),
// al pedir setCode="LEB" se retorna específicamente la carta de LEB.
// Se obtuvo: se retorna la carta de LEA (la primera de la lista),
// ignorando el filtro de setCode.
// Esto podría pasar si alguien reemplaza el .filter(c -> setCode
// .equalsIgnoreCase(c.getSetCode())) por un .findFirst() directo sobre
// "local" sin aplicar el filtro, rompiendo la posibilidad de elegir
// una edición específica cuando existen múltiples versiones de la
// misma carta.

    @Test
    void findByNameAndSet_conLocalPeroSetCodeNoCoincide_buscaEnScryfall() {
        // ARRANGE: hay una carta local, pero de un set distinto al solicitado.
        // Scryfall sí tiene la carta del set correcto.
        String name = "Black Lotus";
        String setCodeSolicitado = "LEB";

        Card cartaLEA = new Card();
        cartaLEA.setScryfallId("id-lea");
        cartaLEA.setName(name);
        cartaLEA.setSetCode("LEA"); // no coincide con "LEB"

        when(cardRepository.searchByName(name)).thenReturn(List.of(cartaLEA));

        ScryfallCardDto dtoLEB = new ScryfallCardDto();
        dtoLEB.setId("id-leb-remoto");
        dtoLEB.setName(name);
        dtoLEB.setSetCode("LEB");
        dtoLEB.setSetName("Limited Edition Beta");

        when(scryfallClient.searchByName(name)).thenReturn(List.of(dtoLEB));
        when(cardSetRepository.existsById("LEB")).thenReturn(false);
        when(cardRepository.findById("id-leb-remoto")).thenReturn(Optional.empty());

        Card cartaGuardada = new Card();
        cartaGuardada.setScryfallId("id-leb-remoto");
        cartaGuardada.setName(name);
        cartaGuardada.setSetCode("LEB");
        when(cardRepository.save(any(Card.class))).thenReturn(cartaGuardada);

        // ACT
        CardResponse response = catalogService.findByNameAndSet(name, setCodeSolicitado);

        // ASSERT: debe retornar la carta del set LEB obtenida desde Scryfall,
        // no la carta local de LEA.
        assertThat(response.getScryfallId()).isEqualTo("id-leb-remoto");
        assertThat(response.getSetCode()).isEqualTo("LEB");

        // VERIFY: al no coincidir el setCode localmente, SÍ se consultó Scryfall.
        verify(scryfallClient, times(1)).searchByName(name);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: si existe una carta local pero de un set distinto al
// solicitado, el sistema busca en Scryfall el set específico pedido.
// Se obtuvo: se retorna la carta local de LEA (incorrecta), ignorando
// que el usuario pidió específicamente LEB.
// Esto podría pasar si alguien reemplaza el .orElseGet(() ->
// fetchFromScryfallByNameAndSet(...)) por un .orElse(toResponse(local
// .get(0))), retornando la primera carta local disponible en vez de
// hacer el fallback correcto a Scryfall cuando el set específico no
// se encuentra localmente.

    @Test
    void findByNameAndSet_conLocalYSinSetCode_retornaPrimeraCoincidenciaLocal() {
        // ARRANGE: hay varias cartas locales con ese nombre, pero no
        // se especifica ningún setCode.
        String name = "Black Lotus";

        Card cartaLEA = new Card();
        cartaLEA.setScryfallId("id-lea");
        cartaLEA.setName(name);
        cartaLEA.setSetCode("LEA");

        Card cartaLEB = new Card();
        cartaLEB.setScryfallId("id-leb");
        cartaLEB.setName(name);
        cartaLEB.setSetCode("LEB");

        when(cardRepository.searchByName(name)).thenReturn(List.of(cartaLEA, cartaLEB));

        // ACT: llamamos con setCode = null, simulando que no se envió
        // ese parámetro opcional en la petición original.
        CardResponse response = catalogService.findByNameAndSet(name, null);

        // ASSERT: debe retornar la PRIMERA carta de la lista (LEA),
        // sin importar que exista también LEB.
        assertThat(response.getScryfallId()).isEqualTo("id-lea");

        // VERIFY: sin setCode, no hay razón para consultar Scryfall,
        // ya que ya hay resultados locales disponibles.
        verify(scryfallClient, never()).searchByName(any());
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: sin setCode, se retorna local.get(0) — la primera
// carta de la lista tal como la devuelve cardRepository.searchByName(...)
// Se obtuvo: una NullPointerException o un comportamiento inesperado
// al intentar evaluar setCode.isBlank() sobre un valor null sin
// haberlo chequeado antes con setCode != null.
// Esto podría pasar si alguien reordena la condición en
// CatalogService.findByNameAndSet() de "setCode != null &&
// !setCode.isBlank()" a "!setCode.isBlank() && setCode != null"
// (invirtiendo el orden del &&), ya que Java evalúa de izquierda a
// derecha: con setCode null, "!setCode.isBlank()" se ejecutaría
// primero y lanzaría la excepción antes de llegar al chequeo de null.

    @Test
    void findByNameAndSet_sinResultadosEnNingunLado_lanzaCardNotFoundException() {
        // ARRANGE: no hay resultados ni local ni en Scryfall.
        String name = "Carta Inexistente";
        String setCode = "XXX";

        when(cardRepository.searchByName(name)).thenReturn(List.of());
        when(scryfallClient.searchByName(name)).thenReturn(List.of());

        // ACT + ASSERT: se debe lanzar CardNotFoundException con el
        // mensaje simple (sin mención del set, porque la lista de
        // resultados de Scryfall ya viene vacía antes de llegar a
        // filtrar por setCode).
        CardNotFoundException ex = org.junit.jupiter.api.Assertions.assertThrows(
                CardNotFoundException.class,
                () -> catalogService.findByNameAndSet(name, setCode)
        );
        assertThat(ex.getMessage()).isEqualTo("Carta no encontrada: " + name);

        // VERIFY: no debió persistirse nada.
        verify(cardRepository, never()).save(any());
        verify(cardSetRepository, never()).save(any());
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: mensaje "Carta no encontrada: Carta Inexistente"
// (sin mención del setCode) cuando Scryfall no encuentra NINGÚN
// resultado para el nombre, ya que el chequeo "if (results.isEmpty())"
// ocurre ANTES del filtro por setCode dentro de
// fetchFromScryfallByNameAndSet().
// Se obtuvo: mensaje "Carta no encontrada: Carta Inexistente en el
// set XXX" (el mensaje más específico), que en realidad correspondería
// a un caso distinto: cuando SÍ hay resultados por nombre, pero NINGUNO
// coincide con el setCode pedido.
// Esto podría confundir a QA si no se distingue claramente cuál
// mensaje corresponde a cuál escenario exacto — vale la pena verificar
// el mensaje EXACTO en cada test, no solo el tipo de excepción.
}