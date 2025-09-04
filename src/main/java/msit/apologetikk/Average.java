package msit.apologetikk;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Average {
    private int count;
    private String name;
    private String title;
    private String denomination;
    private double aprioriPct;
    private double posteriorPct;
    private List<Evidence> evidence = new ArrayList<>();

    public Average() {
    }

    // copy constructor
    public Average(Average other) {
        this.count = other.count;
        this.name = other.name;
        this.title = other.title;
        this.denomination = other.denomination;
        this.aprioriPct = other.aprioriPct;
        this.posteriorPct = other.posteriorPct;
        if (other.evidence != null) {
            this.evidence = new ArrayList<>(other.evidence.size());
            for (Evidence e : other.evidence) {
                this.evidence.add(new Evidence(e));
            }
        } else {
            this.evidence = new ArrayList<>();
        }
    }

    // convenience deepCopy method
    public Average deepCopy() {
        return new Average(this);
    }

    public void updateWith(Average newData) {
        if (newData == null) {
            return;
        }
        this.count++;
        this.aprioriPct = calcAverage(this.aprioriPct, this.count, newData.aprioriPct);
        this.posteriorPct = calcAverage(this.posteriorPct, this.count, newData.posteriorPct);

        Map<Integer, Evidence> currentEvidenceMap = evidence.stream().collect(TreeMap::new, (m, v) -> m.put(v.getId(), v), Map::putAll);


        for (Evidence newEv : newData.evidence) {
            Evidence existingEv = currentEvidenceMap.get(newEv.getId());
            if (existingEv != null) {
                existingEv.setCount(existingEv.getCount() + 1);
                existingEv.setPehPct(calcAverage(existingEv.getPehPct(), existingEv.getCount(), newEv.getPehPct()));
                existingEv.setPenhPct(calcAverage(existingEv.getPenhPct(), existingEv.getCount(), newEv.getPenhPct()));
                if (existingEv.getWeightD() == null) { // første gang vi setter vekt
                    existingEv.setWeightD(newEv.getWeight());
                }
                existingEv.setWeightD(calcAverage(existingEv.getWeightD(), existingEv.getCount(), (double) newEv.getWeight())); // gjennomsnitt av vektene
                existingEv.setWeight((int) Math.round(existingEv.getWeightD()));
            } else {
                currentEvidenceMap.put(newEv.getId(), newEv);
            }
        }

    }

    private double calcAverage(double currentAvg, int currentCount, double newData) {
        double productCurrentAvg = currentAvg * (currentCount - 1);
        double productNewAvg = productCurrentAvg + newData;
        double avg = productNewAvg / currentCount;
        return avg;
    }

    // getters / setters
    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDenomination() {
        return denomination;
    }

    public void setDenomination(String denomination) {
        this.denomination = denomination;
    }

    public double getAprioriPct() {
        return aprioriPct;
    }

    public void setAprioriPct(double aprioriPct) {
        this.aprioriPct = aprioriPct;
    }

    public double getPosteriorPct() {
        return posteriorPct;
    }

    public void setPosteriorPct(double posteriorPct) {
        this.posteriorPct = posteriorPct;
    }

    public List<Evidence> getEvidence() {
        return evidence;
    }

    public void setEvidence(List<Evidence> evidence) {
        this.evidence = evidence;
    }

}

