package com.is32.fulfillment.service.impl;

import com.is32.fulfillment.dao.IS32FulfillmentEntryDao;
import com.is32.fulfillment.dao.IS32ReturnRequestDao;
import com.is32.fulfillment.model.IS32ReturnRequestModel;
import com.is32.fulfillment.enums.IS32ReturnStatus;
import com.is32.fulfillment.service.IS32ReturnService;
import de.hybris.platform.servicelayer.model.ModelService;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultIS32ReturnService implements IS32ReturnService
{
    private static final Logger LOG = Logger.getLogger(DefaultIS32ReturnService.class);

    private IS32ReturnRequestDao is32ReturnRequestDao;
    private IS32FulfillmentEntryDao is32FulfillmentEntryDao;
    private ModelService modelService;

    @Override
    public IS32ReturnRequestModel getReturnByRequestId(final String requestId)
    {
        return is32ReturnRequestDao.findByRequestId(requestId);
    }

    @Override
    public List<IS32ReturnRequestModel> getReturnsByOrderCode(final String orderCode)
    {
        return is32ReturnRequestDao.findByOrderCode(orderCode);
    }

    @Override
    public List<IS32ReturnRequestModel> getReturnsByCustomer(final String customerUid)
    {
        return is32ReturnRequestDao.findByCustomerUid(customerUid);
    }

    @Override
    public List<IS32ReturnRequestModel> getReturnsByDate(final String dateString)
    {
        return is32ReturnRequestDao.findByCreatedDateFormatted(dateString);
    }

    @Override
    public List<Map<String, Object>> getReturnAnalytics(final Date startDate, final Date endDate)
    {
        final List<List<Object>> rawData = is32ReturnRequestDao.getReturnAnalyticsReport(startDate, endDate);
        final List<Map<String, Object>> analytics = new ArrayList<>();

        double totalRefunds = 0;
        int totalReturns = 0;
        Map<String, Integer> reasonCounts = new HashMap<>();

        for (final List<Object> row : rawData)
        {
            final Map<String, Object> entry = new HashMap<>();
            entry.put("reason", row.get(0));
            entry.put("returnStatus", row.get(1));
            entry.put("refundAmount", row.get(2));
            entry.put("productCode", row.get(3));
            entry.put("warehouseCode", row.get(4));
            entry.put("fulfillmentQty", row.get(5));
            entry.put("allocatedQty", row.get(6));
            entry.put("availableQty", row.get(7));
            analytics.add(entry);

            final Double refund = (Double) row.get(2);
            if (refund != null)
            {
                totalRefunds += refund;
            }
            totalReturns++;

            final String reason = (String) row.get(0);
            reasonCounts.merge(reason, 1, Integer::sum);
        }

        if (!analytics.isEmpty())
        {
            final Map<String, Object> summary = new HashMap<>();
            summary.put("_summary", true);
            summary.put("totalReturns", totalReturns);
            summary.put("totalRefunds", totalRefunds);
            summary.put("averageRefund", totalReturns > 0 ? totalRefunds / totalReturns : 0.0);
            summary.put("reasonBreakdown", reasonCounts);
            analytics.add(summary);
        }

        return analytics;
    }

    @Override
    public void approveReturn(final String requestId)
    {
        final IS32ReturnRequestModel returnRequest = is32ReturnRequestDao.findByRequestId(requestId);
        returnRequest.setReturnStatus(IS32ReturnStatus.APPROVED);
        returnRequest.setProcessedDate(new Date());
        modelService.save(returnRequest);
    }

    @Override
    public void rejectReturn(final String requestId)
    {
        final IS32ReturnRequestModel returnRequest = is32ReturnRequestDao.findByRequestId(requestId);
        returnRequest.setReturnStatus(IS32ReturnStatus.REJECTED);
        returnRequest.setProcessedDate(new Date());
        modelService.save(returnRequest);
    }

    @Override
    public List<Map<String, Object>> getReturnWithFulfillmentDetails(final String orderCode)
    {
        final List<IS32ReturnRequestModel> returns = is32ReturnRequestDao.findReturnWithFulfillmentDetails(orderCode);
        final List<Map<String, Object>> details = new ArrayList<>();

        for (final IS32ReturnRequestModel returnRequest : returns)
        {
            final Map<String, Object> entry = new HashMap<>();
            entry.put("requestId", returnRequest.getRequestId());
            entry.put("orderCode", returnRequest.getOrderCode());
            entry.put("reason", returnRequest.getReason());
            entry.put("status", returnRequest.getReturnStatus());
            entry.put("refundAmount", returnRequest.getRefundAmount());
            details.add(entry);
        }

        return details;
    }

    public void setIs32ReturnRequestDao(final IS32ReturnRequestDao is32ReturnRequestDao)
    {
        this.is32ReturnRequestDao = is32ReturnRequestDao;
    }

    public void setIs32FulfillmentEntryDao(final IS32FulfillmentEntryDao is32FulfillmentEntryDao)
    {
        this.is32FulfillmentEntryDao = is32FulfillmentEntryDao;
    }

    public void setModelService(final ModelService modelService)
    {
        this.modelService = modelService;
    }
}
