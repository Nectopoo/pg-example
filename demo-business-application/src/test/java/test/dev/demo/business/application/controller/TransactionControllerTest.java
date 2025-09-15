package test.dev.demo.business.application.controller;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import test.dev.demo.business.application.dto.TransactionDto;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TransactionControllerTest extends AbstractControllerConfigurationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    public void find() throws Exception {
        when(transactionService.findAllTransactionsByAccount(42L)).thenReturn(List.of(TransactionDto.builder().build()));
        mockMvc.perform(get("/api/v1/transaction/42"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
}
