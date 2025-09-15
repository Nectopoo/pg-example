package test.dev.demo.business.application.exception;

public class ConsumerNotFoundException extends RuntimeException {

    public ConsumerNotFoundException(String message) {
        super(message);
    }
}
