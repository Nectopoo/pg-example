package test.dev.smartreplication.example.service;

import test.dev.smartreplication.model.Change;
import test.dev.smartreplication.model.ChangeItem;
import test.dev.smartreplication.model.ChangeOperation;
import test.dev.smartreplication.model.TableId;
import test.dev.streaming.mdc.ChangeKeySupplier;

import java.util.List;
import java.util.Optional;

public class MyChangeKeySupplier extends ChangeKeySupplier {

    /**
     * Расширяем реализацию, добавим к ключу партиционирования имя таблицы.
     *
     * @param change вектор изменения, который будет отправлен в kafka.
     * @return ключ партиционирования для kafka.
     */
    @Override
    public String getKey(Change change) {
        String key = super.getKey(change);
        final List<ChangeItem> changeItems = change.getPayload();
        if (!changeItems.isEmpty()) {
            final Optional<ChangeItem> first = changeItems.stream()
                .filter(ci -> ci.getOperation() == ChangeOperation.INSERT ||
                    ci.getOperation() == ChangeOperation.UPSERT ||
                    ci.getOperation() == ChangeOperation.UPDATE ||
                    ci.getOperation() == ChangeOperation.DELETE)
                .findFirst();
            final Optional<String> tableNameOpt = first
                .map(ChangeItem::getTableId)
                .map(TableId::getName);
            if (tableNameOpt.isPresent()) {
                key = tableNameOpt.get() + "," + key;
            }
        }
        return key;
    }
}
