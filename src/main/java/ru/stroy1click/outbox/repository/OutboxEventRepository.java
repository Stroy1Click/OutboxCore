package ru.stroy1click.outbox.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.stroy1click.outbox.entity.OutboxEvent;

import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Query(value = """
                    SELECT * FROM outbox.outbox_events
                    WHERE status IN ('CREATED', 'RETRYABLE')
                    LIMIT 100
                    FOR UPDATE SKIP LOCKED
                    """,
            nativeQuery = true)
    List<OutboxEvent> findTop100WithCreatedAndRetryableStatus();

    @Query(value = """
                    SELECT * FROM outbox.outbox_events
                    WHERE status = 'PROCESSING'
                                        AND updated_at < NOW()- INTERVAL '10 minutes'
                    LIMIT 100
                    FOR UPDATE SKIP LOCKED
                    """,
            nativeQuery = true)
    List<OutboxEvent> findTop100WithProcessingStatus();
}
