package fjnu.edu.intf;

import fjnu.edu.entity.EachResult;
import fjnu.edu.entity.OutPuter;
import fjnu.edu.exePlanMgr.entity.AlgRunCtx;

import java.util.List;

public interface IAlgRun {

    public List<EachResult>  run(AlgRunCtx algRunCtx);

}
