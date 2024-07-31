package com.bytechainx.digauto.web.web.controller.setting;


import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.bytechainx.digauto.common.EnumConstant.DataStatusEnum;
import com.bytechainx.digauto.common.Permissions;
import com.bytechainx.digauto.common.annotation.Permission;
import com.bytechainx.digauto.common.dto.ConditionFilter;
import com.bytechainx.digauto.common.dto.ConditionFilter.Operator;
import com.bytechainx.digauto.common.kit.AmapApiKit;
import com.bytechainx.digauto.common.model.TenantAdmin;
import com.bytechainx.digauto.common.model.TenantStore;
import com.bytechainx.digauto.common.service.setting.TenantStoreService;
import com.bytechainx.digauto.web.web.controller.base.BaseController;
import com.jfinal.aop.Before;
import com.jfinal.aop.Inject;
import com.jfinal.core.Path;
import com.jfinal.kit.Kv;
import com.jfinal.kit.Ret;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.activerecord.tx.Tx;


/**
* 门店管理
*/
@Path("/setting/tenant/store")
public class TenantStoreController extends BaseController {

	@Inject
	private TenantStoreService storeService;

	/**
	* 首页
	*/
	@Permission(Permissions.setting_tenant_store)
	public void index() {
		setAttr("hideStopFlag", true);
	}

	/**
	* 列表
	*/
	@Permission(Permissions.setting_tenant_store_show)
	public void list() {
		int pageNumber = getInt("pageNumber", 1);
		Boolean hideStopFlag = getBoolean("hide_stop_flag", true); // 隐藏停用
		Kv condKv = Kv.create();
		if(hideStopFlag) {
			condKv.set("data_status", DataStatusEnum.enable.getValue());
		}
		Page<TenantStore> page = storeService.paginate(getTenantOrgId(), condKv, pageNumber, pageSize);
		setAttr("page", page);

	}

	/**
	* 查看
	*/
	@Permission(Permissions.setting_tenant_store_show)
	public void show() {

	}

	/**
	* 添加
	*/
	@Permission(Permissions.setting_tenant_store_create)
	public void add() {

	}

	/**
	* 修改
	*/
	@Permission(Permissions.setting_tenant_store_create)
	@Before(Tx.class)
	public void create() {
		TenantStore store = getModel(TenantStore.class, "", true);
		Ret ret = storeService.create(getTenantOrgId(), store);
		renderJson(ret);
	}



	/**
	* 编辑
	*/
	@Permission(Permissions.setting_tenant_store_update)
	public void edit() {
		Integer id = getInt("id");
		if(id == null || id <= 0) {
			renderError(404);
			return;
		}
		TenantStore store = TenantStore.dao.findById(getTenantOrgId(), id);
		if(store == null) {
			renderError(404);
			return;
		}
		setAttr("tenantStore", store);

	}

	/**
	* 修改
	*/
	@Permission(Permissions.setting_tenant_store_update)
	@Before(Tx.class)
	public void update() {
		TenantStore store = getModel(TenantStore.class, "", true);
		Ret ret = storeService.update(getTenantOrgId(), store);
		renderJson(ret);
	}



	/**
	* 修改
	*/
	@Permission(Permissions.setting_tenant_store_delete)
	@Before(Tx.class)
	public void delete() {
		Integer id = getInt("id");
		if(id == null || id <= 0) {
			renderJson(Ret.fail("ID不能为空"));
			return;
		}
		Ret ret = storeService.delete(getTenantOrgId(), Arrays.asList(id));
		renderJson(ret);
	}

	/**
	* 停用
	*/
	@Permission(Permissions.setting_tenant_store_disable)
	@Before(Tx.class)
	public void disable() {
		Integer id = getInt("id");
		if(id == null || id <= 0) {
			renderJson(Ret.fail("ID不能为空"));
			return;
		}
		Ret ret = storeService.disable(getTenantOrgId(), Arrays.asList(id));

		renderJson(ret);
	}
	
	/**
	* 启用
	*/
	@Permission(Permissions.setting_tenant_store_disable)
	@Before(Tx.class)
	public void enable() {
		Integer id = getInt("id");
		if(id == null || id <= 0) {
			renderJson(Ret.fail("ID不能为空"));
			return;
		}
		Ret ret = storeService.enable(getTenantOrgId(), Arrays.asList(id));

		renderJson(ret);
	}
	
	
	public void listByJson() {
		int pageNumber = getInt("pageNumber", 1);
		String keyword = get("keyword");
		Kv condKv = Kv.create();
		condKv.set("data_status", DataStatusEnum.enable.getValue());
		
		TenantAdmin currentAdmin = getCurrentAdmin();
		if(!currentAdmin.isHasAllStore()) { // 0表示拥有全部门店权限
			ConditionFilter filter = new ConditionFilter();
			filter.setOperator(Operator.in);
			filter.setValue(currentAdmin.getTenantStoreId());
			condKv.set("id", filter);
		}
		
		condKv.set("name,code", keyword); // 多字段模糊查询
		Page<TenantStore> page = storeService.paginate(getTenantOrgId(), condKv, pageNumber, maxPageSize);
		for (TenantStore store : page.getList()) {
			store.remove("mobile");
		}
		TenantStore store = new TenantStore();
		store.setId(0);
		store.setName("非门店");
		store.setCode("fmd");
		page.getList().add(store);
		
		renderJson(Ret.ok().set("data", page.getList()));
	}
	
	
	/**
	 * 搜索地址
	 */
	public void searchMapAddress() {
		String city = get("city", "0797");
		String keyword = get("keyword");
		
		Ret ret = AmapApiKit.searchPoi(keyword, city, null, null);
		
		setAttr("list", ret.get("data"));
		keepPara("keyword");
	}
	
	

	/**
	 * 搜索周边附近POI
	 */
	@SuppressWarnings("unchecked")
	public void searchMapAround() {
		String location = get("location");
		String address = get("address");
		if(StringUtils.isEmpty(location)) {
			return;
		}
		Ret ret = AmapApiKit.searchAround(location.split(",")[0], location.split(",")[1]);
		
		List<Map<String, String>> dataList = (List<Map<String, String>>) ret.get("data");
		if(StringUtils.isNotEmpty(address)) {
			address = address.replace("江西省赣州市", "");
		}
		Map<String, String> map = new HashMap<>();
		map.put("name", address);
		map.put("location", location);
		map.put("address", address);
		
		dataList.add(0, map);
		
		setAttr("list", dataList);
	}
	
}