package com.is32.fulfillment.dao;

import com.is32.fulfillment.model.IS32FulfillmentEntryModel;
import com.is32.fulfillment.enums.IS32FulfillmentStatus;

import java.util.Date;
import java.util.List;

public interface IS32FulfillmentEntryDao
{
    IS32FulfillmentEntryModel findByEntryId(String entryId);

    List<IS32FulfillmentEntryModel> findByOrderCode(String orderCode);

    List<IS32FulfillmentEntryModel> findByStatus(IS32FulfillmentStatus status);

    List<IS32FulfillmentEntryModel> findByOrderCodeAndStatus(String orderCode, IS32FulfillmentStatus status);

    List<IS32FulfillmentEntryModel> findByWarehouseCode(String warehouseCode);

    List<IS32FulfillmentEntryModel> findEntriesWithShipments(IS32FulfillmentStatus status, Date fromDate);

    List<List<Object>> getOrderFulfillmentReport(String warehouseCode, Date startDate, Date endDate);

    List<IS32FulfillmentEntryModel> findByCustomerUid(String customerUid);

    List<IS32FulfillmentEntryModel> findPendingEntriesByWarehouse(String warehouseCode, Date createdBefore);

    List<IS32FulfillmentEntryModel> findByProductCode(String productCode);
}
