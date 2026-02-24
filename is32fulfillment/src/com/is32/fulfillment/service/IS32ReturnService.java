package com.is32.fulfillment.service;

import com.is32.fulfillment.model.IS32ReturnRequestModel;
import com.is32.fulfillment.enums.IS32ReturnStatus;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface IS32ReturnService
{
    IS32ReturnRequestModel getReturnByRequestId(String requestId);

    List<IS32ReturnRequestModel> getReturnsByOrderCode(String orderCode);

    List<IS32ReturnRequestModel> getReturnsByCustomer(String customerUid);

    List<IS32ReturnRequestModel> getReturnsByDate(String dateString);

    List<Map<String, Object>> getReturnAnalytics(Date startDate, Date endDate);

    void approveReturn(String requestId);

    void rejectReturn(String requestId);

    List<Map<String, Object>> getReturnWithFulfillmentDetails(String orderCode);
}
