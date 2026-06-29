package com.quandrix.ms_users.service;

import com.quandrix.ms_users.dto.UserProfileRequest;
import com.quandrix.ms_users.dto.UserProfileResponse;
import com.quandrix.ms_users.exception.ProfileAlreadyExistsException;
import com.quandrix.ms_users.exception.UserNotFoundException;
import com.quandrix.ms_users.model.UserProfile;
import com.quandrix.ms_users.repository.UserProfileRepository;
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
class UserProfileServiceTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @InjectMocks
    private UserProfileService userProfileService;

    @Test
    void create_conUserIdNuevo_persisteYRetornaPerfil() {
        // ARRANGE
        UserProfileRequest request = new UserProfileRequest();
        request.setUserId(1L);
        request.setDisplayName("persona_123");

        when(userProfileRepository.existsByUserId(1L)).thenReturn(false);

        UserProfile guardado = new UserProfile();
        guardado.setId(1L);
        guardado.setUserId(1L);
        guardado.setDisplayName("persona_123");
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(guardado);

        // ACT
        UserProfileResponse response = userProfileService.create(request);

        // ASSERT
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getDisplayName()).isEqualTo("persona_123");
    }

    @Test
    void create_conUserIdExistente_lanzaProfileAlreadyExistsException() {
        // ARRANGE: ya existe un perfil para ese userId.
        UserProfileRequest request = new UserProfileRequest();
        request.setUserId(1L);
        request.setDisplayName("persona_123");

        when(userProfileRepository.existsByUserId(1L)).thenReturn(true);

        // ACT + ASSERT
        ProfileAlreadyExistsException ex = assertThrows(
                ProfileAlreadyExistsException.class,
                () -> userProfileService.create(request)
        );
        assertThat(ex.getMessage()).isEqualTo("Ya existe un perfil para el userId: 1");

        // VERIFY: al detectarse el duplicado, nunca se debió
        // persistir un segundo perfil.
        verify(userProfileRepository, never()).save(any());
    }


