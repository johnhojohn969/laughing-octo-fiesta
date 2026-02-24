package com.is32.fulfillment.service.impl;

import com.is32.fulfillment.dao.IS32FulfillmentEntryDao;
import com.is32.fulfillment.dao.IS32ReturnRequestDao;
import com.is32.fulfillment.model.IS32FulfillmentEntryModel;
import com.is32.fulfillment.model.IS32ReturnRequestModel;
import com.is32.fulfillment.enums.IS32FulfillmentStatus;
import com.is32.fulfillment.service.IS32FulfillmentService;
import de.hybris.platform.servicelayer.model.ModelService;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultIS32FulfillmentService implements IS32FulfillmentService
{
    private static final Logger LOG = Logger.getLogger(DefaultIS32FulfillmentService.class);

    private IS32FulfillmentEntryDao is32FulfillmentEntryDao;
    private IS32ReturnRequestDao is32ReturnRequestDao;
    private ModelService modelService;

    @Override
    public IS32FulfillmentEntryModel getEntryById(final String entryId)
    {
        return is32FulfillmentEntryDao.findByEntryId(entryId);
    }

    @Override
    public List<IS32FulfillmentEntryModel> getEntriesByOrderCode(final String orderCode)
    {
        return is32FulfillmentEntryDao.findByOrderCode(orderCode);
    }

    @Override
    public List<IS32FulfillmentEntryModel> getEntriesByStatus(final IS32FulfillmentStatus status)
    {
        return is32FulfillmentEntryDao.findByStatus(status);
    }

    @Override
    public List<Map<String, Object>> getDetailedFulfillmentEntries(final String orderCode)
    {
        final List<IS32FulfillmentEntryModel> entries = is32FulfillmentEntryDao.findByOrderCode(orderCode);
        final List<Map<String, Object>> detailedEntries = new ArrayList<>();

        for (final IS32FulfillmentEntryModel entry : entries)
        {
            final Map<String, Object> entryMap = new HashMap<>();
            entryMap.put("entryId", entry.getEntryId());
            entryMap.put("productCode", entry.getProductCode());
            entryMap.put("status", entry.getStatus());
            entryMap.put("quantity", entry.getQuantity());
            entryMap.put("warehouseCode", entry.getWarehouseCode());

            final List<IS32ReturnRequestModel> returns = is32ReturnRequestDao.findByOrderCode(entry.getOrderCode());
            entryMap.put("returnCount", returns.size());

            double totalRefund = 0;
            for (final IS32ReturnRequestModel returnRequest : returns)
            {
                if (returnRequest.getRefundAmount() != null)
                {
                    totalRefund += returnRequest.getRefundAmount();
                }
            }
            entryMap.put("totalRefundAmount", totalRefund);

            detailedEntries.add(entryMap);
        }

        return detailedEntries;
    }

    @Override
    public List<Map<String, Object>> getWarehouseReport(final String warehouseCode,
                                                          final Date startDate,
                                                          final Date endDate)
    {
        final List<List<Object>> rawData = is32FulfillmentEntryDao.getOrderFulfillmentReport(warehouseCode, startDate, endDate);
        final List<Map<String, Object>> report = new ArrayList<>();

        for (final List<Object> row : rawData)
        {
            final Map<String, Object> entry = new HashMap<>();
            entry.put("orderCode", row.get(0));
            entry.put("productCode", row.get(1));
            entry.put("fulfillmentStatus", row.get(2));
            entry.put("quantity", row.get(3));
            entry.put("trackingNumber", row.get(4));
            entry.put("carrier", row.get(5));
            entry.put("shipmentStatus", row.get(6));
            entry.put("allocatedQty", row.get(7));
            entry.put("availableQty", row.get(8));
            entry.put("returnRequestId", row.get(9));
            entry.put("returnStatus", row.get(10));
            report.add(entry);
        }

        return report;
    }

    @Override
    public void updateEntryStatus(final String entryId, final IS32FulfillmentStatus newStatus)
    {
        final IS32FulfillmentEntryModel entry = is32FulfillmentEntryDao.findByEntryId(entryId);
        entry.setStatus(newStatus);
        entry.setModifiedDate(new Date());
        modelService.save(entry);
    }

    @Override
    public List<IS32FulfillmentEntryModel> getCustomerFulfillmentHistory(final String customerUid)
    {
        return is32FulfillmentEntryDao.findByCustomerUid(customerUid);
    }

    public void setIs32FulfillmentEntryDao(final IS32FulfillmentEntryDao is32FulfillmentEntryDao)
    {
        this.is32FulfillmentEntryDao = is32FulfillmentEntryDao;
    }

    public void setIs32ReturnRequestDao(final IS32ReturnRequestDao is32ReturnRequestDao)
    {
        this.is32ReturnRequestDao = is32ReturnRequestDao;
    }

    public void setModelService(final ModelService modelService)
    {
        this.modelService = modelService;
    }
}
