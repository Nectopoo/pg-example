package test.dev.demo.business.application.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import test.dev.smartreplication.model.CompressType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties("demo-app")
@Data
public class DemoBusinessConfigurationProperties {

    private KafkaConfigurationProperties kafka;

    private DataSourceProperties mainDataSource;

    private DataSourceProperties standinDataSource;

    private CompressProperties compress;

    private R2dbcProperties r2dbcPropsMain;

    private R2dbcProperties r2dbcPropsStandin;

    @NotBlank
    private String owner;

    @NotEmpty
    private String endpoint;

    @NotEmpty
    private String dbSchema;

    private FailureHandling failureHandling;

    boolean replicationEnabled;
    
    private ChangeKeyMode changeKeyMode;

    @Getter
    @Setter
    public static class KafkaConfigurationProperties {
        @NotEmpty
        private String bootstrapServers;

        private String consumerGroup;

        private Integer concurrency;

        @NotNull
        private Security security;

    }

    @Getter
    @Setter
    public static class DataSourceProperties {

        @NotBlank
        private String url;
        @NotEmpty
        private String username;
        @NotEmpty
        private String password;
    }

    @Getter
    @Setter
    public static class Security {
        private boolean enable;
        private String protocol;
        private String trustStoreLocation;
        private String trustStorePassword;
        private String trustStoreType;
        private String keyStoreLocation;
        private String keyStorePassword;
        private String keyPassword;
        private String keyStoreType;

        public Map<String, String> toMap() {
            Map<String, String> configMap = new HashMap<>();

            if (StringUtils.isNotEmpty(protocol)) {
                configMap.put(AdminClientConfig.SECURITY_PROTOCOL_CONFIG, protocol);
            }

            if (StringUtils.isNotEmpty(trustStoreLocation)) {
                configMap.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, trustStoreLocation);
            }

            if (StringUtils.isNotEmpty(trustStorePassword)) {
                configMap.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, trustStorePassword);
            }

            if (StringUtils.isNotEmpty(trustStoreType)) {
                configMap.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, trustStoreType);
            }

            if (StringUtils.isNotEmpty(keyStoreLocation)) {
                configMap.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, keyStoreLocation);
            }

            if (StringUtils.isNotEmpty(keyStorePassword)) {
                configMap.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, keyStorePassword);
            }

            if (StringUtils.isNotEmpty(keyStoreType)) {
                configMap.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, keyStoreType);
            }

            if (StringUtils.isNotEmpty(keyPassword)) {
                configMap.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, keyPassword);
            }
            return configMap;
        }
    }

    @Getter
    @Setter
    public static class CompressProperties {
        private int minBytesToCompress;
        private int minBytesForSplit;
        private CompressType compressType;
    }

    @Getter
    @Setter
    public static class Timeout {
        @Positive
        private int sessionTimeoutMs;

        @Positive
        private int connectionTimeoutMs;

        @Positive
        private int retryAttempts;
    }

    @Getter
    @Setter
    public static class FailureHandling {
        private FailureHandlingMode mode;
        int maxReplicationErrors;
        int errorPeriodInSeconds;
    }

    @Getter
    @Setter
    public static class R2dbcProperties {
        @NotEmpty
        private String driver = "postgresql";
        @NotEmpty
        private String host;
        @NotEmpty
        private int port;
        @NotEmpty
        private String database;
    }
}
