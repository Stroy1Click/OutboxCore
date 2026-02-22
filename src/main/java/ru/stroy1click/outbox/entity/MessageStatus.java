package ru.stroy1click.outbox.entity;

public enum MessageStatus {

    CREATED,
    SUCCEEDED,
    RETRYABLE,
    PROCESSING,
    FAILED
}
