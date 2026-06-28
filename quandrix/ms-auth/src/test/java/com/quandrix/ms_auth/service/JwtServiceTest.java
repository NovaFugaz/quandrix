package com.quandrix.ms_auth.service;

import com.quandrix.ms_auth.config.JwtConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    // Secret de PRUEBA, exclusivo para este test — nunca usar el
    // secret real de producción aquí. Debe tener al menos 32 caracteres
    // para cumplir el largo mínimo que exige HMAC-SHA256 en JJWT.
    private static final String TEST_SECRET = "clave_secreta_de_prueba_para_jwt_test_123456";
    private static final long TEST_EXPIRATION = 86400000L; // 24 horas, en ms

    @BeforeEach
    void setUp() {
        // ARRANGE (común a todos los tests de esta clase): construimos
        // un JwtConfig real (no mockeado) con datos de prueba, y un
        // JwtService real que lo usa.
        JwtConfig testConfig = new JwtConfig();
        testConfig.setSecret(TEST_SECRET);
        testConfig.setExpiration(TEST_EXPIRATION);

        jwtService = new JwtService(testConfig);
    }

    @Test
    void generateToken_conDatosValidos_retornaTokenConFormatoJwt() {
        // ACT: ejecutamos el metodo real, sin mocks de por medio.
        String token = jwtService.generateToken("quandrix@gmail.com", "PERSONA");

        // ASSERT: verificamos que el token no sea nulo/vacío y que
        // tenga el formato estándar de JWT: header.payload.signature
        assertThat(token).isNotNull();
        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }


// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: un token con 3 segmentos separados por puntos (formato JWT estándar)
// Se obtuvo: una excepción en tiempo de ejecución, o un token con un
// formato distinto.
// Esto podría pasar si jwtConfig.getSecret() retorna un valor null o
// vacío (por ejemplo, si la variable de entorno JWT_SECRET no está
// configurada en el ambiente), ya que Keys.hmacShaKeyFor(...) lanzaría
// una excepción al recibir un arreglo de bytes vacío o demasiado corto
// para el algoritmo de firma — un problema de configuración de despliegue,
// no de lógica de negocio.

    @Test
    void isTokenValid_conTokenRecienGenerado_retornaTrue() {
        // ARRANGE: generamos un token real, firmado con el mismo secret
        // que usará isTokenValid(...) para verificarlo (ambos vienen del
        // mismo jwtService, configurado en el @BeforeEach).
        String token = jwtService.generateToken("quandrix@gmail.com", "PERSONA");

        // ACT: ejecutamos el metodo real de validación.
        boolean esValido = jwtService.isTokenValid(token);

        // ASSERT: un token recién creado, sin alterar y sin expirar,
        // debe ser reconocido como válido.
        assertThat(esValido).isTrue();
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: isTokenValid(token) == true para un token recién generado
// Se obtuvo: isTokenValid(token) == false
// Esto podría pasar si jwtConfig.getExpiration() retorna un valor
// negativo o igual a cero por un error de configuración, haciendo que
// el token se genere ya expirado (con fecha de expiración en el pasado
// o igual al momento de creación) — el token sería rechazado
// inmediatamente, incluso siendo recién emitido.

    @Test
    void isTokenValid_conTokenAlterado_retornaFalse() {
        // ARRANGE: generamos un token real y válido, pero luego lo
        // alteramos manualmente (cambiamos un carácter de la firma),
        // simulando un intento de manipulación.
        String tokenValido = jwtService.generateToken("quandrix@gmail.com", "PERSONA");
        String tokenAlterado = tokenValido.substring(0, tokenValido.length() - 1) + "X";

        // ACT: ejecutamos el metodo real de validación sobre el token alterado.
        boolean esValido = jwtService.isTokenValid(tokenAlterado);

        // ASSERT: la firma ya no coincide con el contenido, así que debe
        // ser rechazado como inválido, sin lanzar ninguna excepción hacia
        // afuera (el try/catch interno debe absorberla).
        assertThat(esValido).isFalse();
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: isTokenValid(tokenAlterado) == false, sin excepción visible
// Se obtuvo: una SignatureException sin capturar, propagándose hacia
// AuthService.validateToken() y de ahí hacia el controller, resultando
// en HTTP 500 en vez de una respuesta normal con "valid": false.
// Esto podría pasar si alguien reduce el catch de
// "JwtException | IllegalArgumentException" a un tipo más específico
// (por ejemplo, solo "ExpiredJwtException"), dejando que otras excepciones
// de la librería JJWT —como una firma inválida— se propaguen sin control,
// rompiendo el endpoint /auth/validate con tokens manipulados.

    @Test
    void extractEmail_deTokenValido_retornaEmailCorrecto() {
        // ARRANGE: generamos un token para un email específico conocido.
        String emailEsperado = "quandrix@gmail.com";
        String token = jwtService.generateToken(emailEsperado, "PERSONA");

        // ACT: ejecutamos el metodo real de extracción.
        String emailExtraido = jwtService.extractEmail(token);

        // ASSERT: el email recuperado debe ser exactamente el mismo
        // que se usó para generar el token originalmente.
        assertThat(emailExtraido).isEqualTo(emailEsperado);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: extractEmail(token) == "quandrix@gmail.com"
// Se obtuvo: extractEmail(token) == "PERSONA" (el valor del rol, no el email)
// Esto podría pasar si alguien invierte por error el orden de los
// argumentos en generateToken(String email, String role), llamando a
// .subject(role) en vez de .subject(email) — el campo "subject" del JWT
// terminaría conteniendo el rol, y extractEmail() (que lee getSubject())
// devolvería el rol en lugar del email, rompiendo silenciosamente la
// identificación del usuario en cada token emitido.

    @Test
    void extractRole_deTokenValido_retornaRolCorrecto() {
        // ARRANGE: generamos un token con un rol específico conocido.
        String rolEsperado = "ADMIN";
        String token = jwtService.generateToken("quandrix@gmail.com", rolEsperado);

        // ACT: ejecutamos el metodo real de extracción.
        String rolExtraido = jwtService.extractRole(token);

        // ASSERT: el rol recuperado debe ser exactamente el mismo
        // que se usó para generar el token originalmente.
        assertThat(rolExtraido).isEqualTo(rolEsperado);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: extractRole(token) == "ADMIN"
// Se obtuvo: extractRole(token) == null
// Esto podría pasar si alguien cambia el nombre del claim al generar
// el token (por ejemplo, de .claim("role", role) a .claim("rol", role),
// un error de tipeo en inglés vs español), mientras que extractRole()
// sigue buscando la clave "role" — el claim existiría en el token, pero
// bajo un nombre distinto al que el metodo de extracción espera,
// resultando en un rol nulo para todos los usuarios autenticados.
}