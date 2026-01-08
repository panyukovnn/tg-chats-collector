package ru.panyukovnn.tgchatscollector.exception;

/**
 * Бизнес-исключение для обработки ожидаемых ошибок
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String errorCode, String message) {
        super(String.format("[%s] %s", errorCode, message));
    }
}
