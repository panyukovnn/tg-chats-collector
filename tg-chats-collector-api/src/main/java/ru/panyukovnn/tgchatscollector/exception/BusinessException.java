package ru.panyukovnn.tgchatscollector.exception;

import lombok.Getter;

/**
 * Бизнес-исключение для обработки ожидаемых ошибок
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * Идентификатор места возникновения ошибки
     */
    private String location;
    /**
     * Код ошибки
     */
    private String code = "business";
    /**
     * Текст ошибки, отображаемый в ответе
     */
    private String displayMessage;

    public BusinessException(String location, String displayMessage) {
        this.location = location;
        this.displayMessage = displayMessage;
    }

    public BusinessException(String location, String displayMessage, Throwable cause) {
        super(cause);
        this.location = location;
        this.displayMessage = displayMessage;
    }

    public BusinessException(String location, String code, String displayMessage) {
        this.location = location;
        this.code = code;
        this.displayMessage = displayMessage;
    }

    public BusinessException(String location, String code, String displayMessage, Throwable cause) {
        super(cause);
        this.location = location;
        this.code = code;
        this.displayMessage = displayMessage;
    }
}