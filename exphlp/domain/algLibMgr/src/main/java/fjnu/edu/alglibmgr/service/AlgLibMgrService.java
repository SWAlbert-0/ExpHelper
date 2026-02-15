package fjnu.edu.alglibmgr.service;

import fjnu.edu.alglibmgr.entity.DefPara;
import fjnu.edu.alglibmgr.entity.AlgInfo;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public interface AlgLibMgrService {

    // 添加方法
    public boolean addAlgInfo(AlgInfo algInfo);
    // 通过Id删除算法
    public boolean deleteAlgInfoById(String algId);
    // 通过Id查找算法
    public AlgInfo getAlgInfoById(String algId);
    // 通过名字查找算法
    public List<AlgInfo> getAlgInfoByName(String algName,int pageNum,int pageSize);
    public AlgInfo getAlgInfoByName(String algName);
    // 分页获取算法列表
    public List<AlgInfo> getAlgInfos(int pageNum, int pageSize);
    // 更新算法
    public boolean updateAlgInfoById(AlgInfo algInfo);
    // 获取指定算法的参数列表
    public List<DefPara> getParasByAlgInfoId(String algId);
    // 计算请求的所有算法的个数
    public long countAllAlgs();
    // 计算通过名字查询的算法的个数
    public long countAlgsByAlgName(String algName);
    // 通过Id获得算法注册的服务名
    public String getServiceNameById(String algId);

}
