package com.bytechainx.digauto.common.model;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.bytechainx.digauto.common.EnumConstant.BizTypeEnum;
import com.bytechainx.digauto.common.EnumConstant.DataStatusEnum;
import com.bytechainx.digauto.common.EnumConstant.FlagEnum;
import com.bytechainx.digauto.common.EnumConstant.StoreTypeEnum;
import com.bytechainx.digauto.common.model.base.BaseTenantStore;

/**
 * 门店
 */
@SuppressWarnings("serial")
public class TenantStore extends BaseTenantStore<TenantStore> {
	
	public static final TenantStore dao = new TenantStore().dao();
	
	/**
	 * 获取汽修店名称
	 * @return
	 */
	public String getStoreName() {
		TenantStore countStore = TenantStore.dao.findFirst("select count(*) as count from tenant_store where tenant_org_id = ? and data_status != ? limit 1", getTenantOrgId(), DataStatusEnum.delete.getValue());
		TenantOrg tenantOrg = TenantOrg.dao.findCacheById(getTenantOrgId());
		if(countStore.getInt("count") == 1) {
			return tenantOrg.getName();
		}
		return tenantOrg.getName()+"("+getName()+")";
	}
	
	public String getStoreAddress() {
		TenantStore countStore = TenantStore.dao.findFirst("select count(*) as count from tenant_store where tenant_org_id = ? and data_status != ? limit 1", getTenantOrgId(), DataStatusEnum.delete.getValue());
		TenantOrg tenantOrg = TenantOrg.dao.findCacheById(getTenantOrgId());
		if(countStore.getInt("count") == 1) {
			return tenantOrg.getAddress();
		}
		return getAddress();
	}
	
	public TenantStore findById(Integer id) {
		return TenantStore.dao.findFirst("select * from tenant_store where id = ? and data_status != ?", id, DataStatusEnum.delete.getValue());
	}
	
	public TenantStore findById(Integer tenantOrgId, Integer id) {
		return TenantStore.dao.findFirst("select * from tenant_store where tenant_org_id = ? and id = ? and data_status != ? limit 1", tenantOrgId, id, DataStatusEnum.delete.getValue());
	}
	
	public List<TenantStore> findAll(Integer tenantOrgId) {
		return TenantStore.dao.find("select * from tenant_store where tenant_org_id = ? and data_status != ?", tenantOrgId, DataStatusEnum.delete.getValue());
	}

	public List<InventoryWarehouse> findWarehouseList(Integer storeId) {
		return InventoryWarehouse.dao.find("select * from inventory_warehouse where id in (select inventory_warehouse_id from inventory_warehouse_store_ref where tenant_store_id = ?) and data_status = ?", storeId, DataStatusEnum.enable.getValue());
	}
	
	/**
	 * 默认门店
	 * @param tenantOrgId
	 * @return
	 */
	public TenantStore findDefaultStore(Integer tenantOrgId) {
		return TenantStore.dao.findFirst("select * from tenant_store where tenant_org_id = ? and default_flag = ? limit 1", tenantOrgId, FlagEnum.YES.getValue());
	}
	
	public TenantEhrAccount getEhrAccount() {
		return TenantEhrAccount.dao.findByStoreId(getTenantOrgId(), getId());
	}
	
	public String getStoreTypeName() {
		return StoreTypeEnum.getEnum(getStoreType()).getName();
	}
	
	public String getBizTypeName() {
		if(getBizType() == null || getBizType() == 0) {
			return "";
		}
		return BizTypeEnum.getEnum(getBizType()).getName();
	}
	
	/**
	 * 自动计算距离
	 * @param id
	 * @param longitude
	 * @param latitude
	 * @return
	 */
	public TenantStore findById(Integer id, String longitude, String latitude) {
		return TenantStore.dao.findFirst("select *, ST_Distance_Sphere(point(longitude, latitude), point("+longitude+", "+latitude+")) as distance from tenant_store where id = ?", id);
	}
	
	/**
	 * 场地设备照片
	 * @return
	 */
	public List<String> getPlaceImgList() {
		return Arrays.asList(StringUtils.split(getPlaceImg() == null ? "" : getPlaceImg(), ","));
	}
	
	/**
	 * 人员照片
	 * @return
	 */
	public List<String> getWorkerImgList() {
		return Arrays.asList(StringUtils.split(getWorkerImg() == null ? "" : getWorkerImg(), ","));
	}
	
	
}

