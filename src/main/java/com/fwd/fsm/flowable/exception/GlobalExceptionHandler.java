package com.fwd.fsm.flowable.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import com.fwd.fsm.flowable.common.Response;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public Response error(HttpServletRequest request, Exception e){
        log.error(e.getMessage(), e);
        return Response.fail(e.getMessage(), null);
    }
}