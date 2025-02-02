package com.semifinished.core.exception;

/**
 * 由代码写得不严谨产生的异常，如一些工具类的入参异常
 */
public class CodeException extends ProjectRuntimeException {
    public CodeException(String message) {
        super(message);
    }

    public CodeException(String message, Object... args) {
        super(String.format(message, args));
    }
}
