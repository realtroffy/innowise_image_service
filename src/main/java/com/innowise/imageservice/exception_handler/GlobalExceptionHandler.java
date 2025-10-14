package com.innowise.imageservice.exception_handler;

import com.innowise.imageservice.dto.ErrorResponse;
import com.innowise.imageservice.exception.CommentNotFoundException;
import com.innowise.imageservice.exception.ImageFileOperationException;
import com.innowise.imageservice.exception.ImageFileRequiredException;
import com.innowise.imageservice.exception.ImageNotFoundException;
import com.innowise.imageservice.exception.InvalidImageSizeException;
import com.innowise.imageservice.exception.InvalidImageTypeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MultipartException;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        int index = exception.getMessage().lastIndexOf("[");
        int index2 = exception.getMessage().lastIndexOf("]");
        String message = exception.getMessage().substring(index+1, index2-1);
        log.error(message);
        return new ResponseEntity<>(message, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Map<String, String>> handleMultipartException(MultipartException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Wrong request format");
        errorResponse.put("message", "Request form is 'multipart/form-data'");
        log.error("Wrong request form: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGlobalException(Exception ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Unhandled error");
        errorResponse.put("message", "Please contact us if you see this error");
        errorResponse.put("details", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.error(e.getMessage());
        return ResponseEntity.status(BAD_REQUEST).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(ImageFileRequiredException.class)
    public ResponseEntity<?> handleImageFileRequiredException(ImageFileRequiredException e) {
        log.error(e.getMessage());
        return ResponseEntity.status(BAD_REQUEST).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(InvalidImageSizeException.class)
    public ResponseEntity<?> handleInvalidImageSizeException(InvalidImageSizeException e) {
        log.error(e.getMessage());
        return ResponseEntity.status(BAD_REQUEST).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(InvalidImageTypeException.class)
    public ResponseEntity<?> handleInvalidImageTypeException(InvalidImageTypeException e) {
        log.error(e.getMessage());
        return ResponseEntity.status(BAD_REQUEST).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(ImageFileOperationException.class)
    public ResponseEntity<?> handleImageFileOperationException(ImageFileOperationException e) {
        log.error(e.getMessage(), e.getCause());
        return ResponseEntity.status(BAD_REQUEST).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(ImageNotFoundException.class)
    public ResponseEntity<?> handleImageNotFoundException(ImageNotFoundException e) {
        log.error(e.getMessage());
        return ResponseEntity.status(NOT_FOUND).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(CommentNotFoundException.class)
    public ResponseEntity<?> handleCommentNotFoundException(CommentNotFoundException e) {
        log.error(e.getMessage());
        return ResponseEntity.status(NOT_FOUND).body(new ErrorResponse(e.getMessage()));
    }
}
