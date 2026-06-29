package com.quandrix.ms_users.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quandrix.ms_users.dto.UserProfileRequest;
import com.quandrix.ms_users.dto.UserProfileResponse;
import com.quandrix.ms_users.exception.ProfileAlreadyExistsException;
import com.quandrix.ms_users.exception.UserNotFoundException;
import com.quandrix.ms_users.service.UserProfileService;
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

@WebMvcTest(UserProfileController.class)
class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserProfileService userProfileService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void create_conDatosValidos_retorna201ConPerfilCreado() throws Exception {
        // ARRANGE
        UserProfileRequest request = new UserProfileRequest();
        request.setUserId(1L);
        request.setDisplayName("persona_123");

        UserProfileResponse fakeResponse = new UserProfileResponse(
                1L, 1L, "persona_123", LocalDateTime.of(2026, 6, 12, 14, 30));

        when(userProfileService.create(any(UserProfileRequest.class))).thenReturn(fakeResponse);

        // ACT + ASSERT
        mockMvc.perform(post("/users")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.displayName").value("persona_123"));

        // VERIFY
        verify(userProfileService, times(1)).create(any(UserProfileRequest.class));
    }


// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 201 Created con displayName mapeado exactamente
// como se envió
// Se obtuvo: HTTP 201 Created con displayName distinto al enviado
// Esto podría pasar si alguien modifica el orden de los argumentos
// en el constructor de UserProfileResponse (@AllArgsConstructor
// posicional de Lombok), intercambiando "userId" y "displayName"
// sin que el compilador lo detecte.


