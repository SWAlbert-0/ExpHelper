package fjnu.edu.probInstMgr.service;

import fjnu.edu.probInstMgr.entity.ProbInst;
import fjnu.edu.probInstMgr.entity.ProbDeleteResult;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public interface ProbInstMgrService {
    public boolean addProbInst(ProbInst probInst);
    public ProbDeleteResult delProbInstByID(String instId);
    public ProbInst getProbInstByID(String instId);
    public boolean updateProbInst(ProbInst probInst);
    public List<ProbInst> listProbInsts(int pageNum,int pageSize);
    public List<ProbInst> listProbInstsByinstName(String instName,int pageNum,int pageSize);

    public long countAllProbInsts();
    public long countProbInstsByInstName(String instName);
}
