package com.cq.maintenance.inventory.service;

import com.cq.maintenance.common.exception.*;
import com.cq.maintenance.common.response.PageResult;
import com.cq.maintenance.inventory.dto.*;
import com.cq.maintenance.inventory.entity.*;
import com.cq.maintenance.inventory.mapper.InventoryMapper;
import com.cq.maintenance.inventory.mapper.InventoryMapper.StockRow;
import com.cq.maintenance.inventory.mapper.InventoryMapper.WarningInsert;
import com.cq.maintenance.inventory.vo.*;
import com.cq.maintenance.security.*;
import com.cq.maintenance.system.vo.UserSummaryVO;
import com.cq.maintenance.workorder.entity.*;
import com.cq.maintenance.workorder.mapper.WorkOrderMapper;
import com.cq.maintenance.workorder.service.BusinessNumberService;
import com.cq.maintenance.workorder.service.WorkOrderScopeService;
import java.math.*;
import java.util.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {
    private static final int SAFETY_PERIOD_DAYS=30;
    private final InventoryMapper mapper;
    private final WorkOrderMapper workOrders;
    private final WorkOrderScopeService workOrderScopes;
    private final BusinessNumberService numbers;

    public InventoryService(InventoryMapper mapper,WorkOrderMapper workOrders,WorkOrderScopeService workOrderScopes,BusinessNumberService numbers){
        this.mapper=mapper;this.workOrders=workOrders;this.workOrderScopes=workOrderScopes;this.numbers=numbers;
    }

    public PageResult<WarehouseVO> warehouses(WarehouseQuery query){
        Scope scope=scope();List<WarehouseRowVO> rows=mapper.findWarehousePage(query,scope.allData(),scope.ids());
        List<WarehouseVO> records=rows.stream().map(this::warehouseVO).toList();
        return PageResult.of(records,mapper.countWarehousePage(query,scope.allData(),scope.ids()),query.getPage(),query.getSize());
    }
    public WarehouseVO warehouseDetail(Long id){assertWarehouseAccess(id);WarehouseRowVO row=mapper.findWarehouseRow(id);if(row==null)notFound("仓库不存在");return warehouseVO(row);}
    public List<UserSummaryVO> warehouseAssignableUsers(){return mapper.findWarehouseAssignableUsers();}
    @Transactional public Long createWarehouse(WarehouseRequest input){
        validateManager(input.managerId());if(mapper.countWarehouseCode(input.warehouseNo().trim(),null)>0)conflict("仓库编号已存在");
        Warehouse warehouse=new Warehouse();copy(warehouse,input);
        try{mapper.insertWarehouse(warehouse);}catch(DuplicateKeyException e){conflict("仓库编号已存在");}
        if(!isAdmin(current()))mapper.insertWarehouseUsers(warehouse.getId(),List.of(current().userId()));
        return warehouse.getId();
    }
    @Transactional public void updateWarehouse(Long id,WarehouseRequest input){
        assertWarehouseAccess(id);Warehouse warehouse=requiredWarehouse(id);validateManager(input.managerId());if(mapper.countWarehouseCode(input.warehouseNo().trim(),id)>0)conflict("仓库编号已存在");copy(warehouse,input);
        try{mapper.updateWarehouse(warehouse);}catch(DuplicateKeyException e){conflict("仓库编号已存在");}
    }
    @Transactional public void authorizeWarehouse(Long id,WarehouseAuthorizationRequest input){
        requiredWarehouse(id);List<Long> userIds=input.userIds().stream().distinct().toList();
        for(Long userId:userIds)if(mapper.countWarehouseAssignableUser(userId)==0)conflict("授权用户不存在、已禁用或角色不适用于仓库授权");
        mapper.clearWarehouseUsers(id);if(!userIds.isEmpty())mapper.insertWarehouseUsers(id,userIds);
    }

    public PageResult<SparePartVO> spareParts(SparePartQuery query){return PageResult.of(mapper.findSparePartPage(query),mapper.countSparePartPage(query),query.getPage(),query.getSize());}
    public SparePartVO sparePartDetail(Long id){SparePartVO result=mapper.findSparePartVO(id);if(result==null)notFound("备件不存在");return result;}
    public List<SupplierVO> suppliers(){return mapper.findSuppliers();}
    @Transactional public Long createSparePart(SparePartRequest input){
        validateSupplier(input.supplierId());if(mapper.countSparePartCode(input.spareNo().trim(),null)>0)conflict("备件编号已存在");SparePart spare=new SparePart();copy(spare,input);
        try{mapper.insertSparePart(spare);}catch(DuplicateKeyException e){conflict("备件编号已存在");}return spare.getId();
    }
    @Transactional public void updateSparePart(Long id,SparePartRequest input){
        SparePart spare=requiredSpare(id);validateSupplier(input.supplierId());if(mapper.countSparePartCode(input.spareNo().trim(),id)>0)conflict("备件编号已存在");copy(spare,input);
        try{mapper.updateSparePart(spare);}catch(DuplicateKeyException e){conflict("备件编号已存在");}
    }

    public PageResult<StockVO> stocks(StockQuery query){Scope scope=scope();if(query.getWarehouseId()!=null)assertWarehouseAccess(query.getWarehouseId());return PageResult.of(mapper.findStockPage(query,scope.allData(),scope.ids()),mapper.countStockPage(query,scope.allData(),scope.ids()),query.getPage(),query.getSize());}
    public PageResult<TransactionVO> transactions(TransactionQuery query){Scope scope=scope();if(query.getWarehouseId()!=null)assertWarehouseAccess(query.getWarehouseId());return PageResult.of(mapper.findTransactionPage(query,scope.allData(),scope.ids()),mapper.countTransactionPage(query,scope.allData(),scope.ids()),query.getPage(),query.getSize());}
    public List<WarningVO> warnings(){Scope scope=scope();return mapper.findOpenWarnings(scope.allData(),scope.ids());}
    public SafetyStockVO safetyStock(Long warehouseId,Long sparePartId){assertWarehouseAccess(warehouseId);requiredWarehouse(warehouseId);SparePart spare=requiredSpare(sparePartId);return calculate(warehouseId,spare,SAFETY_PERIOD_DAYS);}
    public List<WorkOrderSpareVO> workOrderSpares(Long workOrderId){workOrderScopes.assertVisible(workOrderId);return mapper.findWorkOrderSpares(workOrderId);}

    @Transactional public void inbound(InventoryOperationRequest input){
        assertInventoryOperator();Warehouse warehouse=enabledWarehouse(input.warehouseId());SparePart spare=enabledSpare(input.sparePartId());assertWarehouseAccess(warehouse.getId());
        mutate(warehouse,spare,input.qty(),"INBOUND",input.unitPrice()==null?spare.getUnitPrice():input.unitPrice(),null,null,null,input.remark());
    }
    @Transactional public void outbound(InventoryOperationRequest input){
        assertInventoryOperator();Warehouse warehouse=enabledWarehouse(input.warehouseId());SparePart spare=enabledSpare(input.sparePartId());assertWarehouseAccess(warehouse.getId());
        mutate(warehouse,spare,input.qty().negate(),"OUTBOUND",input.unitPrice()==null?spare.getUnitPrice():input.unitPrice(),null,null,null,input.remark());
    }
    @Transactional public void scrap(InventoryOperationRequest input){
        assertInventoryOperator();Warehouse warehouse=enabledWarehouse(input.warehouseId());SparePart spare=enabledSpare(input.sparePartId());assertWarehouseAccess(warehouse.getId());
        mutate(warehouse,spare,input.qty().negate(),"SCRAP",spare.getUnitPrice(),null,null,null,input.remark());
    }
    @Transactional public void stocktake(StocktakeRequest input){
        assertInventoryOperator();Warehouse warehouse=enabledWarehouse(input.warehouseId());SparePart spare=enabledSpare(input.sparePartId());assertWarehouseAccess(warehouse.getId());StockRow stock=lockedStock(warehouse.getId(),spare.getId());BigDecimal change=input.actualQty().subtract(stock.currentQty());if(change.signum()==0)throw new BusinessException(ErrorCode.DUPLICATE_OPERATION,"盘点数量与系统库存一致，无需调整");
        writeChange(stock,warehouse,spare,change,"STOCKTAKE",spare.getUnitPrice(),null,null,null,input.remark());
    }
    @Transactional public void transfer(TransferRequest input){
        assertInventoryOperator();if(input.sourceWarehouseId().equals(input.targetWarehouseId()))conflict("调拨源仓库和目标仓库不能相同");
        Warehouse source=enabledWarehouse(input.sourceWarehouseId()),target=enabledWarehouse(input.targetWarehouseId());SparePart spare=enabledSpare(input.sparePartId());assertWarehouseAccess(source.getId());assertWarehouseAccess(target.getId());
        // 固定按仓库 ID 加锁，避免两个反向调拨形成死锁。
        Long first=source.getId()<target.getId()?source.getId():target.getId(),second=source.getId()<target.getId()?target.getId():source.getId();
        StockRow firstStock=lockedStock(first,spare.getId()),secondStock=lockedStock(second,spare.getId());StockRow sourceStock=source.getId().equals(first)?firstStock:secondStock,targetStock=target.getId().equals(first)?firstStock:secondStock;
        assertEnough(sourceStock.currentQty(),input.qty());
        InventoryTransaction out=writeChange(sourceStock,source,spare,input.qty().negate(),"TRANSFER_OUT",spare.getUnitPrice(),null,null,null,input.remark());
        InventoryTransaction in=writeChange(targetStock,target,spare,input.qty(),"TRANSFER_IN",spare.getUnitPrice(),null,null,out.getId(),input.remark());mapper.updateTransactionRelated(out.getId(),in.getId());
    }
    @Transactional public Long issueForWorkOrder(Long workOrderId,WorkOrderSpareRequest input){
        WorkOrder order=workOrders.lockWorkOrder(workOrderId);if(order==null)notFound("工单不存在");workOrderScopes.assertAssignedEngineer(order);
        if(order.getWorkOrderType()!=WorkOrderType.REPAIR||order.getStatus()!=WorkOrderStatus.PROCESSING)throw new BusinessException(ErrorCode.ILLEGAL_STATE_TRANSITION,"仅处理中的维修工单可以领用备件");
        Warehouse warehouse=enabledWarehouse(input.warehouseId());SparePart spare=enabledSpare(input.sparePartId());assertWarehouseAccess(warehouse.getId());
        InventoryTransaction transaction=mutate(warehouse,spare,input.qty().negate(),"ISSUE",spare.getUnitPrice(),order.getId(),order.getEquipmentId(),null,input.remark());
        WorkOrderSpare issue=new WorkOrderSpare();issue.setWorkOrderId(order.getId());issue.setWarehouseId(warehouse.getId());issue.setSparePartId(spare.getId());issue.setQty(input.qty());issue.setUnitPriceSnapshot(spare.getUnitPrice());issue.setTransactionId(transaction.getId());issue.setStatus("ISSUED");issue.setIssuedBy(current().userId());mapper.insertWorkOrderSpare(issue);return issue.getId();
    }
    @Transactional public void returnForWorkOrder(Long workOrderId,Long issueId,ReturnSpareRequest input){
        WorkOrder order=workOrders.lockWorkOrder(workOrderId);if(order==null)notFound("工单不存在");workOrderScopes.assertAssignedEngineer(order);if(order.getStatus()!=WorkOrderStatus.PROCESSING)throw new BusinessException(ErrorCode.ILLEGAL_STATE_TRANSITION,"仅处理中的工单可以退回备件");
        WorkOrderSpare issue=mapper.lockWorkOrderSpare(issueId);if(issue==null||!workOrderId.equals(issue.getWorkOrderId()))notFound("工单备件领用记录不存在");
        BigDecimal returned=mapper.sumReturned(issue.getTransactionId()),remaining=issue.getQty().subtract(returned);if(input.qty().compareTo(remaining)>0)conflict("退库数量超过尚可退数量");
        Warehouse warehouse=enabledWarehouse(issue.getWarehouseId());SparePart spare=requiredSpare(issue.getSparePartId());assertWarehouseAccess(warehouse.getId());
        mutate(warehouse,spare,input.qty(),"RETURN",issue.getUnitPriceSnapshot(),order.getId(),order.getEquipmentId(),issue.getTransactionId(),input.remark());
        BigDecimal total=returned.add(input.qty());mapper.updateIssueStatus(issueId,total.compareTo(issue.getQty())==0?"RETURNED":"PARTIAL_RETURN");
    }

    private InventoryTransaction mutate(Warehouse warehouse,SparePart spare,BigDecimal change,String type,BigDecimal unitPrice,Long workOrderId,Long equipmentId,Long relatedId,String remark){return writeChange(lockedStock(warehouse.getId(),spare.getId()),warehouse,spare,change,type,unitPrice,workOrderId,equipmentId,relatedId,remark);}
    private InventoryTransaction writeChange(StockRow stock,Warehouse warehouse,SparePart spare,BigDecimal change,String type,BigDecimal unitPrice,Long workOrderId,Long equipmentId,Long relatedId,String remark){
        BigDecimal after=stock.currentQty().add(change);if(after.signum()<0)throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK,"库存不足，当前库存为 "+stock.currentQty().stripTrailingZeros().toPlainString());
        mapper.updateStock(stock.id(),after);InventoryTransaction transaction=new InventoryTransaction();transaction.setTransactionNo(numbers.next("TX"));transaction.setWarehouseId(warehouse.getId());transaction.setSparePartId(spare.getId());transaction.setTransactionType(type);transaction.setQtyChange(change);transaction.setQtyBefore(stock.currentQty());transaction.setQtyAfter(after);transaction.setUnitPrice(unitPrice);transaction.setWorkOrderId(workOrderId);transaction.setEquipmentId(equipmentId);transaction.setRelatedTransactionId(relatedId);transaction.setOperatorId(current().userId());transaction.setRemark(trim(remark));mapper.insertTransaction(transaction);refreshWarning(warehouse,spare,after);return transaction;
    }
    private StockRow lockedStock(Long warehouseId,Long spareId){mapper.ensureStock(warehouseId,spareId);StockRow stock=mapper.lockStock(warehouseId,spareId);if(stock==null)throw new BusinessException(ErrorCode.SYSTEM_ERROR,"库存记录初始化失败");return stock;}
    private void refreshWarning(Warehouse warehouse,SparePart spare,BigDecimal currentQty){SafetyStockVO values=calculate(warehouse.getId(),spare,SAFETY_PERIOD_DAYS);Long warningId=mapper.findOpenWarning(warehouse.getId(),spare.getId());if(currentQty.compareTo(values.suggestedSafetyStock())<0){if(warningId==null){WarningInsert warning=new WarningInsert();warning.setWarehouseId(warehouse.getId());warning.setSparePartId(spare.getId());warning.setThreshold(values.suggestedSafetyStock());warning.setCurrent(currentQty);mapper.insertWarning(warning);mapper.insertWarningMessages(warehouse.getId(),warning.getId(),warehouse.getWarehouseName()+"的"+spare.getSpareName()+"库存低于安全库存");}}else if(warningId!=null)mapper.closeWarning(warningId,currentQty);}
    private SafetyStockVO calculate(Long warehouseId,SparePart spare,int days){BigDecimal total=Optional.ofNullable(mapper.sumNetOutbound(warehouseId,spare.getId(),days)).orElse(BigDecimal.ZERO);BigDecimal daily=total.divide(BigDecimal.valueOf(days),6,RoundingMode.HALF_UP);BigDecimal ss=daily.multiply(BigDecimal.valueOf(spare.getSafetyDays())).setScale(2,RoundingMode.HALF_UP);BigDecimal rop=daily.multiply(BigDecimal.valueOf(spare.getSafetyDays()+spare.getLeadTimeDays())).setScale(2,RoundingMode.HALF_UP);return new SafetyStockVO(warehouseId,spare.getId(),daily.setScale(2,RoundingMode.HALF_UP),ss,rop,days);}
    private void assertEnough(BigDecimal current,BigDecimal required){if(current.compareTo(required)<0)throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK,"库存不足，当前库存为 "+current.stripTrailingZeros().toPlainString());}
    private Warehouse enabledWarehouse(Long id){Warehouse value=requiredWarehouse(id);if(!"ENABLED".equals(value.getStatus()))conflict("仓库已停用");return value;}
    private Warehouse requiredWarehouse(Long id){Warehouse value=mapper.findWarehouse(id);if(value==null)notFound("仓库不存在");return value;}
    private SparePart enabledSpare(Long id){SparePart value=requiredSpare(id);if(!"ENABLED".equals(value.getStatus()))conflict("备件已停用");return value;}
    private SparePart requiredSpare(Long id){SparePart value=mapper.findSparePart(id);if(value==null)notFound("备件不存在");return value;}
    private void assertInventoryOperator(){LoginUser user=current();if(!isAdmin(user)&&!user.roleCodes().contains("WAREHOUSE_ADMIN"))throw new BusinessException(ErrorCode.DATA_FORBIDDEN,"仅仓库管理员可以执行库存操作");}
    private void assertWarehouseAccess(Long warehouseId){LoginUser user=current();if(!isAdmin(user)&&!user.authorizedWarehouseIds().contains(warehouseId))throw new BusinessException(ErrorCode.DATA_FORBIDDEN,"无权操作该仓库");}
    private Scope scope(){LoginUser user=current();boolean all=isAdmin(user);return new Scope(all,all?List.of(-1L):(user.authorizedWarehouseIds().isEmpty()?List.of(-1L):user.authorizedWarehouseIds()));}
    private LoginUser current(){return SecurityUtils.currentUser();}private boolean isAdmin(LoginUser user){return user.roleCodes().contains("ADMIN");}
    private void validateManager(Long id){if(id!=null&&mapper.countEnabledUser(id)==0)conflict("仓库负责人不存在或已禁用");}
    private void validateSupplier(Long id){if(id!=null&&mapper.countEnabledSupplier(id)==0)conflict("供应商不存在或已停用");}
    private WarehouseVO warehouseVO(WarehouseRowVO row){return new WarehouseVO(row.id(),row.warehouseNo(),row.warehouseName(),row.location(),row.managerId(),row.managerName(),row.status(),row.createdAt(),row.updatedAt(),mapper.findAuthorizedUserIds(row.id()));}
    private void copy(Warehouse target,WarehouseRequest input){target.setWarehouseNo(input.warehouseNo().trim());target.setWarehouseName(input.warehouseName().trim());target.setLocation(trim(input.location()));target.setManagerId(input.managerId());target.setStatus(defaultStatus(input.status()));}
    private void copy(SparePart target,SparePartRequest input){target.setSpareNo(input.spareNo().trim());target.setSpareName(input.spareName().trim());target.setSpecification(trim(input.specification()));target.setBrand(trim(input.brand()));target.setUnit(input.unit().trim());target.setUnitPrice(input.unitPrice());target.setSupplierId(input.supplierId());target.setCompatibleModel(trim(input.compatibleModel()));target.setLeadTimeDays(input.leadTimeDays());target.setSafetyDays(input.safetyDays());target.setStatus(defaultStatus(input.status()));}
    private String defaultStatus(String value){return value==null||value.isBlank()?"ENABLED":value;}private String trim(String value){return value==null?null:value.trim();}
    private static void conflict(String message){throw new BusinessException(ErrorCode.BUSINESS_CONFLICT,message);}private static void notFound(String message){throw new BusinessException(ErrorCode.NOT_FOUND,message);}
    private record Scope(boolean allData,List<Long> ids) {}
}
