package test.dev.demo.business.application.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

@Entity
@Data
@Builder
@AllArgsConstructor
@Table(name = "user_session")
@NoArgsConstructor
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_session_id_generator")
    @SequenceGenerator(name = "user_session_id_generator", sequenceName = "user_session_id_seq", allocationSize = 1)
    private Long id;
    @Column(name = "user_id")
    private String userId;
    @Column(name = "session_key")
    private String sessionKey;

}
