package fjnu.edu.controller;

import fjnu.edu.auth.ApiResponse;
import fjnu.edu.auth.ErrorCode;
import fjnu.edu.auth.PasswordService;
import fjnu.edu.auth.TraceContext;
import fjnu.edu.auth.UserFieldValidator;
import fjnu.edu.platmgr.entity.UserInfo;
import fjnu.edu.platmgr.service.PlatMgrService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("/api/PlatController")
public class PlatMgrCtrl {
    private static final Logger log = LoggerFactory.getLogger(PlatMgrCtrl.class);

    @Autowired
    PlatMgrService platMgrService;
    @Autowired
    PasswordService passwordService;

    /**
     * 获取所有用户
     * @return
     */
    @GetMapping("/getUsers")
    public List<UserInfo> getUsers() {
        return sanitizeUsers(platMgrService.getUsers());
    }

    /**
     * 分页获取所有用户
     * @param pageNum
     * @param pageSize
     * @return
     */
    @GetMapping("getUsersByPage")
    public List<UserInfo> getUsersByPage(@RequestParam(value = "pageNum") int pageNum,
                                         @RequestParam(value = "pageSize") int pageSize) {
        return sanitizeUsers(platMgrService.getUsersByPage(pageNum, pageSize));

    }

    /**
     * 通过id获取用户
     * @param userId
     * @return
     */
    @GetMapping("/getUserById")
    public UserInfo getUserById(@RequestParam(value = "userId") String userId) {
        return sanitizeUser(platMgrService.getUserById(userId));
    }

    /**
     * 删除用户
     * @param userId
     */
    @PostMapping("/deleteUserById")
    public Map<String, Object> deleteUserById(@RequestParam(value = "userId") String userId, HttpServletRequest request) {
        if (!StringUtils.hasText(userId)) {
            return ApiResponse.failed(request, 400, "userId不能为空", ErrorCode.USER_ID_REQUIRED.code());
        }
        platMgrService.deleteUserById(userId);
        log.info("traceId={} path={} action=deleteUser userId={}", TraceContext.getTraceId(request), "/api/PlatController/deleteUserById", userId);
        return ApiResponse.ok(request, null);
    }

    /**
     * 增加用户
     * @param user
     */
    @PostMapping("/addUser")
    public Map<String, Object> addUser(@RequestBody UserInfo user, HttpServletRequest request) {
        String validateMsg = validateUserFields(user, true);
        if (validateMsg != null) {
            return ApiResponse.failed(request, 400, validateMsg, ErrorCode.USER_FIELD_INVALID.code());
        }
        if (user != null && user.getRole() == null) {
            user.setRole(1);
        }
        if (user != null && user.getPassword() != null && !user.getPassword().trim().isEmpty()) {
            user.setPassword(passwordService.encode(user.getPassword()));
        }
        platMgrService.addUser(user);
        log.info("traceId={} path={} action=addUser userName={}", TraceContext.getTraceId(request), "/api/PlatController/addUser",
                user == null ? null : user.getUserName());
        return ApiResponse.ok(request, null);
    }

    /**
     * 通过名字模糊查询用户
     * @param userName
     * @param pageNum
     * @param pageSize
     * @return
     */
    @GetMapping("/getUserByRegexName")
    public List<UserInfo> getUserByName(@RequestParam(value = "userName") String userName,
                                  @RequestParam(value = "pageNum") int pageNum,
                                  @RequestParam(value = "pageSize") int pageSize) {
        return sanitizeUsers(platMgrService.getUsersByName(userName, pageNum, pageSize));
    }

    /**
     * 通过名字查询用户
     * @param userName
     * @return
     */
    @GetMapping("/getUserByName")
    public UserInfo getUserByName(@RequestParam(value = "userName") String userName) {
        UserInfo user = platMgrService.getUserByName(userName);
        return sanitizeUser(user);
    }

