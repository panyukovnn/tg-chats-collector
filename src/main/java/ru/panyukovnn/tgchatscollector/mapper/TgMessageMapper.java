package ru.panyukovnn.tgchatscollector.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.panyukovnn.tgchatscollector.dto.TgMessageDto;
import ru.panyukovnn.tgchatscollector.model.TgMessage;

@Mapper(componentModel = "spring")
public interface TgMessageMapper {

    @Mapping(source = "externalId", target = "messageId")
    @Mapping(source = "content", target = "text")
    TgMessageDto toDto(TgMessage tgMessage);
}
