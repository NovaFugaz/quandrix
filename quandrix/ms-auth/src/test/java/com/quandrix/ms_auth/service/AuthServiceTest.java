package com.quandrix.ms_auth.service;

import com.quandrix.ms_auth.dto.LoginResponse;
import com.quandrix.ms_auth.dto.RegisterRequest;
import com.quandrix.ms_auth.dto.TokenValidationResponse;
import com.quandrix.ms_auth.exception.InvalidCredentialsException;
import com.quandrix.ms_auth.model.Role;
import com.quandrix.ms_auth.model.User;
import com.quandrix.ms_auth.repository.UserRepository;
import com.quandrix.ms_auth.exception.UserAlreadyExistsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_conDatosValidos_guardaUsuarioConPasswordEncriptado() {
        // ARRANGE: preparamos el request y simulamos que el email NO existe
        // todavía, y que el encoder devuelve un hash fijo para la password.
        RegisterRequest request = new RegisterRequest();
        request.setEmail("quandrix@gmail.com");
        request.setPassword("Quandrix123");
        request.setRole("PERSONA");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("hash_falso_123");

        // ACT: ejecutamos el metodo real del service.
        authService.register(request);

        // ASSERT + VERIFY: capturamos el User que se intentó guardar
        // para revisar que sus datos sean exactamente los esperados.
        var userCaptor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());

        User userGuardado = userCaptor.getValue();
        assertThat(userGuardado.getEmail()).isEqualTo("quandrix@gmail.com");
        assertThat(userGuardado.getPasswordHash()).isEqualTo("hash_falso_123");
        assertThat(userGuardado.getRole()).isEqualTo(Role.PERSONA);

        // Verificamos también que se haya consultado existsByEmail antes de guardar.
        verify(userRepository, times(1)).existsByEmail(request.getEmail());
    }


// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: el User guardado tiene passwordHash = "hash_falso_123"
// Se obtuvo: el User guardado tiene passwordHash = "Quandrix123" (texto plano)
// Esto podría pasar si alguien modifica AuthService.register() y olvida
// llamar a passwordEncoder.encode(...), guardando la contraseña sin
// encriptar directamente desde el request — una falla de seguridad grave
// que QA debería reportar como bloqueante (severidad crítica), no como
// un bug funcional normal.

    @Test
    void register_conEmailExistente_lanzaUserAlreadyExistsException() {
        // ARRANGE: simulamos que el email YA existe en el repositorio.
        RegisterRequest request = new RegisterRequest();
        request.setEmail("quandrix@gmail.com");
        request.setPassword("Quandrix123");
        request.setRole("PERSONA");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        // ACT + ASSERT: ejecutamos register() y verificamos que lance
        // exactamente la excepción esperada, con el mensaje correcto.
        UserAlreadyExistsException ex = org.junit.jupiter.api.Assertions.assertThrows(
                UserAlreadyExistsException.class,
                () -> authService.register(request)
        );
        assertThat(ex.getMessage())
                .isEqualTo("Ya existe un usuario registrado con el email: " + request.getEmail());

        // VERIFY: el flujo debe detenerse ANTES de codificar la password
        // o guardar el usuario — ninguna de esas llamadas debió ocurrir.
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: passwordEncoder.encode(...) NUNCA se invoca cuando el
// email ya existe.
// Se obtuvo: passwordEncoder.encode(...) SÍ se invoca antes de la
// validación de email duplicado.
// Esto podría pasar si alguien reordena las líneas dentro de
// AuthService.register(), moviendo la codificación de la contraseña
// antes de la verificación de existsByEmail(). No causaría un error
// visible para el usuario final, pero sí un gasto de CPU innecesario
// (el hashing de bcrypt es costoso) en cada intento de registro duplicado,
// lo cual es un problema de rendimiento que QA debería reportar.

    @Test
    void register_conRolInvalido_lanzaInvalidCredentialsException() {
        // ARRANGE: el email no existe (pasa la primera validación),
        // pero el rol enviado no corresponde a ningún valor del enum Role.
        RegisterRequest request = new RegisterRequest();
        request.setEmail("quandrix@gmail.com");
        request.setPassword("Quandrix123");
        request.setRole("SUPERADMIN"); // no existe en el enum Role

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("hash_falso_123");

        // ACT + ASSERT: se debe lanzar InvalidCredentialsException
        // (no una IllegalArgumentException "cruda" del enum).
        org.junit.jupiter.api.Assertions.assertThrows(
                InvalidCredentialsException.class,
                () -> authService.register(request)
        );

        // VERIFY: el usuario nunca debe llegar a guardarse, porque el
        // parseo del rol falla ANTES de la llamada a save(...).
        verify(userRepository, never()).save(any());
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: InvalidCredentialsException al enviar un rol inexistente
// Se obtuvo: una IllegalArgumentException sin capturar, propagándose
// como HTTP 500 en vez de un error controlado.
// Esto podría pasar si alguien elimina por error el bloque try/catch
// alrededor de Role.valueOf(...) en AuthService.register(), dejando
// que la excepción nativa del enum suba sin traducirse a un error
// de negocio reconocible por el GlobalExceptionHandler.

    @Test
    void login_conCredencialesValidas_retornaLoginResponseConToken() {
        // ARRANGE: simulamos un usuario existente en el repositorio,
        // cuya contraseña coincide con la enviada, y un token generado fijo.
        String email = "quandrix@gmail.com";
        String password = "Quandrix123";

        User userExistente = new User();
        userExistente.setEmail(email);
        userExistente.setPasswordHash("hash_almacenado_en_bd");
        userExistente.setRole(Role.PERSONA);

        when(userRepository.findByEmail(email)).thenReturn(java.util.Optional.of(userExistente));
        when(passwordEncoder.matches(password, "hash_almacenado_en_bd")).thenReturn(true);
        when(jwtService.generateToken(email, "PERSONA")).thenReturn("token_jwt_falso");

        // ACT: ejecutamos el metodo real del service.
        LoginResponse response = authService.login(email, password);

        // ASSERT: verificamos que el LoginResponse tenga exactamente
        // los datos esperados.
        assertThat(response.getToken()).isEqualTo("token_jwt_falso");
        assertThat(response.getRole()).isEqualTo("PERSONA");
        assertThat(response.getEmail()).isEqualTo(email);

        // VERIFY: confirmamos que se consultó la password correcta
        // contra el hash correcto, y que el token se generó con los
        // datos correctos del usuario encontrado.
        verify(passwordEncoder, times(1)).matches(password, "hash_almacenado_en_bd");
        verify(jwtService, times(1)).generateToken(email, "PERSONA");
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: response.getRole() == "PERSONA"
// Se obtuvo: response.getRole() == null
// Esto podría pasar si alguien cambia user.getRole().name() por
// request.getRole() (un campo que no existe en este metodo) o si
// el mapeo del Role a String se rompe por un cambio en el enum,
// dejando el campo role del LoginResponse vacío sin que el login
// falle visiblemente — un bug silencioso que solo se nota al
// inspeccionar el contenido real de la respuesta.

    @Test
    void login_conEmailInexistente_lanzaInvalidCredentialsException() {
        // ARRANGE: simulamos que el repositorio NO encuentra ningún
        // usuario con ese email.
        String email = "noexiste@gmail.com";
        String password = "Quandrix123";

        when(userRepository.findByEmail(email)).thenReturn(java.util.Optional.empty());

        // ACT + ASSERT: se debe lanzar InvalidCredentialsException,
        // y no una NoSuchElementException "cruda" del Optional vacío.
        org.junit.jupiter.api.Assertions.assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(email, password)
        );

        // VERIFY: el flujo debe detenerse ANTES de intentar comparar
        // la contraseña o generar un token — ninguna de esas llamadas
        // debió ocurrir, ya que no hay un User sobre el cual operar.
        verify(passwordEncoder, never()).matches(any(), any());
        verify(jwtService, never()).generateToken(any(), any());
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: InvalidCredentialsException al enviar un email inexistente
// Se obtuvo: NoSuchElementException sin capturar, propagándose como
// HTTP 500 en vez de un error controlado de 401.
// Esto podría pasar si alguien reemplaza el .orElseThrow(() -> new
// InvalidCredentialsException()) por un simple .get() en AuthService.login(),
// dejando que el Optional vacío lance su excepción nativa de Java en
// vez de la excepción de negocio esperada por el GlobalExceptionHandler.

    @Test
    void login_conPasswordIncorrecta_lanzaInvalidCredentialsException() {
        // ARRANGE: el usuario SÍ existe, pero la contraseña enviada
        // no coincide con el hash almacenado.
        String email = "quandrix@gmail.com";
        String passwordIncorrecta = "PasswordEquivocada";

        User userExistente = new User();
        userExistente.setEmail(email);
        userExistente.setPasswordHash("hash_almacenado_en_bd");
        userExistente.setRole(Role.PERSONA);

        when(userRepository.findByEmail(email)).thenReturn(java.util.Optional.of(userExistente));
        when(passwordEncoder.matches(passwordIncorrecta, "hash_almacenado_en_bd")).thenReturn(false);

        // ACT + ASSERT: se debe lanzar InvalidCredentialsException
        // al detectar que la contraseña no coincide.
        org.junit.jupiter.api.Assertions.assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(email, passwordIncorrecta)
        );

        // VERIFY: se debió comparar la password (y fallar), pero nunca
        // se debió llegar a generar un token.
        verify(passwordEncoder, times(1)).matches(passwordIncorrecta, "hash_almacenado_en_bd");
        verify(jwtService, never()).generateToken(any(), any());
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: InvalidCredentialsException cuando la contraseña no coincide
// Se obtuvo: jwtService.generateToken(...) fue invocado a pesar de que
// passwordEncoder.matches(...) retornó false, generando un token válido
// para credenciales incorrectas.
// Esto podría pasar si alguien invierte por error la condición
// "if (!passwordEncoder.matches(...))" a "if (passwordEncoder.matches(...))"
// en AuthService.login(), una falla de seguridad crítica que permitiría
// iniciar sesión con cualquier contraseña.

    @Test
    void validateToken_conTokenValido_retornaResponseConValidTrue() {
        // ARRANGE: simulamos que el token es válido, y que JwtService
        // puede extraer el email y rol correctamente desde él.
        String token = "token_jwt_valido";

        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractEmail(token)).thenReturn("quandrix@gmail.com");
        when(jwtService.extractRole(token)).thenReturn("PERSONA");

        // ACT: ejecutamos el metodo real del service.
        TokenValidationResponse response = authService.validateToken(token);

        // ASSERT: verificamos que el response tenga los datos esperados
        // y que valid sea true.
        assertThat(response.isValid()).isTrue();
        assertThat(response.getEmail()).isEqualTo("quandrix@gmail.com");
        assertThat(response.getRole()).isEqualTo("PERSONA");

        // VERIFY: confirmamos que, al ser válido, SÍ se llamó a extraer
        // tanto el email como el rol del token.
        verify(jwtService, times(1)).extractEmail(token);
        verify(jwtService, times(1)).extractRole(token);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: response.isValid() == true cuando el token es válido
// Se obtuvo: response.isValid() == false a pesar de que
// jwtService.isTokenValid(token) retornó true.
// Esto podría pasar si alguien invierte por error la condición
// "if (!jwtService.isTokenValid(token))" a
// "if (jwtService.isTokenValid(token))" en AuthService.validateToken(),
// rechazando tokens válidos y aceptando tokens inválidos — un error
// que rompería completamente la autenticación del sistema.

    @Test
    void validateToken_conTokenInvalido_retornaResponseConValidFalse() {
        // ARRANGE: simulamos que el token NO es válido.
        String token = "token_jwt_invalido_o_expirado";

        when(jwtService.isTokenValid(token)).thenReturn(false);

        // ACT: ejecutamos el metodo real del service.
        TokenValidationResponse response = authService.validateToken(token);

        // ASSERT: verificamos que el response indique explícitamente
        // que el token no es válido, sin datos de usuario.
        assertThat(response.isValid()).isFalse();
        assertThat(response.getEmail()).isNull();
        assertThat(response.getRole()).isNull();

        // VERIFY: si el token es inválido, NUNCA se debe intentar
        // extraer claims de él — sería trabajo innecesario sobre un
        // token que ya sabemos que no es confiable.
        verify(jwtService, never()).extractEmail(any());
        verify(jwtService, never()).extractRole(any());
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: jwtService.extractEmail(...) NUNCA se invoca cuando
// el token es inválido.
// Se obtuvo: extractEmail(...) SÍ se invoca a pesar de que
// isTokenValid(token) retornó false.
// Esto podría pasar si alguien reordena el código en
// AuthService.validateToken() y mueve las llamadas a extractEmail()/
// extractRole() antes de la validación con isTokenValid(), lo cual
// podría lanzar una excepción no controlada (ej. JwtException) al
// intentar parsear un token mal formado o expirado, en vez de
// devolver limpiamente un "valid: false".
}