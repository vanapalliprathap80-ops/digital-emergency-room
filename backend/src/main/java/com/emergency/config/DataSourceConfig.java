package com.emergency.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
@Slf4j
public class DataSourceConfig {

    @Value("${spring.datasource.url:}")
    private String configuredUrl;

    @Value("${spring.datasource.username:}")
    private String configuredUsername;

    @Value("${spring.datasource.password:}")
    private String configuredPassword;

    @Value("${spring.datasource.hikari.maximum-pool-size:10}")
    private int maxPoolSize;

    @Value("${spring.datasource.hikari.minimum-idle:2}")
    private int minIdle;

    @Bean
    @Primary
    public DataSource dataSource() {
        String envDbUrl = System.getenv("DATABASE_URL");
        String finalUrl = StringUtils.hasText(envDbUrl) ? envDbUrl : configuredUrl;
        String finalUser = configuredUsername;
        String finalPass = configuredPassword;

        // Automatically convert Render/Heroku/Railway style URLs (postgres:// or postgresql://) to JDBC (jdbc:postgresql://)
        if (StringUtils.hasText(finalUrl) && (finalUrl.startsWith("postgres://") || (finalUrl.startsWith("postgresql://") && !finalUrl.startsWith("jdbc:")))) {
            try {
                // If it starts with postgres://, replace with postgresql:// for standard URI parsing
                String parseable = finalUrl.startsWith("postgres://") ? "postgresql://" + finalUrl.substring("postgres://".length()) : finalUrl;
                URI uri = new URI(parseable);
                String host = uri.getHost();
                int port = uri.getPort() == -1 ? 5432 : uri.getPort();
                String path = uri.getPath();
                String query = uri.getQuery();

                finalUrl = "jdbc:postgresql://" + host + ":" + port + path + (StringUtils.hasText(query) ? "?" + query : "");

                if (uri.getUserInfo() != null) {
                    String[] userInfo = uri.getUserInfo().split(":", 2);
                    if (userInfo.length > 0 && StringUtils.hasText(userInfo[0])) {
                        finalUser = userInfo[0];
                    }
                    if (userInfo.length > 1 && StringUtils.hasText(userInfo[1])) {
                        finalPass = userInfo[1];
                    }
                }
                log.info("Parsed postgres connection string into JDBC URL: jdbc:postgresql://{}:{}{}", host, port, path);
            } catch (Exception e) {
                log.warn("Failed to parse database URI, using original string: {}", e.getMessage());
            }
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(finalUrl);
        if (StringUtils.hasText(finalUser)) {
            config.setUsername(finalUser);
        }
        if (StringUtils.hasText(finalPass)) {
            config.setPassword(finalPass);
        }
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(minIdle);
        config.setDriverClassName("org.postgresql.Driver");
        config.setConnectionTimeout(30000);

        return new HikariDataSource(config);
    }
}
