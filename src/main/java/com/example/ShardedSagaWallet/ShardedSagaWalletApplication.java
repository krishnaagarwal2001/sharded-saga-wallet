package com.example.ShardedSagaWallet;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ShardedSagaWalletApplication {

    private static void loadEnvironmentVariables() {
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();

        // 1. Load all .env variables into System properties
        dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));

        /*
            Sharding sphere can't access the env variables, it can system variables.
        */
        // 2. Fallback: load all OS env variables if not already set
        System.getenv().forEach((key, value) -> {
            if (System.getProperty(key) == null) {
                System.setProperty(key, value);
            }
        });
    }

    public static void main(String[] args) {
        loadEnvironmentVariables();
        SpringApplication.run(ShardedSagaWalletApplication.class, args);
    }
}