package test.dev.demo.business.application.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.Hibernate;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
@NoArgsConstructor
@Getter
@AllArgsConstructor
public class ExchangeRateId implements Serializable {

    private Double dollar;
    private Double euro;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        ExchangeRateId that = (ExchangeRateId) o;
        return Objects.equals(dollar, that.dollar)
                && Objects.equals(euro, that.euro);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dollar, euro);
    }
}
