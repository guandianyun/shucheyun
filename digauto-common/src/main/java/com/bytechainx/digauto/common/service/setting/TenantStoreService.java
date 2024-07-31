package com.bytechainx.digauto.common.service.setting;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.bytechainx.digauto.common.EnumConstant.DataStatusEnum;
import com.bytechainx.digauto.common.EnumConstant.FlagEnum;
import com.bytechainx.digauto.common.kit.PinYinUtil;
import com.bytechainx.digauto.common.model.CustomerInfo;
import com.bytechainx.digauto.common.model.TenantStore;
import com.bytechainx.digauto.common.model.TraderBalanceAccount;
import com.bytechainx.digauto.common.model.TraderBalanceStoreRef;
import com.bytechainx.digauto.common.model.TraderBookAccount;
import com.bytechainx.digauto.common.service.base.CommonService;
import com.jfinal.kit.Kv;
import com.jfinal.kit.Ret;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Page;


/**
* 门店管理
*/
public class TenantStoreService extends CommonService {


	/**
	* 分页列表
	*/
	public Page<TenantStore> paginate(Integer tenantOrgId, Kv conditionColumns, Integer pageNumber, int pageSize) {
		StringBuffer where = new StringBuffer();
		List<Object> params = new ArrayList<Object>();
		where.append("where tenant_org_id = "+tenantOrgId);
		
		conditionFilter(conditionColumns, where, params);
		
		if(conditionColumns == null || !conditionColumns.containsKey("data_status")) {
			where.append(" and data_status != ?");
			params.add(DataStatusEnum.delete.getValue());
		}
		
		return TenantStore.dao.paginate(pageNumber, pageSize, "select * ", "from tenant_store "+where.toString()+" order by id desc", params.toArray());
	}
	
	/**
	* 分页列表,按照距离排序
	 * @param longitude 
	 * @param latitude 
	*/
	public Page<TenantStore> paginateByDistance(Kv condKv, String longitude, String latitude, Integer pageNumber, int pageSize) {
		StringBuffer where = new StringBuffer();
		List<Object> params = new ArrayList<Object>();
		where.append("where 1 = 1");
		conditionFilter(condKv, where, params);
		
		return TenantStore.dao.paginate(pageNumber, pageSize, "select *, ST_Distance_Sphere(point(longitude, latitude), point("+longitude+", "+latitude+")) as distance", "from tenant_store "+where.toString()+" order by distance", params.toArray());
	}
	

	/**
	* 新增
	*/
	public Ret create(Integer tenantOrgId, TenantStore store) {
		if(tenantOrgId == null || tenantOrgId <= 0 || store == null) {
			return Ret.fail("参数错误");
		}
		if(StringUtils.isEmpty(store.getName())) {
			return Ret.fail("门店名称不能为空");
		}
		TenantStore _store = TenantStore.dao.findFirst("select * from tenant_store where tenant_org_id = ? and name = ? and data_status != ? limit 1", tenantOrgId, store.getName(), DataStatusEnum.delete.getValue());
		if(_store != null) {
			return Ret.fail("门店名称已存在");
		}
		if(StringUtils.isEmpty(store.getContract())) {
			return Ret.fail("联系人不能为空");
		}
		if(StringUtils.isEmpty(store.getMobile())) {
			return Ret.fail("手机号不能为空");
		}
		store.setCode(PinYinUtil.getFirstSpell(store.getName()));
		store.setTenantOrgId(tenantOrgId);
		if(StringUtils.isNotEmpty(store.getLongitude()) && StringUtils.isNotEmpty(store.getLatitude())) {
			store.setShowFlag(FlagEnum.YES.getValue());
		}
		store.setCreatedAt(new Date());
		store.setUpdatedAt(new Date());
		store.save();
		
		// 每创建一个门店，都要自动关联上“现金”结算帐户
		TraderBalanceAccount balanceAccount = TraderBalanceAccount.dao.findCashAccount(tenantOrgId);
		TraderBalanceStoreRef balanceStoreRef = new TraderBalanceStoreRef();
		balanceStoreRef.setTraderBalanceAccountId(balanceAccount.getId());
		balanceStoreRef.setBalance(BigDecimal.ZERO);
		balanceStoreRef.setCreatedAt(new Date());
		balanceStoreRef.setTenantOrgId(tenantOrgId);
		balanceStoreRef.setTenantStoreId(store.getId());
		balanceStoreRef.setUpdatedAt(new Date());
		balanceStoreRef.save();
		
		// 每创建一个门店，都要创建一个散客
		TraderBookAccount traderBookAccount = new TraderBookAccount();
		traderBookAccount.setCreatedAt(new Date());
		traderBookAccount.setInAmount(BigDecimal.ZERO);
		traderBookAccount.setOpenBalance(BigDecimal.ZERO);
		traderBookAccount.setOutAmount(BigDecimal.ZERO);
		traderBookAccount.setPayAmount(BigDecimal.ZERO);
		traderBookAccount.setTenantOrgId(tenantOrgId);
		traderBookAccount.setUpdatedAt(new Date());
		traderBookAccount.save();
		CustomerInfo customer = new CustomerInfo();
		customer.setCreatedAt(new Date());
		customer.setDefaultFlag(FlagEnum.YES.getValue());
		customer.setTenantOrgId(tenantOrgId);
		customer.setTenantStoreId(store.getId());
		customer.setName("散客");
		customer.setCode(PinYinUtil.getFirstSpell(customer.getName()));
		customer.setUpdatedAt(new Date());
		customer.setDataStatus(DataStatusEnum.enable.getValue());
		customer.setMobile("88888888888");
		customer.setTraderBookAccountId(traderBookAccount.getId());
		customer.setCustomerCategoryId(0);
		customer.save();
		
		return Ret.ok("新增门店成功").set("targetId", store.getId());
	}


