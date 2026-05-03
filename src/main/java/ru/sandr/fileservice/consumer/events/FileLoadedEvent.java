package ru.sandr.fileservice.consumer.events;

public record FileLoadedEvent(
        String fileId
) implements FileEvent {
}
