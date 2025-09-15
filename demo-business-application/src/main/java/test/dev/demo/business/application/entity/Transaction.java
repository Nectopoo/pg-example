package test.dev.demo.business.application.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Транзакция по счету.
 */
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@Table(name = "transaction")
@NoArgsConstructor
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    private Instant date;

    @ManyToOne
    @JoinColumn(name = "from_account", nullable = false)
    @JsonIgnoreProperties("transactions")
    private Account fromAccount;

    private double amount;
}
