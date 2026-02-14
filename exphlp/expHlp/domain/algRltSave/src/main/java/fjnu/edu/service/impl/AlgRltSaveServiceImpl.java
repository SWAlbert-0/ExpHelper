package fjnu.edu.service.impl;

import fjnu.edu.common.exception.BusinessException;
import fjnu.edu.dao.AlgRltSaveDao;
import fjnu.edu.entity.EachResult;
import fjnu.edu.entity.PlanExeResult;
import fjnu.edu.service.AlgRltSaveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AlgRltSaveServiceImpl implements AlgRltSaveService {
    @Autowired
    AlgRltSaveDao algRltSaveDao;



    @Override
    public boolean insertPlanExeResult(PlanExeResult planExeResult) {
        try {
            algRltSaveDao.insertPlanExeResult(planExeResult);
            return true;
        } catch (Exception e) {
            throw new BusinessException("添加结果失败");
        }
    }




    @Override
    public PlanExeResult getAlgSaveByAlgName(String planId, String algId, String algName) {
        PlanExeResult planExeResult  = algRltSaveDao.getAlgSaveByAlgName(planId,  algId,algName);
        return planExeResult;
    }
}
