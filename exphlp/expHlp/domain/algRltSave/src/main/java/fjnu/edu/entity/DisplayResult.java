package fjnu.edu.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@ToString
@NoArgsConstructor
public class DisplayResult {
    private String startTime;
    private String outputTime;
    private String probInstName;
    private List<EachResult> eachResults;
}
