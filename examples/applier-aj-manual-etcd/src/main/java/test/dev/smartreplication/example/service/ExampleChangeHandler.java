package test.dev.smartreplication.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import test.dev.smartreplication.api.dto.Change;
import test.dev.smartreplication.client.SmartReplicationApplierDataSource;
import test.dev.smartreplication.core.kafka.consumer.ChangeHandleResult;
import test.dev.smartreplication.core.kafka.consumer.ConsumerChangeHandler;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ExampleChangeHandler implements ConsumerChangeHandler {

    private static final Logger logger = LoggerFactory.getLogger(ExampleChangeHandler.class);

    @Autowired
    private SmartReplicationApplierDataSource dataSource;

    @Override
    public ChangeHandleResult handleChange(String key, Change change) {
        logger.info("key {}, change {}", key, change);
        try {
            handle(change, dataSource);
        } catch (SQLException throwables) {
            throw new RuntimeException(throwables);
        }
        return ChangeHandleResult.success();
    }

    private void handle(Change change, DataSource ds) throws SQLException {
        try (final Connection connection = ds.getConnection();
             final Statement statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            change.getPayload().forEach(item -> {
                handleChangeItem(item).ifPresent(sql -> {
                    try {
                        statement.execute(sql);
                    } catch (SQLException throwable) {
                        throw new RuntimeException(throwable);
                    }
                });
            });
            connection.commit();
        }
    }

    private Optional<String> handleChangeItem(test.dev.smartreplication.api.dto.ChangeItem item) {
        item.getObjectId();
        item.getNewValue();
        String sql;
        switch (item.getOperation()) {
            case INSERT:
                sql = "insert into " + item.getTableId().getName()
                    + "(" + String.join(",", item.getNewValue().keySet()) + ")"
                    + " values(" + item.getNewValue().values().stream().map(this::transform).collect(Collectors.joining(",")) + ")";
                break;
            case UPDATE:
                sql = "update " + item.getTableId().getName()
                    + " set " + item.getNewValue().entrySet().stream().map(entry -> entry.getKey() + "=" + this.transform(entry.getValue())).collect(Collectors.joining(","))
                    + " where " + item.getObjectId().entrySet().stream().map(entry -> entry.getKey() + "=" + this.transform(entry.getValue())).collect(Collectors.joining(","));
                break;
            case DELETE:
                sql = "delete from " + item.getTableId().getName()
                    + " where " + item.getObjectId().entrySet().stream().map(entry -> entry.getKey() + "=" + this.transform(entry.getValue())).collect(Collectors.joining(","));
                break;
            default:
                logger.warn("Unsupported operation");
                return Optional.empty();
        }
        logger.info("sql = {}", sql);
        return Optional.of(sql);
    }


    private String transform(Object o) {
        if (o instanceof String) {
            return "'" + o + "'";
        }
        return o.toString();
    }
}
