package fjnu.edu.controller;

import fjnu.edu.auth.PasswordService;
import fjnu.edu.platmgr.entity.UserInfo;
import fjnu.edu.platmgr.service.PlatMgrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/api/PlatController")
public class PlatMgrCtrl {

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
    public void deleteUserById(@RequestParam(value = "userId") String userId) {
        platMgrService.deleteUserById(userId);
    }

    /**
     * 增加用户
     * @param user
     */
    @PostMapping("/addUser")
    public void addUser(@RequestBody UserInfo user) {
        if (user != null && user.getRole() == null) {
            user.setRole(1);
        }
        if (user != null && user.getPassword() != null && !user.getPassword().trim().isEmpty()) {
            user.setPassword(passwordService.encode(user.getPassword()));
        }
        platMgrService.addUser(user);
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
    public void updateUserById(@RequestBody UserInfo user) {
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
    }

    @PostMapping("/resetUserPassword")
    public void resetUserPassword(@RequestBody UserInfo user) {
        if (user == null || user.getUserId() == null || user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("userId和password不能为空");
        }
        UserInfo current = platMgrService.getUserById(user.getUserId());
        if (current == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        current.setPassword(passwordService.encode(user.getPassword()));
        platMgrService.updateUserById(current);
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
}
