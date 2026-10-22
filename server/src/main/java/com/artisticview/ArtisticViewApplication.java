package com.artisticview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EntityScan("com.artisticview.persistence")
@EnableJpaRepositories("com.artisticview.persistence")
public class ArtisticViewApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArtisticViewApplication.class, args);
    }
}
