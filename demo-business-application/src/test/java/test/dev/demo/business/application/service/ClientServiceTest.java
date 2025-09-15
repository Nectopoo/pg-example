package test.dev.demo.business.application.service;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import test.dev.demo.business.application.dto.ClientDto;
import test.dev.demo.business.application.entity.Client;
import test.dev.demo.business.application.exception.ClientNotFoundException;
import test.dev.demo.business.application.repository.ClientRepository;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@MockBean(AccountService.class)
@MockBean(ExchangeRateService.class)
public class ClientServiceTest extends AbstractServiceConfigurationTest {

    private final Client client = Client.builder()
        .id(1L)
        .firstName("Gena")
        .lastName("Ivanov")
        .build();

    private final ClientDto clientDto = ClientDto.builder()
        .id(1L)
        .firstName("Gena")
        .lastName("Ivanov")
        .build();

    @Autowired
    private ClientService clientService;

    @MockBean
    private ClientRepository clientRepository;

    @Test
    public void findClient() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        var actualClient = clientService.findClient(1L);
        assertEquals("Gena", actualClient.getFirstName());
        assertEquals(1L, actualClient.getId().longValue());
    }

    @Test
    public void saveClient() {
        when(clientRepository.save(any(Client.class))).thenReturn(client);
        var actualClient = clientService.saveClient(clientDto);
        assertEquals("Gena", actualClient.getFirstName());
        assertEquals(1L, actualClient.getId().longValue());
    }

    @Test
    public void updateClient() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        clientDto.setFirstName("Ivan");
        var actualClient = clientService.updateClient(clientDto);
        assertEquals("Ivan", actualClient.getFirstName());
        assertEquals("Ivanov", actualClient.getLastName());
    }

    @Test(expected = ClientNotFoundException.class)
    public void updateNotExistClient() {
        when(clientRepository.findById(2L)).thenReturn(Optional.empty());
        clientDto.setId(2L);
        clientService.updateClient(clientDto);
    }
}
