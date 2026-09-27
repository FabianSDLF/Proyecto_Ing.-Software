package com.ubb.practicas.exception;

import com.ubb.practicas.dto.ApiErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.lang.reflect.Method;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Adversarial Test 2: GlobalExceptionHandler & ApiErrorResponse")
public class GlobalExceptionHandlerAdversarialTest {

    private MockMvc mockMvc;
    private GlobalExceptionHandler exceptionHandler;

    @RestController
    @RequestMapping("/adversarial/errors")
    static class DummyController {

        @GetMapping("/not-found")
        public void throwNotFound() {
            throw new ResourceNotFoundException("Recurso ID 99 no existe");
        }

        @GetMapping("/bad-request")
        public void throwBadRequest() {
            throw new BadRequestException("Parámetro 'email' inválido");
        }

        @GetMapping("/conflict")
        public void throwConflict() {
            throw new ResourceConflictException("El usuario ya existe con ese correo");
        }

        @GetMapping("/unauthorized")
        public void throwUnauthorized() {
            throw new UnauthorizedException("Token JWT expirado");
        }

        @GetMapping("/forbidden")
        public void throwForbidden() {
            throw new ForbiddenException("Rol 'Estudiante' no autorizado");
        }

        @GetMapping("/generic-error")
        public void throwGeneric() {
            throw new RuntimeException("Error inesperado en BD");
        }
    }

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        mockMvc = MockMvcBuilders.standaloneSetup(new DummyController())
                .setControllerAdvice(exceptionHandler)
                .build();
    }

    @Test
    @DisplayName("ResourceNotFoundException returns HTTP 404 with structured ApiErrorResponse")
    void testResourceNotFoundProduces404() throws Exception {
        mockMvc.perform(get("/adversarial/errors/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", is("Recurso ID 99 no existe")))
                .andExpect(jsonPath("$.path", is("/adversarial/errors/not-found")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("BadRequestException returns HTTP 400 with structured ApiErrorResponse")
    void testBadRequestProduces400() throws Exception {
        mockMvc.perform(get("/adversarial/errors/bad-request"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", is("Parámetro 'email' inválido")))
                .andExpect(jsonPath("$.path", is("/adversarial/errors/bad-request")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("ResourceConflictException returns HTTP 409 with structured ApiErrorResponse")
    void testResourceConflictProduces409() throws Exception {
        mockMvc.perform(get("/adversarial/errors/conflict"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message", is("El usuario ya existe con ese correo")))
                .andExpect(jsonPath("$.path", is("/adversarial/errors/conflict")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("UnauthorizedException returns HTTP 401 with structured ApiErrorResponse")
    void testUnauthorizedProduces401() throws Exception {
        mockMvc.perform(get("/adversarial/errors/unauthorized"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")))
                .andExpect(jsonPath("$.message", is("Token JWT expirado")));
    }

    @Test
    @DisplayName("ForbiddenException returns HTTP 403 with structured ApiErrorResponse")
    void testForbiddenProduces403() throws Exception {
        mockMvc.perform(get("/adversarial/errors/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")))
                .andExpect(jsonPath("$.message", is("Rol 'Estudiante' no autorizado")));
    }

    @Test
    @DisplayName("Uncaught generic Exception returns HTTP 500 without leaking stack trace")
    void testGenericExceptionProduces500WithoutLeakingInternals() throws Exception {
        mockMvc.perform(get("/adversarial/errors/generic-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status", is(500)))
                .andExpect(jsonPath("$.error", is("Internal Server Error")))
                .andExpect(jsonPath("$.message", is("Ha ocurrido un error interno inesperado en el servidor.")))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    @DisplayName("handleValidationErrors constructs fieldErrors mapping and returns HTTP 400")
    void testValidationErrorsMapping() throws NoSuchMethodException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/usuarios");

        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "userDto");
        bindingResult.addError(new FieldError("userDto", "email", "El correo es obligatorio"));
        bindingResult.addError(new FieldError("userDto", "fullName", "El nombre es obligatorio"));

        Method method = DummyController.class.getDeclaredMethod("throwBadRequest");
        MethodParameter parameter = new MethodParameter(method, -1);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleValidationErrors(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Validation Failed", response.getBody().getError());
        assertNotNull(response.getBody().getValidationErrors());
        assertEquals("El correo es obligatorio", response.getBody().getValidationErrors().get("email"));
        assertEquals("El nombre es obligatorio", response.getBody().getValidationErrors().get("fullName"));
    }

    @Test
    @DisplayName("Direct exception handler invocation handles null messages and edge cases")
    void testDirectHandlerInvocationWithNullMessages() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/direct-test");

        // 404 with null message
        ResponseEntity<ApiErrorResponse> resp404 = exceptionHandler.handleNotFound(
                new ResourceNotFoundException(null), request);
        assertEquals(HttpStatus.NOT_FOUND, resp404.getStatusCode());
        assertNotNull(resp404.getBody());
        assertEquals("El recurso solicitado no fue encontrado.", resp404.getBody().getMessage());
        assertEquals("/api/direct-test", resp404.getBody().getPath());

        // 400 with null message
        ResponseEntity<ApiErrorResponse> resp400 = exceptionHandler.handleBadRequest(
                new BadRequestException(null), request);
        assertEquals(HttpStatus.BAD_REQUEST, resp400.getStatusCode());
        assertEquals("Datos de solicitud inválidos.", resp400.getBody().getMessage());

        // 409 with null message
        ResponseEntity<ApiErrorResponse> resp409 = exceptionHandler.handleConflict(
                new ResourceConflictException(null), request);
        assertEquals(HttpStatus.CONFLICT, resp409.getStatusCode());
        assertEquals("Conflicto en la operación solicitada.", resp409.getBody().getMessage());

        // DataIntegrityViolationException (foreign key or unique constraint violation) maps to 409
        ResponseEntity<ApiErrorResponse> respIntegrity = exceptionHandler.handleConflict(
                new DataIntegrityViolationException("Duplicate entry 'test@ubb.cl'"), request);
        assertEquals(HttpStatus.CONFLICT, respIntegrity.getStatusCode());
        assertEquals("Conflicto de integridad de datos en la base de datos.", respIntegrity.getBody().getMessage());

        // MissingServletRequestParameterException maps to 400
        ResponseEntity<ApiErrorResponse> respMissingParam = exceptionHandler.handleMissingParams(
                new MissingServletRequestParameterException("file", "MultipartFile"), request);
        assertEquals(HttpStatus.BAD_REQUEST, respMissingParam.getStatusCode());
        assertTrue(respMissingParam.getBody().getMessage().contains("file"));

        // MaxUploadSizeExceededException maps to 400
        ResponseEntity<ApiErrorResponse> respMaxSize = exceptionHandler.handleMaxSizeException(
                new MaxUploadSizeExceededException(20971520), request);
        assertEquals(HttpStatus.BAD_REQUEST, respMaxSize.getStatusCode());

        // StorageException maps to 500
        ResponseEntity<ApiErrorResponse> respStorage = exceptionHandler.handleStorageException(
                new StorageException("Disk full"), request);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, respStorage.getStatusCode());
        assertTrue(respStorage.getBody().getMessage().contains("Disk full"));
    }
}
