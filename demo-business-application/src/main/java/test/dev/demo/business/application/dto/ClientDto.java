package test.dev.demo.business.application.dto;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClientDto {
    @Parameter(name = "Id клиента")
    private Long id;

    @Parameter(name = "Имя клиента")
    private String firstName;

    @Parameter(name = "Фамилия клиента")
    private String lastName;
}
