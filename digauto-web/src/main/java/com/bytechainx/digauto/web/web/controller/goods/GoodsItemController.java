package com.bytechainx.digauto.web.web.controller.goods;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.bytechainx.digauto.common.EnumConstant.DataStatusEnum;
import com.bytechainx.digauto.common.Permissions;
import com.bytechainx.digauto.common.annotation.Permission;
import com.bytechainx.digauto.common.kit.DateUtil;
import com.bytechainx.digauto.common.model.GoodsItem;
import com.bytechainx.digauto.common.model.GoodsItemCategory;
import com.bytechainx.digauto.common.model.GoodsItemRef;
import com.bytechainx.digauto.common.model.GoodsUnit;
import com.bytechainx.digauto.common.model.InventoryWarehouse;
import com.bytechainx.digauto.common.model.SaleOrder;
import com.bytechainx.digauto.common.service.goods.GoodsItemCategoryService;
import com.bytechainx.digauto.common.service.goods.GoodsItemService;
import com.bytechainx.digauto.web.web.controller.base.BaseController;
import com.jfinal.aop.Before;
import com.jfinal.aop.Inject;
import com.jfinal.core.Path;
import com.jfinal.kit.Kv;
import com.jfinal.kit.Ret;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.activerecord.tx.Tx;


/**
* 项目管理
*/
@Path("/sale/project/item")
public class GoodsItemController extends BaseController {

	@Inject
	private GoodsItemService itemService;
	@Inject
	private GoodsItemCategoryService itemCategoryService;

	/**
	* 首页
	*/
	@Permission(Permissions.sale_project_item)
	public void index() {
		List<GoodsItemCategory> topCategoryList = GoodsItemCategory.dao.findTop(getTenantOrgId());
		setAttr("topCategoryList", topCategoryList);
	}

	/**
	* 列表
	*/
	@Permission(Permissions.sale_project_item)
	public void list() {
		int pageNumber = getInt("pageNumber", 1);
		Integer itemCategoryId = getInt("goods_item_category_id");
		Boolean hideStopFlag = getBoolean("hide_stop_flag"); // 隐藏停用商品
		String keyword = get("keyword");
		Kv condKv = Kv.create();
		condKv.set("goods_item_category_id", itemCategoryId);
		condKv.set("name,code", keyword); // 多字段模糊查询
		
		if(hideStopFlag) {
			condKv.set("data_status", DataStatusEnum.enable.getValue());
		}
		
		Page<GoodsItem> page = itemService.paginate(getTenantOrgId(), condKv, pageNumber, pageSize);
		setAttr("page", page);
		setAttr("hideStopFlag", hideStopFlag);
	}

	/**
	* 查看
	*/
	@Permission(Permissions.sale_project_item_show)
	public void show() {
		Integer id = getInt("id");
		if(id == null || id <= 0) {
			renderError(404);
			return;
		}
		GoodsItem goodsItem = GoodsItem.dao.findById(getTenantOrgId(), id);
		if(goodsItem == null) {
			renderError(404);
			return;
		}
		List<InventoryWarehouse> warehouseList = InventoryWarehouse.dao.findAll(getTenantOrgId());
		setAttr("warehouseList", warehouseList);
		setAttr("goodsItem", goodsItem);
	}


	/**
	* 添加
	*/
	@Permission(Permissions.sale_project_item_create)
	public void add() {
		setAttrCommon();
		setAttr("barCode", DateUtil.getSecondNumber(new Date()));
		keepPara("sourcePage");
		keepPara("sale_order_id");
		keepPara("item_type");
	}

	/**
	* 新增
	*/
	@Permission(Permissions.sale_project_item_create)
	@Before(Tx.class)
	public void create() {
		GoodsItem goodsItem = getModel(GoodsItem.class, "", true);
		if(StringUtils.equalsIgnoreCase(getPara("sourcePage"), "saleOrderAddItem")) {
			GoodsItem _goodsItem = GoodsItem.dao.findByName(getTenantOrgId(), goodsItem.getName());
			if(_goodsItem != null) {
				Ret ret = Ret.ok().set("targetId", _goodsItem.getId());
				renderJson(ret);
				return;
			}
		}
		Ret ret = itemService.create(getTenantOrgId(), goodsItem);
		
		renderJson(ret);
	}



