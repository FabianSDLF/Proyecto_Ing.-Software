package com.ubb.practicas.exception;

import com.ubb.practicas.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Centralized REST API Exception Handler.
 * Intercepts all controller and service exceptions and produces RFC 7807 compliant ApiErrorResponse.
 * Resolves BK-26, BK-27, and provides uniform error translation across the platform.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ==========================================
    // 404 NOT FOUND
    // ==========================================
    @ExceptionHandler({
            ResourceNotFoundException.class,
            UserNotFoundException.class,
            RoleNotFoundException.class,
            StorageFileNotFoundException.class,
            NoSuchElementException.class
    })
    public ResponseEntity<ApiErrorResponse> handleNotFound(Exception ex, HttpServletRequest request) {
        log.warn("Recurso no encontrado [{}]: {}", request.getRequestURI(), ex.getMessage());
        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage() != null ? ex.getMessage() : "El recurso solicitado no fue encontrado.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // ==========================================
    // 400 BAD REQUEST & VALIDATION
    // ==========================================
    @ExceptionHandler({
            BadRequestException.class,
            IllegalArgumentException.class,
            FileProcessingException.class,
            UnsupportedFileFormatException.class
    })
    public ResponseEntity<ApiErrorResponse> handleBadRequest(Exception ex, HttpServletRequest request) {
        log.warn("Solicitud inválida [{}]: {}", request.getRequestURI(), ex.getMessage());
        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage() != null ? ex.getMessage() : "Datos de solicitud inválidos.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        String summary = fieldErrors.isEmpty()
                ? "Error de validación en la entrada."
                : fieldErrors.values().iterator().next();

        log.warn("Error de validación de campos [{}]: {}", request.getRequestURI(), fieldErrors);
        ApiErrorResponse response = ApiErrorResponse.ofValidation(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                summary,
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Cuerpo de solicitud ilegible [{}]: {}", request.getRequestURI(), ex.getMessage());
        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "El cuerpo de la solicitud no tiene un formato JSON válido o contiene valores incompatibles.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParams(MissingServletRequestParameterException ex, HttpServletRequest request) {
        log.warn("Parámetro requerido ausente [{}]: {}", request.getRequestURI(), ex.getParameterName());
        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "El parámetro requerido '" + ex.getParameterName() + "' no fue proporcionado.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxSizeException(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        log.warn("Tamaño de archivo excedido [{}]: {}", request.getRequestURI(), ex.getMessage());
        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "El archivo excede el tamaño máximo permitido para subida.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // ==========================================
    // 401 UNAUTHORIZED
    // ==========================================
    @ExceptionHandler({
            UnauthorizedException.class,
            BadCredentialsException.class,
            SecurityException.class
    })
    public ResponseEntity<ApiErrorResponse> handleUnauthorized(Exception ex, HttpServletRequest request) {
        log.warn("Acceso no autenticado [{}]: {}", request.getRequestURI(), ex.getMessage());
        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                ex.getMessage() != null ? ex.getMessage() : "Credenciales inválidas o sesión no autorizada.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    // ==========================================
    // 403 FORBIDDEN
    // ==========================================
    @ExceptionHandler({
            AccessDeniedException.class,
            ForbiddenException.class,
            DisabledException.class
    })
    public ResponseEntity<ApiErrorResponse> handleForbidden(Exception ex, HttpServletRequest request) {
        log.warn("Acceso prohibido [{}]: {}", request.getRequestURI(), ex.getMessage());
        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                ex.getMessage() != null ? ex.getMessage() : "No tiene permisos para acceder a este recurso.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    // ==========================================
    // 409 CONFLICT
    // ==========================================
    @ExceptionHandler({
            ResourceConflictException.class,
            EmailAlreadyInUseException.class,
            IllegalStateException.class,
            DataIntegrityViolationException.class
    })
    public ResponseEntity<ApiErrorResponse> handleConflict(Exception ex, HttpServletRequest request) {
        log.warn("Conflicto de estado o integridad [{}]: {}", request.getRequestURI(), ex.getMessage());
        String message = ex.getMessage();
        if (ex instanceof DataIntegrityViolationException) {
            message = "Conflicto de integridad de datos en la base de datos.";
        }
        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                message != null ? message : "Conflicto en la operación solicitada.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    // ==========================================
    // 500 INTERNAL SERVER ERROR
    // ==========================================
    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ApiErrorResponse> handleStorageException(StorageException ex, HttpServletRequest request) {
        log.error("Falla en servicio de almacenamiento [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);
        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "Error en el almacenamiento de archivos: " + ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Excepción no controlada en servidor [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);
        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "Ha ocurrido un error interno inesperado en el servidor.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
