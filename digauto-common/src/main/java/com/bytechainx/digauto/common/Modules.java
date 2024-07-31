/**
 * 
 */
package com.bytechainx.digauto.common;

/**
 * 系统应用模块
 * @author defier
 *
 */
public enum Modules {

	core("标准模块"),
	multiple_store("连锁门店"),
	multiple_warehouse("多仓库管理"),
	commission("员工业绩提成"),
	auto_marketing("自动化经营"),
	rechargecard("储值卡管理"),
	plancard("套餐卡管理"),
	voucher("代金券管理"),
	shareholder("共享门店"),
	databoard("数据大屏"),
	merchant_pay("商户收款")
	;

	private String name;

	private Modules(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
	
	public static Modules getEnum(String value) {
		for (Modules c : Modules.values()) {
			if (value.equalsIgnoreCase(c.name())) {
				return c;
			}
		}
		return null;
	}
	
}

