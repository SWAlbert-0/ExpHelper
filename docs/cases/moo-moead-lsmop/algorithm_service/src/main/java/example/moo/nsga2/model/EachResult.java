package example.moo.nsga2.model;

public class EachResult {
    public String key;
    public String value;
    public String dataType;

    public EachResult() {
    }

    public EachResult(String key, String value, String dataType) {
        this.key = key;
        this.value = value;
        this.dataType = dataType;
    }
}
