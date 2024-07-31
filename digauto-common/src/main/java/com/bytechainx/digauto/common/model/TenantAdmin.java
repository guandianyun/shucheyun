package com.bytechainx.digauto.common.model;

import java.math.BigDecimal;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.bytechainx.digauto.common.CommonConstant;
import com.bytechainx.digauto.common.EnumConstant.DataStatusEnum;
import com.bytechainx.digauto.common.EnumConstant.FlagEnum;
import com.bytechainx.digauto.common.EnumConstant.UserActiveStatusEnum;
import com.bytechainx.digauto.common.kit.StrUtil;
import com.bytechainx.digauto.common.model.base.BaseTenantAdmin;

/**
 * 租户员工表
 */
@SuppressWarnings("serial")
public class TenantAdmin extends BaseTenantAdmin<TenantAdmin> {
	public static final TenantAdmin dao = new TenantAdmin().dao();

	public TenantAdmin findMainAdmin(Integer tenantOrgId) {
		return TenantAdmin.dao.findFirst("select * from tenant_admin where tenant_org_id = ? and main_account_flag = ?", tenantOrgId, FlagEnum.YES.getValue());
	}
	
	/**
	 * 统计可登录用户数
	 * @param tenantOrgId
	 * @return
	 */
	public int countBy(Integer tenantOrgId) {
		TenantAdmin admin = TenantAdmin.dao.findFirst("select count(*) as counts from tenant_admin where tenant_org_id = ? and active_status = ? and login_flag = ?", tenantOrgId, UserActiveStatusEnum.enable.getValue(), FlagEnum.YES.getValue());
		return admin.getInt("counts");
	}
	
	public TenantAdmin findBy(Integer tenantOrgId, String mobile) {
		return TenantAdmin.dao.findFirst("select * from tenant_admin where tenant_org_id = ? and mobile = ?", tenantOrgId, mobile);
	}
	public TenantAdmin findByMobile(String mobile) {
		return TenantAdmin.dao.findFirst("select * from tenant_admin where mobile = ?", mobile);
	}
	public TenantAdmin findById(Integer tenantOrgId, Integer id) {
		return TenantAdmin.dao.findFirst("select * from tenant_admin where tenant_org_id = ? and id = ? limit 1", tenantOrgId, id);
	}
	
	public TenantRole getRole() {
		return TenantRole.dao.findFirstByCache(CommonConstant.CACHE_NAME_ONE_MINUTE_STORE, "tenant.role.id."+getRoleId(), "select * from tenant_role where id = ?", getRoleId());
	}
	
	/**
	 * 获取所属仓库
	 * @return
	 */
	public List<InventoryWarehouse> getWarehouseList() {
		String warehouseId = getInventoryWarehouseId();
		if(StringUtils.isEmpty(warehouseId)) {
			return null;
		}
		if(isHasAllWarehouse()) {
			return InventoryWarehouse.dao.find("select * from inventory_warehouse where tenant_org_id = ?", getTenantOrgId());
		}
		return InventoryWarehouse.dao.find("select * from inventory_warehouse where tenant_org_id = ? and id in ("+warehouseId+")", getTenantOrgId());
	}
	/**
	 * 判断员工是否拥有仓库权限
	 * @param warehouseId
	 * @return
	 */
	public boolean isHasWarehouse(int warehouseId) {
		String ids = StrUtil.beforeAfterAppendComma(getInventoryWarehouseId());
		return ids.contains(","+warehouseId+",");
	}
	/**
	 * 是否有所有仓库权限
	 * @return
	 */
	public boolean isHasAllWarehouse() {
		return StringUtils.equals(getInventoryWarehouseId(), "0");
	}
	
	/**
	 * 获取所属门店
	 * @return
	 */
	public List<TenantStore> getStoreList() {
		String storeId = getTenantStoreId();
		if(StringUtils.isEmpty(storeId)) {
			return null;
		}
		if(isHasAllStore()) {
			return TenantStore.dao.find("select * from tenant_store where tenant_org_id = ? and data_status = ?", getTenantOrgId(), DataStatusEnum.enable.getValue());
		}
		return TenantStore.dao.find("select * from tenant_store where tenant_org_id = ? and id in ("+storeId+") and data_status = ?", getTenantOrgId(), DataStatusEnum.enable.getValue());
	}
	/**
	 * 判断员工是否拥有门店权限
	 * @param warehouseId
	 * @return
	 */
	public boolean isHasStore(int storeId) {
		if(getRole().getSuperFlag()) {
			return true;
		}
		String ids = StrUtil.beforeAfterAppendComma(getTenantStoreId());
		return ids.contains(","+storeId+",");
	}
	/**
	 * 是否有所有门店权限
	 * @return
	 */
	public boolean isHasAllStore() {
		return StringUtils.equals(getTenantStoreId(), "0");
	}
	
	
	public BigDecimal getCommissionAmount() {
		return getBigDecimal("commissionAmount");
	}
	
	/**
	 * 是否超级管理员
	 * @return
	 */
	public boolean isSuperAdmin() {
		if(getMainAccountFlag()) {
			return true;
		}
		if(getRole().getSuperFlag()) {
			return true;
		}
		return false;
	}
	
	
	public String getOnlineTimeStr() {
		int times = getOnlineTimes();
		if(times < 60) {
			return times+"分钟";
		}
		if(times/60 < 24) {
			return times/60+"小时"+times%60+"分钟";
		}
		return times/1440+"天"+ times/1440/60 + "小时"+times/1440%60+"分钟";
	}

	
}

