package msit.apologetikk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
    static {
        AverageCalculator.init();
    }
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}