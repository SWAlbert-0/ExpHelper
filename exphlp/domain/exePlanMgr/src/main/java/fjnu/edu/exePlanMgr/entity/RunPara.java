package fjnu.edu.exePlanMgr.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;


@Data
@ToString
@NoArgsConstructor
public class RunPara {
	private int paraId;//参数的序号，从1开始顺序编号
	private String paraName;//参数名
	private String paraType;//参数类型
	private String paraValue;//参数值
	private String description;//参数的描述


}//end RunPara