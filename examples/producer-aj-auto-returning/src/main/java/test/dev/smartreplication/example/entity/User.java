package test.dev.smartreplication.example.entity;

import lombok.*;
import org.springframework.data.annotation.Id;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class User {
    @Id
    private Long id;

    private String email;

    private String username;

    private BigDecimal amount;

    private Instant createDate;
}
