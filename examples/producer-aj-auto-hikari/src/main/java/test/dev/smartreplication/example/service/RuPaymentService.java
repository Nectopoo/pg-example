package test.dev.smartreplication.example.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import test.dev.smartreplication.core.provider.ChangeProvider;
import test.dev.smartreplication.example.repository.RuPaymentRepository;
import test.dev.smartreplication.model.Change;
import test.dev.smartreplication.model.ChangeItem;
import test.dev.smartreplication.model.ChangeOperation;
import test.dev.smartreplication.model.ProviderType;
import test.dev.streaming.core.ChangeReplicationMode;
import test.dev.streaming.mdc.MDCKeySupplier;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;

@Service
@RequiredArgsConstructor
public class RuPaymentService {

    private final RuPaymentRepository ruPaymentRepository;

    @Autowired(required = false) // на случай отключения ПЖ
    private ChangeProvider changeProvider;

    @Transactional
    public int deleteByCreateDateBeforeViaManual(OffsetDateTime date) {
        // флаг работает до конца транзакции, сообщение в kafka не будет сформировано автоматически
        MDC.put(MDCKeySupplier.CHANGE_REPLICATION_MODE, ChangeReplicationMode.NONE.name());
        if (changeProvider != null) {
            final LinkedHashMap<Integer, Object> params = new LinkedHashMap<>();
            params.put(1, Timestamp.from(date.toInstant()));
            final ChangeItem.ChangeItemBuilder changeItem = ChangeItem.newBuilder(ChangeOperation.STATEMENT, null, 0)
                    .addAdditionalProperty(ChangeItem.ADDITIONAL_PROPERTY_QUERY, "delete from rrko_ru_payment where create_date < ?")
                    .addAdditionalProperty(ChangeItem.ADDITIONAL_PROPERTY_PARAMS, params);
            final Change.ChangeBuilder changeBuilder = changeProvider.getCurrentBuilder(ProviderType.SMART_REPLICATION);
            changeBuilder.addChangeItem(
                    changeItem
            );
        }
        return ruPaymentRepository.deleteByCreateDateBefore(date);
    }

    @Transactional
    public int deleteByCreateDateBeforeViaStatement(OffsetDateTime date) {
        // флаг работает до конца транзакции, сообщение типа STATEMENT в kafka будет сформировано автоматически
        MDC.put(MDCKeySupplier.CHANGE_REPLICATION_MODE, ChangeReplicationMode.STATEMENT.name());
        return ruPaymentRepository.deleteByCreateDateBefore(date);
    }
}
