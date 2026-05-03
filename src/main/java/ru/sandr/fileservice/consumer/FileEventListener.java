package ru.sandr.fileservice.consumer;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import ru.sandr.fileservice.consumer.events.FileDeletedEvent;
import ru.sandr.fileservice.consumer.events.FileLoadedEvent;
import ru.sandr.fileservice.service.FileService;

import java.util.UUID;

@Service
@KafkaListener(topics = "${file-consumer.fileEventTopicName}", containerFactory = "kafkaListenerContainerFactory")
@RequiredArgsConstructor
public class FileEventListener {

    private final FileService fileService;

    @KafkaHandler
    public void handleFileLoadedEvent(FileLoadedEvent event) {
        // Нужен ретрай на случай, если файл еще не успел загрузиться в s3
        // Нужен ретрай на БД ?
        // Нужно отправлтяь сообщение в dlq и продолжать обработку, если fileId передан не в UUId формате
        var fileId = UUID.fromString(event.fileId());
        fileService.commitFile(fileId);
    }

    @KafkaHandler
    public void handleFileDeletedEvent(FileDeletedEvent event) {
        var fileId = UUID.fromString(event.fileId());
        fileService.deleteFile(fileId);
    }
}
