package com.bytechainx.digauto.common.service.user;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.bytechainx.digauto.common.model.UserInfo;
import com.bytechainx.digauto.common.service.base.CommonService;
import com.jfinal.kit.Kv;
import com.jfinal.kit.Ret;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.weixin.sdk.api.ApiResult;


/**
* 公众号粉丝管理
*/
public class WeixinUserService extends CommonService {

	/**
	* 分页列表
	 * @param moreCondKv 非字段条件
	*/
	public Page<UserInfo> paginate(Integer tenantOrgId, Kv conditionColumns, Integer pageNumber, int pageSize) {
		StringBuffer where = new StringBuffer();
		List<Object> params = new ArrayList<Object>();
		where.append("where tenant_org_id = "+tenantOrgId);
		
		conditionFilter(conditionColumns, where, params);
		
		return UserInfo.dao.paginate(pageNumber, pageSize, "select * ", "from user_info "+where.toString()+" order by id desc", params.toArray());
	}
	
	/**
	 * 创建微信用户
	 * @param tenantOrgId
	 * @param storeId 
	 * @param openId
	 * @param apiResult
	 * @param ip
	 * @return
	 */
	public UserInfo create(String openId, ApiResult apiResult, String ip) {
		UserInfo userInfo = new UserInfo();
		userInfo.setTenantOrgId(0);
		userInfo.setHeadImg(apiResult.getStr("headimgurl"));
		userInfo.setNickName(apiResult.getStr("nickname"));
		userInfo.setWeixinOpenid(openId);
		userInfo.setFirstLoginTime(new Date());
		userInfo.setFirstLoginIp(ip);
		userInfo.setLoginCount(1);
		userInfo.setLastLoginIp(ip);
		userInfo.setLastLoginTime(new Date()); 
		userInfo.setProvince(apiResult.getStr("province"));
		userInfo.setCity(apiResult.getStr("city"));
		userInfo.setGender(apiResult.getInt("sex"));
		userInfo.setCreatedAt(new Date());
		userInfo.setUpdatedAt(new Date());
		userInfo.save();
		
		return userInfo;
	}
	
	/**
	 * 授权登录，更新用户信息
	 * @param openId
	 * @param apiResult
	 * @param ip
	 * @return
	 */
	public Ret update(String openId, ApiResult apiResult, String ip) {
		UserInfo userInfo = UserInfo.dao.findByOpenId(openId);
		if(userInfo == null) {
			return Ret.fail("用户登录失败，需通过扫码关注公众号, openId:"+openId);
		}
		userInfo.setHeadImg(apiResult.getStr("headimgurl"));
		userInfo.setNickName(apiResult.getStr("nickname"));;
		userInfo.setUpdatedAt(new Date());
		if(userInfo.getLoginCount() == 0) {
			userInfo.setFirstLoginTime(new Date());
			userInfo.setFirstLoginIp(ip);
		}
		userInfo.setLoginCount(userInfo.getLoginCount()+1);
		userInfo.setLastLoginIp(ip);
		userInfo.setLastLoginTime(new Date());
		userInfo.setProvince(apiResult.getStr("province"));
		userInfo.setCity(apiResult.getStr("city"));
		userInfo.setGender(apiResult.getInt("sex"));
		userInfo.update();
		
		return Ret.ok();
	}
	
	
}