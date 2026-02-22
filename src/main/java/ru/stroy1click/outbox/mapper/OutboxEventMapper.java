
package ru.stroy1click.outbox.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import ru.stroy1click.common.mapper.Mappable;
import ru.stroy1click.outbox.dto.OutboxEventDto;
import ru.stroy1click.outbox.entity.OutboxEvent;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OutboxEventMapper implements Mappable<OutboxEvent, OutboxEventDto> {

    private final ModelMapper modelMapper;

    private final ObjectMapper objectMapper;

    @Override
    public OutboxEvent toEntity(OutboxEventDto outboxMessageDto) {
        return this.modelMapper.map(outboxMessageDto, OutboxEvent.class);
    }

    @Override
    @SneakyThrows
    public OutboxEventDto toDto(OutboxEvent outboxMessage) {
        JsonNode payloadNode = this.objectMapper.readTree(outboxMessage.getPayload());
        OutboxEventDto dto = this.modelMapper.map(outboxMessage, OutboxEventDto.class);

        dto.setPayload(payloadNode);

        return dto;
    }

    @Override
    public List<OutboxEventDto> toDto(List<OutboxEvent> e) {
        return e.stream()
                .map(this::toDto)
                .toList();
    }
}
