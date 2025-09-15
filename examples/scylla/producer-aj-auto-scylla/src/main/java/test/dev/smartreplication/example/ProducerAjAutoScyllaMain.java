package test.dev.smartreplication.example;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.*;
import com.datastax.oss.driver.api.querybuilder.insert.InsertInto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import test.dev.smartreplication.example.properties.SmartReplicationProperties;
import test.dev.smartreplication.scylla.PlainSessionProvider;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.insertInto;
import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.literal;

@Slf4j
@SpringBootApplication
public class ProducerAjAutoScyllaMain {

    public static void main(String[] args) throws InterruptedException {
        log.info("Main start");
        final ConfigurableApplicationContext context = SpringApplication.run(ProducerAjAutoScyllaMain.class);
        CqlSession plainCqlSession = (CqlSession) context.getBean(PlainSessionProvider.class).getPlainSession();
        initializeKeyspace(plainCqlSession);
        CqlSession proxyReplicationSession = context.getBean("proxyReplicationSession", CqlSession.class);
        final SmartReplicationProperties conf = context.getBean(SmartReplicationProperties.class);
        simpleQuery(proxyReplicationSession, 11, conf.getMainScylla().getKeyspace());
        preparedStatement(proxyReplicationSession);
        testBatchStatement(proxyReplicationSession);
        testPreparedStatementAsync(proxyReplicationSession);
        testSimplePositionalParamsStatement(proxyReplicationSession);
        log.info("Main finish");
    }

    /**
     * Создает тестовый кейспейс и таблицы.
     */
    private static void initializeKeyspace(CqlSession session) {
        log.info("initializeKeyspace start");
        session.execute("CREATE KEYSPACE IF NOT EXISTS baeldung WITH replication = {'class': 'SimpleStrategy', 'replication_factor' : 1};");
        {
            final ResultSet rs =
                session.execute("CREATE COLUMNFAMILY IF NOT EXISTS baeldung.User (id bigint PRIMARY KEY, name text);");
            boolean response = rs.getExecutionInfo().getErrors().isEmpty();
            log.info("execute finish, is success " + response);
        }
        {
            final ResultSet rs =
                session.execute("CREATE COLUMNFAMILY IF NOT EXISTS baeldung.sensors (" +
                    "  sensor_id uuid," +
                    "  timestamp timestamp," +
                    "  value text," +
                    "  PRIMARY KEY (sensor_id, timestamp)" +
                    ") WITH CLUSTERING ORDER BY (timestamp DESC);");
            boolean response = rs.getExecutionInfo().getErrors().isEmpty();
            log.info("execute sensors finish, is success " + response);
        }
        {
            final ResultSet rs =
                session.execute("CREATE COLUMNFAMILY IF NOT EXISTS baeldung.order_item (" +
                    "  order_id uuid," +
                    "  order_item_id uuid," +
                    "  timestamp timestamp," +
                    "  value text," +
                    "  PRIMARY KEY (order_id, timestamp, order_item_id)" +
                    ") WITH CLUSTERING ORDER BY (timestamp DESC);");
            boolean response = rs.getExecutionInfo().getErrors().isEmpty();
            log.info("execute finish, is success " + response);
        }
        {
            final ResultSet rs =
                session.execute("CREATE TABLE IF NOT EXISTS baeldung.players (\n" +
                    "    player_id int primary key,\n" +
                    "    name text,\n" +
                    "    salary DOUBLE,\n" +
                    "    start_time TIME,\n" +
                    "    summary BLOB,\n" +
                    "    assigned boolean,\n" +
                    "    stats list<int>\n" +
                    ");");
            boolean response = rs.getExecutionInfo().getErrors().isEmpty();
            log.info("execute finish, is success " + response);
        }
        log.info("initializeKeyspace finish");
    }

    private static ResultSet simpleQuery(CqlSession proxySession, int id, String keyspace) {
        log.info("try block");
        InsertInto insert = insertInto(keyspace, "User");
        SimpleStatement statement = insert.value("id", literal(id))
            .value("name", literal("temp" + id))
            .build();
        log.info("execute finish");
        return proxySession.execute(statement);
    }