	/**
	* 修改
	*/
	public Ret update(Integer tenantOrgId, TenantStore store) {
		if(tenantOrgId == null || tenantOrgId <= 0 || store == null || store.getId() == null || store.getId() <= 0) {
			return Ret.fail("参数错误");
		}
		if(StringUtils.isEmpty(store.getName())) {
			return Ret.fail("门店名称不能为空");
		}
		TenantStore _store = TenantStore.dao.findFirst("select * from tenant_store where tenant_org_id = ? and name = ? and data_status != ? limit 1", tenantOrgId, store.getName(), DataStatusEnum.delete.getValue());
		if(_store != null && _store.getId().intValue() != store.getId().intValue()) {
			return Ret.fail("门店名称已存在");
		}
		if(StringUtils.isEmpty(store.getContract())) {
			return Ret.fail("联系人不能为空");
		}
		if(StringUtils.isEmpty(store.getMobile())) {
			return Ret.fail("手机号不能为空");
		}
		_store = TenantStore.dao.findById(tenantOrgId, store.getId());
		if(_store == null) {
			return Ret.fail("门店不存在，无法修改");
		}
		store.setCode(PinYinUtil.getFirstSpell(store.getName()));
		store.setTenantOrgId(tenantOrgId);
		if(StringUtils.isNotEmpty(store.getLongitude()) && StringUtils.isNotEmpty(store.getLatitude())) {
			store.setShowFlag(FlagEnum.YES.getValue());
		}
		store.setUpdatedAt(new Date());
		store.update();
		
		return Ret.ok("修改门店成功");
	}


	/**
	* 删除
	*/
	
	public Ret delete(Integer tenantOrgId, List<Integer> ids) {
		if(tenantOrgId == null || tenantOrgId <= 0 || ids == null || ids.isEmpty()) {
			return Ret.fail("参数错误");
		}
		StringBuffer resp = new StringBuffer();
		for (Integer id : ids) {
			TenantStore store = TenantStore.dao.findById(tenantOrgId, id);
			if(store == null) {
				continue;
			}
			if(store.getDefaultFlag()) {
				continue;
			}
			int orderCount = Db.queryInt("select count(*) from sale_order where tenant_org_id = ? and tenant_store_id = ? limit 1", tenantOrgId, store.getId());
			if(orderCount > 0) {
				resp.append("门店["+store.getName()+"]已使用,不能删除\r\n");
				continue;
			}
			store.setDataStatus(DataStatusEnum.delete.getValue());
			store.setUpdatedAt(new Date());
			store.update();
			
			TraderBalanceAccount balanceAccount = TraderBalanceAccount.dao.findCashAccount(tenantOrgId);
			Db.delete("delete from trader_balance_store_ref where tenant_org_id = ? and trader_balance_account_id = ? and tenant_store_id = ?", tenantOrgId, balanceAccount.getId(), store.getId());
			
		}
		if(resp.length() > 0) {
			return Ret.fail(resp.toString());
		}
		return Ret.ok("删除门店成功");
	}
	
	/**
	* 停用
	*/
	public Ret disable(Integer tenantOrgId, List<Integer> ids) {
		if(tenantOrgId == null || tenantOrgId <= 0 || ids == null || ids.isEmpty()) {
			return Ret.fail("参数错误");
		}
		for (Integer id : ids) {
			TenantStore store = TenantStore.dao.findById(tenantOrgId, id);
			if(store == null) {
				continue;
			}
			store.setDataStatus(DataStatusEnum.disable.getValue());
			store.setUpdatedAt(new Date());
			store.update();
		}
		return Ret.ok("停用门店成功");
	}

	
	/**
	* 启用
	*/
	public Ret enable(Integer tenantOrgId, List<Integer> ids) {
		if(tenantOrgId == null || tenantOrgId <= 0 || ids == null || ids.isEmpty()) {
			return Ret.fail("参数错误");
		}
		for (Integer id : ids) {
			TenantStore store = TenantStore.dao.findById(tenantOrgId, id);
			if(store == null) {
				continue;
			}
			store.setDataStatus(DataStatusEnum.enable.getValue());
			store.setUpdatedAt(new Date());
			store.update();
		}
		return Ret.ok("启用门店成功");
	}

}