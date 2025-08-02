package msit.apologetikk.valgomat;

import jakarta.annotation.Nullable;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping(value= "/api", produces = MediaType.APPLICATION_JSON_VALUE)

class ValgomatAPI {

    @GetMapping("/lagre")
    public ResponseEntity<String> lagre(@Nullable @RequestParam String team,
                                        @RequestParam String navn,
                                        @Nullable @RequestParam String epost) {

        if (navn == null) {
            return ResponseEntity
                    .status(BAD_REQUEST)
                    .body("Ny ansatt må ha et navn");
        }
        // … opprettelse OK
        return ResponseEntity.status(CREATED).body("OK - lagret ansatt " + navn + " i team " + team + " med epost " + epost);

    }

    @GetMapping("/hello2")
    public Map<String, String> hello() {
        return Map.of("message2", "Hei fra Spring Boot!");
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
