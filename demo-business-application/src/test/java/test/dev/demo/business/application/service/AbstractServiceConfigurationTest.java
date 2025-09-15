package test.dev.demo.business.application.service;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.PostgreSQLContainer;
import test.dev.demo.business.application.repository.StudentRepository;
import test.dev.smartreplication.client.SmartReplicationConfiguration;
import test.dev.smartreplication.client.SmartReplicationDataSource;
import test.dev.smartreplication.client.kafka.producer.KafkaChangeHandler;
import test.dev.smartreplication.core.provider.ChangeProvider;

import javax.sql.DataSource;

@RunWith(SpringRunner.class)
@SpringBootTest
@Import({DataSourceAutoConfiguration.class})
@ContextConfiguration(
    classes = AbstractServiceConfigurationTest.TestConfig.class,
    initializers = AbstractServiceConfigurationTest.Initializer.class
)
@MockBean(SmartReplicationConfiguration.class)
@MockBean(ConsumerService.class)
@MockBean(KafkaChangeHandler.class)
@MockBean(ProduceService.class)
public abstract class AbstractServiceConfigurationTest {

    private static final PostgreSQLContainer<?> DB = new PostgreSQLContainer<>("postgres:14.8-alpine");

    @MockBean
    protected TransactionService transactionService;

    @MockBean
    private StudentRepository studentRepository;

    @MockBean
    private DatabaseClient databaseClient;

    @MockBean
    private ChangeProvider changeProvider;

    @BeforeClass
    public static void beforeClass() {
        DB.start();
    }

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            TestPropertyValues.of(
                    "spring.datasource.url=" + DB.getJdbcUrl(),
                "spring.datasource.username=postgres",
                "spring.datasource.password=postgres1"
            ).applyTo(configurableApplicationContext.getEnvironment());
        }
    }

    @ComponentScan({"test.dev.demo.business.application.service"})
    static class TestConfig {
        @Bean
        public DataSource smartReplicationDataSource(DataSource dataSource,
                                                     SmartReplicationConfiguration smartReplicationConfiguration) {
            return new SmartReplicationDataSource(
                mainDataSource(dataSource),
                standinDataSource(dataSource),
                smartReplicationConfiguration
            );
        }

        @Bean
        public DataSource mainDataSource(DataSource dataSource) {
            return dataSource;
        }

        @Bean
        public DataSource standinDataSource(DataSource dataSource) {
            return dataSource;
        }
    }
}
