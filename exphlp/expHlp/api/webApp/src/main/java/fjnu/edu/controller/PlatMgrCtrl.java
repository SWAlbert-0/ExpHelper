package fjnu.edu.controller;

import fjnu.edu.platmgr.entity.UserInfo;
import fjnu.edu.platmgr.service.PlatMgrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/api/PlatController")
public class PlatMgrCtrl {

    @Autowired
    PlatMgrService platMgrService;

    /**
     * 获取所有用户
     * @return
     */
    @GetMapping("/getUsers")
    public List<UserInfo> getUsers() {
        return platMgrService.getUsers();
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
        return platMgrService.getUsersByPage(pageNum, pageSize);

    }

    /**
     * 通过id获取用户
     * @param userId
     * @return
     */
    @GetMapping("/getUserById")
    public UserInfo getUserById(@RequestParam(value = "userId") String userId) {
        return platMgrService.getUserById(userId);
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
        return platMgrService.getUsersByName(userName, pageNum, pageSize);
    }

    /**
     * 通过名字查询用户
     * @param userName
     * @return
     */
    @GetMapping("/getUserByName")
    public UserInfo getUserByName(@RequestParam(value = "userName") String userName) {
        UserInfo user = platMgrService.getUserByName(userName);
        return user;
    }

    /**
     * 更新用户
     * @param user
     */
    @PostMapping("/updateUserById")
    public void updateUserById(@RequestBody UserInfo user) {
        platMgrService.updateUserById(user);
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

}
