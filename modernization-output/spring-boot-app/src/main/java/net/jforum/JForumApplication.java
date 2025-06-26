package net.jforum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class JForumApplication {

    public static void main(String[] args) {
        SpringApplication.run(JForumApplication.class, args);
    }
}