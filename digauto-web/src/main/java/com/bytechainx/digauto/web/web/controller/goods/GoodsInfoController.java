package com.bytechainx.digauto.web.web.controller.goods;


import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.bytechainx.digauto.common.EnumConstant.DataStatusEnum;
import com.bytechainx.digauto.common.Permissions;
import com.bytechainx.digauto.common.annotation.Permission;
import com.bytechainx.digauto.common.kit.DateUtil;
import com.bytechainx.digauto.common.model.GoodsCategory;
import com.bytechainx.digauto.common.model.GoodsInfo;
import com.bytechainx.digauto.common.model.GoodsUnit;
import com.bytechainx.digauto.common.model.InventoryWarehouse;
import com.bytechainx.digauto.common.model.SaleOrder;
import com.bytechainx.digauto.common.service.goods.GoodsCategoryService;
import com.bytechainx.digauto.common.service.goods.GoodsInfoService;
import com.bytechainx.digauto.web.web.controller.base.BaseController;
import com.jfinal.aop.Before;
import com.jfinal.aop.Inject;
import com.jfinal.core.Path;
import com.jfinal.kit.Kv;
import com.jfinal.kit.Ret;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.activerecord.tx.Tx;


/**
* 商品管理
*/
@Path("/sale/project/goods")
public class GoodsInfoController extends BaseController {

	@Inject
	private GoodsInfoService goodsInfoService;
	@Inject
	private GoodsCategoryService goodsCategoryService;

	/**
	* 首页
	*/
	@Permission(Permissions.sale_project_goods)
	public void index() {
		List<GoodsCategory> topCategoryList = GoodsCategory.dao.findTop(getTenantOrgId());
		setAttr("topCategoryList", topCategoryList);
	}

	/**
	* 列表
	*/
	@Permission(Permissions.sale_project_goods)
	public void list() {
		int pageNumber = getInt("pageNumber", 1);
		Integer goodsCategoryId = getInt("goods_category_id");
		Boolean hideStopFlag = getBoolean("hide_stop_flag"); // 隐藏停用商品
		String keyword = get("keyword");
		Kv condKv = Kv.create();
		condKv.set("goods_category_id", goodsCategoryId);
		condKv.set("name,code", keyword); // 多字段模糊查询
		
		if(hideStopFlag) {
			condKv.set("data_status", DataStatusEnum.enable.getValue());
		}
		
		Page<GoodsInfo> page = goodsInfoService.paginate(getTenantOrgId(), condKv, pageNumber, pageSize);
		setAttr("page", page);
		setAttr("hideStopFlag", hideStopFlag);
	}
	
	/**
	* 列表
	*/
	@Permission(Permissions.sale)
	public void listBySaleOrder() {
		int pageNumber = getInt("pageNumber", 1);
		Integer goodsCategoryId = getInt("goods_category_id");
		Kv condKv = Kv.create();
		condKv.set("data_status", DataStatusEnum.enable.getValue());
		
		if(goodsCategoryId != null && goodsCategoryId == 0) { // 客户已购商品
			Integer saleOrderId = getInt("sale_order_id");
			SaleOrder saleOrder = SaleOrder.dao.findById(getTenantOrgId(), saleOrderId);
			Page<GoodsInfo> page = goodsInfoService.paginateByCustomer(getTenantOrgId(), saleOrder.getCustomerInfoId(), condKv, pageNumber, pageSize);
			setAttr("page", page);
			
		} else {
			String keyword = get("keyword");
			condKv.set("goods_category_id", goodsCategoryId);
			condKv.set("name,code", keyword); // 多字段模糊查询
			Page<GoodsInfo> page = goodsInfoService.paginate(getTenantOrgId(), condKv, pageNumber, pageSize);
			setAttr("page", page);
		}
		
		
	}

	/**
	* 查看
	*/
	@Permission(Permissions.sale_project_goods_show)
	public void show() {
		Integer id = getInt("id");
		if(id == null || id <= 0) {
			renderError(404);
			return;
		}
		GoodsInfo info = GoodsInfo.dao.findById(getTenantOrgId(), id);
		if(info == null) {
			renderError(404);
			return;
		}
		List<InventoryWarehouse> warehouseList = InventoryWarehouse.dao.findAll(getTenantOrgId());
		setAttr("warehouseList", warehouseList);
		setAttr("goodsInfo", info);
	}


