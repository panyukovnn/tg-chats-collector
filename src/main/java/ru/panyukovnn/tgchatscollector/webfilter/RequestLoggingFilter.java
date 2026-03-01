package ru.panyukovnn.tgchatscollector.webfilter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

/**
 * Фильтр для логирования входящих HTTP-запросов и исходящих ответов
 */
@Slf4j
@Provider
public class RequestLoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String START_TIME_PROPERTY = "requestStartTime";
    private static final Set<String> METHODS_WITH_BODY = Set.of("POST", "PUT", "PATCH");

    @Override
    public void filter(ContainerRequestContext requestContext) {
        requestContext.setProperty(START_TIME_PROPERTY, System.currentTimeMillis());

        String requestBody = readRequestBody(requestContext);

        log.info("Входящий запрос: {} {} {}{}",
                requestContext.getMethod(),
                requestContext.getUriInfo().getPath(),
                formatQueryParams(requestContext),
                requestBody.isEmpty() ? "" : ", тело: " + requestBody);
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        long startTime = (long) requestContext.getProperty(START_TIME_PROPERTY);
        long durationMs = System.currentTimeMillis() - startTime;
        Object entity = responseContext.getEntity();

        log.info("Исходящий ответ: {} {} - статус: {}, время: {} мс{}",
                requestContext.getMethod(),
                requestContext.getUriInfo().getPath(),
                responseContext.getStatus(),
                durationMs,
                entity != null ? ", тело: " + entity : "");
    }

    private String readRequestBody(ContainerRequestContext requestContext) {
        if (!METHODS_WITH_BODY.contains(requestContext.getMethod())) {
            return "";
        }

        try {
            byte[] body = requestContext.getEntityStream().readAllBytes();
            requestContext.setEntityStream(new ByteArrayInputStream(body));

            return new String(body, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("Не удалось прочитать тело запроса", e);

            return "";
        }
    }

    private String formatQueryParams(ContainerRequestContext requestContext) {
        var queryParameters = requestContext.getUriInfo().getQueryParameters();

        if (queryParameters.isEmpty()) {
            return "";
        }

        return ", параметры: " + queryParameters;
    }
}