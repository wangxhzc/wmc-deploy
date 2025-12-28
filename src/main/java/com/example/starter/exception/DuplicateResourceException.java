package com.example.starter.exception;

/**
 * 资源重复异常
 * 当尝试创建已存在的资源时抛出
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
