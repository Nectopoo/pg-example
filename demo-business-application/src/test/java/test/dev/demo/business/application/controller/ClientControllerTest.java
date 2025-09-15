package test.dev.demo.business.application.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import test.dev.demo.business.application.dto.ClientDto;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ClientControllerTest extends AbstractControllerConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private final ClientDto clientDto = ClientDto.builder()
        .id(1L)
        .firstName("Gena")
        .lastName("Ivanov")
        .build();

    @Test
    public void getClient() throws Exception {
        when(clientService.findClient(1L)).thenReturn(clientDto);
        mockMvc.perform(get("/api/v1/client/1"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    public void save() throws Exception {
        when(clientService.saveClient(clientDto)).thenReturn(clientDto);
        mockMvc.perform(post("/api/v1/client")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(clientDto)))
            .andExpect(status().isCreated());
    }

    @Test
    public void delete() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/client/1"))
            .andExpect(status().isNoContent());
    }

    @Test
    public void changeAmount() throws Exception {
        when(clientService.updateClient(clientDto)).thenReturn(clientDto);
        mockMvc.perform(patch("/api/v1/client")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(clientDto)))
            .andExpect(status().isOk());
    }
}