	/**
	* 添加
	*/
	@Permission(Permissions.sale_project_goods_create)
	public void add() {
		setAttrCommon();
		setAttr("barCode", DateUtil.getSecondNumber(new Date()));
		keepPara("sourcePage");
	}

	/**
	* 新增
	*/
	@Permission(Permissions.sale_project_goods_create)
	@Before(Tx.class)
	public void create() {
		GoodsInfo info = getModel(GoodsInfo.class, "", true);
		info.setGoodsImages(getParaValues("thumbs"), getParaValues("originals"));
		if(StringUtils.equalsIgnoreCase(getPara("sourcePage"), "saleOrderAddGoods")) {
			GoodsInfo _goodsInfo= GoodsInfo.dao.findByName(getTenantOrgId(), info.getName());
			if(_goodsInfo != null) {
				Ret ret = Ret.ok().set("targetId", _goodsInfo.getId());
				renderJson(ret);
				return;
			}
		}
		Ret ret = goodsInfoService.create(getTenantOrgId(), info);
		
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
		GoodsInfo info = GoodsInfo.dao.findById(getTenantOrgId(), id);
		if(info == null) {
			renderError(404);
			return;
		}
		setAttr("goodsInfo", info);
		if(StringUtils.isEmpty(info.getBarCode())) {
			setAttr("barCode", DateUtil.getSecondNumber(new Date()));
		}
		setAttrCommon();
		
	}
	
	
	/**
	* 编辑价格
	*/
	@Permission(Permissions.sale_project_goods_updatePrice)
	public void editPrice() {
		Integer id = getInt("id");
		if(id == null || id <= 0) {
			renderError(404);
			return;
		}
		GoodsInfo info = GoodsInfo.dao.findById(getTenantOrgId(), id);
		if(info == null) {
			renderError(404);
			return;
		}
		setAttr("goodsInfo", info);
		keepPara("sourcePage");
	}
	
	
	/**
	* 修改
	*/
	@Permission(Permissions.sale_project_goods_update)
	@Before(Tx.class)
	public void update() {
		GoodsInfo info = getModel(GoodsInfo.class, "", true);
		info.setGoodsImages(getParaValues("thumbs"), getParaValues("originals"));
		
		Ret ret = goodsInfoService.update(getTenantOrgId(), info);
		
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
		Ret ret = goodsInfoService.delete(getTenantOrgId(), Arrays.asList(id));
		renderJson(ret);
	}



	/**
	* 停用
	*/
	@Permission(Permissions.sale_project_goods_disable)
	@Before(Tx.class)
	public void disable() {
		Integer id = getInt("id");
		if(id == null || id <= 0) {
			renderJson(Ret.fail("ID不能为空"));
			return;
		}
		Ret ret = goodsInfoService.disable(getTenantOrgId(), Arrays.asList(id));

		renderJson(ret);
	}
	
	/**
	* 启用
	*/
	@Permission(Permissions.sale_project_goods_disable)
	@Before(Tx.class)
	public void enable() {
		Integer id = getInt("id");
		if(id == null || id <= 0) {
			renderJson(Ret.fail("ID不能为空"));
			return;
		}
		Ret ret = goodsInfoService.enable(getTenantOrgId(), Arrays.asList(id));

		renderJson(ret);
	}



	/**
	 * 设置公共数据
	 */
	private void setAttrCommon() {
		List<GoodsCategory> topCategoryList = GoodsCategory.dao.findTop(getTenantOrgId());
		List<GoodsUnit> goodsUnitList = GoodsUnit.dao.findAll(getTenantOrgId());
		setAttr("topCategoryList", topCategoryList);
		setAttr("goodsUnitList", goodsUnitList);
		
	}

	
	/**
	 * 加载货品信息
	 */
	public void listByJson() {
		int pageNumber = getInt("pageNumber", 1);
		String keyword = get("keyword");
		Kv condKv = Kv.create();
		condKv.set("name,code", keyword); // 多字段模糊查询
		condKv.set("data_status", DataStatusEnum.enable.getValue());
		
		Page<GoodsInfo> page = goodsInfoService.paginate(getTenantOrgId(), condKv, pageNumber, pageSize);
		renderJson(Ret.ok().set("data", page.getList()));
	}
	
	
}