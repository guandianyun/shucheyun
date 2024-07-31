package com.bytechainx.digauto.common.service.base;

import java.util.Date;
import java.util.List;

import com.bytechainx.digauto.common.EnumConstant.AuditStatusEnum;
import com.bytechainx.digauto.common.EnumConstant.FlagEnum;
import com.bytechainx.digauto.common.EnumConstant.FlowTypeEnum;
import com.bytechainx.digauto.common.EnumConstant.MsgDataTypeEnum;
import com.bytechainx.digauto.common.EnumConstant.MsgLevelEnum;
import com.bytechainx.digauto.common.EnumConstant.MsgTypeEnum;
import com.bytechainx.digauto.common.EnumConstant.StockWarnTypeEnum;
import com.bytechainx.digauto.common.dto.ConditionFilter;
import com.bytechainx.digauto.common.dto.ConditionFilter.Operator;
import com.bytechainx.digauto.common.kit.SmsKit;
import com.bytechainx.digauto.common.kit.WeixinApiKit;
import com.bytechainx.digauto.common.model.CustomerCar;
import com.bytechainx.digauto.common.model.CustomerInfo;
import com.bytechainx.digauto.common.model.GoodsPurchase;
import com.bytechainx.digauto.common.model.InventoryCheckingGoods;
import com.bytechainx.digauto.common.model.InventoryCheckoutGoods;
import com.bytechainx.digauto.common.model.InventoryStock;
import com.bytechainx.digauto.common.model.InventorySwapGoods;
import com.bytechainx.digauto.common.model.MarketPlancardOrder;
import com.bytechainx.digauto.common.model.MarketRechargecardOrder;
import com.bytechainx.digauto.common.model.MarketVoucherOrder;
import com.bytechainx.digauto.common.model.MsgNotice;
import com.bytechainx.digauto.common.model.MsgNoticeSend;
import com.bytechainx.digauto.common.model.PurchaseOrderGoods;
import com.bytechainx.digauto.common.model.PurchaseRejectOrderGoods;
import com.bytechainx.digauto.common.model.SaleOrder;
import com.bytechainx.digauto.common.model.SaleOrderGoodsPurchase;
import com.bytechainx.digauto.common.model.TenantAdmin;
import com.bytechainx.digauto.common.model.TenantStoreFlow;
import com.jfinal.kit.Kv;
import com.jfinal.kit.ThreadPoolKit;

public class CommonService {
	
	
	/**
	 * 保存门店动态
	 * @param tenantOrgId
	 * @param operAdminId 
	 * @param order
	 * @param flowTypeEnum 
	 * @param content 
	 */
	protected void saveStoreFlow(Integer tenantOrgId, Integer operAdminId, SaleOrder order, FlowTypeEnum flowTypeEnum, String content) {
		ThreadPoolKit.execute(() -> {
			TenantStoreFlow storeFlow = new TenantStoreFlow();
			storeFlow.setCreatedAt(new Date());
			storeFlow.setCustomerCarId(order.getCustomerCarId());
			storeFlow.setCustomerInfoId(order.getCustomerInfoId());
			storeFlow.setFlowType(flowTypeEnum.getValue());
			storeFlow.setSaleOrderId(order.getId());
			storeFlow.setTenantAdminId(operAdminId);
			storeFlow.setTenantOrgId(tenantOrgId);
			storeFlow.setTenantStoreId(order.getTenantStoreId());
			storeFlow.setContent(content);
			storeFlow.save();
		});
	}

	
	/**
	 * 更新库存告警状态
	 */
	protected void updateStockWarn(Integer warehouseId, List<?> orderGoodsList) {
		ThreadPoolKit.execute(() -> {
			for (Object model : orderGoodsList) {
				if(model instanceof InventoryCheckingGoods) {
					InventoryCheckingGoods _model = (InventoryCheckingGoods)model;
					updateStockWarn(_model.getTenantOrgId(), _model.getGoodsPurchaseId(), warehouseId);
				} else if(model instanceof InventorySwapGoods) {
					InventorySwapGoods _model = (InventorySwapGoods)model;
					updateStockWarn(_model.getTenantOrgId(), _model.getGoodsPurchaseId(), warehouseId);

				} else if(model instanceof PurchaseOrderGoods) {
					PurchaseOrderGoods _model = (PurchaseOrderGoods)model;
					updateStockWarn(_model.getTenantOrgId(), _model.getGoodsPurchaseId(), warehouseId);

				} else if(model instanceof PurchaseRejectOrderGoods) {
					PurchaseRejectOrderGoods _model = (PurchaseRejectOrderGoods)model;
					updateStockWarn(_model.getTenantOrgId(), _model.getGoodsPurchaseId(), warehouseId);

				} else if(model instanceof SaleOrderGoodsPurchase) {
					SaleOrderGoodsPurchase _model = (SaleOrderGoodsPurchase)model;
					updateStockWarn(_model.getTenantOrgId(), _model.getGoodsPurchaseId(), _model.getInventoryWarehouseId());

				} else if(model instanceof InventoryCheckoutGoods) {
					InventoryCheckoutGoods _model = (InventoryCheckoutGoods)model;
					updateStockWarn(_model.getTenantOrgId(), _model.getGoodsPurchaseId(), warehouseId);
				}
			}
		});
	}

