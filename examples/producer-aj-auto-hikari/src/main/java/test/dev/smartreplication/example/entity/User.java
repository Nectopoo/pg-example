package test.dev.smartreplication.example.entity;

import lombok.*;

import javax.persistence.*;
import java.time.Instant;

@Table(name = "users")
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class User {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "users_id_gen")
    @SequenceGenerator(name="users_id_gen", schema = "schema1", sequenceName = "users_id_sec", allocationSize = 1)
    private Long id;

    private String email;

    private String username;

    @Column(name = "create_date")
    private Instant createDate;
}
