package ru.stroy1click.outbox.service;


import ru.stroy1click.outbox.dto.OutboxEventDto;

import java.util.List;

public interface OutboxEventService {

    void save(String topic, Object entity);

    List<OutboxEventDto> getCreatedAndRetryableEvents();

    void resetProcessingToRetryableStatus();

    void setSucceededStatus(Long id);

    void setFailedStatus(Long id, String errorMessage);

    void setRetryStatus(Long id, String errorMessage);
}
