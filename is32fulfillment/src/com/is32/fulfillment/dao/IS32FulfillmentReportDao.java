package com.is32.fulfillment.dao;

import com.is32.fulfillment.model.IS32FulfillmentEntryModel;
import com.is32.fulfillment.model.IS32ReturnRequestModel;

import java.util.Date;
import java.util.List;

public interface IS32FulfillmentReportDao
{
    List<IS32FulfillmentEntryModel> findEntriesByOrderCodeNumeric(long orderCode);

    List<IS32FulfillmentEntryModel> findEntriesByQuantityStr(String quantity);

    List<IS32FulfillmentEntryModel> findEntriesByPriorityStr(String priority);

    List<IS32FulfillmentEntryModel> findEntriesByEntryIdNumeric(int entryId);

    List<IS32ReturnRequestModel> findReturnsByRefundAmount(String refundAmount);

    List<IS32ReturnRequestModel> findReturnsByReturnQuantity(String returnQuantity);

    List<IS32ReturnRequestModel> findReturnsByCustomerUidNumeric(long customerUid);

    List<List<Object>> getFulfillmentAllocationReport(int warehouseCode, String allocatedQty);
}
