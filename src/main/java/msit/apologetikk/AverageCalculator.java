package msit.apologetikk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


public class AverageCalculator implements Runnable {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void init() {
        AverageCalculator job = new AverageCalculator();
        Thread thread = new Thread(job);
        thread.start();
    }

    public static void main(String[] args) throws InterruptedException {
        init();
        Thread.sleep(10000); // Vent 10 sekunder for å la bakgrunnsjobben kjøre ferdig og skrive til stdout
    }

    public void run() {

        System.out.println("Kjører bakgrunnsjobb for å beregne gjennomsnitt...");
        Path dataPath = Paths.get("data");
        for (File hypotesemappe : dataPath.toFile().listFiles(File::isDirectory)) {
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
                        if (variantAvg == null) {
                            variantAvg = MAPPER.convertValue(newJson, Average.class);
                            variantAvg.setCount(1);
                            variantAvg.getEvidence().forEach(e -> {
                                e.setCount(1);
                                e.setWeightD(e.getWeight());
                            });
                        } else {
                            variantAvg.updateWith(newData);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                // Lagre gjennomsnittet i variantmappen
                if (variantAvg != null) {
                    writeAverageToFileAsJson(variantmappe, variantAvg);
                    variantAverages.add(variantAvg);
                }
            }
            // Beregn gjennomsnittet av alle variantgjennomsnittene
            if (!variantAverages.isEmpty()) {
                Average hypotesemappeAverage = null;
                for (Average variantAvg : variantAverages) {
                    if (hypotesemappeAverage == null) {
                        // lag en kopi av første som grunnlag
                        hypotesemappeAverage = variantAvg.deepCopy();
                        hypotesemappeAverage.setDenomination("All");
                    } else {
                        hypotesemappeAverage.updateWith(variantAvg);
                    }
                }
                // Lagre gjennomsnittet i hypotesemappen
                writeAverageToFileAsJson(hypotesemappe, hypotesemappeAverage);

            }
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
