package ru.sandr.fileservice.consumer.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "file-consumer")
@Getter
@NoArgsConstructor
@Setter
@Configuration
public class FileConsumerConfig {
    private String fileEventTopicName;
    private String typeHeaderName;
    private String fileDeletedEventName;
    private String fileLoadedEventName;
    private Long delayBetweenRetries;
    private Integer maxAttempts;
}
