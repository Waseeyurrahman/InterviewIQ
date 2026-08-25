package com.interviewiq.interviewstarter.exception;

import com.interviewiq.interviewstarter.dto.AuthDtos;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<AuthDtos.ApiResponse> handleNotFound(ResourceNotFoundException ex){
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new AuthDtos.ApiResponse(false,ex.getMessage()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<AuthDtos.ApiResponse> handleForbidden(ForbiddenException ex){
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new AuthDtos.ApiResponse(false,ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AuthDtos.ApiResponse> handleGeneralException(Exception ex) {

        ex.printStackTrace();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new AuthDtos.ApiResponse(false, ex.getMessage()));
    }
}
