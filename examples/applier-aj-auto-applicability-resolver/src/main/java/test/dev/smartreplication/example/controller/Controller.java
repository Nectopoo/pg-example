package test.dev.smartreplication.example.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import test.dev.smartreplication.core.compress.JsonConverter;
import test.dev.smartreplication.model.Change;
import test.dev.smartreplication.model.ChangeItem;
import test.dev.smartreplication.model.ChangeOperation;
import test.dev.smartreplication.model.ChangeSource;
import test.dev.smartreplication.model.SchemaId;
import test.dev.smartreplication.model.TableId;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

@RestController
@AllArgsConstructor
public class Controller {

    public static final JsonConverter JSON_CONVERTER = new JsonConverter();
    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    @PostMapping("/user")
    public ResponseEntity<Void> postUser(@RequestParam Long id, @RequestParam Long countryId) throws ExecutionException, InterruptedException {
        String userChangeKey = String.valueOf(UUID.randomUUID());
        Change changeUser = Change.newBuilder()
            .setChangeKey(userChangeKey)
            .setChangeSource(ChangeSource.MAIN)
            .addChangeItem(ChangeItem.newBuilder(ChangeOperation.INSERT, TableId.of("users"), 1)
                .addObjectId("id", id) // первичный ключ
                .addNewValue("id", id)
                .schemaId(SchemaId.of("schema1"))
                .addNewValue("username", "user1")
                .addNewValue("country_id", countryId))
            .build();
        byte[] jsonUser = JSON_CONVERTER.toJson(changeUser);
        // Отправка сообщения
        kafkaTemplate.send("smart_replication_change_request_towner_default", userChangeKey, jsonUser).get();

        return new ResponseEntity<>(HttpStatusCode.valueOf(200));
    }

    @PostMapping("/country")
    public ResponseEntity<Void> postCountry(@RequestParam Long id, @RequestParam boolean isStatement) throws ExecutionException, InterruptedException {
        String countryChangeKey = String.valueOf(UUID.randomUUID());
        Change.ChangeBuilder changeCountryBuilder = Change.newBuilder()
                .setChangeKey(countryChangeKey)
                .setChangeSource(ChangeSource.MAIN);
        if (isStatement) {
            final ChangeItem.ChangeItemBuilder changeItem = ChangeItem.newBuilder(ChangeOperation.STATEMENT, null, 0)
                    .addAdditionalProperty(ChangeItem.ADDITIONAL_PROPERTY_QUERY, "INSERT INTO country (id, name, population) values (222, 222, 222)");
            changeCountryBuilder.addChangeItem(changeItem);
        } else {
            changeCountryBuilder
                    .addChangeItem(ChangeItem.newBuilder(ChangeOperation.INSERT, TableId.of("country"), 1)
                            .addObjectId("id", id) // первичный ключ
                            .addNewValue("id", id)
                            .schemaId(SchemaId.of("schema1"))
                            .addNewValue("name", "Россия")
                            .addNewValue("population", 150_000_000));
        }

        byte[] jsonCountry = JSON_CONVERTER.toJson(changeCountryBuilder.build());
        // Отправка сообщения
        kafkaTemplate.send(
            "smart_replication_change_request_towner_default",
            countryChangeKey, jsonCountry).get();

        return new ResponseEntity<>(HttpStatusCode.valueOf(200));
    }

    @PutMapping("/user")
    public ResponseEntity<Void> putUser(@RequestParam Long id, @RequestParam Long countryId, @RequestParam String username) throws ExecutionException, InterruptedException {
        String userChangeKey = String.valueOf(UUID.randomUUID());
        Change changeUser = Change.newBuilder()
            .setChangeKey(userChangeKey)
            .setChangeSource(ChangeSource.MAIN)
            .addChangeItem(ChangeItem.newBuilder(ChangeOperation.UPDATE, TableId.of("users"), 1)
                .addObjectId("id", id) // первичный ключ
                .addNewValue("id", id)
                .schemaId(SchemaId.of("schema1"))
                .addNewValue("username", username)
                .addNewValue("country_id", countryId))
            .build();
        byte[] jsonUser = JSON_CONVERTER.toJson(changeUser);
        // Отправка сообщения
        kafkaTemplate.send("smart_replication_change_request_towner_default", userChangeKey, jsonUser).get();

        return new ResponseEntity<>(HttpStatusCode.valueOf(200));
    }

    @PutMapping("/country")
    public ResponseEntity<Void> putCountry(@RequestParam Long id, @RequestParam String name) throws ExecutionException, InterruptedException {
        String countryChangeKey = String.valueOf(UUID.randomUUID());
        Change changeCountry = Change.newBuilder()
            .setChangeKey(countryChangeKey)
            .setChangeSource(ChangeSource.MAIN)
            .addChangeItem(ChangeItem.newBuilder(ChangeOperation.UPDATE, TableId.of("country"), 1)
                .addObjectId("id", id) // первичный ключ
                .addNewValue("id", id)
                .schemaId(SchemaId.of("schema1"))
                .addNewValue("name", name))
            .build();

        byte[] jsonCountry = JSON_CONVERTER.toJson(changeCountry);
        // Отправка сообщения
        kafkaTemplate.send(
            "smart_replication_change_request_towner_default",
            countryChangeKey, jsonCountry).get();

        return new ResponseEntity<>(HttpStatusCode.valueOf(200));
    }
}
