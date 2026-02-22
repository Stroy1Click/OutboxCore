package ru.stroy1click.outbox.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.stroy1click.outbox.entity.MessageStatus;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OutboxEventDto {

    private Long id;

    private String topic;

    private String classType;

    private JsonNode payload;

    private LocalDateTime createdAt;

    private LocalDateTime sendAt;

    private MessageStatus status;

    private String errorMessage;

    private int retryAttempts;
}
