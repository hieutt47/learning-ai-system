package vn.edu.learningai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class LearningaiApplication {
	public static void main(String[] args) {
		SpringApplication.run(LearningaiApplication.class, args);
	}

}
