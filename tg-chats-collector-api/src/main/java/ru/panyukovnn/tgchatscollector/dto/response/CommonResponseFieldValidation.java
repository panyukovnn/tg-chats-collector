package ru.panyukovnn.tgchatscollector.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ошибка валидации поля
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Ошибка валидации поля")
public class CommonResponseFieldValidation {

    @Schema(description = "Путь до поля, не прошедшего валидацию")
    private String path;

    @Schema(description = "Отображаемый текст ошибки")
    private String message;
}
