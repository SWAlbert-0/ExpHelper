package fjnu.edu.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthUser {
    private String userId;
    private String userName;
    private Integer role;
}