    private static ResultSet preparedStatement(CqlSession cqlSession) {
        String insertQuery = "INSERT INTO baeldung.sensors (sensor_id, timestamp, value) VALUES (?, ?, ?)";
        PreparedStatement preparedStatement = cqlSession.prepare(insertQuery);
            {
            UUID id = UUID.randomUUID();
            Instant timestamp = Instant.now();
            String value = "value9794";
            BoundStatement boundStatement = preparedStatement.bind(id, timestamp, value);
            cqlSession.execute(boundStatement);
            }
            // делаем переключение в стендин - задать ключ etcd
            {
            UUID id = UUID.randomUUID();
            Instant timestamp = Instant.now();
            String value = "value9795";
            BoundStatement boundStatement = preparedStatement.bind(id, timestamp, value);
            return cqlSession.execute(boundStatement);
            }
    }

    private static ResultSet testBatchStatement(CqlSession cqlSession) {
        String insertQuery = "INSERT INTO baeldung.order_item (order_id, timestamp, order_item_id, value) VALUES (?, ?, ?, ?)";
        PreparedStatement insertStatement = cqlSession.prepare(insertQuery);

        final UUID orderId = UUID.randomUUID();
        final UUID orderItemId = UUID.randomUUID();
        final UUID orderItemId2 = UUID.randomUUID();
        Instant timestamp = Instant.now();

        BatchStatement batch = BatchStatement.builder(BatchType.LOGGED)
            .addStatement(insertStatement.bind(orderId, timestamp, orderItemId, "value1"))
            .addStatement(insertStatement.bind(orderId, timestamp, orderItemId2, "value2"))
            .build();

        // Execute the batch statement
        return cqlSession.execute(batch);
    }

    public static void testPreparedStatementAsync(CqlSession cqlSession) throws InterruptedException {
        String insertQuery = "INSERT INTO sensors (sensor_id, timestamp, value) VALUES (?, ?, ?)";
        PreparedStatement preparedStatement = cqlSession.prepare(insertQuery);
        {
            UUID id = UUID.randomUUID();
            Instant timestamp = Instant.now();
            String value = "13001";
            BoundStatement boundStatement = preparedStatement.bind(id, timestamp, value);

            CompletionStage<AsyncResultSet> futureResultSet = cqlSession.executeAsync(boundStatement);

            CompletableFuture<AsyncResultSet> completableFuture = futureResultSet
                    .toCompletableFuture()
                    .whenComplete((resultSet, throwable) -> {
                        if (throwable != null) {
                            log.error("Error executing query: " + throwable.getMessage());
                        } else {
                            log.info("Test session has executed prepared async {}", resultSet);
                            for (Row row : resultSet.currentPage()) {
                                log.info("Row", row);
                            }
                        }
                    });
            try {
                completableFuture.join();
                log.info("Query executed successfully");
            } catch (CompletionException e) {
                log.info("Error executing query: " + e.getMessage());
            }
        }
        // произвести переключение в стендин
        {
            UUID id = UUID.randomUUID();
            Instant timestamp = Instant.now();
            String value = "13002";
            BoundStatement boundStatement = preparedStatement.bind(id, timestamp, value);

            CompletionStage<AsyncResultSet> futureResultSet = cqlSession.executeAsync(boundStatement);

            CompletableFuture<AsyncResultSet> completableFuture = futureResultSet
                    .toCompletableFuture()
                    .whenComplete((resultSet, throwable) -> {
                        if (throwable != null) {
                            log.error("Error executing query: " + throwable.getMessage());
                        } else {
                            log.info("Test session has executed prepared async {}", resultSet);
                            for (Row row : resultSet.currentPage()) {
                                log.info("Row", row);
                            }
                        }
                    });
            try {
                completableFuture.join();
                log.info("Query executed successfully");
            } catch (CompletionException e) {
                log.info("Error executing query: " + e.getMessage());
            }
        }
        log.info("Test async finish !!!");
    }

    public static void testSimplePositionalParamsStatement(CqlSession cqlSession) {
        String insertQuery = "INSERT INTO counters.lwt_counters (name, value) VALUES (?, ?) IF NOT EXISTS";
        final SimpleStatement simpleStatement = SimpleStatement.newInstance(insertQuery, "counter8", 15L)
            .setConsistencyLevel(ConsistencyLevel.QUORUM);
        final ResultSet resultSet = cqlSession.execute(simpleStatement);
        log.info("{}", resultSet);
        log.info("Data successfully inserted into ScyllaDB");
    }
}
