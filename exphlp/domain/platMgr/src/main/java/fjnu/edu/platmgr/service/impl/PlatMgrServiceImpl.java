package fjnu.edu.platmgr.service.impl;

import fjnu.edu.common.exception.BusinessException;
import fjnu.edu.platmgr.dao.UserInfoDao;
import fjnu.edu.platmgr.entity.UserInfo;
import fjnu.edu.platmgr.service.PlatMgrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class PlatMgrServiceImpl implements PlatMgrService {

    @Autowired
    UserInfoDao userDao;

    @Override
    public List<UserInfo> getUsers() {
        try {
            List<UserInfo> users = userDao.getUserInfos();
            return users;
        } catch (Exception e) {
            throw new BusinessException("获取用户列表失败");
        }
    }

    @Override
    public List<UserInfo> getUsersByPage(int pageNum, int pageSize) {
        try {
            List<UserInfo> users = userDao.getUsersByPage(pageNum, pageSize);
            return users;
        } catch (Exception e) {
            throw new BusinessException("获取用户分页列表失败");
        }
    }

    @Override
    public UserInfo getUserById(String userId) {
        try {
            UserInfo user = userDao.getUserById(userId);
            return user;
        } catch (Exception e) {
            throw new BusinessException("获取用户详情失败");
        }
    }

    @Override
    public boolean deleteUserById(String userId) {
        try {
            userDao.deleteUserById(userId);
            return true;
        } catch (Exception e) {
            throw new BusinessException("删除用户失败");
        }
    }

    @Override
    public boolean addUser(UserInfo user) {
        try {
            userDao.addUser(user);
            return true;
        } catch (Exception e) {
            throw new BusinessException("添加用户失败");
        }
    }

    @Override
    public List<UserInfo> getUsersByName(String userName, int pageNum, int pageSize) {
        try {
            List<UserInfo> users = userDao.getUsersByName(userName, pageNum, pageSize);
            return users;
        } catch (Exception e) {
            throw new BusinessException("查询失败");
        }
    }

    @Override
    public UserInfo getUserByName(String userName) {
        try {
            UserInfo user = userDao.getUserByName(userName);
            return user;
        } catch (Exception e) {
            throw new BusinessException("查询失败");
        }
    }

    @Override
    public UserInfo getUserByRememberTokenHash(String rememberTokenHash) {
        try {
            return userDao.getUserByRememberTokenHash(rememberTokenHash);
        } catch (Exception e) {
            throw new BusinessException("查询失败");
        }
    }

    @Override
    public boolean updateUserById(UserInfo user) {
        try {
            return userDao.updateUserById(user);
        } catch (Exception e) {
            throw new BusinessException("更新失败");
        }
    }

    @Override
    public boolean updateRememberToken(String userId, String tokenHash, long issuedAt, long expireAt, int version) {
        try {
            return userDao.updateRememberToken(userId, tokenHash, issuedAt, expireAt, version);
        } catch (Exception e) {
            throw new BusinessException("更新失败");
        }
    }

    @Override
    public boolean clearRememberToken(String userId, int version) {
        try {
            return userDao.clearRememberToken(userId, version);
        } catch (Exception e) {
            throw new BusinessException("更新失败");
        }
    }

    @Override
    public long countAllUsers() {
        try {
            return userDao.countAllUsers();
        } catch (Exception e) {
            throw new BusinessException("获取用户数量失败");
        }
    }

    @Override
    public long countByUserName(String userName) {
        try {
            return userDao.countByUserName(userName);
        } catch (Exception e) {
            throw new BusinessException("获取用户数量失败");
        }
    }

    @Override
    public Map<String, Object> repairInvalidUserIds() {
        try {
            return userDao.repairInvalidUserIds();
        } catch (Exception e) {
            throw new BusinessException("修复用户ID失败");
        }
    }
}
