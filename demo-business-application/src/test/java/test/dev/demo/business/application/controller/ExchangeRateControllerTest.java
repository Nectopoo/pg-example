package test.dev.demo.business.application.controller;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import test.dev.demo.business.application.dto.ExchangeRateDto;
import test.dev.demo.business.application.service.ExchangeRateService;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ExchangeRateControllerTest extends AbstractControllerConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExchangeRateService exchangeRateService;

    @Test
    public void find() throws Exception {
        when(exchangeRateService.findAll()).thenReturn(List.of(ExchangeRateDto.builder().build()));
        mockMvc.perform(get("/api/v1/exchange-rate"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
}
