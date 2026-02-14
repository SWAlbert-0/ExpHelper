package fjnu.edu.platmgr.service;

import fjnu.edu.platmgr.entity.UserInfo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface PlatMgrService {

    /**
     * @Author wsn
     * @Description 获取用户列表
     **/
    List<UserInfo> getUsers();

    /**
     * @Author wsn
     * @Description 获取用户分页列表
     **/
    List<UserInfo> getUsersByPage(int pageNum, int pageSize);

    /**
     * @Author wsn
     * @Description 获取用户详情页
     **/
    UserInfo getUserById(String userId);

    /**
     * @Author wsn
     * @Description 根据Id删除用户
     **/
    boolean deleteUserById(String userId);

    /**
     * @Author wsn
     * @Description 添加一个新的用户
     **/
    boolean addUser(UserInfo user);

    /**
     * @Author wsn
     * @Description 通过名字模糊查询用户
     **/
    List<UserInfo> getUsersByName(String userName,int pageNum,int pageSize);

    /**
     * @Author wsn
     * @Description 通过名字查询用户
     **/
    UserInfo getUserByName(String userName);

    /**
     * @Author wsn
     * @Description 根据Id更新用户
     **/
    boolean updateUserById(UserInfo user);

    public long countAllUsers();

    public long countByUserName(String userName);

}
