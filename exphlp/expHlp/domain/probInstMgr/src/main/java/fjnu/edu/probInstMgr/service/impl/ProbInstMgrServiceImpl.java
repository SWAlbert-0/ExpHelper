package fjnu.edu.probInstMgr.service.impl;

import fjnu.edu.common.exception.BusinessException;
import fjnu.edu.probInstMgr.dao.ProbInstDao;
import fjnu.edu.probInstMgr.entity.ProbInst;
import fjnu.edu.probInstMgr.service.ProbInstMgrService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class ProbInstMgrServiceImpl implements ProbInstMgrService {

    @Autowired
    ProbInstDao probInstDao;

    @Override
    public boolean addProbInst(ProbInst probInst) {
        try {
            probInstDao.addProbInst(probInst);
            return true;
        } catch (Exception e) {
            throw new BusinessException("添加问题实例失败");
        }
    }

    @Override
    public boolean delProbInstByID(String instId) {
        try {
            probInstDao.delProbInstByID(instId);
            return true;
        } catch (Exception e) {
            throw new BusinessException("删除问题实例失败");
        }
    }

    @Override
    public ProbInst getProbInstByID(String instId) {
        try {
            return probInstDao.getProbInstByID(instId);
        } catch (Exception e) {
            throw new BusinessException("获取问题实例失败");
        }
    }

    @Override
    public boolean updateProbInst(ProbInst probInst) {
        try {
            probInstDao.updateProbInst(probInst);
            return true;
        } catch (Exception e) {
            throw new BusinessException("更新问题实例失败");
        }
    }

    @Override
    public List<ProbInst> listProbInsts(int pageNum, int pageSize) {
        try {
            return probInstDao.listProbInsts(pageNum, pageSize);
        } catch (Exception e) {
            throw new BusinessException("获取问题实例列表失败");
        }
    }

    @Override
    public List<ProbInst> listProbInstsByinstName(String instName, int pageNum, int pageSize) {
        try {
            return probInstDao.listProbInstsByinstName(instName, pageNum, pageSize);
        } catch (Exception e) {
            throw new BusinessException("获取问题实例列表失败");
        }
    }

    @Override
    public long countAllProbInsts() {
        try {
            return probInstDao.countAllProbInsts();
        } catch (Exception e) {
            throw new BusinessException("获取问题实例个数失败");
        }
    }

    @Override
    public long countProbInstsByInstName(String instName) {
        try {
            return probInstDao.countProbInstsByInstName(instName);
        } catch (Exception e) {
            throw new BusinessException("获取问题实例个数失败");
        }
    }
}
