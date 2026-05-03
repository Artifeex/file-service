package ru.sandr.fileservice.consumer.events;

public record FileDeletedEvent(
        String fileId
) implements FileEvent {
}
