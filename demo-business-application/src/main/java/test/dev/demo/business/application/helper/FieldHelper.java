package test.dev.demo.business.application.helper;

import lombok.experimental.UtilityClass;

import java.util.function.Consumer;
import java.util.function.Supplier;

@UtilityClass
public class FieldHelper {
    public static <T> void updateField(final Supplier<T> nextSupplier, final Consumer<T> setter) {
        final var next = nextSupplier.get();
        if (next != null) {
            setter.accept(next);
        }
    }
}
