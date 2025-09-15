package test.dev.demo.business.application.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import org.hibernate.annotations.Cascade;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Клиент.
 */
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@Table(name = "client")
@NoArgsConstructor
public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @OneToMany(mappedBy = "client",
            fetch = FetchType.EAGER)
    @Cascade(org.hibernate.annotations.CascadeType.ALL)
    @JsonIgnoreProperties("client")
    private Set<Account> accounts = new HashSet<>();

    @Column(name = "create_date")
    @Builder.Default
    private Instant createDate = Instant.now();
}
