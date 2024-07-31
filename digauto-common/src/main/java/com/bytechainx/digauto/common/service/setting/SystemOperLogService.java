package com.bytechainx.digauto.common.service.setting;


import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.bytechainx.digauto.common.model.TenantOperLog;
import com.bytechainx.digauto.common.service.base.CommonService;
import com.jfinal.kit.Kv;
import com.jfinal.plugin.activerecord.Page;


/**
* 操作日志
*/
public class SystemOperLogService extends CommonService {


	/**
	* 分页列表
	*/
	public Page<TenantOperLog> paginate(Integer tenantOrgId, Kv conditionColumns, String startTime, String endTime, Integer pageNumber, int pageSize) {
		StringBuffer where = new StringBuffer();
		List<Object> params = new ArrayList<Object>();
		where.append("where tenant_org_id = "+tenantOrgId);

		conditionFilter(conditionColumns, where, params);
		
		if(StringUtils.isNotEmpty(startTime)) {
			where.append(" and oper_time  >= ?");
			params.add(startTime);
		}
		if(StringUtils.isNotEmpty(endTime)) {
			where.append(" and oper_time  <= ?");
			params.add(endTime);
		}
		return TenantOperLog.dao.paginate(pageNumber, pageSize, "select * ", "from tenant_oper_log "+where.toString()+" order by id desc", params.toArray());
	}

}