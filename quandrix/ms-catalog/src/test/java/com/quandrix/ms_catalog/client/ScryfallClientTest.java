package com.quandrix.ms_catalog.client;

import com.quandrix.ms_catalog.dto.ScryfallCardDto;
import com.quandrix.ms_catalog.exception.CardNotFoundException;
import com.quandrix.ms_catalog.exception.ScryfallApiException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScryfallClientTest {

    private MockWebServer mockWebServer;
    private ScryfallClient scryfallClient;

    @BeforeEach
    void setUp() throws IOException {
        // ARRANGE (común a todos los tests): levantamos un servidor
        // HTTP falso real en un puerto local, y construimos un WebClient
        // de prueba que le apunta a él (en vez de a la URL real de Scryfall).
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        WebClient testWebClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .defaultHeader("User-Agent", "Quandrix-MTG-Marketplace/1.0")
                .defaultHeader("Accept", "application/json")
                .build();

        scryfallClient = new ScryfallClient(testWebClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        // Cerramos el servidor falso después de cada test, para no
        // dejar puertos abiertos ni que un test interfiera con otro.
        mockWebServer.shutdown();
    }

    @Test
    void getCardById_conRespuesta200_retornaScryfallCardDtoCorrecto() throws Exception {
        // ARRANGE: programamos al servidor falso para que, ante la
        // próxima petición que reciba, responda con este JSON y 200 OK.
        String jsonRespuesta = """
                {
                    "id": "bd8fa8c8-7e1c-4f5d-a6d3-123456789abc",
                    "name": "Black Lotus",
                    "set": "lea",
                    "set_name": "Limited Edition Alpha",
                    "image_uris": {
                        "normal": "https://cards.scryfall.io/normal/front/example.jpg"
                    }
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(jsonRespuesta)
                .addHeader("Content-Type", "application/json"));

        // ACT: ejecutamos el metodo real, que le hace una petición
        // HTTP genuina al servidor falso.
        ScryfallCardDto dto = scryfallClient.getCardById("bd8fa8c8-7e1c-4f5d-a6d3-123456789abc");

        // ASSERT: verificamos que el DTO se deserializó correctamente,
        // incluyendo los campos renombrados (@JsonProperty).
        assertThat(dto.getId()).isEqualTo("bd8fa8c8-7e1c-4f5d-a6d3-123456789abc");
        assertThat(dto.getName()).isEqualTo("Black Lotus");
        assertThat(dto.getSetCode()).isEqualTo("lea");
        assertThat(dto.getSetName()).isEqualTo("Limited Edition Alpha");
        assertThat(dto.getImageUris().getNormal())
                .isEqualTo("https://cards.scryfall.io/normal/front/example.jpg");
    }


// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: dto.getSetCode() == "lea" (mapeado desde el campo
// JSON "set", vía @JsonProperty("set"))
// Se obtuvo: dto.getSetCode() == null
// Esto podría pasar si alguien modifica ScryfallCardDto y elimina por
// error la anotación @JsonProperty("set") del campo setCode, dejando
// que Jackson intente mapearlo por el nombre del campo Java ("setCode")
// en vez del nombre real que usa la API de Scryfall ("set") — Jackson
// no lanzaría ningún error, simplemente dejaría el campo en null
// silenciosamente, ya que @JsonIgnoreProperties(ignoreUnknown = true)
// está activo en la clase.

    @Test
    void getCardById_conRespuesta404_lanzaCardNotFoundException() {
        // ARRANGE: programamos al servidor falso para que responda 404,
        // simulando que Scryfall no tiene ninguna carta con ese ID.
        String scryfallId = "id-que-no-existe-en-scryfall";

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(404)
                .setBody("""
                    {"object": "error", "code": "not_found", "details": "No card found"}
                    """)
                .addHeader("Content-Type", "application/json"));

        // ACT + ASSERT: se debe lanzar CardNotFoundException con el
        // scryfallId como parte del mensaje.
        CardNotFoundException ex = org.junit.jupiter.api.Assertions.assertThrows(
                CardNotFoundException.class,
                () -> scryfallClient.getCardById(scryfallId)
        );
        assertThat(ex.getMessage()).isEqualTo("Carta no encontrada: " + scryfallId);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: CardNotFoundException cuando Scryfall responde 404
// Se obtuvo: ScryfallApiException (la excepción genérica) en su lugar
// Esto podría pasar si alguien reordena los bloques catch en
// ScryfallClient.getCardById(), poniendo "catch (Exception e)" ANTES
// de "catch (WebClientResponseException.NotFound e)" — en Java, el
// primer catch que coincida con el tipo de excepción es el que se
// ejecuta, así que un catch genérico puesto primero "atraparía" el
// 404 también, ocultando el caso específico y devolviendo siempre
// un error 502 Bad Gateway en vez de un 404 Not Found al usuario final.

    @Test
    void getCardById_conRespuesta500_lanzaScryfallApiException() {
        // ARRANGE: programamos al servidor falso para que responda 500,
        // simulando una falla del lado del servidor de Scryfall.
        String scryfallId = "id-cualquiera";

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error"));

        // ACT + ASSERT: se debe lanzar ScryfallApiException (la excepción
        // genérica), no CardNotFoundException ni una excepción cruda de WebClient.
        org.junit.jupiter.api.Assertions.assertThrows(
                ScryfallApiException.class,
                () -> scryfallClient.getCardById(scryfallId)
        );
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: ScryfallApiException cuando Scryfall responde 500
// (un código de error que NO es 404, por lo tanto cae en el catch
// genérico de Exception)
// Se obtuvo: una WebClientResponseException sin capturar, propagándose
// directamente hasta el controller y generando una respuesta HTTP
// poco informativa o un stack trace expuesto al cliente final.
// Esto podría pasar si alguien acota por error el segundo catch de
// "catch (Exception e)" a un tipo más específico (por ejemplo, solo
// "catch (WebClientResponseException.ServiceUnavailable e)"), dejando
// que otros códigos de error 5xx —o errores de red genéricos, como
// un timeout— se propaguen sin control hacia las capas superiores.

    @Test
    void searchByName_conRespuesta200YResultados_retornaListaDeDtos() {
        // ARRANGE: Scryfall responde 200 con 2 cartas en el campo "data".
        String name = "Black Lotus";

        String jsonRespuesta = """
            {
                "data": [
                    {
                        "id": "id-1",
                        "name": "Black Lotus",
                        "set": "lea",
                        "set_name": "Limited Edition Alpha"
                    },
                    {
                        "id": "id-2",
                        "name": "Black Lotus",
                        "set": "leb",
                        "set_name": "Limited Edition Beta"
                    }
                ],
                "has_more": false,
                "total_cards": 2
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(jsonRespuesta)
                .addHeader("Content-Type", "application/json"));

        // ACT
        List<ScryfallCardDto> resultados = scryfallClient.searchByName(name);

        // ASSERT: verificamos que la lista tenga las 2 cartas, correctamente
        // deserializadas desde el campo "data" del response.
        assertThat(resultados).hasSize(2);
        assertThat(resultados.get(0).getId()).isEqualTo("id-1");
        assertThat(resultados.get(0).getSetCode()).isEqualTo("lea");
        assertThat(resultados.get(1).getId()).isEqualTo("id-2");
        assertThat(resultados.get(1).getSetCode()).isEqualTo("leb");
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: una lista con 2 ScryfallCardDto, leídos desde el
// campo JSON "data" del ScryfallSearchResponse
// Se obtuvo: una lista vacía, a pesar de que el JSON sí contiene datos
// Esto podría pasar si alguien renombra por error el campo "data" en
// ScryfallSearchResponse sin agregar el @JsonProperty correspondiente,
// rompiendo el mapeo entre el nombre real del campo en la API de
// Scryfall y el nombre de la propiedad Java.


    @Test
    void searchByName_conRespuesta200PeroSinResultados_retornaListaVacia() {
        // ARRANGE: Scryfall responde 200 (no 404), pero con "data" vacío —
        // esto es lo que realmente hace la API de Scryfall cuando la
        // búsqueda no encuentra coincidencias en algunos casos.
        String name = "Carta Que No Existe";

        String jsonRespuesta = """
            {
                "data": [],
                "has_more": false,
                "total_cards": 0
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(jsonRespuesta)
                .addHeader("Content-Type", "application/json"));

        // ACT
        List<ScryfallCardDto> resultados = scryfallClient.searchByName(name);

        // ASSERT: debe retornar una lista vacía, NO lanzar ninguna excepción
        // (la decisión de lanzar CardNotFoundException le corresponde a
        // CatalogService, no a ScryfallClient).
        assertThat(resultados).isEmpty();
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: una lista vacía (List.of()) cuando "data" está vacío,
// sin lanzar ninguna excepción desde ScryfallClient
// Se obtuvo: una NullPointerException u otra excepción no controlada
// al intentar procesar una lista vacía como si tuviera elementos
// Esto podría pasar si alguien elimina por error la validación
// "if (response == null || response.getData() == null)" en
// ScryfallClient.searchByName(), aunque en este caso específico
// "data" no es null sino una lista vacía — vale la pena notar que
// el código actual NO distingue entre "data: null" y "data: []",
// tratando ambos casos correctamente gracias a response.getData().isEmpty()
// implícito en el manejo posterior, pero es un detalle sutil que merece
// este test específico para confirmarlo.


    @Test
    void searchByName_conRespuesta404_retornaListaVacia() {
        // ARRANGE: Scryfall responde 404 directamente (sin body de datos),
        // simulando que el endpoint de búsqueda no encontró nada en absoluto.
        String name = "Otra Carta Inexistente";

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(404)
                .setBody("""
                    {"object": "error", "code": "not_found", "details": "No cards found"}
                    """)
                .addHeader("Content-Type", "application/json"));

        // ACT
        List<ScryfallCardDto> resultados = scryfallClient.searchByName(name);

        // ASSERT: a diferencia de getCardById (que lanza CardNotFoundException
        // ante un 404), searchByName debe retornar una lista vacía silenciosamente.
        assertThat(resultados).isEmpty();
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: una lista vacía cuando Scryfall responde 404 para una
// búsqueda por nombre (comportamiento intencionalmente distinto a
// getCardById, donde un 404 SÍ lanza CardNotFoundException)
// Se obtuvo: CardNotFoundException lanzada también aquí, rompiendo
// CatalogService.searchByName(), que espera poder revisar
// "if (dtos.isEmpty())" para decidir si lanzar su propia excepción
// con un mensaje más específico del contexto de búsqueda por nombre.
// Esto podría pasar si alguien "unifica" por error el manejo de 404
// entre getCardById y searchByName, copiando el bloque catch de uno
// al otro sin notar que el comportamiento esperado es deliberadamente
// distinto entre ambos métodos.


    @Test
    void searchByName_conRespuesta500_lanzaScryfallApiException() {
        // ARRANGE: Scryfall responde con un error de servidor genérico.
        String name = "Cualquier Nombre";

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error"));

        // ACT + ASSERT: a diferencia de los 404 (que retornan lista vacía),
        // un error 500 SÍ debe propagarse como ScryfallApiException.
        org.junit.jupiter.api.Assertions.assertThrows(
                ScryfallApiException.class,
                () -> scryfallClient.searchByName(name)
        );
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: ScryfallApiException cuando Scryfall responde 500
// Se obtuvo: una lista vacía silenciosa, ocultando que en realidad
// hubo una falla real de comunicación con el servicio externo,
// distinta a "no hay resultados para esta búsqueda".
// Esto podría pasar si alguien amplía por error el catch de
// "WebClientResponseException.NotFound" a un tipo más genérico que
// también atrape errores 500, devolviendo siempre List.of() sin
// distinguir entre "no encontrado" (esperado) y "error del servidor"
// (anómalo) — un problema serio porque CatalogService nunca se
// enteraría de que Scryfall está caído, y simplemente reportaría
// "carta no encontrada" en vez de un error de servicio.
}