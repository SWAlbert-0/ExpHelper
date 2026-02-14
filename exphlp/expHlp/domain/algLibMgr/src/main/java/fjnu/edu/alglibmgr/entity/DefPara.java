package fjnu.edu.alglibmgr.entity;



import lombok.Data;
import lombok.ToString;



@Data
@ToString
public class DefPara {

	private int paraId;            // 参数序号，从1开始递增
	private String paraName;       // 参数名
	private String paraType;       // 参数类型
	private String paraValue;      // 参数值
	private String description;    // 参数描述


}//end DefPara
