package com.example.ShardedSagaWallet;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ShardedSagaWalletApplication {

    private static void loadEnvironmentVariables(){
        Dotenv dotenv = Dotenv.configure().load();
        dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));
    }

	public static void main(String[] args) {
        loadEnvironmentVariables();

        SpringApplication.run(ShardedSagaWalletApplication.class, args);
	}

}
