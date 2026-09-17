package ru.nsu.marketplace.controller;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import ru.nsu.marketplace.dto.error.ApiErrorResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.nsu.marketplace.dto.error.FieldErrorResponse;
import ru.nsu.marketplace.exceptions.*;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler({EmailAlreadyExistException.class, NumberAlreadyExistException.class})
    public ResponseEntity<ApiErrorResponse> handleUserAlreadyExists(RuntimeException exception) {
        ApiErrorResponse response = new ApiErrorResponse(
                HttpStatus.CONFLICT.value(),
                exception.getMessage(),
                List.of(),
                Instant.now()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        List<FieldErrorResponse> errors = exception.getFieldErrors()
                .stream()
                .map(fieldError -> new FieldErrorResponse(
                        fieldError.getField(),
                        fieldError.getDefaultMessage()
                ))
                .toList();
        ApiErrorResponse errorResponse = new ApiErrorResponse(
                400,
                "Validation failed",
                errors,
                Instant.now()
        );

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(ImageNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleImageUrlValidation(ImageNotFoundException exception){
        ApiErrorResponse response = new ApiErrorResponse(
                400,
                exception.getMessage(),
                List.of(),
                Instant.now()
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleProductNotFound(ProductNotFoundException exception){
        ApiErrorResponse response = new ApiErrorResponse(
                404,
                exception.getMessage(),
                List.of(),
                Instant.now()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleUserNotFound(UserNotFoundException exception) {
        ApiErrorResponse response = new ApiErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                exception.getMessage(),
                List.of(),
                Instant.now()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidLogin(InvalidCredentialsException exception) {
        ApiErrorResponse response = new ApiErrorResponse(
                401,
                "Неверные данные для входа",
                List.of(),
                Instant.now()
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(InvalidPhoneNumberException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidPhoneNumber(
            InvalidPhoneNumberException exception
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                exception.getMessage(),
                List.of(),
                Instant.now()
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(IllegalArgumentException exception) {
        ApiErrorResponse response = new ApiErrorResponse(
                400,
                exception.getMessage(),
                List.of(),
                Instant.now()
        );

        return ResponseEntity.badRequest().body(response);
    }
}
