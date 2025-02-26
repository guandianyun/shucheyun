package com.bytechainx.digauto.common.service.setting;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.bytechainx.digauto.common.model.TenantConfig;
import com.bytechainx.digauto.common.service.base.CommonService;
import com.jfinal.kit.Ret;
import com.jfinal.plugin.activerecord.Page;


/**
* 系统设置
*/
public class ConfigSystemService extends CommonService {

	/**
	* 分页列表
	*/
	public Page<TenantConfig> paginate(Integer tenantOrgId, Integer pageNumber, int pageSize) {
		StringBuffer where = new StringBuffer();
		List<Object> params = new ArrayList<Object>();
		where.append("where tenant_org_id = "+tenantOrgId);

		return TenantConfig.dao.paginate(pageNumber, pageSize, "select * ", "from tenant_config "+where.toString()+" order by id desc", params.toArray());
	}


	/**
	* 修改
	*/
	public Ret update(Integer tenantOrgId, TenantConfig tenantConfig) {
		if(tenantOrgId == null || tenantOrgId <= 0 || tenantConfig == null || tenantConfig.getId() == null || tenantConfig.getId() <= 0) {
			return Ret.fail("参数错误");
		}
		if(StringUtils.isEmpty(tenantConfig.getAttrKey())) {
			return Ret.fail("配置KEY不能为空");
		}
		if(StringUtils.isEmpty(tenantConfig.getAttrValue())) {
			return Ret.fail("配置值不能为空");
		}
		TenantConfig _tenantConfig = TenantConfig.dao.findFirst("select * from tenant_config where tenant_org_id = ? and attr_key = ? limit 1", tenantOrgId, tenantConfig.getAttrKey());
		if(_tenantConfig != null && _tenantConfig.getId().intValue() != tenantConfig.getId().intValue()) {
			return Ret.fail("配置已存在");
		}
		_tenantConfig = TenantConfig.dao.findById(tenantOrgId, tenantConfig.getId());
		if(_tenantConfig == null) {
			return Ret.fail("配置不存在，无法修改");
		}
		
		tenantConfig.setTenantOrgId(tenantOrgId);
		tenantConfig.setUpdatedAt(new Date());
		tenantConfig.update();
		
		return Ret.ok();
	}

}