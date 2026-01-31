package quizApplication.quiz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@EntityScan("quizApplication.quiz.entity")
@SpringBootApplication
public class QuizApplication extends SpringBootServletInitializer{

	public static void main(String[] args) {
		SpringApplication.run(QuizApplication.class, args);
	}
	// THIS IS CHANGE
	
	@Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(QuizApplication.class);
    }
	

}
