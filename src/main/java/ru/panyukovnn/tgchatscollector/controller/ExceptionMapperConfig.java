package ru.panyukovnn.tgchatscollector.controller;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import ru.panyukovnn.tgchatscollector.dto.response.CommonResponse;
import ru.panyukovnn.tgchatscollector.dto.response.CommonResponseError;
import ru.panyukovnn.tgchatscollector.dto.response.CommonResponseFieldValidation;
import ru.panyukovnn.tgchatscollector.exception.BusinessException;

import java.util.List;

/**
 * Обработчик исключений для REST-контроллеров
 */
@Slf4j
public class ExceptionMapperConfig {

    private static final String VALIDATION_DEFAULT_MESSAGE = "Ошибка валидации входящего запроса";
    private static final String FATAL_DEFAULT_MESSAGE = "Что-то пошло не так, обратитесь к администратору";

    @ServerExceptionMapper
    public Response handleBusinessException(BusinessException e) {
        log.warn("Бизнес-исключение. location: '{}'. code: '{}'. Сообщение: '{}'",
                e.getLocation(), e.getCode(), e.getDisplayMessage(), e);

        CommonResponseError responseError = CommonResponseError.builder()
                .location(e.getLocation())
                .code(e.getCode())
                .message(e.getDisplayMessage())
                .build();

        CommonResponse<Void> response = CommonResponse.<Void>builder()
                .error(responseError)
                .build();

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(response)
                .build();
    }

    @ServerExceptionMapper
    public Response handleConstraintViolationException(ConstraintViolationException e) {
        List<CommonResponseFieldValidation> fieldValidations = e.getConstraintViolations().stream()
                .map(this::toFieldValidation)
                .toList();

        log.warn("Ошибка валидации: {}", fieldValidations, e);

        CommonResponseError responseError = CommonResponseError.builder()
                .code("validation")
                .message(VALIDATION_DEFAULT_MESSAGE)
                .validations(fieldValidations)
                .build();

        CommonResponse<Void> response = CommonResponse.<Void>builder()
                .error(responseError)
                .build();

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(response)
                .build();
    }

    @ServerExceptionMapper
    public Response handleException(Exception e) {
        log.error("Непредвиденное критическое исключение: {}", e.getMessage(), e);

        CommonResponseError responseError = CommonResponseError.builder()
                .code("fatal")
                .message(FATAL_DEFAULT_MESSAGE)
                .build();

        CommonResponse<Void> response = CommonResponse.<Void>builder()
                .error(responseError)
                .build();

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(response)
                .build();
    }

    private CommonResponseFieldValidation toFieldValidation(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        int dotIndex = path.lastIndexOf('.');

        if (dotIndex >= 0) {
            path = path.substring(dotIndex + 1);
        }

        return CommonResponseFieldValidation.builder()
                .path(path)
                .message(violation.getMessage())
                .build();
    }
}
