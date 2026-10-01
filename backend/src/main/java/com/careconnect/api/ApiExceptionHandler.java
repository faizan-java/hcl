package com.careconnect.api;

import com.careconnect.service.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(ApiException.class)
  ResponseEntity<?> api(ApiException e, HttpServletRequest request) {
    return ResponseEntity.status(e.status()).body(error(e.getMessage(), request));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<?> validation(MethodArgumentNotValidException e, HttpServletRequest request) {
    Map<String, String> fields =
        e.getBindingResult().getFieldErrors().stream()
            .collect(
                Collectors.toMap(
                    error -> error.getField(),
                    error ->
                        error.getDefaultMessage() == null
                            ? "Invalid value."
                            : error.getDefaultMessage(),
                    (first, ignored) -> first,
                    LinkedHashMap::new));
    return ResponseEntity.badRequest()
        .body(Map.of("error", "Check the submitted details and try again.", "fields", fields));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<?> malformedBody(HttpMessageNotReadableException e, HttpServletRequest request) {
    return ResponseEntity.badRequest().body(error("The request body is malformed.", request));
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  ResponseEntity<?> missingParameter(
      MissingServletRequestParameterException e, HttpServletRequest request) {
    return ResponseEntity.badRequest()
        .body(error("A required request parameter is missing.", request));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<?> dataConflict(DataIntegrityViolationException e, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(error("The request conflicts with existing data.", request));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<?> other(Exception e, HttpServletRequest request) {
    log.error("Unhandled API error for {} {}", request.getMethod(), request.getRequestURI(), e);
    return ResponseEntity.status(500)
        .body(error("The server could not complete this request.", request));
  }

  private Map<String, String> error(String message, HttpServletRequest request) {
    return Map.of("error", message, "path", request.getRequestURI());
  }
}
