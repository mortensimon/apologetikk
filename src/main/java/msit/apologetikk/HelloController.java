package msit.apologetikk;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api")
class HelloController {

    @GetMapping("/hello")
    public Map<String, String> hello() {
        return Map.of("message", "Hei fra Spring Boot!");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail badRequest(MissingServletRequestParameterException ex) {
        var pd = ProblemDetail.forStatus(BAD_REQUEST);
        pd.setTitle("Valideringsfeil");
        pd.setDetail("Her mangler et obligatorisk parameter: " + ex.getParameterName());
        pd.setProperty("code", "HK400");
        return pd; // Spring serialiserer til JSON automatisk
    }
}
