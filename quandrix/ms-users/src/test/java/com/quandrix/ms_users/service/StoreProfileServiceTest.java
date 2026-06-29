package com.quandrix.ms_users.service;

import com.quandrix.ms_users.dto.StoreProfileRequest;
import com.quandrix.ms_users.dto.StoreProfileResponse;
import com.quandrix.ms_users.exception.ProfileAlreadyExistsException;
import com.quandrix.ms_users.exception.UserNotFoundException;
import com.quandrix.ms_users.model.StoreProfile;
import com.quandrix.ms_users.repository.StoreProfileRepository;
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
class StoreProfileServiceTest {

    @Mock
    private StoreProfileRepository storeProfileRepository;

    @InjectMocks
    private StoreProfileService storeProfileService;

    @Test
    void create_conUserIdNuevo_persisteYRetornaPerfil() {
        // ARRANGE
        StoreProfileRequest request = new StoreProfileRequest();
        request.setUserId(1L);
        request.setStoreName("Tienda de cartas - MTG");
        request.setLocation("Santiago, Chile");
        request.setDescription("Se venden cartas de MTG");

        when(storeProfileRepository.existsByUserId(1L)).thenReturn(false);

        StoreProfile guardado = new StoreProfile();
        guardado.setId(1L);
        guardado.setUserId(1L);
        guardado.setStoreName("Tienda de cartas - MTG");
        when(storeProfileRepository.save(any(StoreProfile.class))).thenReturn(guardado);

        // ACT
        StoreProfileResponse response = storeProfileService.create(request);

        // ASSERT
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getStoreName()).isEqualTo("Tienda de cartas - MTG");
    }

    // CASO HIPOTÉTICO DE FALLA (para QA):
    // Se esperaba: response.getStoreName() == "Tienda de cartas - MTG",
    // mapeado correctamente desde la entidad guardada
    // Se obtuvo: el campo no se mapea correctamente, mismo riesgo ya
    // documentado en toda Quandrix sobre el metodo privado toResponse()

    @Test
    void create_conUserIdExistente_lanzaProfileAlreadyExistsException() {
        // ARRANGE: el usuario ya tiene un perfil de tienda creado.
        StoreProfileRequest request = new StoreProfileRequest();
        request.setUserId(1L);
        request.setStoreName("Tienda de cartas - MTG");

        when(storeProfileRepository.existsByUserId(1L)).thenReturn(true);

        // ACT + ASSERT
        ProfileAlreadyExistsException ex = assertThrows(
                ProfileAlreadyExistsException.class,
                () -> storeProfileService.create(request)
        );
        assertThat(ex.getMessage()).isEqualTo("Ya existe un perfil para el userId: 1");

        // VERIFY: al detectarse el duplicado, nunca se debió
        // persistir un segundo perfil de tienda.
        verify(storeProfileRepository, never()).save(any());
    }

    // CASO HIPOTÉTICO DE FALLA (para QA):
    // Se esperaba: ProfileAlreadyExistsException cuando ya existe
    // un perfil de tienda para ese userId (reforzado también por
    // @Column(unique = true) a nivel de base de datos)
    // Se obtuvo: se intenta crear un segundo perfil de tienda para
    // el mismo usuario, lo que en un entorno real fallaría con una
    // excepción de violación de constraint en vez de un error de
    // negocio claro.

    @Test
    void getByUserId_conUserIdExistente_retornaPerfil() {
        // ARRANGE
        Long userId = 1L;
        StoreProfile profile = new StoreProfile();
        profile.setId(1L);
        profile.setUserId(userId);
        profile.setStoreName("Tienda de cartas - MTG");

        when(storeProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        // ACT
        StoreProfileResponse response = storeProfileService.getByUserId(userId);

        // ASSERT
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getStoreName()).isEqualTo("Tienda de cartas - MTG");
    }

    // CASO HIPOTÉTICO DE FALLA (para QA):
    // Se esperaba: response.getStoreName() refleja exactamente el
    // nombre real del perfil de tienda almacenado
    // Se obtuvo: response.getStoreName() == null
    // Esto podría pasar si alguien modifica el metodo privado
    // toResponse() y olvida mapear el campo storeName.

    @Test
    void getByUserId_conUserIdInexistente_lanzaUserNotFoundException() {
        // ARRANGE
        Long userIdInexistente = 999L;
        when(storeProfileRepository.findByUserId(userIdInexistente))
                .thenReturn(Optional.empty());

        // ACT + ASSERT
        UserNotFoundException ex = assertThrows(
                UserNotFoundException.class,
                () -> storeProfileService.getByUserId(userIdInexistente)
        );
        assertThat(ex.getMessage()).isEqualTo("No se encontró perfil para el userId: 999");
    }

    // CASO HIPOTÉTICO DE FALLA (para QA):
    // Se esperaba: UserNotFoundException cuando el repositorio no
    // encuentra el perfil de tienda (Optional vacío)
    // Se obtuvo: NoSuchElementException sin capturar (HTTP 500 en
    // vez de 404), mismo patrón de riesgo ya documentado en
    // Quandrix al reemplazar .orElseThrow(...) por un .get() directo.

    @Test
    void getAll_conPerfilesExistentes_retornaListaCompleta() {
        // ARRANGE
        StoreProfile store1 = new StoreProfile();
        store1.setId(1L);
        store1.setUserId(1L);
        store1.setStoreName("Tienda A");

        StoreProfile store2 = new StoreProfile();
        store2.setId(2L);
        store2.setUserId(2L);
        store2.setStoreName("Tienda B");

        when(storeProfileRepository.findAll()).thenReturn(List.of(store1, store2));

        // ACT
        List<StoreProfileResponse> response = storeProfileService.getAll();

        // ASSERT
        assertThat(response).hasSize(2);
    }

    // CASO HIPOTÉTICO DE FALLA (para QA):
    // Se esperaba: getAll() retorna TODOS los perfiles de tienda
    // registrados, sin ningún filtro
    // Se obtuvo: la lista viene incompleta o filtrada por error

    @Test
    void update_conUserIdExistente_actualizaYRetornaPerfil() {
        // ARRANGE
        Long userId = 1L;
        StoreProfile profileExistente = new StoreProfile();
        profileExistente.setId(1L);
        profileExistente.setUserId(userId);
        profileExistente.setStoreName("Nombre viejo");

        when(storeProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profileExistente));
        when(storeProfileRepository.save(any(StoreProfile.class))).thenReturn(profileExistente);

        StoreProfileRequest request = new StoreProfileRequest();
        request.setUserId(userId);
        request.setStoreName("Nombre nuevo");

        // ACT
        StoreProfileResponse response = storeProfileService.update(userId, request);

        // ASSERT: confirmamos que el objeto en memoria fue
        // actualizado antes de guardarse.
        assertThat(profileExistente.getStoreName()).isEqualTo("Nombre nuevo");
        assertThat(response.getStoreName()).isEqualTo("Nombre nuevo");

        // VERIFY
        verify(storeProfileRepository, times(1)).save(profileExistente);
    }

    // CASO HIPOTÉTICO DE FALLA (para QA):
    // Se esperaba: profileExistente.getStoreName() == "Nombre nuevo"
    // después de la actualización
    // Se obtuvo: el perfil conserva su storeName ORIGINAL
    // Esto podría pasar si alguien olvida la línea
    // profile.setStoreName(request.getStoreName()) antes de
    // repository.save(profile) en StoreProfileService.update().

    @Test
    void update_conUserIdInexistente_lanzaUserNotFoundException() {
        // ARRANGE
        Long userIdInexistente = 999L;
        when(storeProfileRepository.findByUserId(userIdInexistente))
                .thenReturn(Optional.empty());

        StoreProfileRequest request = new StoreProfileRequest();
        request.setUserId(userIdInexistente);
        request.setStoreName("Nombre nuevo");

        // ACT + ASSERT
        assertThrows(
                UserNotFoundException.class,
                () -> storeProfileService.update(userIdInexistente, request)
        );

        // VERIFY
        verify(storeProfileRepository, never()).save(any());
    }

    // CASO HIPOTÉTICO DE FALLA (para QA):
    // Se esperaba: UserNotFoundException al actualizar un userId
    // inexistente, sin invocar save() en absoluto
    // Se obtuvo: NoSuchElementException sin capturar (HTTP 500 en
    // vez de 404)

    @Test
    void delete_conUserIdExistente_eliminaElPerfil() {
        // ARRANGE
        Long userId = 1L;
        StoreProfile profile = new StoreProfile();
        profile.setId(1L);
        profile.setUserId(userId);

        when(storeProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        // ACT
        storeProfileService.delete(userId);

        // VERIFY: confirmamos que se llamó a delete() con el objeto
        // real obtenido de findByUserId(...), no con un deleteById
        // directo sin validar existencia.
        verify(storeProfileRepository, times(1)).delete(profile);
    }

    // CASO HIPOTÉTICO DE FALLA (para QA):
    // Se esperaba: storeProfileRepository.delete(profile) se invoca
    // con el objeto real obtenido de findByUserId(...), confirmando
    // primero que existe
    // Se obtuvo: deleteById(id) se invoca directamente sin esa
    // validación previa — mismo riesgo ya documentado repetidamente
    // en otros microservicios.

    @Test
    void delete_conUserIdInexistente_lanzaUserNotFoundException() {
        // ARRANGE
        Long userIdInexistente = 999L;
        when(storeProfileRepository.findByUserId(userIdInexistente))
                .thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThrows(
                UserNotFoundException.class,
                () -> storeProfileService.delete(userIdInexistente)
        );

        // VERIFY
        verify(storeProfileRepository, never()).delete(any());
    }

    // CASO HIPOTÉTICO DE FALLA (para QA):
    // Se esperaba: UserNotFoundException al eliminar un userId
    // inexistente, sin invocar delete() en absoluto
    // Se obtuvo: NoSuchElementException sin capturar (HTTP 500 en
    // vez de 404)
}