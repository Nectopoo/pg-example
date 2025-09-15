package test.dev.demo.business.application.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import test.dev.demo.business.application.dto.AccountDto;
import test.dev.demo.business.application.dto.AccountType;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AccountControllerTest extends AbstractControllerConfigurationTest {

    private final AccountDto accountDto = AccountDto.builder()
        .id(1L)
        .accountNumber(42L)
        .accountType(AccountType.RUBLE)
        .amount(1000)
        .build();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void save() throws Exception {
        when(accountService.createAccount(accountDto)).thenReturn(accountDto);
        mockMvc.perform(post("/api/v1/account")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(accountDto)))
            .andExpect(status().isCreated());
    }

    @Test
    public void find() throws Exception {
        when(accountService.findAccount(42L)).thenReturn(accountDto);
        mockMvc.perform(get("/api/v1/account/42"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    public void delete() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/account/42"))
            .andExpect(status().isNoContent());
    }

    @Test
    public void changeRubbleAccountType() throws Exception {
        when(accountService.changeRubleAccountType(accountDto)).thenReturn(accountDto);
        mockMvc.perform(patch("/api/v1/account/change-type")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(accountDto)))
            .andExpect(status().isOk());
    }

    @Test
    public void changeAmount() throws Exception {
        when(accountService.updateAmount(accountDto)).thenReturn(accountDto);
        mockMvc.perform(patch("/api/v1/account")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(accountDto)))
            .andExpect(status().isOk());
    }
}
