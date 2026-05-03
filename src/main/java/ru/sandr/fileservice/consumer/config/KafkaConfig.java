package ru.sandr.fileservice.consumer.config;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.mapping.DefaultJackson2JavaTypeMapper;
import org.springframework.kafka.support.mapping.Jackson2JavaTypeMapper;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.util.backoff.FixedBackOff;
import ru.sandr.fileservice.consumer.events.FileDeletedEvent;
import ru.sandr.fileservice.consumer.events.FileLoadedEvent;

import java.util.HashMap;
import java.util.Map;

@Configuration
@AllArgsConstructor
@EnableKafka
public class KafkaConfig {

    private FileConsumerConfig fileConsumerConfig;

    @Bean
    public ConsumerFactory<String, Object> consumerFactory(
            KafkaProperties kafkaProperties
    ) {
        Map<String, Object> props = kafkaProperties.buildConsumerProperties(null);
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        // Тип определяется по хедеру
        typeMapper.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.TYPE_ID);
        // Название хедера
        typeMapper.setClassIdFieldName(fileConsumerConfig.getTypeHeaderName());
        typeMapper.addTrustedPackages("*");
        Map<String, Class<?>> mappings = new HashMap<>();
        mappings.put(fileConsumerConfig.getFileDeletedEventName(), FileDeletedEvent.class);
        mappings.put(fileConsumerConfig.getFileLoadedEventName(), FileLoadedEvent.class);

        typeMapper.setIdClassMapping(mappings);

        JsonDeserializer<Object> delegateDeserializer = new JsonDeserializer<>(Object.class);
        delegateDeserializer.addTrustedPackages("*");
        delegateDeserializer.dontRemoveTypeHeaders();
        delegateDeserializer.setUseTypeHeaders(true);
        delegateDeserializer.setTypeMapper(typeMapper);

        ErrorHandlingDeserializer<Object> errorHandlingValueDeserializer =
                new ErrorHandlingDeserializer<>(delegateDeserializer);

        ErrorHandlingDeserializer<String> errorHandlingKeyDeserializer =
                new ErrorHandlingDeserializer<>(new StringDeserializer());

        return new DefaultKafkaConsumerFactory<>(props, errorHandlingKeyDeserializer, errorHandlingValueDeserializer);
    }

    @Bean
    public CommonErrorHandler commonErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer deadLetterPublishingRecoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);

        FixedBackOff fixedBackOff = new FixedBackOff(
                fileConsumerConfig.getDelayBetweenRetries(),
                fileConsumerConfig.getMaxAttempts()
        );

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(deadLetterPublishingRecoverer, fixedBackOff);

        errorHandler.addNotRetryableExceptions(DeserializationException.class);

        return errorHandler;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            CommonErrorHandler commonErrorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(commonErrorHandler);
        return factory;
    }
}
