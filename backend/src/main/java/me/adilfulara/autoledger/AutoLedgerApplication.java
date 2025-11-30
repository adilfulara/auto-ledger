package me.adilfulara.autoledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;

@SpringBootApplication
@EnableJdbcAuditing
public class AutoLedgerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AutoLedgerApplication.class, args);
    }

}
