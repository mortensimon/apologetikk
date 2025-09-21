package msit.apologetikk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class AverageCalculator implements Runnable {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Logger log = LoggerFactory.getLogger(AverageCalculator.class);

    public static void init() {
        AverageCalculator job = new AverageCalculator();
        Thread thread = new Thread(job);
        thread.start();
    }

    public static void main(String[] args) {
        init();
    }

    @PostConstruct
    public void startBackgroundJob() {
        init();
    }

    public void run() {

        try {
            // Print to stdout on the form of 2025-09-21T14:15:40.187Z Text
            log.info("AverageCalculator.run() starter...");
            File dataDir = new File("data");
            if (!dataDir.exists()) {
                dataDir.mkdirs();
            }

            for (File hypotesemappe : dataDir.listFiles(File::isDirectory)) {
                int fileCount = 0;
                int variantCount = 0;
                List<Average> variantAverages = new ArrayList<>();
                for (File variantmappe : hypotesemappe.listFiles(File::isDirectory)) {
                    Average variantAvg = null;
                    for (File jsonFile : variantmappe.listFiles((dir, name) -> name.endsWith(".json"))) {
                        if (jsonFile.getName().equals("average.json")) {
                            continue; // Hopp over tidligere genererte average.json filer
                        }
                        fileCount++;
                        try {
                            JsonNode newJson = MAPPER.readTree(jsonFile);
                            Average newData = MAPPER.convertValue(newJson, Average.class);
                            newData.setCount(1);

                            if (variantAvg == null) { // Lager gjennomsnitt av *første* fil => samme som den filen
                                variantAvg = MAPPER.convertValue(newJson, Average.class);
                                variantAvg.setCount(1);
                                variantAvg.getEvidence().forEach(e -> {
                                    e.setCount(1);
                                    e.setWeightD(e.getWeight());
                                    if (e.getWeight() == 0) {
                                        e.setCountDisregard(1);
                                    }
                                });
                            } else { // Oppdater gjennomsnittet med ny data
                                variantAvg.updateWith(newData);
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    if (variantAvg != null) {
                        variantCount++;
                        writeAverageToFileAsJson(variantmappe, variantAvg);
                        variantAverages.add(variantAvg);
                    }
                }
                // Print to stdout on the form of 2025-09-21T14:15:40.187Z Text
                log.info("Har behandlet " + fileCount + " filer i " + variantCount + " varianter i hypotesemappen " + hypotesemappe.getName());

                // På dette tidspunktet har vi laget gjennomsnitt for hver variant i hypotesemappen. Vi skal nå
                // lage et gjennomsnitt for hele hypotesemappen basert på variant-gjennomsnittene - altså et
                // gjennomsnitt av gjennomsnittene (vi kaller denominasjonen "All")
                if (!variantAverages.isEmpty()) {
                    Average hypotesemappeAverage = null;
                    for (Average variantAvg : variantAverages) {
                        if (hypotesemappeAverage == null) { // Lager total-gjennomsnitt av *første* variant => samme som den varianten
                            hypotesemappeAverage = new Average(variantAvg);
                            hypotesemappeAverage.setDenomination("All");
                        } else { // Oppdater gjennomsnittet med ny variant
                            hypotesemappeAverage.updateWith(variantAvg);
                        }
                    }
                    // Lagre gjennomsnittet i hypotesemappen
                    writeAverageToFileAsJson(hypotesemappe, hypotesemappeAverage);

                }
            }
        } catch (Exception e) {
            String stacktrace = Arrays.stream(e.getStackTrace()).map(StackTraceElement::toString).reduce("", (a, b) -> a + "\n    at " + b);
            // Print to stderr on the form of 2025-09-21T14:15:40.187Z Text
            log.error("Feil under beregning av gjennomsnitt: " + e.getMessage() + "\n" + stacktrace);
        }
    }

    private static void writeAverageToFileAsJson(File variantmappe, Average variantAvg) {
        try {
            Path averagePath = variantmappe.toPath().resolve("average.json");
            // Avrund alle double-verdier til 0 desimaler og gjør dem til integer
            variantAvg.setAprioriPct(Math.round(variantAvg.getAprioriPct()));
            variantAvg.setPosteriorPct(Math.round(variantAvg.getPosteriorPct()));
            for (Evidence ev : variantAvg.getEvidence()) {
                ev.setPehPct(Math.round(ev.getPehPct()));
                ev.setPenhPct(Math.round(ev.getPenhPct()));
                ev.setWeight((int) Math.round(ev.getWeightD()));
            }
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(averagePath.toFile(), variantAvg);
            // Print to stdout on the form of 2025-09-21T14:15:40.187Z Text
            log.info("\tLaget " + averagePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
