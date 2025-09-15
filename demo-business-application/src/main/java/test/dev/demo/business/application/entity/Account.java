package test.dev.demo.business.application.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import org.hibernate.annotations.Cascade;
import test.dev.demo.business.application.dto.AccountType;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Счет клиента.
 */
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@Table(name = "account")
@NoArgsConstructor
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "account_number")
    private Long accountNumber;

    @Column(name = "account_type")
    private AccountType accountType;

    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    @JsonIgnoreProperties("accounts")
    private Client client;

    @OneToMany(mappedBy = "fromAccount",
            fetch = FetchType.EAGER)
    @Cascade(org.hibernate.annotations.CascadeType.ALL)
    @JsonIgnoreProperties("fromAccount")
    private Set<Transaction> transactions = new HashSet<>();

    private double amount;
}
