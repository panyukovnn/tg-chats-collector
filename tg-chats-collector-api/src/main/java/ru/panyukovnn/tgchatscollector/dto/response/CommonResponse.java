package ru.panyukovnn.tgchatscollector.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Общая обертка ответа
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Общая обертка ответа")
public class CommonResponse<T> {

    @Builder.Default
    @Schema(description = "Уникальный идентификатор ответа")
    private UUID id = UUID.randomUUID();

    @Builder.Default
    @Schema(description = "Время ответа (в UTC)")
    private OffsetDateTime timestamp = OffsetDateTime.now(ZoneOffset.UTC);

    @Schema(description = "Данные ответа")
    private T data;

    @Schema(description = "Информация об ошибке")
    private CommonResponseError error;
}
