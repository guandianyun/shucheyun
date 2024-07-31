/**
 * 
 */
package com.bytechainx.digauto.web.web.controller.setting;

import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.bytechainx.digauto.common.model.SystemRegion;
import com.bytechainx.digauto.web.web.controller.base.BaseController;
import com.jfinal.core.Path;

/**
 * 地区
 * @author defier
 */
@Path("/system/region")
public class SystemRegionController extends BaseController {

	/**
	 * 查询城市
	 */
	public void cityList() {
		String parentCode = get("parent_code");
		if(StringUtils.isEmpty(parentCode)) {
			renderError(404);
			return;
		}
		List<SystemRegion> cityList = SystemRegion.dao.findCityList(parentCode);
		
		setAttr("cityList", cityList);
	}
	
	/**
	 * 查询区县
	 */
	public void countyList() {
		String parentCode = get("parent_code");
		if(StringUtils.isEmpty(parentCode)) {
			renderError(404);
			return;
		}
		List<SystemRegion> countyList = SystemRegion.dao.findCountyList(parentCode);
		
		setAttr("countyList", countyList);
	}

}
