/**
 * 
 */
package com.bytechainx.digauto.web.web.controller;

import java.util.List;

import com.bytechainx.digauto.common.CommonConstant;
import com.bytechainx.digauto.common.Modules;
import com.bytechainx.digauto.common.model.TenantAdmin;
import com.bytechainx.digauto.common.model.TenantModule;
import com.bytechainx.digauto.common.model.TenantStore;
import com.bytechainx.digauto.web.web.controller.base.BaseController;
import com.jfinal.core.Path;

/**
 * 十字路口，要选择哪个入口
 * @author defier
 */
@Path("/roadMap")
public class RoadMapController extends BaseController {

	public void index() {
		Boolean isOpenModuleMultipleStore = TenantModule.dao.isOpen(getTenantOrgId(), Modules.multiple_store); // 是否开通多门店
		if(!isOpenModuleMultipleStore) {
			TenantStore tenantStore = TenantStore.dao.findDefaultStore(getTenantOrgId());
			setSessionAttr(CommonConstant.SESSION_STORE_ID, tenantStore.getId());
			setAttr("currentStore", tenantStore);
			
			redirect(getCurrentTenant().getDomainUrl());
			return;
		}
		TenantAdmin admin = getCurrentAdmin();
		List<TenantStore> storeList = admin.getStoreList();
		if(storeList == null || storeList.isEmpty()) { // 没有门店权限
			render("not_store.html");
			return;
		}
		if(storeList.size() > 1) { // 
			redirect("/roadMap/storeList");
			return;
		}
		// 只有有一个门店，则直接选择门店
		TenantStore store = storeList.get(0);
		setSessionAttr(CommonConstant.SESSION_STORE_ID, store.getId());
		setAttr("currentStore", store);
		
		redirect(getCurrentTenant().getDomainUrl());
	}
	
	/**
	 * 选择门店
	 */
	public void storeList() {
		TenantAdmin admin = getCurrentAdmin();
		List<TenantStore> storeList = admin.getStoreList();
		setAttr("storeList", storeList);
	}
	
	/**
	 * 选择门店
	 */
	public void selectStore() {
		Integer storeId = getInt("id");
		if(storeId == null || storeId <= 0) {
			renderError(404);
			return;
		}
		TenantStore store = TenantStore.dao.findById(getTenantOrgId(), storeId);
		if(store == null) {
			renderError(404);
			return;
		}
		setSessionAttr(CommonConstant.SESSION_STORE_ID, storeId);
		setAttr("currentStore", store);
		redirect("/dashboard/");
	}

	/**
	 * 产品到期
	 * 登录时，如果到期，则跳转到到期续费界面，续费后才能继续操作
	 */
	public void expire() {
		setAttr("accountPrice", CommonConstant.PRODUCT_ACCOUNT_PRICE);
	}
	
}