	private void updateStockWarn(Integer tenantOrgId, Integer goodsId, Integer warehouseId) {
		GoodsPurchase goodsPurchase = GoodsPurchase.dao.findById(tenantOrgId, goodsId);
		if(goodsPurchase == null || goodsPurchase.getStockWarnFlag() == FlagEnum.NO.getValue()) {
			return;
		}
		InventoryStock inventoryStock = InventoryStock.dao.findByWarehouse(tenantOrgId, goodsId, warehouseId);
		if(inventoryStock == null || inventoryStock.getStock() == null) {
			return;
		}
		if(goodsPurchase.getLowStock() != null && inventoryStock.getStock().compareTo(goodsPurchase.getLowStock()) < 0) { // 小于最低库存
			inventoryStock.setWarnType(StockWarnTypeEnum.lowest.getValue());
			
		} else if(goodsPurchase.getHighStock() != null && inventoryStock.getStock().compareTo(goodsPurchase.getHighStock()) > 0) {
			inventoryStock.setWarnType(StockWarnTypeEnum.highest.getValue());
			
		} else if(goodsPurchase.getSafeStock() != null && inventoryStock.getStock().compareTo(goodsPurchase.getSafeStock()) < 0) {
			inventoryStock.setWarnType(StockWarnTypeEnum.lowsafe.getValue());
			
		} else {
			inventoryStock.setWarnType(StockWarnTypeEnum.ok.getValue());
		}
		inventoryStock.setUpdatedAt(new Date());
		inventoryStock.update();
	}
	
	/**
	 * 接车开单通知，异步执行
	 * 
	 * @param saleOrder
	 */
	protected void sendSaleOrderNotice(SaleOrder saleOrder) {
		ThreadPoolKit.execute(() -> {
			if (saleOrder.getAuditStatus() != AuditStatusEnum.pass.getValue()) {
				return;
			}
			CustomerInfo customerInfo = saleOrder.getCustomerInfo();
			CustomerCar customerCar = saleOrder.getCustomerCar();
			if(customerInfo.getWeixinFlag() != null && customerInfo.getWeixinFlag()) {
				WeixinApiKit.sendSaleOrderMsg(customerInfo, saleOrder);
			} else {
				String content = "尊敬的"+customerInfo.getName()+"，您的爱车"+customerCar.getCarNumber()+"已接车，正在安排技师检测，请您耐心等候。";
				SmsKit.sendNoticeSms(customerInfo.getTenantOrgId(), customerInfo.getMobile(), content);
			}
		});
	}
	
	/**
	 * 车检报告通知，异步执行
	 * 
	 * @param saleOrder
	 */
	protected void sendFinishCheckNotice(SaleOrder saleOrder) {
		ThreadPoolKit.execute(() -> {
			if (saleOrder.getAuditStatus() != AuditStatusEnum.pass.getValue()) {
				return;
			}
			CustomerInfo customerInfo = saleOrder.getCustomerInfo();
			CustomerCar customerCar = saleOrder.getCustomerCar();
			if(customerInfo.getWeixinFlag() != null && customerInfo.getWeixinFlag()) {
				WeixinApiKit.sendFinishCheckMsg(customerInfo, saleOrder);
			} else {
				String content = "尊敬的"+customerInfo.getName()+"，您的爱车"+customerCar.getCarNumber()+"已完成车辆检测。";
				SmsKit.sendNoticeSms(customerInfo.getTenantOrgId(), customerInfo.getMobile(), content);
			}
		});
	}
	
