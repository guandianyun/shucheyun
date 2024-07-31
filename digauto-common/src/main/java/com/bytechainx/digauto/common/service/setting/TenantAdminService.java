package com.bytechainx.digauto.common.service.setting;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.bytechainx.digauto.common.Modules;
import com.bytechainx.digauto.common.EnumConstant.UserActiveStatusEnum;
import com.bytechainx.digauto.common.kit.CipherkeyUtil;
import com.bytechainx.digauto.common.kit.PinYinUtil;
import com.bytechainx.digauto.common.kit.RandomUtil;
import com.bytechainx.digauto.common.kit.SmsKit;
import com.bytechainx.digauto.common.model.TenantAccount;
import com.bytechainx.digauto.common.model.TenantAdmin;
import com.bytechainx.digauto.common.model.TenantModule;
import com.bytechainx.digauto.common.model.TenantStore;
import com.bytechainx.digauto.common.service.base.CommonService;
import com.jfinal.kit.Kv;
import com.jfinal.kit.Ret;
import com.jfinal.plugin.activerecord.Page;


/**
* 员工管理
*/
public class TenantAdminService extends CommonService {

	/**
	* 分页列表
	*/
	public Page<TenantAdmin> paginate(Integer tenantOrgId, Kv conditionColumns, Integer pageNumber, int pageSize) {
		StringBuffer where = new StringBuffer();
		List<Object> params = new ArrayList<Object>();
		where.append("where tenant_org_id = "+tenantOrgId);

		conditionFilter(conditionColumns, where, params);
		
		return TenantAdmin.dao.paginate(pageNumber, pageSize, "select * ", "from tenant_admin "+where.toString()+" order by id desc", params.toArray());
	}

	/**
	* 新增
	*/
	public Ret create(Integer tenantOrgId, TenantAdmin admin) {
		if(tenantOrgId == null || tenantOrgId <= 0 || admin == null) {
			return Ret.fail("参数错误");
		}
		if(StringUtils.isEmpty(admin.getRealName())) {
			return Ret.fail("员工名称不能为空");
		}
		if(StringUtils.isEmpty(admin.getMobile())) {
			return Ret.fail("手机号不能为空");
		}
		if(admin.getRoleId() == null || admin.getRoleId() <= 0) {
			return Ret.fail("角色不能为空");
		}
		TenantAdmin _admin = TenantAdmin.dao.findBy(tenantOrgId, admin.getMobile());
		if(_admin != null) {
			return Ret.fail("手机号已存在");
		}
		Boolean isOpenModuleMultipleStore = TenantModule.dao.isOpen(tenantOrgId, Modules.multiple_store); // 是否开通多门店
		if(isOpenModuleMultipleStore && StringUtils.isEmpty(admin.getTenantStoreId())) { // 开通了多门店
			return Ret.fail("所属门店不能为空");
		}
		if(admin.getLoginFlag()) { // 可登录帐户
			TenantAccount tenantAccount = TenantAccount.dao.findBy(tenantOrgId);
			int count = TenantAdmin.dao.countBy(tenantOrgId); // 总用户数
			if(tenantAccount.getAccountCount() <= count) {
				return Ret.fail("可新增登录员工数不足，请联系客服增购");
			}
		}
		
		if(!isOpenModuleMultipleStore) { // 未开通多门店模块
			TenantStore tenantStore = TenantStore.dao.findDefaultStore(tenantOrgId); // 获取默认门店
			admin.setTenantStoreId(tenantStore.getId()+"");
		}
		Boolean isOpenModuleMultipleWarehouse = TenantModule.dao.isOpen(tenantOrgId, Modules.multiple_warehouse); // 是否开通多仓库
		if(!isOpenModuleMultipleWarehouse) {
			admin.setInventoryWarehouseId("0");
		}
		admin.setEncrypt(CipherkeyUtil.encodeSalt(RandomUtil.genRandomNum(10)));
		String randomPwd = RandomUtil.getRandomNum(4);
		String encodePassword = CipherkeyUtil.encodePassword(randomPwd, admin.getEncrypt());
		admin.setPassword(encodePassword);
		admin.setActiveStatus(UserActiveStatusEnum.waiting.getValue());
		admin.setCode(PinYinUtil.getFirstSpell(admin.getRealName()));
		admin.setTenantOrgId(tenantOrgId);
		admin.setCreatedAt(new Date());
		admin.setUpdatedAt(new Date());
		admin.save();
		if(admin.getLoginFlag()) { // 可登录帐户
			// 发送激活短信
			SmsKit.sendAdminActive(tenantOrgId, admin, randomPwd);
		}
		
		return Ret.ok("新增员工成功").set("targetId", admin.getId());
	}