	/**
	* 编辑
	*/
	@Permission(Permissions.sale_project_item_update)
	public void edit() {
		Integer id = getInt("id");
		if(id == null || id <= 0) {
			renderError(404);
			return;
		}
		GoodsItem goodsItem = GoodsItem.dao.findById(getTenantOrgId(), id);
		if(goodsItem == null) {
			renderError(404);
			return;
		}
		setAttr("goodsItem", goodsItem);
		setAttrCommon();
		
	}
	
	
	/**
	* 编辑价格
	*/
	@Permission(Permissions.sale_project_item_updatePrice)
	public void editPrice() {
		Integer id = getInt("id");
		if(id == null || id <= 0) {
			renderError(404);
			return;
		}
		GoodsItem goodsItem = GoodsItem.dao.findById(getTenantOrgId(), id);
		if(goodsItem == null) {
			renderError(404);
			return;
		}
		setAttr("goodsItem", goodsItem);
		keepPara("sourcePage");
	}
	
	
	/**
	* 修改
	*/
	@Permission(Permissions.sale_project_item_update)
	@Before(Tx.class)
	public void update() {
		GoodsItem goodsItem = getModel(GoodsItem.class, "", true);
		
		Ret ret = itemService.update(getTenantOrgId(), goodsItem);
		
		renderJson(ret);
	}



	/**
	* 删除
	*/
	@Permission(Permissions.sale_project_item_delete)
	@Before(Tx.class)
	public void delete() {
		Integer id = getInt("id");
		if(id == null || id <= 0) {
			renderJson(Ret.fail("ID不能为空"));
			return;
		}
		Ret ret = itemService.delete(getTenantOrgId(), Arrays.asList(id));
		renderJson(ret);
	}



	/**
	* 停用
	*/
	@Permission(Permissions.sale_project_item_disable)
	@Before(Tx.class)
	public void disable() {
		Integer id = getInt("id");
		if(id == null || id <= 0) {
			renderJson(Ret.fail("ID不能为空"));
			return;
		}
		Ret ret = itemService.disable(getTenantOrgId(), Arrays.asList(id));

		renderJson(ret);
	}
	
	/**
	* 启用
	*/
	@Permission(Permissions.sale_project_item_disable)
	@Before(Tx.class)
	public void enable() {
		Integer id = getInt("id");
		if(id == null || id <= 0) {
			renderJson(Ret.fail("ID不能为空"));
			return;
		}
		Ret ret = itemService.enable(getTenantOrgId(), Arrays.asList(id));

		renderJson(ret);
	}



	/**
	 * 设置公共数据
	 */
	private void setAttrCommon() {
		List<GoodsItemCategory> topCategoryList = GoodsItemCategory.dao.findTop(getTenantOrgId());
		List<GoodsUnit> itemUnitList = GoodsUnit.dao.findAll(getTenantOrgId());
		setAttr("topCategoryList", topCategoryList);
		setAttr("itemUnitList", itemUnitList);
		
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
		
		Page<GoodsItem> page = itemService.paginate(getTenantOrgId(), condKv, pageNumber, pageSize);
		renderJson(Ret.ok().set("data", page.getList()));
	}
	
	
	
	
	/**
	* 工单页面添加项目列表
	*/
	@Permission(Permissions.sale_sale_order)
	public void listBySaleOrder() {
		int pageNumber = getInt("pageNumber", 1);
		Integer itemCategoryId = getInt("goods_item_category_id");
		Kv condKv = Kv.create();
		condKv.set("data_status", DataStatusEnum.enable.getValue());
		
		if(itemCategoryId != null && itemCategoryId == 0) { // 客户已购项目
			Integer saleOrderId = getInt("sale_order_id");
			SaleOrder saleOrder = SaleOrder.dao.findById(getTenantOrgId(), saleOrderId);
			Page<GoodsItem> page = itemService.paginateByCustomer(getTenantOrgId(), saleOrder.getCustomerInfoId(), condKv, pageNumber, pageSize);
			setAttr("page", page);
			
		} else {
			String keyword = get("keyword");
			condKv.set("goods_item_category_id", itemCategoryId);
			condKv.set("name,code", keyword); // 多字段模糊查询
			Page<GoodsItem> page = itemService.paginate(getTenantOrgId(), condKv, pageNumber, pageSize);
			setAttr("page", page);
		}
	}
	
	/**
	 * 关联商品
	 */
	public void addGoodsInfoRef() {
		Integer id = getInt("id");
		if(id == null || id <= 0) {
			renderError(404);
			return;
		}
		GoodsItem goodsItem = GoodsItem.dao.findById(getTenantOrgId(), id);
		setAttr("goodsItem", goodsItem);
	}
	
	/**
	 * 关联商品更新
	 */
	public void updateGoodsInfoRef() {
		Integer goodsItemId = getInt("id");
		Integer[] goodsInfoIds = getParaValuesToInt("goods_info_id");
		
		List<GoodsItemRef> goodsItemRefs = new ArrayList<>();
		for (int i = 0; i < goodsInfoIds.length; i++) {
			GoodsItemRef goodsItemRef = new GoodsItemRef();
			goodsItemRef.setGoodsInfoId(goodsInfoIds[i]);
			goodsItemRef.setGoodsItemId(goodsItemId);
			
			goodsItemRefs.add(goodsItemRef);
		}
		
		Ret ret = itemService.updateGoodsItem(getTenantOrgId(), goodsItemId, goodsItemRefs);
		
		renderJson(ret);
	}
	
	
}