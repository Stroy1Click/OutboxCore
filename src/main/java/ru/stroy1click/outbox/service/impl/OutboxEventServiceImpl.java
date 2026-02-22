package ru.stroy1click.outbox.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.stroy1click.common.exception.NotFoundException;
import ru.stroy1click.outbox.dto.OutboxEventDto;
import ru.stroy1click.outbox.entity.MessageStatus;
import ru.stroy1click.outbox.entity.OutboxEvent;
import ru.stroy1click.outbox.mapper.OutboxEventMapper;
import ru.stroy1click.outbox.repository.OutboxEventRepository;
import ru.stroy1click.outbox.service.OutboxEventService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class OutboxEventServiceImpl implements OutboxEventService {

    private final OutboxEventRepository outboxEventRepository;

    private final ObjectMapper objectMapper;

    private final OutboxEventMapper outboxEventMapper;

    @Override
    public void save(String topic, Object entity) {
        log.info("save {}, {}", topic, entity);

        try {
            this.outboxEventRepository.save(
                    OutboxEvent.builder()
                            .classType(entity.getClass().getName())
                            .payload(this.objectMapper.writeValueAsString(entity))
                            .status(MessageStatus.CREATED)
                            .retryAttempts(3)
                            .topic(topic)
                            .build()
            );
        } catch (JsonProcessingException e) {
            log.error("json processing error ", e);
            throw new IllegalStateException("Outbox serialization failed", e);
        }
    }

    @Override
    public List<OutboxEventDto> getCreatedAndRetryableEvents() {
        log.info("getCreatedAndRetryableEvents");

        List<OutboxEvent> outboxEvents = this.outboxEventRepository.findTop100WithCreatedAndRetryableStatus();

        outboxEvents.forEach(outboxEvent ->
                outboxEvent.setStatus(MessageStatus.PROCESSING));

        return this.outboxEventMapper.toDto(outboxEvents);
    }

    @Override
    public void resetProcessingToRetryableStatus() {
        log.info("resetProcessingToRetryableStatus");

        this.outboxEventRepository.findTop100WithProcessingStatus()
                .forEach(outboxEvent -> outboxEvent.setStatus(MessageStatus.RETRYABLE));
    }

    @Override
    public void setSucceededStatus(Long id) {
        log.info("setSucceededStatus {}", id);

        OutboxEvent outboxEvent = this.outboxEventRepository.findById(id).orElseThrow(
                () -> new NotFoundException("OutboxEvent not found with id {}" + id));

        outboxEvent.setStatus(MessageStatus.SUCCEEDED);
        outboxEvent.setSendAt(LocalDateTime.now());
    }

    @Override
    public void setFailedStatus(Long id, String errorMessage) {
        log.info("setFailedStatus {}, {}", id, errorMessage);
        OutboxEvent outboxEvent = this.outboxEventRepository.findById(id).orElseThrow(
                () -> new NotFoundException("OutboxEvent not found with id {}" + id));

        outboxEvent.setStatus(MessageStatus.FAILED);
        outboxEvent.setErrorMessage(errorMessage);
    }

    @Override
    public void setRetryStatus(Long id, String errorMessage) {
        log.info("setRetryStatus {}, {}", id, errorMessage);

        OutboxEvent outboxEvent = this.outboxEventRepository.findById(id).orElseThrow(
                () -> new NotFoundException("OutboxEvent not found with id {}" + id));

        outboxEvent.setErrorMessage(errorMessage);

        if(outboxEvent.getRetryAttempts() <= 0){
            outboxEvent.setStatus(MessageStatus.FAILED);
        } else {
            outboxEvent.setStatus(MessageStatus.RETRYABLE);
            outboxEvent.setRetryAttempts(outboxEvent.getRetryAttempts() - 1);
        }
    }
}
