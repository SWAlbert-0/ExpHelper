package fjnu.edu.exePlanMgr.entity;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Accessors(chain = true)
public class AlgRunInfo {

	private String algRunInfoId;
	private String algId;//引用的算法的Id
    private String algName;//算法名称
	private String serviceName; // 算法注册的服务名
	private int runNum;//方法运行次数
	private List<RunPara> runParas;

}//end MethodInfo
