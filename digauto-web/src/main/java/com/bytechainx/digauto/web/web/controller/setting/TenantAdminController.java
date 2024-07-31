package com.bytechainx.digauto.web.web.controller.setting;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.bytechainx.digauto.common.EnumConstant.DataStatusEnum;
import com.bytechainx.digauto.common.EnumConstant.UserActiveStatusEnum;
import com.bytechainx.digauto.common.Permissions;
import com.bytechainx.digauto.common.annotation.Permission;
import com.bytechainx.digauto.common.dto.ConditionFilter;
import com.bytechainx.digauto.common.dto.ConditionFilter.Operator;
import com.bytechainx.digauto.common.model.TenantAccount;
import com.bytechainx.digauto.common.model.TenantAdmin;
import com.bytechainx.digauto.common.service.setting.TenantAdminService;
import com.bytechainx.digauto.common.service.setting.TenantRoleService;
import com.bytechainx.digauto.common.service.setting.TenantStoreService;
import com.bytechainx.digauto.purchase.service.StockWarehouseService;
import com.bytechainx.digauto.web.web.controller.base.BaseController;
import com.jfinal.aop.Before;
import com.jfinal.aop.Inject;
import com.jfinal.core.Path;
import com.jfinal.kit.Kv;
import com.jfinal.kit.Ret;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.activerecord.tx.Tx;


/**
* 员工管理
*/
@Path("/setting/tenant/admin")
public class TenantAdminController extends BaseController {
	
	@Inject
	private TenantAdminService adminService;
	@Inject
	private TenantRoleService roleService;
	@Inject
	private TenantStoreService storeService;
	@Inject
	private StockWarehouseService warehouseService;

	/**
	* 首页
	*/
	@Permission(Permissions.setting_tenant_admin)
	public void index() {
		setAttr("hideStopFlag", true);
	}

	/**
	* 列表
	*/
	@Permission(Permissions.setting_tenant_admin)
	public void list() {
		int pageNumber = getInt("pageNumber", 1);
		Boolean hideStopFlag = getBoolean("hide_stop_flag", true); // 隐藏停用
		Kv condKv = Kv.create();
		if(hideStopFlag) {
			ConditionFilter filter = new ConditionFilter();
			filter.setOperator(Operator.neq);
			filter.setValue(UserActiveStatusEnum.disable.getValue());
			condKv.set("active_status", filter);
		}
		Page<TenantAdmin> page = adminService.paginate(getTenantOrgId(), condKv, pageNumber, pageSize);
		setAttr("page", page);
	}
	
	/**
	* 统计用户数
	*/
	@Permission(Permissions.setting_tenant_admin_show)
	public void stat() {
		Integer tenantOrgId = getTenantOrgId();
		int count = TenantAdmin.dao.countBy(tenantOrgId); // 总用户数
		TenantAccount account = TenantAccount.dao.findBy(tenantOrgId);
		
		renderJson(Ret.ok("count", count).set("remain", account.getAccountCount() - count));
	}
	
	

	/**
	* 查看
	*/
	@Permission(Permissions.setting_tenant_admin_show)
	public void show() {

		renderJson(Ret.ok());
	}


	/**
	* 添加
	*/
	@Permission(Permissions.setting_tenant_admin_create)
	public void add() {
		setAttrCommon();
	}

	/**
	* 修改
	*/
	@Permission(Permissions.setting_tenant_admin_create)
	@Before(Tx.class)
	public void create() {
		TenantAdmin admin = getModel(TenantAdmin.class, "", true);
		String[] tenantStoreIds = getParaValues("tenant_store_id");
		String[] inventoryWarehouseIds = getParaValues("inventory_warehouse_id");
		admin.setTenantStoreId(StringUtils.join(tenantStoreIds, ","));
		admin.setInventoryWarehouseId(StringUtils.join(inventoryWarehouseIds, ","));
		
		Ret ret = adminService.create(getTenantOrgId(), admin);
		
		renderJson(ret);
	}

