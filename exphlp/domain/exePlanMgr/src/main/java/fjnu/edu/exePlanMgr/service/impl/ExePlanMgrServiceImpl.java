package fjnu.edu.exePlanMgr.service.impl;

import fjnu.edu.common.exception.BusinessException;
import fjnu.edu.exePlanMgr.dao.ExePlanMgrDao;
import fjnu.edu.exePlanMgr.entity.ExePlan;
import fjnu.edu.exePlanMgr.service.ExePlanMgrService;
import fjnu.edu.probInstMgr.dao.ProbInstDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExePlanMgrServiceImpl implements ExePlanMgrService {

    @Autowired
    ExePlanMgrDao exePlanMgrDao;

    @Autowired
    ProbInstDao probInstDao;

    @Override
    public List<ExePlan> getExePlans(int pageNum, int pageSize) {
        return exePlanMgrDao.getExePlans(pageNum,pageSize);
    }

    @Override
    public String addExePlan(ExePlan exeplan) throws Exception {
        return exePlanMgrDao.addExePlan(exeplan);
    }

    @Override
    public ExePlan getExePlanByName(String planName) {
        return exePlanMgrDao.getExePlanByName(planName);
    }

    @Override
    public ExePlan getExePlanById(String planId) {
        return exePlanMgrDao.getExePlanById(planId);
    }

    @Override
    public boolean deleteExePlanById(String planId) {
        return exePlanMgrDao.deleteExePlanById(planId);
    }

    @Override
    public boolean updateExePlanById(ExePlan exeplan) {
        return exePlanMgrDao.updateExePlanById(exeplan);
    }

    @Override
    public long countAllExePlans() {
        try {
            return exePlanMgrDao.countAllExePlans();
        } catch (Exception e) {
            throw new BusinessException("获取执行计划个数失败");
        }
    }

}
