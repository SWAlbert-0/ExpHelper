package fjnu.edu.controller;

import fjnu.edu.auth.ApiResponse;
import fjnu.edu.auth.AuthUser;
import fjnu.edu.auth.ErrorCode;
import fjnu.edu.auth.PasswordService;
import fjnu.edu.auth.TraceContext;
import fjnu.edu.auth.UserFieldValidator;
import fjnu.edu.platmgr.entity.UserInfo;
import fjnu.edu.platmgr.service.PlatMgrService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.Collections;

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
    public Object getUsers(HttpServletRequest request) {
        List<UserInfo> users = sortUsersForPlatform(platMgrService.getUsers());
        return sanitizeUsers(users);
    }

    /**
     * 分页获取所有用户
     * @param pageNum
     * @param pageSize
     * @return
     */
    @GetMapping("getUsersByPage")
    public Object getUsersByPage(@RequestParam(value = "pageNum") int pageNum,
                                         @RequestParam(value = "pageSize") int pageSize,
                                         HttpServletRequest request) {
        int safePageNum = Math.max(1, pageNum);
        int safePageSize = pageSize <= 0 ? 10 : pageSize;
        List<UserInfo> all = sortUsersForPlatform(platMgrService.getUsers());
        if (all.isEmpty()) {
            return Collections.emptyList();
        }
        int from = Math.min((safePageNum - 1) * safePageSize, all.size());
        int to = Math.min(from + safePageSize, all.size());
        return sanitizeUsers(all.subList(from, to));

    }

    /**
     * 通过id获取用户
     * @param userId
     * @return
     */
    @GetMapping("/getUserById")
    public Object getUserById(@RequestParam(value = "userId") String userId, HttpServletRequest request) {
        return sanitizeUser(platMgrService.getUserById(userId));
    }

    /**
     * 删除用户
     * @param userId
     */
    @PostMapping("/deleteUserById")
    public Map<String, Object> deleteUserById(@RequestParam(value = "userId") String userId, HttpServletRequest request) {
        if (!isAdmin(request)) {
            return ApiResponse.failed(request, 403, "仅管理员可执行该操作", ErrorCode.AUTH_FORBIDDEN.code());
        }
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
        if (!isAdmin(request)) {
            return ApiResponse.failed(request, 403, "仅管理员可执行该操作", ErrorCode.AUTH_FORBIDDEN.code());
        }
        String validateMsg = validateUserFields(user, true);
        if (validateMsg != null) {
            return ApiResponse.failed(request, 400, validateMsg, ErrorCode.USER_FIELD_INVALID.code());
        }
        if (user != null && !StringUtils.hasText(user.getUserId())) {
            // 避免前端传空字符串ID导致Mongo将空ID落库，后续登录/重置密码失效
            user.setUserId(null);
        }
        if (user != null && user.getRole() == null) {
            // 默认新用户为普通用户，避免权限越权
            user.setRole(0);
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
    public Object getUserByName(@RequestParam(value = "userName") String userName,
                                  @RequestParam(value = "pageNum") int pageNum,
                                  @RequestParam(value = "pageSize") int pageSize,
                                  HttpServletRequest request) {
        int safePageNum = Math.max(1, pageNum);
        int safePageSize = pageSize <= 0 ? 10 : pageSize;
        String keyword = userName == null ? "" : userName.trim().toLowerCase();
        List<UserInfo> filtered = new ArrayList<>();
        List<UserInfo> all = platMgrService.getUsers();
        if (all != null) {
            for (UserInfo item : all) {
                if (item == null || !StringUtils.hasText(item.getUserName())) {
                    continue;
                }
                if (!StringUtils.hasText(keyword) || item.getUserName().trim().toLowerCase().contains(keyword)) {
                    filtered.add(item);
                }
            }
        }
        List<UserInfo> sorted = sortUsersForPlatform(filtered);
        if (sorted.isEmpty()) {
            return Collections.emptyList();
        }
        int from = Math.min((safePageNum - 1) * safePageSize, sorted.size());
        int to = Math.min(from + safePageSize, sorted.size());
        return sanitizeUsers(sorted.subList(from, to));
    }

    /**
     * 通过名字查询用户
     * @param userName
     * @return
     */
    @GetMapping("/getUserByName")
    public Object getUserByName(@RequestParam(value = "userName") String userName, HttpServletRequest request) {
        UserInfo user = platMgrService.getUserByName(userName);
        return sanitizeUser(user);
    }

    /**
     * 更新用户
     * @param user
     */
    @PostMapping("/updateUserById")
    public Map<String, Object> updateUserById(@RequestBody UserInfo user, HttpServletRequest request) {
        if (user == null) {
            return ApiResponse.failed(request, 400, "用户信息不能为空", ErrorCode.USER_FIELD_INVALID.code());
        }
        String targetUserId = resolveTargetUserId(user, request);
        if (!StringUtils.hasText(targetUserId)) {
            return ApiResponse.failed(request, 400, "用户标识异常，无法保存，请刷新后重试", ErrorCode.USER_ID_REQUIRED.code());
        }
        if (!canManageTargetUser(request, targetUserId)) {
            return ApiResponse.failed(request, 403, "当前账号无权限修改该用户信息", ErrorCode.AUTH_FORBIDDEN.code());
        }
        user.setUserId(targetUserId);
        String validateMsg = validateUserFields(user, false);
        if (validateMsg != null) {
            return ApiResponse.failed(request, 400, validateMsg, ErrorCode.USER_FIELD_INVALID.code());
        }
        if (user.getRole() == null) {
            UserInfo current = platMgrService.getUserById(user.getUserId());
            if (current != null) {
                user.setRole(current.getRole());
            }
        }
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            user.setPassword(null);
        } else {
            user.setPassword(passwordService.encode(user.getPassword()));
        }
        platMgrService.updateUserById(user);
        log.info("traceId={} path={} action=updateUser userId={}", TraceContext.getTraceId(request), "/api/PlatController/updateUserById",
                user == null ? null : user.getUserId());
        return ApiResponse.ok(request, null);
    }

    @PostMapping("/resetUserPassword")
    public Map<String, Object> resetUserPassword(@RequestBody UserInfo user, HttpServletRequest request) {
        if (user == null || !StringUtils.hasText(user.getPassword())) {
            return ApiResponse.failed(request, 400, "password不能为空", ErrorCode.USER_RESET_PASSWORD_INVALID.code());
        }
        String targetUserId = resolveTargetUserId(user, request);
        if (!StringUtils.hasText(targetUserId)) {
            return ApiResponse.failed(request, 400, "用户标识异常，无法重置密码，请刷新后重试", ErrorCode.USER_ID_REQUIRED.code());
        }
        if (!canManageTargetUser(request, targetUserId)) {
            return ApiResponse.failed(request, 403, "当前账号无权限重置该用户密码", ErrorCode.AUTH_FORBIDDEN.code());
        }
        UserInfo current = platMgrService.getUserById(targetUserId);
        if (current == null) {
            return ApiResponse.failed(request, 404, "用户不存在", ErrorCode.USER_NOT_FOUND.code());
        }
        if (user.getPassword().length() < 6 || user.getPassword().length() > 50) {
            return ApiResponse.failed(request, 400, "密码长度需在6到50之间", ErrorCode.PASSWORD_LENGTH_INVALID.code());
        }
        current.setPassword(passwordService.encode(user.getPassword()));
        platMgrService.updateUserById(current);
        log.info("traceId={} path={} action=resetPassword userId={}", TraceContext.getTraceId(request), "/api/PlatController/resetUserPassword", targetUserId);
        return ApiResponse.ok(request, null);
    }

    /**
     * 计算用户数量
     * @return
     */
    @GetMapping("/countAllUsers")
    public Object countAllUsers(HttpServletRequest request) {
        long count = platMgrService.countAllUsers();
        return count;
    }

    /**
     * 计算通过姓名查询出来的用户数量
     * @param userName
     * @return
     */
    @GetMapping("/countByUserName")
    public Object countByUserName(@RequestParam(value = "userName") String userName, HttpServletRequest request) {
        long count = platMgrService.countByUserName(userName);
        return count;
    }

    @PostMapping("/repairInvalidUserIds")
    public Map<String, Object> repairInvalidUserIds(HttpServletRequest request) {
        if (!isAdmin(request)) {
            return ApiResponse.failed(request, 403, "仅管理员可执行该操作", ErrorCode.AUTH_FORBIDDEN.code());
        }
        Map<String, Object> result = platMgrService.repairInvalidUserIds();
        log.info("traceId={} path={} action=repairInvalidUserIds result={}",
                TraceContext.getTraceId(request), "/api/PlatController/repairInvalidUserIds", result);
        return ApiResponse.ok(request, result);
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

    private boolean isAdmin(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        Object authObj = request.getAttribute("authUser");
        if (!(authObj instanceof AuthUser)) {
            return false;
        }
        AuthUser auth = (AuthUser) authObj;
        return auth.getRole() != null && auth.getRole() == 1;
    }

    private boolean canManageTargetUser(HttpServletRequest request, String targetUserId) {
        if (isAdmin(request)) {
            return true;
        }
        if (!StringUtils.hasText(targetUserId) || request == null) {
            return false;
        }
        Object authObj = request.getAttribute("authUser");
        if (!(authObj instanceof AuthUser)) {
            return false;
        }
        AuthUser auth = (AuthUser) authObj;
        String authUserId = auth.getUserId();
        if (StringUtils.hasText(authUserId) && targetUserId.trim().equals(authUserId.trim())) {
            return true;
        }
        if (!StringUtils.hasText(authUserId) && StringUtils.hasText(auth.getUserName())) {
            UserInfo self = platMgrService.getUserByName(auth.getUserName());
            return self != null && StringUtils.hasText(self.getUserId()) && targetUserId.trim().equals(self.getUserId().trim());
        }
        return false;
    }

    private String resolveTargetUserId(UserInfo user, HttpServletRequest request) {
        if (user == null) {
            return "";
        }
        if (StringUtils.hasText(user.getUserId())) {
            return user.getUserId().trim();
        }
        if (StringUtils.hasText(user.getUserName())) {
            UserInfo existed = findByUserNameLoose(user.getUserName().trim());
            if (existed != null && StringUtils.hasText(existed.getUserId())) {
                return existed.getUserId().trim();
            }
        }
        AuthUser auth = currentAuth(request);
        if (auth == null) {
            return "";
        }
        if (StringUtils.hasText(auth.getUserId())) {
            return auth.getUserId().trim();
        }
        if (StringUtils.hasText(auth.getUserName())) {
            UserInfo self = findByUserNameLoose(auth.getUserName().trim());
            if (self != null && StringUtils.hasText(self.getUserId())) {
                return self.getUserId().trim();
            }
        }
        return "";
    }

    private AuthUser currentAuth(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Object authObj = request.getAttribute("authUser");
        if (!(authObj instanceof AuthUser)) {
            return null;
        }
        return (AuthUser) authObj;
    }

    private UserInfo findByUserNameLoose(String userName) {
        if (!StringUtils.hasText(userName)) {
            return null;
        }
        UserInfo exact = platMgrService.getUserByName(userName);
        if (exact != null) {
            return exact;
        }
        List<UserInfo> candidates = platMgrService.getUsersByName(userName, 1, 20);
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        for (UserInfo candidate : candidates) {
            if (candidate != null
                    && StringUtils.hasText(candidate.getUserName())
                    && userName.equalsIgnoreCase(candidate.getUserName().trim())) {
                return candidate;
            }
        }
        return candidates.get(0);
    }

    private List<UserInfo> sortUsersForPlatform(List<UserInfo> users) {
        if (users == null || users.isEmpty()) {
            return users == null ? Collections.emptyList() : users;
        }
        List<UserInfo> sorted = new ArrayList<>(users);
        sorted.sort(Comparator
                .comparing((UserInfo u) -> !isAdminUserName(u))
                .thenComparing(this::extractCreatedAt, Comparator.reverseOrder()));
        return sorted;
    }

    private boolean isAdminUserName(UserInfo user) {
        return user != null && StringUtils.hasText(user.getUserName())
                && "admin".equalsIgnoreCase(user.getUserName().trim());
    }

    private long extractCreatedAt(UserInfo user) {
        if (user == null || !StringUtils.hasText(user.getUserId())) {
            return 0L;
        }
        String id = user.getUserId().trim();
        if (!ObjectId.isValid(id)) {
            return 0L;
        }
        return new ObjectId(id).getDate().getTime();
    }
}