// CASOS HIPOTÉTICOS DE FALLA (para QA):
// Test 1 - Se esperaba: response.getDisplayName() == "persona_123",
// mapeado correctamente desde la entidad guardada
// Se obtuvo: el campo no se mapea correctamente, mismo riesgo ya
// documentado en toda Quandrix sobre el metodo privado toResponse()
//
// Test 2 - Se esperaba: ProfileAlreadyExistsException cuando ya
// existe un perfil para ese userId (reforzado también por
// @Column(unique = true) a nivel de base de datos)
// Se obtuvo: se intenta crear un segundo perfil para el mismo
// usuario, lo que en un entorno real fallaría con una excepción de
// violación de constraint en vez de un error de negocio claro.

    @Test
    void getByUserId_conUserIdExistente_retornaPerfil() {
        // ARRANGE
        Long userId = 1L;
        UserProfile profile = new UserProfile();
        profile.setId(1L);
        profile.setUserId(userId);
        profile.setDisplayName("persona_123");

        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        // ACT
        UserProfileResponse response = userProfileService.getByUserId(userId);

        // ASSERT
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getDisplayName()).isEqualTo("persona_123");
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: response.getDisplayName() refleja exactamente el
// nombre real del perfil almacenado
// Se obtuvo: response.getDisplayName() == null
// Esto podría pasar si alguien modifica el metodo privado toResponse()
// y olvida mapear el campo displayName.


    @Test
    void getByUserId_conUserIdInexistente_lanzaUserNotFoundException() {
        // ARRANGE
        Long userIdInexistente = 999L;
        when(userProfileRepository.findByUserId(userIdInexistente))
                .thenReturn(Optional.empty());

        // ACT + ASSERT
        UserNotFoundException ex = assertThrows(
                UserNotFoundException.class,
                () -> userProfileService.getByUserId(userIdInexistente)
        );
        assertThat(ex.getMessage()).isEqualTo("No se encontró perfil para el userId: 999");
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: UserNotFoundException cuando el repositorio no
// encuentra el perfil (Optional vacío)
// Se obtuvo: NoSuchElementException sin capturar (HTTP 500 en vez
// de 404), mismo patrón de riesgo ya documentado en Quandrix
// al reemplazar .orElseThrow(...) por un .get() directo.


    @Test
    void getAll_conPerfilesExistentes_retornaListaCompleta() {
        // ARRANGE
        UserProfile profile1 = new UserProfile();
        profile1.setId(1L);
        profile1.setUserId(1L);
        profile1.setDisplayName("persona_123");

        UserProfile profile2 = new UserProfile();
        profile2.setId(2L);
        profile2.setUserId(2L);
        profile2.setDisplayName("otro_usuario");

        when(userProfileRepository.findAll()).thenReturn(List.of(profile1, profile2));

        // ACT
        List<UserProfileResponse> response = userProfileService.getAll();

        // ASSERT
        assertThat(response).hasSize(2);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: getAll() retorna los perfiles registrados,
// sin ningún filtro
// Se obtuvo: la lista viene incompleta o filtrada por error


    @Test
    void update_conUserIdExistente_actualizaYRetornaPerfil() {
        // ARRANGE
        Long userId = 1L;
        UserProfile profileExistente = new UserProfile();
        profileExistente.setId(1L);
        profileExistente.setUserId(userId);
        profileExistente.setDisplayName("nombre_viejo");

        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profileExistente));
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(profileExistente);

        UserProfileRequest request = new UserProfileRequest();
        request.setUserId(userId);
        request.setDisplayName("nombre_nuevo");

        // ACT
        UserProfileResponse response = userProfileService.update(userId, request);

        // ASSERT: confirmamos que el objeto en memoria fue actualizado
        // antes de guardarse.
        assertThat(profileExistente.getDisplayName()).isEqualTo("nombre_nuevo");
        assertThat(response.getDisplayName()).isEqualTo("nombre_nuevo");

        // VERIFY
        verify(userProfileRepository, times(1)).save(profileExistente);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: profileExistente.getDisplayName() == "nombre_nuevo"
// después de la actualización
// Se obtuvo: el perfil conserva su displayName ORIGINAL
// Esto podría pasar si alguien olvida la línea
// profile.setDisplayName(request.getDisplayName()) antes de
// repository.save(profile) en UserProfileService.update().


    @Test
    void update_conUserIdInexistente_lanzaUserNotFoundException() {
        // ARRANGE
        Long userIdInexistente = 999L;
        when(userProfileRepository.findByUserId(userIdInexistente))
                .thenReturn(Optional.empty());

        UserProfileRequest request = new UserProfileRequest();
        request.setUserId(userIdInexistente);
        request.setDisplayName("nombre_nuevo");

        // ACT + ASSERT
        assertThrows(
                UserNotFoundException.class,
                () -> userProfileService.update(userIdInexistente, request)
        );

        // VERIFY
        verify(userProfileRepository, never()).save(any());
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: UserNotFoundException al actualizar un userId
// inexistente, sin invocar save() en absoluto
// Se obtuvo: NoSuchElementException sin capturar (HTTP 500 en vez
// de 404)


    @Test
    void delete_conUserIdExistente_eliminaElPerfil() {
        // ARRANGE
        Long userId = 1L;
        UserProfile profile = new UserProfile();
        profile.setId(1L);
        profile.setUserId(userId);

        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        // ACT
        userProfileService.delete(userId);

        // VERIFY: confirmamos que se llamó a delete() con el objeto
        // real obtenido de findByUserId(...), no con un deleteById
        // directo sin validar existencia.
        verify(userProfileRepository, times(1)).delete(profile);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: userProfileRepository.delete(profile) se invoca con
// el objeto real obtenido de findByUserId(...), confirmando primero
// que existe
// Se obtuvo: deleteById(id) se invoca directamente sin esa
// validación previa — mismo riesgo ya documentado repetidamente en
// otros microservicios.


    @Test
    void delete_conUserIdInexistente_lanzaUserNotFoundException() {
        // ARRANGE
        Long userIdInexistente = 999L;
        when(userProfileRepository.findByUserId(userIdInexistente))
                .thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThrows(
                UserNotFoundException.class,
                () -> userProfileService.delete(userIdInexistente)
        );

        // VERIFY
        verify(userProfileRepository, never()).delete(any());
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: UserNotFoundException al eliminar un userId
// inexistente, sin invocar delete() en absoluto
// Se obtuvo: NoSuchElementException sin capturar (HTTP 500 en vez
// de 404)
}