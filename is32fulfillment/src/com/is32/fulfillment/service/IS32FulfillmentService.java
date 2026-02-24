package com.is32.fulfillment.service;

import com.is32.fulfillment.model.IS32FulfillmentEntryModel;
import com.is32.fulfillment.enums.IS32FulfillmentStatus;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface IS32FulfillmentService
{
    IS32FulfillmentEntryModel getEntryById(String entryId);

    List<IS32FulfillmentEntryModel> getEntriesByOrderCode(String orderCode);

    List<IS32FulfillmentEntryModel> getEntriesByStatus(IS32FulfillmentStatus status);

    List<Map<String, Object>> getDetailedFulfillmentEntries(String orderCode);

    List<Map<String, Object>> getWarehouseReport(String warehouseCode, Date startDate, Date endDate);

    void updateEntryStatus(String entryId, IS32FulfillmentStatus newStatus);

    List<IS32FulfillmentEntryModel> getCustomerFulfillmentHistory(String customerUid);
}
