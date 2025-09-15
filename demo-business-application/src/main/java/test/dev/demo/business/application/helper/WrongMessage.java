package test.dev.demo.business.application.helper;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WrongMessage {
    private final String text;
}
