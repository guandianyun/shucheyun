package com.bytechainx.digauto.common.model;

import java.util.List;

import com.bytechainx.digauto.common.EnumConstant.RegionLevelEnum;
import com.bytechainx.digauto.common.model.base.BaseSystemRegion;

/**
 * 地区
 */
@SuppressWarnings("serial")
public class SystemRegion extends BaseSystemRegion<SystemRegion> {
	
	public static final SystemRegion dao = new SystemRegion().dao();
	
	
	public SystemRegion findRegion(String regionCode) {
		return SystemRegion.dao.findFirst("select * from system_region where region_code = ? limit 1", regionCode);
	}
	
	/**
	 * 所有省份
	 * @return
	 */
	public List<SystemRegion> findProvinceList() {
		return SystemRegion.dao.find("select * from system_region where region_level = ?", RegionLevelEnum.PROVINCE.getValue());
	}
	
	public List<SystemRegion> findCityList(Integer parentId) {
		return SystemRegion.dao.find("select * from system_region where region_level = ? and parent_id = ?", RegionLevelEnum.CITY.getValue(), parentId);
	}
	public List<SystemRegion> findCityList(String parentCode) {
		SystemRegion systemRegion = findRegion(parentCode);
		return findCityList(systemRegion.getId());
	}
	
	public List<SystemRegion> findCountyList(Integer parentId) {
		return SystemRegion.dao.find("select * from system_region where region_level = ? and parent_id = ?", RegionLevelEnum.COUNTY.getValue(), parentId);
	}
	
	public List<SystemRegion> findCountyList(String parentCode) {
		SystemRegion systemRegion = findRegion(parentCode);
		return findCountyList(systemRegion.getId());
	}
	
	public List<SystemRegion> getCityList() {
		return findCityList(getId());
	}
	
	public List<SystemRegion> getCountyList() {
		return findCountyList(getId());
	}

	public SystemRegion findRegionByName(String regionName) {
		return SystemRegion.dao.findFirst("select * from system_region where region_name = ? limit 1", regionName);
	}

	
	
}

