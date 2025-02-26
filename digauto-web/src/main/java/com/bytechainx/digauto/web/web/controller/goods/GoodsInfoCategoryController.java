package com.bytechainx.digauto.web.web.controller.goods;


import java.util.ArrayList;
import java.util.List;

import com.bytechainx.digauto.common.Permissions;
import com.bytechainx.digauto.common.annotation.Permission;
import com.bytechainx.digauto.common.model.GoodsCategory;
import com.bytechainx.digauto.common.service.goods.GoodsCategoryService;
import com.bytechainx.digauto.web.web.controller.base.BaseController;
import com.jfinal.aop.Before;
import com.jfinal.aop.Inject;
import com.jfinal.core.Path;
import com.jfinal.kit.Ret;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.activerecord.tx.Tx;


/**
* 商品分类
*/
@Path("/sale/project/goodsCategory")
public class GoodsInfoCategoryController extends BaseController {


	@Inject
	private GoodsCategoryService goodsCategoryService;

	/**
	* 首页
	*/
	@Permission(Permissions.sale_project_goods)
	public void index() {

	}

	/**
	* 列表
	*/
	@Permission(Permissions.sale_project_goods)
	public void list() {
		List<GoodsCategory> topList = GoodsCategory.dao.findTop(getTenantOrgId());
		setAttr("topList", topList);
	}
	
	@Permission({Permissions.sale_project_goods,Permissions.inventory_stock_info} )
	public void listByJson() {
		List<GoodsCategory> topList = GoodsCategory.dao.findTop(getTenantOrgId());
		List<GoodsCategory> categoryList = new ArrayList<>();
		for (GoodsCategory top : topList) {
			StringBuffer levelSb = new StringBuffer();
			top.loopChilds(categoryList, levelSb);
		}
		renderJson(Ret.ok().set("data", categoryList));
	}
	
	@Permission(Permissions.sale_project_goods)
	public void optionList() {
		Page<GoodsCategory> page = goodsCategoryService.paginate(getTenantOrgId(), 1, maxPageSize);
		setAttr("page", page);
	}

	/**
	* 查看
	*/
	@Permission(Permissions.sale_project_goods_show)
	public void show() {

		renderJson(Ret.ok());
	}


	/**
	* 添加
	*/
	@Permission(Permissions.sale_project_goods_create)
	public void add() {
		setAttrCommon();
		setAttr("parent_id", getInt("parent_id"));
		setAttr("sourcePage", get("sourcePage"));
	}

	/**
	* 新增
	*/
	@Permission(Permissions.sale_project_goods_create)
	@Before(Tx.class)
	public void create() {
		GoodsCategory category = getModel(GoodsCategory.class, "", true);
		Ret ret = goodsCategoryService.create(getTenantOrgId(), category);
		renderJson(ret);
	}


	/**
	* 编辑
	*/
	@Permission(Permissions.sale_project_goods_update)
	public void edit() {
		Integer id = getInt("id");
		if(id == null || id <= 0) {
			renderError(404);
			return;
		}
		GoodsCategory category = GoodsCategory.dao.findById(getTenantOrgId(), id);
		if(category == null) {
			renderError(404);
			return;
		}
		setAttrCommon();
		setAttr("goodsCategory", category);
	}

	/**
	* 修改
	*/
	@Permission(Permissions.sale_project_goods_update)
	@Before(Tx.class)
	public void update() {
		GoodsCategory category = getModel(GoodsCategory.class, "", true);
		Ret ret = goodsCategoryService.update(getTenantOrgId(), category);
		renderJson(ret);
	}


	/**
	* 删除
	*/
	@Permission(Permissions.sale_project_goods_delete)
	@Before(Tx.class)
	public void delete() {
		Integer id = getInt("id");
		if(id == null || id <= 0) {
			renderJson(Ret.fail("ID不能为空"));
			return;
		}
		Ret ret = goodsCategoryService.delete(getTenantOrgId(), id);
		renderJson(ret);
	}

	/**
	 * 设置公共数据
	 */
	private void setAttrCommon() {
		List<GoodsCategory> topList = GoodsCategory.dao.findTop(getTenantOrgId());
		setAttr("topList", topList);
	}
	
}