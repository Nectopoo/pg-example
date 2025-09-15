package test.dev.demo.business.application.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import test.dev.demo.business.application.dto.ClientDto;
import test.dev.demo.business.application.dto.SearchCriteriaDto;
import test.dev.demo.business.application.exception.ClientNotFoundException;
import test.dev.demo.business.application.mapper.EntityDtoMapper;
import test.dev.demo.business.application.repository.ClientRepository;
import test.dev.smartreplication.core.provider.ChangeProvider;
import test.dev.smartreplication.model.Change;
import test.dev.smartreplication.model.ChangeItem;
import test.dev.smartreplication.model.ChangeOperation;
import test.dev.smartreplication.model.ProviderType;
import test.dev.streaming.core.ChangeReplicationMode;
import test.dev.streaming.mdc.MDCKeySupplier;

import java.sql.Timestamp;
import java.util.LinkedHashMap;

import static test.dev.demo.business.application.helper.FieldHelper.updateField;

@Service
@RequiredArgsConstructor
public class ClientService {
    private final ClientRepository clientRepository;

    private final ChangeProvider changeProvider;

    public ClientDto findClient(Long id) {
        var client = clientRepository.findById(id);
        if (client.isEmpty()) {
            return null;
        }
        return EntityDtoMapper.map(client.get());
    }

    public ClientDto saveClient(ClientDto clientDto) {
        var client = clientRepository.save(EntityDtoMapper.map(clientDto));
        return EntityDtoMapper.map(client);
    }

    public ClientDto updateClient(ClientDto clientDto) {
        var client = clientRepository.findById(clientDto.getId())
                .orElseThrow(() -> new ClientNotFoundException("Клиент с id:" + clientDto.getId() + " не найден"));
        updateField(clientDto::getFirstName, client::setFirstName);
        updateField(clientDto::getLastName, client::setLastName);
        clientRepository.update(client.getFirstName(), client.getLastName(), client.getId());
        return EntityDtoMapper.map(client);
    }

    public void deleteClient(Long id) {
        clientRepository.deleteById(id);
    }

    @Transactional
    public int deleteByCreateDateBeforeViaManual(SearchCriteriaDto criteria) {
        // флаг работает до конца транзакции, сообщение в kafka не будет сформировано автоматически
        MDC.put(MDCKeySupplier.CHANGE_REPLICATION_MODE, ChangeReplicationMode.NONE.name());
        final LinkedHashMap<Integer, Object> params = new LinkedHashMap<>();
        params.put(1, Timestamp.from(criteria.getDateBefore()));
        final ChangeItem.ChangeItemBuilder changeItem = ChangeItem.newBuilder(ChangeOperation.STATEMENT, null, 0)
                .addAdditionalProperty(ChangeItem.ADDITIONAL_PROPERTY_QUERY, "delete from client where create_date < ?")
                .addAdditionalProperty(ChangeItem.ADDITIONAL_PROPERTY_PARAMS, params);
        final Change.ChangeBuilder changeBuilder = changeProvider.getCurrentBuilder(ProviderType.SMART_REPLICATION);
        changeBuilder.addChangeItem(
                changeItem
        );
        return clientRepository.deleteByCreateDateBefore(criteria.getDateBefore());
    }

    @Transactional
    public int deleteByCreateDateBeforeViaStatement(SearchCriteriaDto criteria) {
        // флаг работает до конца транзакции, сообщение типа STATEMENT в kafka будет сформировано автоматически
        MDC.put(MDCKeySupplier.CHANGE_REPLICATION_MODE, ChangeReplicationMode.STATEMENT.name());
        return clientRepository.deleteByCreateDateBefore(criteria.getDateBefore());
    }
}

