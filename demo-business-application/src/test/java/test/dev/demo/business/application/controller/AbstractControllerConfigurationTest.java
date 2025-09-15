package test.dev.demo.business.application.controller;

import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.PostgreSQLContainer;
import test.dev.demo.business.application.config.DemoBusinessConfigurationProperties;
import test.dev.demo.business.application.service.AccountService;
import test.dev.demo.business.application.service.ChangeService;
import test.dev.demo.business.application.service.ClientService;
import test.dev.demo.business.application.service.ConsumerService;
import test.dev.demo.business.application.service.ExchangeRateService;
import test.dev.demo.business.application.service.GrpcService;
import test.dev.demo.business.application.service.ProduceService;
import test.dev.demo.business.application.service.StudentService;
import test.dev.demo.business.application.service.TransactionService;
import test.dev.demo.business.application.service.UserService;
import test.dev.smartreplication.client.SmartReplicationConfiguration;

@RunWith(SpringRunner.class)
@MockBean(DemoBusinessConfigurationProperties.class)
@MockBean(SmartReplicationConfiguration.class)
@MockBean(ProduceService.class)
@MockBean(ConsumerService.class)
@MockBean(ChangeService.class)
@MockBean(GrpcService.class)
@WebMvcTest
public abstract class AbstractControllerConfigurationTest {

    private static final PostgreSQLContainer<?> DB = new PostgreSQLContainer<>("postgres:14.8-alpine");

    @MockBean
    protected ClientService clientService;

    @MockBean
    protected TransactionService transactionService;

    @MockBean
    protected AccountService accountService;

    @MockBean
    protected ExchangeRateService exchangeRateService;

    @MockBean
    protected StudentService studentService;

    @MockBean
    protected UserService userService;

}
