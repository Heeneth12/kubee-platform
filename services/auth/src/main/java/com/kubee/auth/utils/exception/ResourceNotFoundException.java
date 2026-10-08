package com.kubee.auth.utils.exception;

import com.kubee.common.CommonException;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends CommonException{

    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause, HttpStatus.NOT_FOUND);
    }
}
