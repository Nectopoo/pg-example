package test.dev.demo.business.application.service;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.stereotype.Service;
import test.dev.streaming.proto.ChangeRequest;
import test.dev.streaming.proto.ChangeResponse;
import test.dev.streaming.proto.PublishServiceGrpc;

@Service
public class GrpcService {

    public ChangeResponse callGrpcService(ChangeRequest changeRequest) {
        /*
            Установка соединения c сервером
        */
        final ManagedChannel channel = ManagedChannelBuilder.forTarget("localhost:8080")
            .usePlaintext()
            .build();
        /*
            Создание блокирующего клиента
        */
        final PublishServiceGrpc.PublishServiceBlockingStub stub = PublishServiceGrpc.newBlockingStub(channel);
        /*
            Отправка вектора изменений
        */
        ChangeResponse response = stub.publish(changeRequest);

        channel.shutdownNow();
        return response;
    }
}
