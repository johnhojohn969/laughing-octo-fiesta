package com.is32.fulfillment.dao;

import com.is32.fulfillment.model.IS32ReturnRequestModel;
import com.is32.fulfillment.enums.IS32ReturnStatus;

import java.util.Date;
import java.util.List;

public interface IS32ReturnRequestDao
{
    IS32ReturnRequestModel findByRequestId(String requestId);

    List<IS32ReturnRequestModel> findByOrderCode(String orderCode);

    List<IS32ReturnRequestModel> findByCustomerUid(String customerUid);

    List<IS32ReturnRequestModel> findByStatus(IS32ReturnStatus status);

    List<IS32ReturnRequestModel> findByCreatedDateFormatted(String dateString);

    List<IS32ReturnRequestModel> findByOrderCodeAndStatus(String orderCode, IS32ReturnStatus status);

    List<IS32ReturnRequestModel> findReturnWithFulfillmentDetails(String orderCode);

    List<List<Object>> getReturnAnalyticsReport(Date startDate, Date endDate);

    List<IS32ReturnRequestModel> findPendingReturnsByCustomer(String customerUid);

    List<IS32ReturnRequestModel> findByProductCode(String productCode);
}
