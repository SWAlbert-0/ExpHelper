package fjnu.edu.service;

import fjnu.edu.entity.EachResult;
import fjnu.edu.entity.PlanExeResult;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface AlgRltSaveService {


    boolean insertPlanExeResult(PlanExeResult planExeResult);


   PlanExeResult  getAlgSaveByAlgName(String planId, String algId, String algName);
}
