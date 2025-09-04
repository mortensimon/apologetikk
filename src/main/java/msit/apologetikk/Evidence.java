package msit.apologetikk;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Evidence {
    private int count;
    private int id;
    private String label;
    private double pehPct;
    private double penhPct;
    private int weight;
    @JsonIgnore
    private Double weightD;

    public Evidence() {
    }

    // copy constructor
    public Evidence(Evidence other) {
        this.count = other.count;
        this.id = other.id;
        this.label = other.label;
        this.pehPct = other.pehPct;
        this.penhPct = other.penhPct;
        this.weight = other.weight;
        this.weightD = other.weightD == null ? other.weight : other.weightD;
    }

    // convenience deepCopy if needed
    public Evidence deepCopy() {
        return new Evidence(this);
    }

    // getters / setters
    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public double getPehPct() {
        return pehPct;
    }

    public void setPehPct(double pehPct) {
        this.pehPct = pehPct;
    }

    public double getPenhPct() {
        return penhPct;
    }

    public void setPenhPct(double penhPct) {
        this.penhPct = penhPct;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    // weightD is a double representation of weight for more precise calculations - not serialized to JSON, it will be
    // converted to int before serialization
    public Double getWeightD() {
        return weightD;
    }

    public void setWeightD(double weightD) {
        this.weightD = weightD;
    }
}