	/**
	* 修改
	*/
	public Ret update(Integer tenantOrgId, TenantAdmin admin) {
		if(tenantOrgId == null || tenantOrgId <= 0 || admin == null || admin.getId() == null || admin.getId() <= 0) {
			return Ret.fail("参数错误");
		}
		if(StringUtils.isEmpty(admin.getRealName())) {
			return Ret.fail("员工名称不能为空");
		}
		if(StringUtils.isEmpty(admin.getMobile())) {
			return Ret.fail("手机号不能为空");
		}
		TenantAdmin _admin = TenantAdmin.dao.findBy(tenantOrgId, admin.getMobile());
		if(_admin != null && _admin.getId().intValue() != admin.getId().intValue()) {
			return Ret.fail("手机号已存在");
		}
		TenantAdmin tenantAdmin = TenantAdmin.dao.findById(tenantOrgId, admin.getId());
		if(tenantAdmin == null) {
			return Ret.fail("帐户不存在，无法修改");
		}
		Boolean isOpenModuleMultipleStore = TenantModule.dao.isOpen(tenantOrgId, Modules.multiple_store); // 是否开通多门店
		if(!tenantAdmin.getMainAccountFlag()) {
			if(admin.getRoleId() == null || admin.getRoleId() <= 0) {
				return Ret.fail("角色不能为空");
			}
			if(isOpenModuleMultipleStore && StringUtils.isEmpty(admin.getTenantStoreId())) { // 开通了多门店
				return Ret.fail("所属门店不能为空");
			}
		}
		
		if(!tenantAdmin.isSuperAdmin() && admin.getLoginFlag() && !tenantAdmin.getLoginFlag()) { // 修改成可登录帐户，要判断是否可以新增
			TenantAccount tenantAccount = TenantAccount.dao.findBy(tenantOrgId);
			int count = TenantAdmin.dao.countBy(tenantOrgId); // 总用户数
			if(tenantAccount.getAccountCount() <= count) {
				return Ret.fail("可新增登录员工数不足，请联系客服增购");
			}
		}
		
		if(StringUtils.isNotEmpty(admin.getPassword())) {
			String encodePassword = CipherkeyUtil.encodePassword(admin.getPassword(), tenantAdmin.getEncrypt());
			admin.setPassword(encodePassword);
		} else {
			admin.remove("password"); // 不修改密码
		}
		if(!isOpenModuleMultipleStore || tenantAdmin.isSuperAdmin()) { // 未开通模块
			if(StringUtils.isEmpty(admin.getTenantStoreId())) {
				admin.setTenantStoreId("0");
			}
		}
		Boolean isOpenModuleMultipleWarehouse = TenantModule.dao.isOpen(tenantOrgId, Modules.multiple_warehouse); // 是否开通多仓库
		if(!isOpenModuleMultipleWarehouse || tenantAdmin.isSuperAdmin()) {
			if(StringUtils.isEmpty(admin.getInventoryWarehouseId())) {
				admin.setInventoryWarehouseId("0");
			}
		}
		admin.setTenantOrgId(tenantOrgId);
		admin.setUpdatedAt(new Date());
		admin.update();
		
		if(tenantAdmin.getActiveStatus() == UserActiveStatusEnum.waiting.getValue() && admin.getLoginFlag()) { // 如果是待激活用户和可登录帐户，则发送激活短信
			String randomPwd = RandomUtil.getRandomNum(4);
			String encodePassword = CipherkeyUtil.encodePassword(randomPwd, tenantAdmin.getEncrypt());
			admin.setPassword(encodePassword);
			admin.update();
			
			SmsKit.sendAdminActive(tenantOrgId, admin, randomPwd);
		}
		
		return Ret.ok("员工修改成功");
	}

	/**
	 * 修改密码
	 * @param tenantOrgId
	 * @param adminId
	 * @param oldPassword
	 * @param password
	 * @return
	 */
	public Ret updatePwd(Integer tenantOrgId, Integer adminId, String oldPassword, String password) {
		if(tenantOrgId == null || tenantOrgId <= 0 || adminId == null) {
			return Ret.fail("参数错误");
		}
		if(StringUtils.isEmpty(oldPassword)) {
			return Ret.fail("原密码不能为空");
		}
		if(StringUtils.isEmpty(password)) {
			return Ret.fail("新密码不能为空");
		}
		TenantAdmin admin = TenantAdmin.dao.findById(tenantOrgId, adminId);
		if(admin == null) {
			return Ret.fail("用户不存在");
		}
		String encodeOldPassword = CipherkeyUtil.encodePassword(oldPassword, admin.getEncrypt());
		if(!StringUtils.equals(admin.getPassword(), encodeOldPassword)) {
			return Ret.fail("原密码不正确");
		}
		
		String encodePassword = CipherkeyUtil.encodePassword(password, admin.getEncrypt());
		admin.setPassword(encodePassword);
		admin.setUpdatedAt(new Date());
		admin.update();
		
		return Ret.ok("修改密码成功");
	}

	/**
	* 停用
	*/
	public Ret disable(Integer tenantOrgId, List<Integer> ids) {
		if(tenantOrgId == null || tenantOrgId <= 0 || ids == null || ids.isEmpty()) {
			return Ret.fail("参数错误");
		}
		for (Integer id : ids) {
			TenantAdmin admin = TenantAdmin.dao.findById(tenantOrgId, id);
			if(admin == null) {
				continue;
			}
			admin.setActiveStatus(UserActiveStatusEnum.disable.getValue());
			admin.setUpdatedAt(new Date());
			admin.update();
		}
		return Ret.ok("禁用成功");
	}

	
	/**
	* 启用
	*/
	public Ret enable(Integer tenantOrgId, List<Integer> ids) {
		if(tenantOrgId == null || tenantOrgId <= 0 || ids == null || ids.isEmpty()) {
			return Ret.fail("参数错误");
		}
		for (Integer id : ids) {
			TenantAdmin admin = TenantAdmin.dao.findById(tenantOrgId, id);
			if(admin == null) {
				continue;
			}
			admin.setActiveStatus(UserActiveStatusEnum.enable.getValue());
			admin.setUpdatedAt(new Date());
			admin.update();
		}
		return Ret.ok("启用成功");
	}

}