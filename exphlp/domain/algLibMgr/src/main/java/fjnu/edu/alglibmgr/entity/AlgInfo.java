package fjnu.edu.alglibmgr.entity;



import lombok.Data;

import lombok.ToString;

import org.springframework.data.mongodb.core.mapping.MongoId;

import java.util.List;

@Data
@ToString
public class AlgInfo {
	@MongoId
	private String algId;            // MongoDB自动生成的Id
	private String ownerUserId;      // 创建者用户ID
	private String ownerUserName;    // 创建者用户名
	private String algName;          // 算法名
	private String serviceName;     // 算法注册的服务名
	private String runtimeType;     // 运行时类型: java/python
	private String description;      // 算法描述
	private List<DefPara> defParas;  // 算法参数



}//end MothedEntity
