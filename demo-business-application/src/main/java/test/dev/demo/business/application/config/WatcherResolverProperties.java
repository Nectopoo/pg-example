package test.dev.demo.business.application.config;

import lombok.Data;
import test.dev.smartreplication.client.producer.ResolvingMode;

@Data
public class WatcherResolverProperties {
    private ResolvingMode fallbackMode = ResolvingMode.GET;
    private int cacheExpiredMs = 300;
}
