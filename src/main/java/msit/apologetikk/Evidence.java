package msit.apologetikk;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Evidence {
    private int count;
    private int countDisregard;
    private int id;
    private String head;
    private Double pehPct;
    private Double penhPct;
    private int weight;
    @JsonIgnore
    private Double weightD;

    public Evidence() {
    }

    // copy constructor
    public Evidence(Evidence other) {
        this.count = other.count;
        this.countDisregard = other.countDisregard;
        this.id = other.id;
        this.head = other.head;
        this.pehPct = other.pehPct;
        this.penhPct = other.penhPct;
        this.weight = other.weight;
        this.weightD = other.weightD == null ? other.weight : other.weightD;
    }

    // getters / setters
    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getCountDisregard() {
        return countDisregard;
    }

    public void setCountDisregard(int countDisregard) {
        this.countDisregard = countDisregard;
    }

    public int getCountDisregardPct () {
        return count == 0 ? 0 : (int) Math.round((double) countDisregard / count * 100);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getHead() {
        return head;
    }

    public void setHead(String head) {
        this.head = head;
    }

    public Double getPehPct() {
        return pehPct;
    }

    public void setPehPct(double pehPct) {
        this.pehPct = pehPct;
    }

    public Double getPenhPct() {
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
