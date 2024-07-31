/**
 * 
 */
package com.bytechainx.digauto.common;

/**
 * 产品版本
 * @author defier
 *
 */
public enum ProductVersions {

	common("汽服店数字化系统"),
	;

	private String name;

	private ProductVersions(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
	
	public static ProductVersions getEnum(String value) {
		for (ProductVersions c : ProductVersions.values()) {
			if (value.equalsIgnoreCase(c.name())) {
				return c;
			}
		}
		return null;
	}
	
	
}
