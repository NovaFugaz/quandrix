package com.quandrix.ms_catalog.controller;

import com.quandrix.ms_catalog.dto.CardResponse;
import com.quandrix.ms_catalog.exception.CardNotFoundException;
import com.quandrix.ms_catalog.service.CatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CatalogController.class)
class CatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CatalogService catalogService;

    @Test
    void getById_conScryfallIdExistente_retorna200ConCarta() throws Exception {
        // ARRANGE: preparamos el id a consultar y la respuesta simulada
        // que debería devolver el service si la carta existe.
        String scryfallId = "bd8fa8c8-7e1c-4f5d-a6d3-123456789abc";
        CardResponse fakeResponse = new CardResponse(
                scryfallId, "Black Lotus", "LEA", "Limited Edition Alpha",
                "https://cards.scryfall.io/normal/front/example.jpg"
        );

        when(catalogService.getCardById(scryfallId)).thenReturn(fakeResponse);

        // ACT: ejecutamos el endpoint GET /catalog/{scryfallId} con MockMvc.
        // ASSERT: verificamos 200 OK y los datos esperados en el body.
        mockMvc.perform(get("/catalog/{scryfallId}", scryfallId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scryfallId").value(scryfallId))
                .andExpect(jsonPath("$.name").value("Black Lotus"))
                .andExpect(jsonPath("$.setCode").value("LEA"))
                .andExpect(jsonPath("$.setName").value("Limited Edition Alpha"))
                .andExpect(jsonPath("$.imageUrl").value("https://cards.scryfall.io/normal/front/example.jpg"));

        // VERIFY: confirmamos que el controller delegó la consulta
        // al service con exactamente el scryfallId recibido en la ruta.
        verify(catalogService, times(1)).getCardById(scryfallId);
    }


// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 200 OK con el campo "name": "Black Lotus"
// Se obtuvo: HTTP 200 OK con el campo "name" vacío o nulo
// Esto podría pasar si alguien cambia el orden de los argumentos en el
// constructor de CardResponse (por ejemplo, intercambiando "name" y
// "setName" por error), ya que CardResponse usa @AllArgsConstructor de
// Lombok, que es posicional y no detecta este tipo de error en
// tiempo de compilación.

    @Test
    void getById_conScryfallIdInexistente_retorna404NotFound() throws Exception {
        // ARRANGE: preparamos un id que no existe y simulamos que el
        // service lanza la excepción de negocio real al no encontrarlo.
        String scryfallIdInexistente = "id-que-no-existe-en-ningun-lado";

        when(catalogService.getCardById(scryfallIdInexistente))
                .thenThrow(new CardNotFoundException(scryfallIdInexistente));

        // ACT: ejecutamos el endpoint GET /catalog/{scryfallId} con MockMvc.
        // ASSERT: verificamos 404 Not Found y el mensaje de error esperado.
        mockMvc.perform(get("/catalog/{scryfallId}", scryfallIdInexistente))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(
                        "Carta no encontrada: " + scryfallIdInexistente));

        // VERIFY: confirmamos que el controller intentó delegar la consulta
        // al service, exactamente una vez.
        verify(catalogService, times(1)).getCardById(scryfallIdInexistente);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 404 Not Found
// Se obtuvo: HTTP 500 Internal Server Error
// Esto podría pasar si alguien elimina por error el @ExceptionHandler
// de CardNotFoundException en GlobalExceptionHandler, dejando que la
// excepción caiga en el handler genérico de Exception.class, que
// siempre responde 500 sin distinguir el tipo de error real.


    @Test
    void search_conNombreExistente_retorna200ConListaDeCartas() throws Exception {
        // ARRANGE: preparamos el nombre a buscar y una lista simulada
        // de resultados que debería devolver el service.
        String name = "Black Lotus";

        CardResponse carta1 = new CardResponse(
                "id-1", "Black Lotus", "LEA", "Limited Edition Alpha",
                "https://cards.scryfall.io/normal/front/id-1.jpg"
        );
        CardResponse carta2 = new CardResponse(
                "id-2", "Black Lotus", "LEB", "Limited Edition Beta",
                "https://cards.scryfall.io/normal/front/id-2.jpg"
        );

        when(catalogService.searchByName(name)).thenReturn(List.of(carta1, carta2));

        // ACT: ejecutamos el endpoint GET /catalog/search con MockMvc,
        // enviando el parámetro "name" en la query string.
        // ASSERT: verificamos 200 OK y que la lista tenga exactamente
        // los 2 elementos esperados, con sus datos correctos.
        mockMvc.perform(get("/catalog/search").param("name", name))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].scryfallId").value("id-1"))
                .andExpect(jsonPath("$[0].setCode").value("LEA"))
                .andExpect(jsonPath("$[1].scryfallId").value("id-2"))
                .andExpect(jsonPath("$[1].setCode").value("LEB"));

        // VERIFY: confirmamos que el controller delegó la búsqueda
        // al service con exactamente el nombre recibido.
        verify(catalogService, times(1)).searchByName(name);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: una lista con 2 cartas distintas (LEA y LEB) para
// el mismo nombre "Black Lotus"
// Se obtuvo: una lista con solo 1 elemento, o con elementos duplicados
// Esto podría pasar si alguien cambia por error
// "cardRepository.searchByName(name)" en CatalogService por un metodo
// que retorna solo el primer resultado (como "findFirstByName"), rompiendo
// la posibilidad de listar todas las versiones/sets de una misma carta.

    @Test
    void search_conNombreInexistente_retorna404NotFound() throws Exception {
        // ARRANGE: preparamos un nombre que no coincide con ninguna
        // carta, y simulamos que el service lanza la excepción real.
        String nombreInexistente = "Carta Que No Existe";

        when(catalogService.searchByName(nombreInexistente))
                .thenThrow(new CardNotFoundException(nombreInexistente));

        // ACT: ejecutamos el endpoint GET /catalog/search con MockMvc.
        // ASSERT: verificamos 404 Not Found y el mensaje de error esperado.
        mockMvc.perform(get("/catalog/search").param("name", nombreInexistente))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(
                        "Carta no encontrada: " + nombreInexistente));

        // VERIFY: confirmamos que el controller delegó la búsqueda
        // al service con exactamente el nombre recibido.
        verify(catalogService, times(1)).searchByName(nombreInexistente);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 404 Not Found con un array vacío o mensaje claro
// Se obtuvo: HTTP 200 OK con un array vacío "[]"
// Esto podría pasar si alguien cambia CatalogService.searchByName()
// para retornar una lista vacía en vez de lanzar CardNotFoundException
// cuando Scryfall no encuentra resultados — un cambio de comportamiento
// que rompería el contrato documentado en el Swagger del controller
// (que especifica 404 para este caso), sin que el código deje de
// compilar o el test de éxito deje de pasar.

    @Test
    void getAllSets_retorna200ConListaDeCardResponse() throws Exception {
        // ARRANGE: preparamos una lista simulada de CardResponse.
        // NOTA: a pesar del nombre "getAllSets", el service real retorna
        // CardResponse (cartas), no CardSet — ver CatalogService.getAllSets(),
        // que usa cardRepository.findAll() en vez de cardSetRepository.findAll().
        // El test refleja el comportamiento real del código, no el que
        // sugiere el nombre del metodo.
        CardResponse carta1 = new CardResponse(
                "id-1", "Black Lotus", "LEA", "Limited Edition Alpha",
                "https://cards.scryfall.io/normal/front/id-1.jpg"
        );
        CardResponse carta2 = new CardResponse(
                "id-2", "Mox Sapphire", "LEA", "Limited Edition Alpha",
                "https://cards.scryfall.io/normal/front/id-2.jpg"
        );

        when(catalogService.getAllSets()).thenReturn(List.of(carta1, carta2));

        // ACT: ejecutamos el endpoint GET /catalog/sets con MockMvc.
        // ASSERT: verificamos 200 OK y los datos esperados en el body.
        mockMvc.perform(get("/catalog/sets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Black Lotus"))
                .andExpect(jsonPath("$[1].name").value("Mox Sapphire"));

        // VERIFY: confirmamos que el controller delegó correctamente
        // al service, sin parámetros (este endpoint no recibe ninguno).
        verify(catalogService, times(1)).getAllSets();
    }

// CASO HIPOTÉTICO DE FALLA (para QA) — ESTE ES EL BUG REAL DOCUMENTADO:
// Se esperaba (según el nombre del metodo y el Swagger): una lista de
// SETS (códigos y nombres de edición, ej. "LEA - Limited Edition Alpha")
// Se obtuvo: una lista de CARTAS individuales con sus propios datos
// Esto es un bug real y ya presente en CatalogService.getAllSets(),
// que usa cardRepository.findAll() en vez de cardSetRepository.findAll().
// Debería reportarse a desarrollo como un defecto de severidad media:
// el endpoint funciona sin errores técnicos, pero entrega datos
// semánticamente incorrectos respecto a su nombre y documentación.


    @Test
    void findByNameAndSet_conNombreYSetCode_retorna200ConCartaEspecifica() throws Exception {
        // ARRANGE: preparamos nombre y setCode, y la respuesta simulada
        // que debería devolver el service para esa combinación exacta.
        String name = "Black Lotus";
        String setCode = "LEB";

        CardResponse fakeResponse = new CardResponse(
                "id-leb", name, setCode, "Limited Edition Beta",
                "https://cards.scryfall.io/normal/front/id-leb.jpg"
        );

        when(catalogService.findByNameAndSet(name, setCode)).thenReturn(fakeResponse);

        // ACT: ejecutamos el endpoint GET /catalog/find con ambos parámetros.
        // ASSERT: verificamos 200 OK y que la carta retornada sea
        // exactamente la del set solicitado.
        mockMvc.perform(get("/catalog/find")
                        .param("name", name)
                        .param("setCode", setCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scryfallId").value("id-leb"))
                .andExpect(jsonPath("$.setCode").value(setCode));

        // VERIFY: confirmamos que el controller delegó al service con
        // exactamente el nombre y setCode recibidos.
        verify(catalogService, times(1)).findByNameAndSet(name, setCode);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: la carta retornada tiene setCode == "LEB" (el solicitado)
// Se obtuvo: la carta retornada tiene setCode == "LEA" (otro set distinto)
// Esto podría pasar si alguien modifica CatalogService.findByNameAndSet()
// y rompe el filtro ".filter(c -> setCode.equalsIgnoreCase(c.getSetCode()))",
// retornando la primera coincidencia de nombre sin respetar el set
// específico solicitado por el usuario.

    @Test
    void findByNameAndSet_sinSetCode_retorna200ConPrimeraCoincidencia() throws Exception {
        // ARRANGE: solo enviamos el nombre, sin setCode (parámetro opcional).
        String name = "Black Lotus";

        CardResponse fakeResponse = new CardResponse(
                "id-lea", name, "LEA", "Limited Edition Alpha",
                "https://cards.scryfall.io/normal/front/id-lea.jpg"
        );

        // Importante: cuando setCode no se envía, Spring lo resuelve como
        // null (es un @RequestParam(required = false) de tipo String).
        when(catalogService.findByNameAndSet(name, null)).thenReturn(fakeResponse);

        // ACT: ejecutamos el endpoint enviando solo "name", sin "setCode".
        // ASSERT: verificamos 200 OK con la carta esperada.
        mockMvc.perform(get("/catalog/find").param("name", name))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scryfallId").value("id-lea"));

        // VERIFY: confirmamos que el controller llamó al service pasando
        // null como setCode, tal como espera la firma real del metodo.
        verify(catalogService, times(1)).findByNameAndSet(name, null);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 200 OK al omitir el parámetro setCode (es opcional)
// Se obtuvo: HTTP 400 Bad Request, tratando setCode como obligatorio
// Esto podría pasar si alguien elimina por error el atributo
// "required = false" de la anotación @RequestParam en setCode dentro
// de CatalogController.findByNameAndSet(), volviendo obligatorio un
// parámetro que la documentación de Swagger describe explícitamente
// como opcional.

    @Test
    void findByNameAndSet_sinCoincidencias_retorna404NotFound() throws Exception {
        // ARRANGE: preparamos nombre y setCode que no tienen ninguna
        // coincidencia, y simulamos que el service lanza la excepción real.
        String name = "Carta Inexistente";
        String setCode = "XXX";

        when(catalogService.findByNameAndSet(name, setCode))
                .thenThrow(new CardNotFoundException(name + " en el set " + setCode));

        // ACT: ejecutamos el endpoint GET /catalog/find con MockMvc.
        // ASSERT: verificamos 404 Not Found y el mensaje de error esperado.
        mockMvc.perform(get("/catalog/find")
                        .param("name", name)
                        .param("setCode", setCode))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(
                        "Carta no encontrada: " + name + " en el set " + setCode));

        // VERIFY: confirmamos que el controller delegó al service con
        // exactamente el nombre y setCode recibidos.
        verify(catalogService, times(1)).findByNameAndSet(name, setCode);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 404 Not Found con mensaje "Carta no encontrada:
// Carta Inexistente en el set XXX"
// Se obtuvo: HTTP 404 Not Found con mensaje genérico "Carta no
// encontrada: Carta Inexistente" (sin mencionar el set)
// Esto podría pasar si alguien simplifica por error el mensaje de
// CardNotFoundException en CatalogService.fetchFromScryfallByNameAndSet(),
// perdiendo el detalle de qué setCode específico no tuvo coincidencias —
// un problema menor de UX/debugging, no funcional, pero que dificulta
// diagnosticar por qué una búsqueda específica falló cuando el usuario
// sí proporcionó un set.
}