package com.quandrix.ms_users.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quandrix.ms_users.dto.StoreProfileRequest;
import com.quandrix.ms_users.dto.StoreProfileResponse;
import com.quandrix.ms_users.exception.ProfileAlreadyExistsException;
import com.quandrix.ms_users.exception.UserNotFoundException;
import com.quandrix.ms_users.service.StoreProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StoreProfileController.class)
class StoreProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StoreProfileService storeProfileService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void create_conDatosValidos_retorna201ConPerfilCreado() throws Exception {
        // ARRANGE
        StoreProfileRequest request = new StoreProfileRequest();
        request.setUserId(1L);
        request.setStoreName("Tienda de cartas - MTG");
        request.setLocation("Santiago, Chile");
        request.setDescription("Se venden cartas de MTG");

        StoreProfileResponse fakeResponse = new StoreProfileResponse(
                1L, 1L, "Tienda de cartas - MTG", LocalDateTime.of(2026, 6, 12, 14, 30));

        when(storeProfileService.create(any(StoreProfileRequest.class))).thenReturn(fakeResponse);

        // ACT + ASSERT
        mockMvc.perform(post("/stores")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.storeName").value("Tienda de cartas - MTG"));

        // VERIFY
        verify(storeProfileService, times(1)).create(any(StoreProfileRequest.class));
    }

    // CASO HIPOTÉTICO DE FALLA (para QA):
    // Se esperaba: HTTP 201 Created con storeName mapeado exactamente
    // como se envió
    // Se obtuvo: HTTP 201 Created con storeName distinto al enviado
    // Esto podría pasar si alguien modifica el orden de los argumentos
    // en el constructor de StoreProfileResponse (@AllArgsConstructor
    // posicional de Lombok), intercambiando "userId" y "storeName"
    // sin que el compilador lo detecte.

    @Test
    void create_conUserIdDuplicado_retorna409Conflict() throws Exception {
        // ARRANGE
        StoreProfileRequest request = new StoreProfileRequest();
        request.setUserId(1L);
        request.setStoreName("Tienda de cartas - MTG");

        when(storeProfileService.create(any(StoreProfileRequest.class)))
                .thenThrow(new ProfileAlreadyExistsException(1L));

        // ACT + ASSERT
        mockMvc.perform(post("/stores")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Ya existe un perfil para el userId: 1"));

        // VERIFY
        verify(storeProfileService, times(1)).create(any(StoreProfileRequest.class));
    }

    // CASO HIPOTÉTICO DE FALLA (para QA):
    // Se esperaba: HTTP 409 Conflict cuando el userId ya tiene un
    // perfil de tienda creado (reforzado también por
    // @Column(unique = true) en la entidad StoreProfile)
    // Se obtuvo: HTTP 500 Internal Server Error
    // Esto podría pasar si alguien elimina el @ExceptionHandler de
    // ProfileAlreadyExistsException.

    @Test
    void getByUserId_conUserIdExistente_retorna200ConPerfil() throws Exception {
        // ARRANGE
        Long userId = 1L;
        StoreProfileResponse fakeResponse = new StoreProfileResponse(
                1L, userId, "Tienda de cartas - MTG", LocalDateTime.of(2026, 6, 12, 14, 30));

        when(storeProfileService.getByUserId(userId)).thenReturn(fakeResponse);

        // ACT + ASSERT
        mockMvc.perform(get("/stores/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.storeName").value("Tienda de cartas - MTG"));

        // VERIFY
        verify(storeProfileService, times(1)).getByUserId(userId);
    }

    // CASO HIPOTÉTICO DE FALLA (para QA):
    // Se esperaba: HTTP 200 OK al consultar un perfil de tienda
    // existente
    // Se obtuvo: HTTP 404 Not Found a pesar de que el perfil existe
    // Esto podría pasar si alguien cambia por error el tipo del
    // @PathVariable de Long a String en
    // StoreProfileController.getByUserId().

    @Test
    void getByUserId_conUserIdInexistente_retorna404NotFound() throws Exception {
        // ARRANGE
        Long userIdInexistente = 999L;

        when(storeProfileService.getByUserId(userIdInexistente))
                .thenThrow(new UserNotFoundException(userIdInexistente));

        // ACT + ASSERT
        mockMvc.perform(get("/stores/{userId}", userIdInexistente))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(
                        "No se encontró perfil para el userId: " + userIdInexistente));

        // VERIFY
        verify(storeProfileService, times(1)).getByUserId(userIdInexistente);
    }

    // CASO HIPOTÉTICO DE FALLA (para QA):
    // Se esperaba: HTTP 404 Not Found cuando no existe ningún perfil
    // de tienda para ese userId
    // Se obtuvo: HTTP 500 Internal Server Error
    // Esto podría pasar si alguien elimina el @ExceptionHandler de
    // UserNotFoundException.