    /**
     * 更新用户
     * @param user
     */
    @PostMapping("/updateUserById")
    public Map<String, Object> updateUserById(@RequestBody UserInfo user, HttpServletRequest request) {
        String validateMsg = validateUserFields(user, false);
        if (validateMsg != null) {
            return ApiResponse.failed(request, 400, validateMsg, ErrorCode.USER_FIELD_INVALID.code());
        }
        if (user != null && user.getRole() == null && user.getUserId() != null) {
            UserInfo current = platMgrService.getUserById(user.getUserId());
            if (current != null) {
                user.setRole(current.getRole());
            }
        }
        if (user != null && (user.getPassword() == null || user.getPassword().trim().isEmpty())) {
            user.setPassword(null);
        } else if (user != null) {
            user.setPassword(passwordService.encode(user.getPassword()));
        }
        platMgrService.updateUserById(user);
        log.info("traceId={} path={} action=updateUser userId={}", TraceContext.getTraceId(request), "/api/PlatController/updateUserById",
                user == null ? null : user.getUserId());
        return ApiResponse.ok(request, null);
    }

    @PostMapping("/resetUserPassword")
    public Map<String, Object> resetUserPassword(@RequestBody UserInfo user, HttpServletRequest request) {
        if (user == null || user.getUserId() == null || user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            return ApiResponse.failed(request, 400, "userId和password不能为空", ErrorCode.USER_RESET_PASSWORD_INVALID.code());
        }
        UserInfo current = platMgrService.getUserById(user.getUserId());
        if (current == null) {
            return ApiResponse.failed(request, 404, "用户不存在", ErrorCode.USER_NOT_FOUND.code());
        }
        if (user.getPassword().length() < 6 || user.getPassword().length() > 50) {
            return ApiResponse.failed(request, 400, "密码长度需在6到50之间", ErrorCode.PASSWORD_LENGTH_INVALID.code());
        }
        current.setPassword(passwordService.encode(user.getPassword()));
        platMgrService.updateUserById(current);
        log.info("traceId={} path={} action=resetPassword userId={}", TraceContext.getTraceId(request), "/api/PlatController/resetUserPassword", user.getUserId());
        return ApiResponse.ok(request, null);
    }

    /**
     * 计算用户数量
     * @return
     */
    @GetMapping("/countAllUsers")
    public long countAllUsers() {
        long count = platMgrService.countAllUsers();
        return count;
    }

    /**
     * 计算通过姓名查询出来的用户数量
     * @param userName
     * @return
     */
    @GetMapping("/countByUserName")
    public long countByUserName(@RequestParam(value = "userName") String userName) {
        long count = platMgrService.countByUserName(userName);
        return count;
    }

    private List<UserInfo> sanitizeUsers(List<UserInfo> users) {
        List<UserInfo> out = new ArrayList<>();
        if (users == null) {
            return out;
        }
        for (UserInfo user : users) {
            out.add(sanitizeUser(user));
        }
        return out;
    }

    private UserInfo sanitizeUser(UserInfo user) {
        if (user == null) {
            return null;
        }
        UserInfo out = new UserInfo();
        out.setUserId(user.getUserId());
        out.setUserName(user.getUserName());
        out.setRole(user.getRole());
        out.setEmail(user.getEmail());
        out.setWechat(user.getWechat());
        out.setMobile(user.getMobile());
        out.setQq(user.getQq());
        out.setAvatar(user.getAvatar());
        out.setPassword(null);
        return out;
    }

    private String validateUserFields(UserInfo user, boolean requirePassword) {
        if (user == null) {
            return "用户信息不能为空";
        }
        if (!StringUtils.hasText(user.getUserName())) {
            return "用户名不能为空";
        }
        if (requirePassword && !StringUtils.hasText(user.getPassword())) {
            return "初始密码不能为空";
        }
        if (StringUtils.hasText(user.getPassword()) && (user.getPassword().length() < 6 || user.getPassword().length() > 50)) {
            return "密码长度需在6到50之间";
        }
        String emailMsg = UserFieldValidator.validateEmail(user.getEmail());
        if (emailMsg != null) {
            return emailMsg;
        }
        String mobileMsg = UserFieldValidator.validateMobile(user.getMobile());
        if (mobileMsg != null) {
            return mobileMsg;
        }
        String qqMsg = UserFieldValidator.validateQq(user.getQq());
        if (qqMsg != null) {
            return qqMsg;
        }
        return null;
    }
}
