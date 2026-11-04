package ru.nsu.marketplace.common.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.nsu.marketplace.auth.InvalidCredentialsException;
import ru.nsu.marketplace.cart.exception.CartItemNotFoundException;
import ru.nsu.marketplace.cart.exception.CartNotFoundException;
import ru.nsu.marketplace.common.phone.InvalidPhoneNumberException;
import ru.nsu.marketplace.media.ImageNotFoundException;
import ru.nsu.marketplace.order.exception.OrderNotFoundException;
import ru.nsu.marketplace.order.exception.OrderStateException;
import ru.nsu.marketplace.product.exception.ModerationRequestNotFoundException;
import ru.nsu.marketplace.product.exception.ProductModerationStateException;
import ru.nsu.marketplace.product.exception.ProductNotFoundException;
import ru.nsu.marketplace.product.exception.ProductStateException;
import ru.nsu.marketplace.review.exception.ProductReviewNotFoundException;
import ru.nsu.marketplace.review.exception.ProductReviewStateException;
import ru.nsu.marketplace.review.exception.ReviewReplyNotFoundException;
import ru.nsu.marketplace.review.reaction.exception.ReactionAlreadyExistsException;
import ru.nsu.marketplace.review.reaction.exception.ReactionNotFoundException;
import ru.nsu.marketplace.user.exception.EmailAlreadyExistsException;
import ru.nsu.marketplace.user.exception.TelephoneAlreadyExistsException;
import ru.nsu.marketplace.user.exception.UserNotFoundException;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidParameter(MethodArgumentTypeMismatchException exception) {
        ApiErrorResponse response = new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Invalid request parameter: " + exception.getName(),
                List.of(),
                Instant.now()
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableRequest(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Invalid JSON request body",
                List.of(),
                Instant.now()
        ));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleStaticResourceNotFound(NoResourceFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Resource not found",
                List.of(),
                Instant.now()
        ));
    }

    @ExceptionHandler({EmailAlreadyExistsException.class, TelephoneAlreadyExistsException.class})
    public ResponseEntity<ApiErrorResponse> handleDuplicateUserContact(RuntimeException exception) {
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
    public ResponseEntity<ApiErrorResponse> handleImageNotFound(ImageNotFoundException exception){
        ApiErrorResponse response = new ApiErrorResponse(
                404,
                exception.getMessage(),
                List.of(),
                Instant.now()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
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

    @ExceptionHandler({CartNotFoundException.class, CartItemNotFoundException.class,
            OrderNotFoundException.class, ModerationRequestNotFoundException.class, ProductReviewNotFoundException.class,
            ReactionNotFoundException.class, ReviewReplyNotFoundException.class})
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(RuntimeException exception) {
        ApiErrorResponse response = new ApiErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                exception.getMessage(),
                List.of(),
                Instant.now()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler({OrderStateException.class, ProductStateException.class, ProductModerationStateException.class,
            ProductReviewStateException.class, ReactionAlreadyExistsException.class})
    public ResponseEntity<ApiErrorResponse> handleStateConflict(RuntimeException exception) {
        ApiErrorResponse response = new ApiErrorResponse(
                HttpStatus.CONFLICT.value(),
                exception.getMessage(),
                List.of(),
                Instant.now()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(InvalidCredentialsException exception) {
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(Exception exception) throws Exception {
        if (exception instanceof AccessDeniedException) {
            throw exception;
        }

        if (exception instanceof ErrorResponse errorResponse && errorResponse.getStatusCode().is4xxClientError()) {
            ApiErrorResponse response = new ApiErrorResponse(
                    errorResponse.getStatusCode().value(),
                    errorResponse.getBody().getDetail(),
                    List.of(),
                    Instant.now()
            );

            return ResponseEntity.status(errorResponse.getStatusCode())
                    .headers(errorResponse.getHeaders())
                    .body(response);
        }

        log.error("Unexpected server error", exception);

        ApiErrorResponse response = new ApiErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal server error",
                List.of(),
                Instant.now()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