	/**
	* 编辑
	*/
	@Permission(Permissions.setting_tenant_admin_update)
	public void edit() {
		Integer id = getInt("id");
		if(id == null || id <= 0) {
			renderError(404);
			return;
		}
		TenantAdmin admin = TenantAdmin.dao.findById(getTenantOrgId(), id);
		if(admin == null) {
			renderError(404);
			return;
		}
		setAttrCommon();
		admin.remove("password", "encrypt");
		setAttr("tenantAdmin", admin);
	}

	/**
	* 修改
	*/
	@Permission(Permissions.setting_tenant_admin_update)
	@Before(Tx.class)
	public void update() {
		TenantAdmin admin = getModel(TenantAdmin.class, "", true);
		String[] tenantStoreIds = getParaValues("tenant_store_id");
		String[] inventoryWarehouseIds = getParaValues("inventory_warehouse_id");
		admin.setTenantStoreId(StringUtils.join(tenantStoreIds, ","));
		admin.setInventoryWarehouseId(StringUtils.join(inventoryWarehouseIds, ","));
		
		Ret ret = adminService.update(getTenantOrgId(), admin);

		renderJson(ret);
	}



	/**
	* 停用
	*/
	@Permission(Permissions.setting_tenant_admin_disable)
	@Before(Tx.class)
	public void disable() {
		Integer id = getInt("id");
		if(id == null || id <= 0) {
			renderJson(Ret.fail("ID不能为空"));
			return;
		}
		Ret ret = adminService.disable(getTenantOrgId(), Arrays.asList(id));

		renderJson(ret);
	}
	
	/**
	* 启用
	*/
	@Permission(Permissions.setting_tenant_admin_disable)
	@Before(Tx.class)
	public void enable() {
		Integer id = getInt("id");
		if(id == null || id <= 0) {
			renderJson(Ret.fail("ID不能为空"));
			return;
		}
		Ret ret = adminService.enable(getTenantOrgId(), Arrays.asList(id));

		renderJson(ret);
	}

	/**
	 * 加载公共数据
	 */
	private void setAttrCommon() {
		Page<?> rolePage = roleService.paginate(getTenantOrgId(), 1, maxPageSize);
		Page<?> storePage = storeService.paginate(getTenantOrgId(), Kv.by("data_status", DataStatusEnum.enable.getValue()), 1, maxPageSize);
		Page<?> warehousePage = warehouseService.paginate(getTenantOrgId(), null, 1, maxPageSize);
		
		setAttr("roleList", rolePage.getList());
		setAttr("storeList", storePage.getList());
		setAttr("warehouseList", warehousePage.getList());
	}
	
	/**
	 * 经手人列表
	 */
	@Permission({Permissions.inventory_purchase, Permissions.inventory_stock, Permissions.fund, Permissions.sale})
	public void listByJson() {
		int pageNumber = getInt("pageNumber", 1);
		String keyword = get("keyword");
		Kv condKv = Kv.create();
		conditionFilterStore(condKv, null);
		condKv.set("active_status", UserActiveStatusEnum.enable.getValue());
		condKv.set("real_name,code", keyword); // 多字段模糊查询
		TenantAdmin currentAdmin = getCurrentAdmin();
		String[] tenantStoreIds = StringUtils.split(currentAdmin.getTenantStoreId(), ",");
		
		List<TenantAdmin> adminList = new ArrayList<>();
		Page<TenantAdmin> page = adminService.paginate(getTenantOrgId(), condKv, pageNumber, maxPageSize);
		for (TenantAdmin admin : page.getList()) {
			// 当前用户是主帐号，或者遍历员工是主帐号，或者遍历员工有所有门店权限
			if(currentAdmin.getMainAccountFlag() || admin.getMainAccountFlag() || admin.isHasAllStore()) {
				adminList.add(admin);
				continue;
			}
			for (String storeId : tenantStoreIds) {
				if(admin.isHasStore(Integer.parseInt(storeId))) { // 与当前用户拥有相同门店，才能显示
					adminList.add(admin);
					break;
				}
			}
		}
		TenantAdmin tenantAdmin = TenantAdmin.dao.findMainAdmin(getTenantOrgId());
		adminList.add(tenantAdmin);
		page.setList(adminList);
		renderJson(Ret.ok().set("data", page.getList()));
	}
	
}