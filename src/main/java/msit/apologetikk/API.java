package msit.apologetikk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping(value= "/api", produces = MediaType.APPLICATION_JSON_VALUE)
class API {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");
    private static final Path ROOT = Paths.get("data"); // => data
    private static final Pattern SAFE_SEG = Pattern.compile("[A-Za-z0-9._-]+");


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

    @PostMapping(value = "/results", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> saveResults(@RequestBody JsonNode root) {
        try {
            Files.createDirectories(ROOT);

            // Krev feltene som styrer filplassering:
            String title = textOrBadRequest(root, "title");
            String denomination = textOrBadRequest(root, "denomination");

            String titSeg = sanitizeSegment(title);
            String denSeg = sanitizeSegment(denomination);

            // Generer stabil unik id (ULID/UUID). UUID er ok:
            String id = UUID.randomUUID().toString();

            // data/<title>/<denominasjon>/<id>.json
            Path dir = ROOT.resolve(titSeg).resolve(denSeg);
            Files.createDirectories(dir);
            Path file = dir.resolve(id + ".json");

            // Skriv fila
            MAPPER.writeValue(file.toFile(), root);

            URI href = URI.create("/api/results/" + id);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "status", "ok",
                    "id", id,
                    "href", href.toString()
            ));
        } catch (IllegalArgumentException bad) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", bad.getMessage()));
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "error",
                    "message", ex.getMessage()
            ));
        }
    }

    @GetMapping("/results/{id}")
    public ResponseEntity<?> getResultsById(@PathVariable String id) {
        // Validate UUID format to avoid path traversal-like inputs
        try {
            UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Invalid id format"
            ));
        }

        // Search for data/*/*/{id}.json
        try (Stream<Path> walk = Files.walk(ROOT, 3)) {
            Optional<Path> found = walk
                    .filter(p -> p.getFileName() != null && p.getFileName().toString().equals(id + ".json"))
                    .findFirst();

            if (found.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "status", "error",
                        "message", "Result not found"
                ));
            }

            JsonNode json = MAPPER.readTree(found.get().toFile());
            return ResponseEntity.ok(json);
        } catch (IOException io) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "error",
                    "message", io.getMessage()
            ));
        }
    }

    private static String textOrBadRequest(JsonNode root, String field) {
        String v = root.path(field).asText(null);
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException("Missing required field: " + field);
        }
        return v;
    }

    private static String sanitizeSegment(String seg) {
        if (!SAFE_SEG.matcher(seg).matches()) {
            // Ikke godta /, .., mellomrom osv.
            throw new IllegalArgumentException("Illegal characters in path segment: " + seg);
        }
        return seg;
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
