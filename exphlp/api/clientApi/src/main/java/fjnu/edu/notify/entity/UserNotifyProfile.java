package fjnu.edu.notify.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

@Data
@NoArgsConstructor
public class UserNotifyProfile {
    @Id
    private String userId;
    private String email;
    private Boolean emailEnabled;
    private Boolean eventPlanDoneEnabled;
    private Boolean quietHoursEnabled;
    private String quietHoursStart;
    private String quietHoursEnd;
    private String timezone;
    private long updatedAt;
}