	/**
	 * 施工完成通知，异步执行
	 * 
	 * @param saleOrder
	 */
	protected void sendFinishWorkNotice(SaleOrder saleOrder) {
		ThreadPoolKit.execute(() -> {
			if (saleOrder.getAuditStatus() != AuditStatusEnum.pass.getValue()) {
				return;
			}
			CustomerInfo customerInfo = saleOrder.getCustomerInfo();
			CustomerCar customerCar = saleOrder.getCustomerCar();
			if(customerInfo.getWeixinFlag() != null && customerInfo.getWeixinFlag()) {
				WeixinApiKit.sendFinishWorkMsg(customerInfo, saleOrder);
			} else {
				String content = "尊敬的"+customerInfo.getName()+"，您的爱车"+customerCar.getCarNumber()+"已完成施工。";
				SmsKit.sendNoticeSms(customerInfo.getTenantOrgId(), customerInfo.getMobile(), content);
			}
		});
	}
	
	/**
	 * 结算完成通知，异步执行
	 * 
	 * @param saleOrder
	 */
	protected void sendFinishFundNotice(SaleOrder saleOrder) {
		ThreadPoolKit.execute(() -> {
			if (saleOrder.getAuditStatus() != AuditStatusEnum.pass.getValue()) {
				return;
			}
			CustomerInfo customerInfo = saleOrder.getCustomerInfo();
			CustomerCar customerCar = saleOrder.getCustomerCar();
			if(customerInfo.getWeixinFlag() != null && customerInfo.getWeixinFlag()) {
				WeixinApiKit.sendFinishFundMsg(customerInfo, saleOrder);
			} else {
				String content = "尊敬的"+customerInfo.getName()+"，您的爱车"+customerCar.getCarNumber()+"已完成维保，感谢惠顾";
				SmsKit.sendNoticeSms(customerInfo.getTenantOrgId(), customerInfo.getMobile(), content);
			}
		});
	}
	
	/**
	 * 套餐卡开卡消息通知，异步执行
	 * 
	 * @param saleOrder
	 */
	protected void sendPlancardMsgNotice(MarketPlancardOrder marketPlancardOrder) {
		ThreadPoolKit.execute(() -> {
			CustomerInfo customerInfo = marketPlancardOrder.getCustomerInfo();
			if(customerInfo.getWeixinFlag() != null && customerInfo.getWeixinFlag()) {
				WeixinApiKit.sendPlancardMsg(customerInfo, marketPlancardOrder);
			} else {
				String content = "尊敬的"+customerInfo.getName()+"，"+marketPlancardOrder.getPlancard().getName()+"套餐卡已开卡成功！";
				SmsKit.sendNoticeSms(customerInfo.getTenantOrgId(), customerInfo.getMobile(), content);
			}
		});
	}
	
	/**
	 * 代金券卡开卡消息通知，异步执行
	 * 
	 * @param saleOrder
	 */
	protected void sendVoucherMsgNotice(MarketVoucherOrder marketVoucherOrder) {
		ThreadPoolKit.execute(() -> {
			CustomerInfo customerInfo = marketVoucherOrder.getCustomerInfo();
			if(customerInfo.getWeixinFlag() != null && customerInfo.getWeixinFlag()) {
				WeixinApiKit.sendVoucherMsg(customerInfo, marketVoucherOrder);
			} else {
				String content = "尊敬的"+customerInfo.getName()+"，"+marketVoucherOrder.getVoucher().getName()+"代金券已发放成功，可抵扣金额："+marketVoucherOrder.getUseAmount().toPlainString();
				SmsKit.sendNoticeSms(customerInfo.getTenantOrgId(), customerInfo.getMobile(), content);
			}
		});
	}
	
	/**
	 * 储值卡充值消息通知，异步执行
	 * 
	 * @param saleOrder
	 */
	protected void sendRechargecardMsgNotice(MarketRechargecardOrder marketRechargecardOrder) {
		ThreadPoolKit.execute(() -> {
			CustomerInfo customerInfo = marketRechargecardOrder.getCustomerInfo();
			if(customerInfo.getWeixinFlag() != null && customerInfo.getWeixinFlag()) {
				WeixinApiKit.sendRechargecardMsg(customerInfo, marketRechargecardOrder);
			} else {
				String content = "尊敬的"+customerInfo.getName()+"，储值卡已充值成功，充值金额："+marketRechargecardOrder.getRechargeAmount().toPlainString();
				SmsKit.sendNoticeSms(customerInfo.getTenantOrgId(), customerInfo.getMobile(), content);
			}
		});
	}
	