    @Test
    void getAll_conPerfilesExistentes_retorna200ConLista() throws Exception {
        // ARRANGE
        StoreProfileResponse store1 = new StoreProfileResponse(
                1L, 1L, "Tienda A", LocalDateTime.of(2026, 6, 12, 14, 30));
        StoreProfileResponse store2 = new StoreProfileResponse(
                2L, 2L, "Tienda B", LocalDateTime.of(2026, 6, 11, 10, 0));

        when(storeProfileService.getAll()).thenReturn(List.of(store1, store2));

        // ACT + ASSERT
        mockMvc.perform(get("/stores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // VERIFY
        verify(storeProfileService, times(1)).getAll();
    }

    // CASO HIPOTÉTICO DE FALLA (para QA):
    // Se esperaba: la lista incluye TODOS los perfiles de tienda
    // registrados, sin ningún filtro
    // Se obtuvo: la lista viene incompleta o filtrada incorrectamente
    // Esto podría pasar si alguien modifica por error
    // repository.findAll() en StoreProfileService.getAll() y aplica
    // algún filtro no documentado.

    @Test
    void getAll_sinPerfiles_retorna200ConListaVacia() throws Exception {
        // ARRANGE: mismo criterio de contrato ya aplicado en
        // Quandrix — el Swagger documenta un posible 404, pero el
        // comportamiento real es 200 OK + lista vacía.
        when(storeProfileService.getAll()).thenReturn(List.of());

        // ACT + ASSERT
        mockMvc.perform(get("/stores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // VERIFY
        verify(storeProfileService, times(1)).getAll();
    }

    // NOTA PARA QA (no es un caso de falla, es una aclaración de
    // contrato): 200 OK + lista vacía representa "aún no hay
    // tiendas registradas en el marketplace", no un error.

    @Test
    void update_conDatosValidos_retorna200ConPerfilActualizado() throws Exception {
        // ARRANGE
        Long userId = 1L;
        StoreProfileRequest request = new StoreProfileRequest();
        request.setUserId(userId);
        request.setStoreName("Nuevo nombre de tienda");

        StoreProfileResponse fakeResponse = new StoreProfileResponse(
                1L, userId, "Nuevo nombre de tienda", LocalDateTime.of(2026, 6, 12, 14, 30));

        when(storeProfileService.update(eq(userId), any(StoreProfileRequest.class)))
                .thenReturn(fakeResponse);

        // ACT + ASSERT
        mockMvc.perform(put("/stores/{userId}", userId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeName").value("Nuevo nombre de tienda"));

        // VERIFY
        verify(storeProfileService, times(1)).update(eq(userId), any(StoreProfileRequest.class));
    }

    // CASO HIPOTÉTICO DE FALLA (para QA):
    // Se esperaba: HTTP 200 OK con el nuevo storeName reflejado
    // Se obtuvo: HTTP 200 OK pero con el storeName ORIGINAL sin
    // actualizar
    // Esto podría pasar si alguien modifica StoreProfileService
    // .update() y olvida llamar a
    // profile.setStoreName(request.getStoreName()) antes de
    // repository.save(profile).

    @Test
    void update_conUserIdInexistente_retorna404NotFound() throws Exception {
        // ARRANGE
        Long userIdInexistente = 999L;
        StoreProfileRequest request = new StoreProfileRequest();
        request.setUserId(userIdInexistente);
        request.setStoreName("Nuevo nombre de tienda");

        when(storeProfileService.update(eq(userIdInexistente), any(StoreProfileRequest.class)))
                .thenThrow(new UserNotFoundException(userIdInexistente));

        // ACT + ASSERT
        mockMvc.perform(put("/stores/{userId}", userIdInexistente)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(
                        "No se encontró perfil para el userId: " + userIdInexistente));

        // VERIFY
        verify(storeProfileService, times(1)).update(eq(userIdInexistente), any(StoreProfileRequest.class));
    }

    // CASO HIPOTÉTICO DE FALLA (para QA):
    // Se esperaba: HTTP 404 Not Found al intentar actualizar un
    // perfil de tienda que no existe
    // Se obtuvo: HTTP 500 Internal Server Error
    // Esto podría pasar si alguien elimina el @ExceptionHandler de
    // UserNotFoundException.

    @Test
    void delete_conUserIdExistente_retorna204NoContent() throws Exception {
        // ARRANGE
        Long userId = 1L;
        doNothing().when(storeProfileService).delete(userId);

        // ACT + ASSERT
        mockMvc.perform(delete("/stores/{userId}", userId))
                .andExpect(status().isNoContent());

        // VERIFY
        verify(storeProfileService, times(1)).delete(userId);
    }

    // CASO HIPOTÉTICO DE FALLA (para QA):
    // Se esperaba: HTTP 204 No Content al eliminar un perfil de
    // tienda existente
    // Se obtuvo: HTTP 404 Not Found a pesar de que el perfil sí
    // existe
    // Esto podría pasar si alguien invierte por error la lógica de
    // existencia en StoreProfileService.delete().

    @Test
    void delete_conUserIdInexistente_retorna404NotFound() throws Exception {
        // ARRANGE
        Long userIdInexistente = 999L;
        doThrow(new UserNotFoundException(userIdInexistente))
                .when(storeProfileService).delete(userIdInexistente);

        // ACT + ASSERT
        mockMvc.perform(delete("/stores/{userId}", userIdInexistente))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(
                        "No se encontró perfil para el userId: " + userIdInexistente));

        // VERIFY
        verify(storeProfileService, times(1)).delete(userIdInexistente);
    }

    // CASO HIPOTÉTICO DE FALLA (para QA):
    // Se esperaba: HTTP 404 Not Found al eliminar un perfil de
    // tienda que no existe
    // Se obtuvo: HTTP 204 No Content (silenciosamente "exitoso")
    // Esto podría pasar si alguien reemplaza la validación de
    // existencia por un deleteById(id) directo de Spring Data JPA.
}