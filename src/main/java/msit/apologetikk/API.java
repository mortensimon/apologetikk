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
    private static final Path ROOT = Paths.get("data"); // => data
    private static final Pattern SAFE_SEG = Pattern.compile("[A-Za-z0-9._-]+");


    // Lag en ny GET-endpoint /api/average som tar en parameter "hypothesis" og så returnerer alle average.json for alle varianter av denne hypotesen
    @GetMapping("/average")
    public ResponseEntity<?> getAverages(@RequestParam String hypothesis) {
        Path hypPath = ROOT.resolve(hypothesis);
        if (!Files.exists(hypPath) || !Files.isDirectory(hypPath))
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", "warn", "message", "No data published yet"));

        try (Stream<Path> walk = Files.walk(hypPath, 2)) {
            // Finn alle filer som heter average.json
            var averages = walk
                    .filter(p -> p.getFileName() != null && p.getFileName().toString().equals("average.json"))
                    .map(p -> {
                        try {
                            return MAPPER.readTree(p.toFile());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }).toList();
            return ResponseEntity.ok(Map.of(
                    "status", "ok",
                    "hypothesis", hypothesis,
                    "count", averages.size(),
                    "averages", averages
            ));
        } catch (IOException io) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "error",
                    "message", io.getMessage()
            ));
        }
    }




    @PostMapping(value = "/results", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> saveResults(@RequestBody JsonNode root) {
        try {
            Files.createDirectories(ROOT);
            String name = root.path("name").textValue();
            String denomination = getSanitizedField(root, "denomination");
            String id = UUID.randomUUID().toString();
            Path dir = ROOT.resolve(name).resolve(denomination);
            Files.createDirectories(dir);
            Path file = dir.resolve(id + ".json");
            MAPPER.writeValue(file.toFile(), root);
            URI href = URI.create("/api/results/" + id);
            AverageCalculator.init();
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

    private static String getSanitizedField(JsonNode root, String field) {
        String v = root.path(field).asText(null);
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException("Missing required field: " + field);
        }
        v = v.replaceAll(" ", "_").replaceAll("/", "_");
        if (!SAFE_SEG.matcher(v).matches()) {
            // Ikke godta /, .., mellomrom osv.
            throw new IllegalArgumentException("Illegal characters in path segment: " + v);
        }
        return v;
    }
}
