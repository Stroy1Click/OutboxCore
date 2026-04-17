package ru.stroy1click.outbox.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.RetriableException;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.stroy1click.outbox.dto.OutboxEventDto;
import ru.stroy1click.outbox.service.OutboxEventService;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventHandler {

    private final OutboxEventService outboxEventService;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 2000)
    public void handle(){
        log.debug("handle");
        List<OutboxEventDto> outboxEvents = this.outboxEventService.getCreatedAndRetryableEvents();

        if(!outboxEvents.isEmpty()){
            log.debug("handle");

            for(OutboxEventDto outboxEvent : outboxEvents) {
                try {
                    Class<?> clazz = Class.forName(outboxEvent.getClassType());
                    //treeToValue - превращает payload в конкретный объект
                    Object payload =
                            this.objectMapper.treeToValue(outboxEvent.getPayload(), clazz);

                    ProducerRecord<String, Object> producerRecord =
                            new ProducerRecord<>(outboxEvent.getTopic(),
                                    payload);
                    producerRecord.headers().add("messageId", outboxEvent.getId().toString().getBytes());


                    CompletableFuture<SendResult<String, Object>> future
                            = this.kafkaTemplate.send(producerRecord);

                    future.whenComplete((result, exception) -> handleFutureSendResult(outboxEvent.getId(), exception));
                } catch (KafkaException e) {
                    // Проверяем, можно ли повторить
                    log.warn("event id {} marked as RETRYABLE due to KafkaException", outboxEvent.getId(), e);
                    this.outboxEventService.setRetryStatus(outboxEvent.getId(), e.getMessage());
                } catch (Exception e){
                    log.warn("event id {} marked as FAILED", outboxEvent.getId(), e);
                    this.outboxEventService.setFailedStatus(outboxEvent.getId(), e.getMessage());
                }
            }
        }
    }

    @Scheduled(fixedDelay = 3_600_000) // 1 час
    public void handleProcessingEvents(){
        log.debug("handleProcessingEvents");

        this.outboxEventService.resetProcessingToRetryableStatus();
    }

    private void handleFutureSendResult(Long eventId, Throwable e){
        if(e != null){
            Throwable actualException = (e instanceof CompletionException || e instanceof ExecutionException)
                    ? e.getCause()
                    : e;

            if(actualException instanceof RetriableException){
                log.warn("event id {} marked as RETRYABLE", eventId, actualException);
                this.outboxEventService.setRetryStatus(eventId, actualException.getMessage());
            } else {
                log.warn("event id {} marked as FAILED", eventId, actualException);
                this.outboxEventService.setFailedStatus(eventId, actualException.getMessage());
            }
        } else {
            log.debug("event id {} successfully sent", eventId);
            this.outboxEventService.setSucceededStatus(eventId);
        }
    }
}
