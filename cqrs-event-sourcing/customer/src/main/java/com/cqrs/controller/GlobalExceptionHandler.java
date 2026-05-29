package com.cqrs.controller;

import com.cqrs.domain.exception.ConcurrencyException;
import com.cqrs.domain.exception.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomain(DomainException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        problem.setDetail(ex.getMessage());
        return problem;
    }

    @ExceptionHandler(ConcurrencyException.class)
    public ProblemDetail handleConcurrency(ConcurrencyException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problem.setDetail(ex.getMessage());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setDetail(ex.getBindingResult().getAllErrors().get(0).getDefaultMessage());
        return problem;
    }
}
