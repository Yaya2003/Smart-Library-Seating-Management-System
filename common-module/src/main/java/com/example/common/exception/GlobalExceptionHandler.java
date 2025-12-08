package com.example.common.exception;

import com.example.common.result.ResultView;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.yaml.snakeyaml.constructor.DuplicateKeyException;

import java.sql.SQLIntegrityConstraintViolationException;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ResponseBody
    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler({
            DuplicateKeyException.class,
            SQLIntegrityConstraintViolationException.class
    })
    public ResultView<?> handleSQLIntegrityConstraintViolationException(HttpServletRequest request, Exception e) {
        log.error("DuplicateKeyException occurred while processing request: {}", request.getRequestURI(), e);
        return ResultView.error(HttpStatus.CONFLICT.value(), "资源已存在，请勿重复创建");
    }
}
