package pt.ulusofona.cd.project.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import pt.ulusofona.cd.project.event.MessageEnvelope;
import pt.ulusofona.cd.project.event.ReservationCreatedEvent;
import pt.ulusofona.cd.project.event.ReservationConfirmedEvent;
import pt.ulusofona.cd.project.event.ReservationCancelledEvent;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Bean
    public ProducerFactory<String, MessageEnvelope<ReservationCreatedEvent>> createdEventProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092");
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, MessageEnvelope<ReservationCreatedEvent>> createdEventTemplate() {
        return new KafkaTemplate<>(createdEventProducerFactory());
    }

    @Bean
    public ProducerFactory<String, MessageEnvelope<ReservationConfirmedEvent>> confirmedEventProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092");
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, MessageEnvelope<ReservationConfirmedEvent>> confirmedEventTemplate() {
        return new KafkaTemplate<>(confirmedEventProducerFactory());
    }

    @Bean
    public ProducerFactory<String, MessageEnvelope<ReservationCancelledEvent>> cancelledEventProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092");
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, MessageEnvelope<ReservationCancelledEvent>> cancelledEventTemplate() {
        return new KafkaTemplate<>(cancelledEventProducerFactory());
    }
}