	/**
	 * 发送消息通知
	 * @param tenantOrgId 
	 * @param payOrderAudit
	 * @param title
	 * @param content
	 * @param auditorId
	 * @param smsFlag
	 * @param sysFlag
	 */
	protected void sendNoticeMsg(Integer tenantOrgId, MsgDataTypeEnum dataType, String title, String content, Integer auditorId, Boolean smsFlag, Boolean sysFlag) {
		if(smsFlag != null && smsFlag) {
			TenantAdmin admin = TenantAdmin.dao.findById(tenantOrgId, auditorId);
			SmsKit.sendNoticeSms(tenantOrgId, admin.getMobile(), title);
		}
		if(sysFlag != null && sysFlag) {
			MsgNotice msgNotice = new MsgNotice();
			msgNotice.setTenantOrgId(tenantOrgId);
			msgNotice.setContent(content);
			msgNotice.setCreatedAt(new Date());
			msgNotice.setDataType(dataType.getValue());
			msgNotice.setMsgLevel(MsgLevelEnum.general.getValue());
			msgNotice.setMsgType(MsgTypeEnum.systemNotice.getValue());
			msgNotice.setSenderId(0);
			msgNotice.setSenderName("系统");
			msgNotice.setTitle(title);
			msgNotice.save();
			
			MsgNoticeSend msgNoticeSend = new MsgNoticeSend();
			msgNoticeSend.setCreatedAt(new Date());
			msgNoticeSend.setMsgNoticeId(msgNotice.getId());
			msgNoticeSend.setReadFlag(false);
			msgNoticeSend.setReceiverId(auditorId);
			msgNoticeSend.setSenderId(0);
			msgNoticeSend.setSenderName("系统");
			msgNoticeSend.setTenantOrgId(tenantOrgId);
			msgNoticeSend.save();
		}
	}
	
	/**
	 * 组装条件查询
	 * @param conditionColumns
	 * @param where
	 * @param params
	 */
	protected void conditionFilter(Kv conditionColumns, StringBuffer where, List<Object> params) {
		if(conditionColumns == null || conditionColumns.isEmpty()) {
			return;
		}
		for (Object key : conditionColumns.keySet()) {
			Object value = conditionColumns.get(key);
			if(value == null) {
				continue;
			}
			if(value instanceof Integer) {
				where.append(" and "+ key +"  = ?");
				params.add(value);
			} else if(value instanceof String) {
				String _key = (String)key;
				String[] columns = _key.split(","); // 多字段模糊查询，字段用逗号隔开
				where.append(" and (");
				for(int index = 0; index < columns.length; index++) {
					where.append(columns[index] +"  like ?");
					params.add("%"+value+"%");
					if(index < columns.length -1) {
						where.append(" or ");
					}
				}
				where.append(" )");
			} else if(value instanceof Boolean) {
				where.append(" and "+ key +"  = ?");
				params.add(value);
			}  else if(value instanceof ConditionFilter) {
				ConditionFilter filter = (ConditionFilter) value;
				if(filter.getOperator().equals(Operator.eq)) {
					where.append(" and "+ key +"  = ?");
					params.add(filter.getValue());
				} else if(filter.getOperator().equals(Operator.neq)) {
					where.append(" and "+ key +"  != ?");
					params.add(filter.getValue());
				} else if(filter.getOperator().equals(Operator.in)) {
					where.append(" and "+ key +"  in ("+ filter.getValue() +") ");
				} else if(filter.getOperator().equals(Operator.notIn)) {
					where.append(" and "+ key +" not in ("+ filter.getValue() +") ");
				} else if(filter.getOperator().equals(Operator.like)) {
					where.append(" and "+ key +"  like ? ");
					params.add("%"+filter.getValue()+"%");
				}  else if(filter.getOperator().equals(Operator.lt)) {
					where.append(" and "+ key +"  < ?");
					params.add(filter.getValue());
				}   else if(filter.getOperator().equals(Operator.gt)) {
					where.append(" and "+ key +"  > ?");
					params.add(filter.getValue());
				}   else if(filter.getOperator().equals(Operator.lte)) {
					where.append(" and "+ key +"  <= ?");
					params.add(filter.getValue());
				}   else if(filter.getOperator().equals(Operator.gte)) {
					where.append(" and "+ key +"  >= ?");
					params.add(filter.getValue());
				} else if(filter.getOperator().equals(Operator.more)) {
					int paramLength = ((String)key).split("\\?").length-1;
					where.append(" and "+ key);
					for (int i = 0; i < paramLength; i++) {
						params.add(filter.getValue());
					}
				}
			}
		}
	}
	
	
}
