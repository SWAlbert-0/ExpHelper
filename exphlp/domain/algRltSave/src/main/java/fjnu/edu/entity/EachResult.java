package fjnu.edu.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EachResult {

    private String key;
    private String value;
    private String dataType;

    public EachResult(String key, String value, String dataType) {
        this.key = key;
        this.value = value;
        this.dataType = dataType;
    }
}
