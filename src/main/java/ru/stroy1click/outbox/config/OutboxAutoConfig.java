package ru.stroy1click.outbox.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.modelmapper.ModelMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import ru.stroy1click.outbox.handler.OutboxEventHandler;
import ru.stroy1click.outbox.mapper.OutboxEventMapper;
import ru.stroy1click.outbox.repository.OutboxEventRepository;
import ru.stroy1click.outbox.service.OutboxEventService;
import ru.stroy1click.outbox.service.impl.OutboxEventServiceImpl;


@AutoConfiguration
@EnableScheduling
public class OutboxAutoConfig {

    @Bean
    public OutboxEventHandler outboxEventHandler(
            OutboxEventService service,
            KafkaTemplate<String, Object> kafka,
            ObjectMapper mapper) {
        return new OutboxEventHandler(service, kafka, mapper);
    }

    @Bean
    public OutboxEventService outboxEventService(OutboxEventRepository repository,
                                                 ObjectMapper objectMapper,
                                                 OutboxEventMapper mapper) {
        return new OutboxEventServiceImpl(repository, objectMapper, mapper);
    }

    @Bean
    public OutboxEventMapper outboxEventMapper(ModelMapper modelMapper, ObjectMapper objectMapper) {
        return new OutboxEventMapper(modelMapper, objectMapper);
    }
}