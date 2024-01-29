// package com.noyes.jogakbo.global.exception;

// import java.util.Date;

// import org.springframework.http.HttpStatus;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.ExceptionHandler;
// import org.springframework.web.bind.annotation.RestControllerAdvice;
// import org.springframework.web.context.request.WebRequest;
// import
// org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

// import lombok.extern.slf4j.Slf4j;

// @Slf4j
// @RestControllerAdvice
// public class CustomizedResponseEntityExceptionHandler extends
// ResponseEntityExceptionHandler {

// @ExceptionHandler(InvalidLoginTokenException.class)
// public final ResponseEntity<?>
// handleInvalidLoginTokenException(InvalidLoginTokenException ex, WebRequest
// request) {

// ExceptionResponse exceptionResponse = new ExceptionResponse(new Date(),
// ex.getMessage(),
// request.getDescription(false));

// log.error(ex.getStackTrace().toString());

// return new ResponseEntity<>(exceptionResponse, HttpStatus.UNAUTHORIZED);
// }

// // 모든 예외를 처리하는 메소드
// // Bean 내에서 발생하는 예외를 처리
// @ExceptionHandler(Exception.class)
// public final ResponseEntity<?> handleAllExceptions(Exception ex, WebRequest
// request) {

// ExceptionResponse exceptionResponse = new ExceptionResponse(new Date(),
// ex.toString(),
// request.getDescription(false));

// log.error(ex.getStackTrace().toString());

// return new ResponseEntity<>(exceptionResponse,
// HttpStatus.INTERNAL_SERVER_ERROR);
// }

// }
