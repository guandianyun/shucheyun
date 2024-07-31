package com.bytechainx.digauto.web.web.controller.base;

import org.apache.commons.lang.StringUtils;

import com.bytechainx.digauto.common.CommonConstant;
import com.bytechainx.digauto.common.EnumConstant.DataStatusEnum;
import com.bytechainx.digauto.common.Permissions;
import com.bytechainx.digauto.common.dto.ConditionFilter;
import com.bytechainx.digauto.common.dto.ConditionFilter.Operator;
import com.bytechainx.digauto.common.dto.UserSession;
import com.bytechainx.digauto.common.model.TenantAdmin;
import com.bytechainx.digauto.common.model.TenantOrg;
import com.bytechainx.digauto.common.model.TenantStore;
import com.bytechainx.digauto.common.service.setting.TenantStoreService;
import com.jfinal.aop.Aop;
import com.jfinal.core.Controller;
import com.jfinal.core.NotAction;
import com.jfinal.core.paragetter.JsonRequest;
import com.jfinal.kit.Kv;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.ehcache.CacheKit;
import com.jfinal.plugin.redis.Cache;
import com.jfinal.plugin.redis.Redis;

/**
 * @author defier
 *
 */
public abstract class BaseController extends Controller {
	
	public int pageSize = 20;
	public final int maxPageSize = 100; // 每页最大显示数量
	

	@NotAction
	public TenantOrg getCurrentTenant() {
		return getTenantOrg(getTenantOrgId());
	}
	
	@NotAction
	protected TenantOrg getTenantOrg(Integer id) {
		TenantOrg tenantOrg = TenantOrg.dao.findFirstByCache(CommonConstant.CACHE_NAME_ONE_MINUTE_STORE, "tenant.org.id."+id, "select * from tenant_org where id=?", id);
		if(tenantOrg == null) {
			throw new IllegalArgumentException("tenant org is null!!!!!!!!!!!!!!");
		}
		return tenantOrg;
	}
	
	@NotAction
	public Integer getTenantOrgId() {
		return getSessionAttr(CommonConstant.SESSION_TENANT_ID);
	}
	
	@NotAction
	public TenantAdmin getCurrentAdmin() {
		Integer adminId = getAdminId();
		return TenantAdmin.dao.findFirstByCache(CommonConstant.CACHE_NAME_ONE_MINUTE_STORE, "tenant.admin.id."+adminId, "select * from tenant_admin where id = ?", adminId);
	}
	
	@NotAction
	public UserSession getUserSession() {
		return getSessionAttr(CommonConstant.SESSION_ID_KEY);
	}
	
	@NotAction
	public Integer getAdminId() {
		Object session = getSessionAttr(CommonConstant.SESSION_ID_KEY);
		if(session == null) {
			return null;
		}
		return ((UserSession)session).getTenantAdminId();
	}
	
	@NotAction
	protected TenantStore getCurrentStore() {
		Integer storeId = getTenantStoreId();
		TenantStore tenantStore = TenantStore.dao.findFirstByCache(CommonConstant.CACHE_NAME_FIVE_MINUTE_STORE, "tenant.store.id."+storeId, "select * from tenant_store where id = ?", storeId);
		if(tenantStore == null) {
			throw new IllegalArgumentException("tenant store is null!!!!!!!!!!!!!!");
		}
		return tenantStore;
	}
	
	@NotAction
	public Integer getTenantStoreId() {
		return getSessionAttr(CommonConstant.SESSION_STORE_ID);
	}
	
	@NotAction
	public Cache getRedis() {
		return Redis.use();
	}
	
	@NotAction
	@Override
	public <T> T getSessionAttr(String key) {
		String sessionId = getCookie("JSESSIONID");
		if(StringUtils.isEmpty(sessionId)) {
			return null;
		}
		T result = CacheKit.get(CommonConstant.CACHE_NAME_SESSION_MEM_STORE, sessionId+"_"+key);
		if(result == null) {
			result = super.getSessionAttr(key);
			if(result != null) {
				CacheKit.put(CommonConstant.CACHE_NAME_SESSION_MEM_STORE, sessionId+"_"+key, result);
			}
		}
		return result;
	}
	
