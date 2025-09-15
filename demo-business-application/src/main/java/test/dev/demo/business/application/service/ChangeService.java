package test.dev.demo.business.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import test.dev.smartreplication.client.kafka.producer.KafkaChangeHandler;
import test.dev.smartreplication.model.Change;

@Service
@RequiredArgsConstructor
public class ChangeService {

    private final KafkaChangeHandler kafkaChangeHandler;

    public void sendChange(Change change) {
        kafkaChangeHandler.handleChange(change);
    }
}
