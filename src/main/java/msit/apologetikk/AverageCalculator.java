package msit.apologetikk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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

                // HypEvidences will drive the order of the evidences in the average.json files - and also be used to fill in "blank/disregarded" evidences
                List<Evidence> hypEvidences = getHypothesisEvidences(hypotesemappe);


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
                        Average normalizedAvg = normalizeAverageEvidence(variantAvg, hypEvidences);
                        writeAverageToFileAsJson(variantmappe, normalizedAvg);
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
                    Average normalizedAvg = normalizeAverageEvidence(hypotesemappeAverage, hypEvidences);
                    writeAverageToFileAsJson(hypotesemappe, normalizedAvg);

                }
            }
        } catch (Exception e) {
            String stacktrace = Arrays.stream(e.getStackTrace()).map(StackTraceElement::toString).reduce("", (a, b) -> a + "\n    at " + b);
            // Print to stderr on the form of 2025-09-21T14:15:40.187Z Text
            log.error("Feil under beregning av gjennomsnitt: " + e.getMessage() + "\n" + stacktrace);
        }
    }

    private Average normalizeAverageEvidence(Average avg, List<Evidence> hypEvidences) {
        // The Average may not have all evidences from the hypothesis (if some evidences were never used in any variant)
        // Also, the order of evidences in Average should match the order in hypEvidences - there could be old variants that find their way into the Average
        // (Because the average is calculated from individual results collected over a long time). Therefor both the order and presence of evidences must be normalized.
        // If an evidence is missing in the Average, we add it as a "disregarded" evidence (weight=0)

        Average normalizedAverage = new Average(avg.getCount(), avg.getName(), avg.getTitle(), avg.getDenomination(), avg.getAprioriPct(), avg.getPosteriorPct());
        for (Evidence hypEv : hypEvidences) {
            Evidence ev = avg.getEvidence(hypEv.getId());
            if (ev == null) {
                // Evidence is missing in Average - add a disregarded evidence
                Evidence disregardedEv = new Evidence();
                disregardedEv.setId(hypEv.getId());
                disregardedEv.setHead(hypEv.getHead());
                disregardedEv.setCount(avg.getCount());
                disregardedEv.setCountDisregard(avg.getCount());
                disregardedEv.setPehPct(0.0);
                disregardedEv.setPenhPct(0.0);
                disregardedEv.setWeight(0);
                disregardedEv.setWeightD(0.0);
                normalizedAverage.addEvidence(disregardedEv);
            } else {
                normalizedAverage.addEvidence(ev);
            }
        }
        return normalizedAverage;
    }

    /* This method read the evidences found in the original hypothesis JSON file from the classpath static/evidence directory */
    private static List<Evidence> getHypothesisEvidences(File hypotesemappe) throws IOException {
        // I need to find the corresponding File in the static/evidence directory for each hypothesis directory
        // If hypotesemappe is "data/Hypothesis1", then the evidence directory is "static/evidence/Hypothesis1"
        ClassPathResource hypoteseEvidenceCPR = new ClassPathResource("static/evidence/" + hypotesemappe.getName() + ".json");
        if (!hypoteseEvidenceCPR.exists()) {
            log.warn("No evidence resource found for hypothesis " + hypotesemappe.getName() + " at " + hypoteseEvidenceCPR.getPath() + " - skipping average calculation for this hypothesis.");
            return new ArrayList<>();
        }
        InputStream in = hypoteseEvidenceCPR.getInputStream();
        String hypEvidenceJSONStr = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        in.close();
        JsonNode hypEvidenceJson = MAPPER.readTree(hypEvidenceJSONStr);
        // just to verify it's valid JSON
        List<Evidence> evidences = new ArrayList<>();
        hypEvidenceJson.get("evidence").forEach(ev -> {
            evidences.add(new Evidence(ev.get("id").asInt(), ev.get("head").asText()));
        });
        return evidences;
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
