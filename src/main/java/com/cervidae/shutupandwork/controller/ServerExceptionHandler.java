package com.cervidae.shutupandwork.controller;

import com.cervidae.shutupandwork.util.Response;
import lombok.SneakyThrows;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.lang.reflect.Method;

/**
 * @author AaronDu
 */
@RestControllerAdvice
public class ServerExceptionHandler implements AsyncUncaughtExceptionHandler {

    /* AOP exception handlers */

    /**
     * All Other Exceptions: return "Internal Error"
     * @param e Exception instance thrown
     * @return fail response
     */
    @ExceptionHandler(Exception.class)
    public Response<?> allExceptionHandler(Exception e) {
        return Response.fail("1002", e.getMessage());
    }

    /**
     * DataAccessException: thrown in case of database related errors
     * @param e Exception instance thrown
     * @return fail response
     */
    @ExceptionHandler(DataAccessException.class)
    public Response<?> dataAccessExceptionHandler(Exception e) {
        return Response.fail("1003", e.getMessage());
    }

    /**
     * NoHandlerFoundException: thrown in case of 404 errors
     * @param e Exception instance thrown
     * @return fail response
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public Response<?> noHandlerFoundExceptionHandler(Exception e) {
        return Response.fail("1404");
    }

    /**
     * IllegalArgumentException: thrown in case of illegal arguments provided by the request
     * @param e Exception instance thrown
     * @return fail response
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Response<?> invalidParameterHandler(Exception e) {
        return Response.fail(e.getMessage());
    }

    @Override
    public void handleUncaughtException(Throwable throwable, Method method, Object... objects) {
        if (throwable instanceof Exception) {
            if (throwable instanceof IllegalArgumentException) {
                invalidParameterHandler((Exception) throwable);
            } else {
                allExceptionHandler((Exception) throwable);
            }
        }
    }
}