	@NotAction
	@Override
	public Controller setSessionAttr(String key, Object value) {
		super.setSessionAttr(key, value);
		String sessionId = getCookie("JSESSIONID");
		if(StringUtils.isNotEmpty(sessionId)) {
			CacheKit.put(CommonConstant.CACHE_NAME_SESSION_MEM_STORE, sessionId+"_"+key, value);
		}
		return this;
	}
	@NotAction
	@Override
	public Controller removeSessionAttr(String key) {
		super.removeSessionAttr(key);
		String sessionId = getCookie("JSESSIONID");
		if(StringUtils.isNotEmpty(sessionId)) {
			CacheKit.remove(CommonConstant.CACHE_NAME_SESSION_MEM_STORE, sessionId+"_"+key);
		}
		return this;
	}
	
	@NotAction
	public void removeOnlineSession(String loginClient, Integer adminId) {
		Cache redis = Redis.use();
		String adminSessionId = redis.get(loginClient+adminId);
		if(StringUtils.isNotEmpty(adminSessionId)) {
			redis.del(adminSessionId);
			redis.del(loginClient+adminId);
		}
		CacheKit.remove(CommonConstant.CACHE_NAME_SESSION_MEM_STORE, adminSessionId+"_"+CommonConstant.SESSION_ID_KEY);
	}
	
	@NotAction
	public void addOnlineSession(String loginClient, Integer adminId, String adminSessionId) {
		Cache redis = Redis.use();
		redis.set(loginClient+adminId, adminSessionId);
		redis.expire(loginClient+adminId, 1800);
	}
	
	/**
	 * 判断是否为 json 请求，Accept 包含 "json" 被认定为 json 请求
	 * 
	 */
	@NotAction
	@Override
	public boolean isJsonRequest() {
		if (getRequest() instanceof JsonRequest) {
			return true;
		}
		String accept = getRequest().getHeader("Accept");
		return accept != null && accept.contains("json");
	}
	
	/**
	 * 判断是否ajax html请求
	 * @return
	 */
	@NotAction
	public boolean isAjaxHtmlRequest() {
		String accept = getRequest().getHeader("Accept");
		String xrw = getRequest().getHeader("X-Requested-With");
		return accept != null && accept.contains("text/html") && xrw != null && xrw.contains("XMLHttpRequest");
	}
	
	@NotAction
	protected int getPageSize() {
		pageSize = getInt("pageSize", pageSize);
		if(pageSize > maxPageSize) {
			return maxPageSize;
		}
		return pageSize;
	}
	
	/**
	 * 添加门店查询条件
	 * @param storeId
	 * @param condKv
	 */
	protected void conditionFilterStore(Kv condKv, Permissions permission) {
		Integer storeId = getTenantStoreId();
		condKv.set("tenant_store_id", storeId);
		if(permission == null) {
			return;
		}
		UserSession session = getUserSession();
		if((permission.name().equals(Permissions.sale_sale.name()) && session.hasOper(Permissions.sensitiveData_order_showSaleOrder)) 
				|| (permission.name().equals(Permissions.inventory_purchase.name()) && session.hasOper(Permissions.sensitiveData_order_showPurchaseOrder))
				|| (permission.name().equals(Permissions.fund_book.name()) && session.hasOper(Permissions.sensitiveData_order_showFundOrder))
				|| (permission.name().equals(Permissions.fund_flow.name()) && session.hasOper(Permissions.sensitiveData_order_showFlowOrder))
				|| (permission.name().equals(Permissions.inventory_stock.name()) && session.hasOper(Permissions.sensitiveData_order_showStockOrder))
				) { // 具有查看他人单据权限
			
			Integer handlerId = getInt("handler_id");
			if(handlerId != null && handlerId > 0) {
				condKv.set("handler_id", handlerId);
			}
			
		} else { // 没有查看他人单据权限，只查询自己的
			TenantAdmin currentAdmin = getCurrentAdmin();
			ConditionFilter filter = new ConditionFilter();
			filter.setOperator(Operator.more);
			filter.setValue(currentAdmin.getId());
			condKv.set("(handler_id = ? or make_man_id = ?)", filter);
		}
		
	}
	
	/**
	 * 查询门店
	 */
	protected void loadStoreList() {
		Kv condKv = Kv.create();
		condKv.set("data_status", DataStatusEnum.enable.getValue());
		
		TenantAdmin currentAdmin = getCurrentAdmin();
		if(!currentAdmin.isHasAllStore()) { // 0表示拥有全部门店权限
			ConditionFilter filter = new ConditionFilter();
			filter.setOperator(Operator.in);
			filter.setValue(currentAdmin.getTenantStoreId());
			condKv.set("id", filter);
		}
		TenantStoreService storeService = Aop.get(TenantStoreService.class);
		Page<TenantStore> storePage = storeService.paginate(getTenantOrgId(), condKv, 1, maxPageSize);
		setAttr("storePage", storePage);
	}
	
}
