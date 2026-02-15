package fjnu.edu.alglibmgr.service.impl;


import fjnu.edu.alglibmgr.dao.AlgLibMgrDao;
import fjnu.edu.alglibmgr.entity.DefPara;
import fjnu.edu.alglibmgr.entity.AlgInfo;
import fjnu.edu.alglibmgr.service.AlgLibMgrService;
import fjnu.edu.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AlgLibMgrServiceImpl implements AlgLibMgrService {

	@Autowired
	AlgLibMgrDao AlgLibDao;

	//添加一种方法
	@Override
	public boolean addAlgInfo(AlgInfo algInfo) {
		try {
			AlgLibDao.addAlgInfo(algInfo);
			return true;
		}catch (Exception e){
			throw new BusinessException("添加算法失败");
		}
	}

	@Override
	public boolean deleteAlgInfoById(String algId) {
		try {
			AlgLibDao.deleteAlgInfoById(algId);
			return true;
		}catch (Exception e){
			throw new BusinessException("删除算法失败");
		}
	}

	@Override
	public AlgInfo getAlgInfoById(String algId) {
		try{
			return AlgLibDao.getAlgInfoById(algId);
		}catch (Exception e){
			throw new BusinessException("获取算法详情失败");
		}
	}

	@Override
	public List<AlgInfo> getAlgInfoByName(String algName,int pageNum,int pageSize) {
		try{
			List<AlgInfo> algInfos = AlgLibDao.getAlgInfoByName(algName, pageNum, pageSize);
			return algInfos;
		}catch (Exception e){
			throw new BusinessException("获取算法详情失败");
		}
	}

	@Override
	public AlgInfo getAlgInfoByName(String algName) {
		try{
			AlgInfo algInfo = AlgLibDao.getAlgInfoByName(algName);
			return algInfo;
		}catch (Exception e){
			throw new BusinessException("获取算法详情失败");
		}
	}

	@Override
	public List<AlgInfo> getAlgInfos(int pageNum, int pageSize) {
		try{
			List<AlgInfo> algInfos = AlgLibDao.getAlgInfos(pageNum, pageSize);
			return algInfos;
		}catch (Exception e){
			throw new BusinessException("获取算法列表失败");
		}
	}

	@Override
	public boolean updateAlgInfoById(AlgInfo algInfo) {
		try{
			AlgLibDao.updateAlgInfoById(algInfo);
			return true;
		}catch (Exception e){
			throw new BusinessException("更新失败");
		}
	}

	@Override
	public List<DefPara> getParasByAlgInfoId(String algId) {
		try{
			List<DefPara> paras = AlgLibDao.getParasByAlgInfoId(algId);
			return paras;
		}catch (Exception e){
			throw new BusinessException("获取参数列表失败");
		}
	}

	@Override
	public long countAllAlgs() {
		try {
			return AlgLibDao.countAllAlgs();
		} catch (Exception e) {
			throw new BusinessException("获取算法个数失败");
		}
	}

	@Override
	public long countAlgsByAlgName(String algName) {
		try {
			return AlgLibDao.countAlgsByAlgName(algName);
		} catch (Exception e) {
			throw new BusinessException("获取算法个数失败");
		}
	}

	@Override
	public String getServiceNameById(String algId) {
		try{
			return AlgLibDao.getServiceNameById(algId);
		}catch (Exception e){
			throw new BusinessException("获取算法注册服务名失败");
		}
	}

}//end AlgLibCfgService