    @Test
    void create_conUserIdDuplicado_retorna409Conflict() throws Exception {
        // ARRANGE
        UserProfileRequest request = new UserProfileRequest();
        request.setUserId(1L);
        request.setDisplayName("persona_123");

        when(userProfileService.create(any(UserProfileRequest.class)))
                .thenThrow(new ProfileAlreadyExistsException(1L));

        // ACT + ASSERT
        mockMvc.perform(post("/users")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Ya existe un perfil para el userId: 1"));

        // VERIFY
        verify(userProfileService, times(1)).create(any(UserProfileRequest.class));
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 409 Conflict cuando el userId ya tiene un
// perfil creado (reforzado también por @Column(unique = true) en
// la entidad UserProfile)
// Se obtuvo: HTTP 500 Internal Server Error
// Esto podría pasar si alguien elimina el @ExceptionHandler de
// ProfileAlreadyExistsException.


    @Test
    void getByUserId_conUserIdExistente_retorna200ConPerfil() throws Exception {
        // ARRANGE
        Long userId = 1L;
        UserProfileResponse fakeResponse = new UserProfileResponse(
                1L, userId, "persona_123", LocalDateTime.of(2026, 6, 12, 14, 30));

        when(userProfileService.getByUserId(userId)).thenReturn(fakeResponse);

        // ACT + ASSERT
        mockMvc.perform(get("/users/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.displayName").value("persona_123"));

        // VERIFY
        verify(userProfileService, times(1)).getByUserId(userId);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 200 OK al consultar un perfil existente
// Se obtuvo: HTTP 404 Not Found a pesar de que el perfil existe
// Esto podría pasar si alguien cambia por error el tipo del
// @PathVariable de Long a String en UserProfileController
// .getByUserId().


    @Test
    void getByUserId_conUserIdInexistente_retorna404NotFound() throws Exception {
        // ARRANGE
        Long userIdInexistente = 999L;

        when(userProfileService.getByUserId(userIdInexistente))
                .thenThrow(new UserNotFoundException(userIdInexistente));

        // ACT + ASSERT
        mockMvc.perform(get("/users/{userId}", userIdInexistente))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(
                        "No se encontró perfil para el userId: " + userIdInexistente));

        // VERIFY
        verify(userProfileService, times(1)).getByUserId(userIdInexistente);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 404 Not Found cuando no existe ningún perfil
// para ese userId
// Se obtuvo: HTTP 500 Internal Server Error
// Esto podría pasar si alguien elimina el @ExceptionHandler de
// UserNotFoundException.

    @Test
    void getAll_conPerfilesExistentes_retorna200ConLista() throws Exception {
        // ARRANGE
        UserProfileResponse profile1 = new UserProfileResponse(
                1L, 1L, "persona_123", LocalDateTime.of(2026, 6, 12, 14, 30));
        UserProfileResponse profile2 = new UserProfileResponse(
                2L, 2L, "otro_usuario", LocalDateTime.of(2026, 6, 11, 10, 0));

        when(userProfileService.getAll()).thenReturn(List.of(profile1, profile2));

        // ACT + ASSERT
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // VERIFY
        verify(userProfileService, times(1)).getAll();
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: la lista incluye TODOS los perfiles registrados,
// sin ningún filtro
// Se obtuvo: la lista viene incompleta o filtrada incorrectamente
// Esto podría pasar si alguien modifica por error
// repository.findAll() en UserProfileService.getAll() y aplica
// algún filtro no documentado.


    @Test
    void getAll_sinPerfiles_retorna200ConListaVacia() throws Exception {
        // ARRANGE: mismo criterio de contrato ya aplicado en
        // Quandrix — el Swagger documenta un posible 404, pero el
        // comportamiento real es 200 OK + lista vacía.
        when(userProfileService.getAll()).thenReturn(List.of());

        // ACT + ASSERT
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // VERIFY
        verify(userProfileService, times(1)).getAll();
    }

// NOTA PARA QA (no es un caso de falla, es una aclaración de contrato):
// 200 OK + lista vacía representa "aún no hay perfiles de usuario
// registrados" (marketplace recién lanzado), no un error.

    @Test
    void update_conDatosValidos_retorna200ConPerfilActualizado() throws Exception {
        // ARRANGE
        Long userId = 1L;
        UserProfileRequest request = new UserProfileRequest();
        request.setUserId(userId);
        request.setDisplayName("nuevo_nombre");

        UserProfileResponse fakeResponse = new UserProfileResponse(
                1L, userId, "nuevo_nombre", LocalDateTime.of(2026, 6, 12, 14, 30));

        when(userProfileService.update(eq(userId), any(UserProfileRequest.class)))
                .thenReturn(fakeResponse);

        // ACT + ASSERT
        mockMvc.perform(put("/users/{userId}", userId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("nuevo_nombre"));

        // VERIFY
        verify(userProfileService, times(1)).update(eq(userId), any(UserProfileRequest.class));
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 200 OK con el nuevo displayName reflejado
// Se obtuvo: HTTP 200 OK pero con el displayName ORIGINAL sin
// actualizar
// Esto podría pasar si alguien modifica UserProfileService.update()
// y olvida llamar a profile.setDisplayName(request.getDisplayName())
// antes de repository.save(profile).


    @Test
    void update_conUserIdInexistente_retorna404NotFound() throws Exception {
        // ARRANGE
        Long userIdInexistente = 999L;
        UserProfileRequest request = new UserProfileRequest();
        request.setUserId(userIdInexistente);
        request.setDisplayName("nuevo_nombre");

        when(userProfileService.update(eq(userIdInexistente), any(UserProfileRequest.class)))
                .thenThrow(new UserNotFoundException(userIdInexistente));

        // ACT + ASSERT
        mockMvc.perform(put("/users/{userId}", userIdInexistente)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(
                        "No se encontró perfil para el userId: " + userIdInexistente));

        // VERIFY
        verify(userProfileService, times(1)).update(eq(userIdInexistente), any(UserProfileRequest.class));
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 404 Not Found al intentar actualizar un perfil
// que no existe
// Se obtuvo: HTTP 500 Internal Server Error
// Esto podría pasar si alguien elimina el @ExceptionHandler de
// UserNotFoundException.


    @Test
    void delete_conUserIdExistente_retorna204NoContent() throws Exception {
        // ARRANGE
        Long userId = 1L;
        doNothing().when(userProfileService).delete(userId);

        // ACT + ASSERT
        mockMvc.perform(delete("/users/{userId}", userId))
                .andExpect(status().isNoContent());

        // VERIFY
        verify(userProfileService, times(1)).delete(userId);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 204 No Content al eliminar un perfil existente
// Se obtuvo: HTTP 404 Not Found a pesar de que el perfil sí existe
// Esto podría pasar si alguien invierte por error la lógica de
// existencia en UserProfileService.delete().


    @Test
    void delete_conUserIdInexistente_retorna404NotFound() throws Exception {
        // ARRANGE
        Long userIdInexistente = 999L;
        doThrow(new UserNotFoundException(userIdInexistente))
                .when(userProfileService).delete(userIdInexistente);

        // ACT + ASSERT
        mockMvc.perform(delete("/users/{userId}", userIdInexistente))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(
                        "No se encontró perfil para el userId: " + userIdInexistente));

        // VERIFY
        verify(userProfileService, times(1)).delete(userIdInexistente);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 404 Not Found al eliminar un perfil que no existe
// Se obtuvo: HTTP 204 No Content (silenciosamente "exitoso")
// Esto podría pasar si alguien reemplaza la validación de existencia
// por un deleteById(id) directo de Spring Data JPA — mismo riesgo
// ya documentado repetidamente en otros microservicios.
}