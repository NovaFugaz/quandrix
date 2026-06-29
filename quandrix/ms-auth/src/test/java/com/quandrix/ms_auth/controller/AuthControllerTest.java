package com.quandrix.ms_auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quandrix.ms_auth.dto.RegisterRequest;
import com.quandrix.ms_auth.dto.LoginRequest;
import com.quandrix.ms_auth.dto.LoginResponse;
import com.quandrix.ms_auth.dto.TokenValidationResponse;
import com.quandrix.ms_auth.service.AuthService;
import com.quandrix.ms_auth.exception.UserAlreadyExistsException;
import com.quandrix.ms_auth.exception.InvalidCredentialsException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void register_conDatosValidos_retorna201Created() throws Exception {
        // ARRANGE: preparamos el request body y el comportamiento del mock.
        // authService.register(...) es void, así que con doNothing() basta
        // para simular que el registro fue exitoso (no lanza excepción).
        RegisterRequest request = new RegisterRequest();
        request.setEmail("quandrix@gmail.com");
        request.setPassword("Quandrix123");
        request.setRole("PERSONA");

        doNothing().when(authService).register(any(RegisterRequest.class));

        // ACT: ejecutamos el endpoint POST /auth/register con MockMvc.
        // ASSERT: verificamos que la respuesta sea 201 Created.
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // VERIFY: confirmamos que el controller efectivamente llamó
        // al service con los datos esperados, exactamente una vez.
        verify(authService, times(1)).register(any(RegisterRequest.class));
    }

    // CASO HIPOTÉTICO DE FALLA (para QA):
    // Se esperaba: HTTP 201 Created
    // Se obtuvo: HTTP 200 OK
    // Esto podría pasar si alguien cambia por error
    // "ResponseEntity.status(HttpStatus.CREATED).build()" por
    // "ResponseEntity.ok().build()" en AuthController.

    @Test
    void register_conEmailExistente_retorna409Conflict() throws Exception {
        // ARRANGE: preparamos el request y simulamos que el service detecta
        // que el email ya existe, lanzando la excepción de negocio real.
        RegisterRequest request = new RegisterRequest();
        request.setEmail("quandrix@gmail.com");
        request.setPassword("Quandrix123");
        request.setRole("PERSONA");

        doThrow(new UserAlreadyExistsException(request.getEmail()))
                .when(authService).register(any(RegisterRequest.class));

        // ACT: ejecutamos el endpoint POST /auth/register con MockMvc.
        // ASSERT: verificamos que la respuesta sea 409 Conflict y que el
        // body contenga el campo "error" con el mensaje esperado.
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(
                        "Ya existe un usuario registrado con el email: " + request.getEmail()));

        // VERIFY: confirmamos que el controller efectivamente intentó
        // delegar el registro al service, exactamente una vez.
        verify(authService, times(1)).register(any(RegisterRequest.class));
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 409 Conflict
// Se obtuvo: HTTP 500 Internal Server Error
// Esto podría pasar si alguien elimina por error el @ExceptionHandler
// de UserAlreadyExistsException en GlobalExceptionHandler, dejando que
// la excepción caiga en el handler genérico de Exception.class.

    @Test
    void login_conCredencialesValidas_retorna200ConToken() throws Exception {
        // ARRANGE: preparamos el request y la respuesta simulada que
        // debería devolver el service si las credenciales son correctas.
        LoginRequest request = new LoginRequest();
        request.setEmail("quandrix@gmail.com");
        request.setPassword("Quandrix123");

        LoginResponse fakeResponse = new LoginResponse(
                "eyJhbGciOiJIUzI1NiJ9.fake.token",
                "PERSONA",
                "quandrix@gmail.com"
        );

        when(authService.login(request.getEmail(), request.getPassword()))
                .thenReturn(fakeResponse);

        // ACT: ejecutamos el endpoint POST /auth/login con MockMvc.
        // ASSERT: verificamos 200 OK y que el body tenga el token, rol y email esperados.
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("eyJhbGciOiJIUzI1NiJ9.fake.token"))
                .andExpect(jsonPath("$.role").value("PERSONA"))
                .andExpect(jsonPath("$.email").value("quandrix@gmail.com"));

        // VERIFY: confirmamos que el controller llamó al service con
        // exactamente el email y password recibidos en el request.
        verify(authService, times(1)).login(request.getEmail(), request.getPassword());
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 200 OK con token JWT en el body
// Se obtuvo: HTTP 200 OK con "token": null
// Esto podría pasar si JwtService.generateToken(...) retorna null
// silenciosamente en vez de lanzar una excepción ante un error de firma,
// y AuthService no valida ese resultado antes de construir el LoginResponse.

    @Test
    void login_conCredencialesInvalidas_retorna401Unauthorized() throws Exception {
        // ARRANGE: preparamos el request y simulamos que el service detecta
        // credenciales inválidas (email inexistente o password incorrecto).
        LoginRequest request = new LoginRequest();
        request.setEmail("quandrix@gmail.com");
        request.setPassword("ContraseñaIncorrecta");

        when(authService.login(request.getEmail(), request.getPassword()))
                .thenThrow(new InvalidCredentialsException());

        // ACT: ejecutamos el endpoint POST /auth/login con MockMvc.
        // ASSERT: verificamos 401 Unauthorized y el mensaje de error esperado.
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Email o contraseña incorrectos"));

        // VERIFY: confirmamos que el controller llamó al service con
        // exactamente el email y password recibidos en el request.
        verify(authService, times(1)).login(request.getEmail(), request.getPassword());
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 401 Unauthorized
// Se obtuvo: HTTP 500 Internal Server Error
// Esto podría pasar si alguien elimina por error el @ExceptionHandler
// de InvalidCredentialsException en GlobalExceptionHandler, dejando que
// la excepción caiga en el handler genérico de Exception.class en su lugar.

    @Test
    void validate_conTokenValido_retorna200ConDatosDelToken() throws Exception {
        // ARRANGE: preparamos el token con prefijo "Bearer " (como llega
        // realmente en el header HTTP) y la respuesta simulada del service.
        // Importante: el mock se configura con el token YA SIN el prefijo,
        // porque el controller lo recorta antes de llamar al service.
        String tokenSinPrefijo = "eyJhbGciOiJIUzI1NiJ9.fake.token";
        String headerConPrefijo = "Bearer " + tokenSinPrefijo;

        TokenValidationResponse fakeResponse = new TokenValidationResponse(
                "quandrix@gmail.com", "PERSONA", true
        );

        when(authService.validateToken(tokenSinPrefijo)).thenReturn(fakeResponse);

        // ACT: ejecutamos el endpoint GET /auth/validate con MockMvc,
        // enviando el header Authorization con el prefijo Bearer incluido.
        // ASSERT: verificamos 200 OK y los datos esperados en el body.
        mockMvc.perform(get("/auth/validate")
                        .header("Authorization", headerConPrefijo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.email").value("quandrix@gmail.com"))
                .andExpect(jsonPath("$.role").value("PERSONA"));

        // VERIFY: confirmamos que el controller le pasó al service el token
        // YA SIN el prefijo "Bearer ", es decir, que el recorte funcionó.
        verify(authService, times(1)).validateToken(tokenSinPrefijo);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 200 OK con "valid": true
// Se obtuvo: HTTP 200 OK con "valid": false, a pesar de enviar un token
// realmente válido.
// Esto podría pasar si alguien cambia el prefijo esperado de "Bearer "
// a "bearer " (minúscula) en AuthController, ya que authHeader.startsWith(...)
// es sensible a mayúsculas/minúsculas — el recorte fallaría silenciosamente
// y el service recibiría el token con el prefijo incluido, invalidándolo.

    @Test
    void validate_conTokenInvalido_retorna200ConValidFalse() throws Exception {
        // ARRANGE: preparamos un token cualquiera y simulamos que el service
        // determina que NO es válido (sin lanzar excepción, solo retornando
        // un resultado negativo, tal como hace JwtService.isTokenValid()).
        String tokenSinPrefijo = "token.invalido.o.expirado";
        String headerConPrefijo = "Bearer " + tokenSinPrefijo;

        TokenValidationResponse fakeResponse = new TokenValidationResponse(null, null, false);

        when(authService.validateToken(tokenSinPrefijo)).thenReturn(fakeResponse);

        // ACT: ejecutamos el endpoint GET /auth/validate con MockMvc.
        // ASSERT: verificamos que sigue siendo 200 OK, pero con valid=false
        // y los campos email/role en null.
        mockMvc.perform(get("/auth/validate")
                        .header("Authorization", headerConPrefijo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.role").doesNotExist());

        // VERIFY: confirmamos que el controller le pasó al service el token
        // sin el prefijo, igual que en el caso de token válido.
        verify(authService, times(1)).validateToken(tokenSinPrefijo);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: HTTP 200 OK con "valid": false
// Se obtuvo: HTTP 500 Internal Server Error
// Esto podría pasar si JwtService.isTokenValid(...) lanza una excepción
// no controlada (por ejemplo, una ExpiredJwtException de la librería JJWT)
// en vez de devolver simplemente "false", y nadie la captura antes de
// que llegue al handler genérico de Exception.class.
}