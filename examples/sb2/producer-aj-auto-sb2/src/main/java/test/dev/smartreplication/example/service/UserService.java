package test.dev.smartreplication.example.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import test.dev.smartreplication.core.provider.ChangeProvider;
import test.dev.smartreplication.example.entity.User;
import test.dev.smartreplication.example.repository.UserRepository;
import test.dev.smartreplication.model.Change;
import test.dev.smartreplication.model.ChangeItem;
import test.dev.smartreplication.model.ChangeOperation;
import test.dev.smartreplication.model.ProviderType;
import test.dev.streaming.core.ChangeReplicationMode;
import test.dev.streaming.mdc.MDCKeySupplier;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.concurrent.CountDownLatch;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Autowired(required = false) // на случай отключения ПЖ
    private ChangeProvider changeProvider;

    @Transactional
    public Long save() {
        final User user = new User();
        user.setEmail("testuser@test");
        user.setUsername("testuser");
        user.setCreateDate(Instant.now());
        final User saved = userRepository.save(user);
        return saved.getId();
    }

    @Transactional
    public Long txParallelTest(User user, CountDownLatch latch) throws InterruptedException {
        latch.await();
        final User saved = userRepository.save(user);
        return saved.getId();
    }

    /**
     * Пример работы флага MDCKeySupplier.CHANGE_REPLICATION_MODE, {@link ChangeReplicationMode.NONE}.
     * При таком флаге сообщение для kafka НЕ будет сформировано автоматически.
     * В этом примере сформирована вручную структура Change(сообщение для kafka) типа STATEMENT.
     */
    @Transactional
    public int deleteByCreateDateBeforeViaManual(Instant date) {
        // флаг работает до конца транзакции, сообщение в kafka не будет сформировано автоматически
        MDC.put(MDCKeySupplier.CHANGE_REPLICATION_MODE, ChangeReplicationMode.NONE.name());
        if (changeProvider != null) {
            final LinkedHashMap<Integer, Object> params = new LinkedHashMap<>();
            params.put(1, Timestamp.from(date));
            final ChangeItem.ChangeItemBuilder changeItem = ChangeItem.newBuilder(ChangeOperation.STATEMENT, null, 0)
                    .addAdditionalProperty(ChangeItem.ADDITIONAL_PROPERTY_QUERY, "delete from users where create_date < ?")
                    .addAdditionalProperty(ChangeItem.ADDITIONAL_PROPERTY_PARAMS, params);
            final Change.ChangeBuilder changeBuilder = changeProvider.getCurrentBuilder(ProviderType.SMART_REPLICATION);
            changeBuilder.addChangeItem(
                    changeItem
            );
        }
        return userRepository.deleteByCreateDateBefore(date);
    }

    /**
     * Пример работы флага MDCKeySupplier.CHANGE_REPLICATION_MODE, {@link ChangeReplicationMode.STATEMENT}.
     * При таком флаге сообщение для kafka будет сформировано автоматически.
     * Statement передается как есть: query, params.
     */
    @Transactional
    public int deleteByCreateDateBeforeViaStatement(Instant date) {
        // флаг работает до конца транзакции, сообщение типа STATEMENT в kafka будет сформировано автоматически
        MDC.put(MDCKeySupplier.CHANGE_REPLICATION_MODE, ChangeReplicationMode.STATEMENT.name());
        return userRepository.deleteByCreateDateBefore(date);
    }
}
