package com.bytechainx.digauto.common.model;

import java.util.ArrayList;
import java.util.List;

import com.bytechainx.digauto.common.EnumConstant.OperTypeEnum;
import com.bytechainx.digauto.common.model.base.BaseSystemOper;

/**
 * 权限点
 */
@SuppressWarnings("serial")
public class SystemOper extends BaseSystemOper<SystemOper> {
	
	public static final SystemOper dao = new SystemOper().dao();
	
	/**
	 * 根据CODE查询权限点
	 * @param operCode
	 * @return
	 */
	public SystemOper findByOperCode(String operCode) {
		return SystemOper.dao.findFirst("select * from system_oper where oper_code = ? limit 1", operCode);
	}
	
	/**
	 * 获取子权限
	 * @param parent
	 * @return
	 */
	public List<SystemOper> getChildList(Integer tenantOrgId) {
		List<SystemOper> operList = SystemOper.dao.find("select * from system_oper where parent_id = ?", getId());
		return filterOperList(tenantOrgId, operList);
	}
	
	/**
	 * 获取第三级权限数量
	 * @param parent
	 * @return
	 */
	public int getThirdCount(Integer tenantOrgId) {
		int thirdSize = 0;
		List<SystemOper> childList = getChildList(tenantOrgId);
		for (SystemOper c : childList) {
			List<SystemOper> thirdList = c.getChildList(tenantOrgId);
			thirdSize += thirdList.size();
		}
		return thirdSize;
	}

	/**
	 * 获取数据顶级权限
	 * @param integer 
	 * @param parent
	 * @return
	 */
	public List<SystemOper> findDataTopList(Integer tenantOrgId) {
		List<SystemOper> operList = SystemOper.dao.find("select * from system_oper where parent_id = ? and oper_type = ?", 0, OperTypeEnum.data.getValue());
		return filterOperList(tenantOrgId, operList);
	}

	/**
	 * 获取功能顶级权限
	 * @param parent
	 * @return
	 */
	public List<SystemOper> findFeatureTopList(Integer tenantOrgId) {
		List<SystemOper> operList = SystemOper.dao.find("select * from system_oper where parent_id = ? and oper_type = ?", 0, OperTypeEnum.feature.getValue());
		return filterOperList(tenantOrgId, operList);
	}
	
	/**
	 * 根据是否开通模块，过滤权限
	 * @param tenantOrgId
	 * @param operList
	 * @return
	 */
	private List<SystemOper> filterOperList(Integer tenantOrgId, List<SystemOper> operList) {
		TenantOrg tenantOrg = TenantOrg.dao.findById(tenantOrgId);
		List<SystemOper> _operList = new ArrayList<>();
		for (SystemOper oper : operList) {
			if(tenantOrg.hasModule(oper.getModuleCode())) {
				_operList.add(oper);
			}
		}
		return _operList;
	}
}

