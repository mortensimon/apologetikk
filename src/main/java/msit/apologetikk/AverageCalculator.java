package msit.apologetikk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
public class AverageCalculator implements Runnable {

    private static final ObjectMapper MAPPER = new ObjectMapper();

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
            System.out.println("Kjører bakgrunnsjobb for å beregne gjennomsnitt...");
            File dataDir = new File("data");
            if (!dataDir.exists()) {
                dataDir.mkdirs();
            }

            for (File hypotesemappe : dataDir.listFiles(File::isDirectory)) {
                List<Average> variantAverages = new ArrayList<>();
                for (File variantmappe : hypotesemappe.listFiles(File::isDirectory)) {
                    Average variantAvg = null;
                    for (File jsonFile : variantmappe.listFiles((dir, name) -> name.endsWith(".json"))) {
                        if (jsonFile.getName().equals("average.json")) {
                            continue; // Hopp over tidligere genererte average.json filer
                        }
                        try {
                            JsonNode newJson = MAPPER.readTree(jsonFile);
                            Average newData = MAPPER.convertValue(newJson, Average.class);

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
                        writeAverageToFileAsJson(variantmappe, variantAvg);
                        variantAverages.add(variantAvg);
                    }
                }

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
            System.err.println("Feil under beregning av gjennomsnitt: " + e.getMessage());
            e.printStackTrace();
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
            System.out.println("Laget " + averagePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
