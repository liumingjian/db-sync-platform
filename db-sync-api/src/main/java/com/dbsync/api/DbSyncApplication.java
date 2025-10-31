package com.dbsync.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * DB Sync Platform Main Application
 *
 * @author DB Sync Platform
 */
@SpringBootApplication(scanBasePackages = "com.dbsync")
@EnableJpaRepositories(basePackages = "com.dbsync.core.repository")
@EntityScan(basePackages = "com.dbsync.core.domain.entity")
public class DbSyncApplication {

    public static void main(String[] args) {
        SpringApplication.run(DbSyncApplication.class, args);
    }
}
