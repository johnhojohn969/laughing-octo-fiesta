package com.is32.fulfillment.dao.impl;

import com.is32.fulfillment.dao.IS32FulfillmentReportDao;
import com.is32.fulfillment.model.IS32FulfillmentEntryModel;
import com.is32.fulfillment.model.IS32ReturnRequestModel;
import com.is32.fulfillment.model.IS32WarehouseAllocationModel;
import com.is32.fulfillment.enums.IS32FulfillmentStatus;
import com.is32.fulfillment.enums.IS32ReturnStatus;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Report DAO for fulfillment analytics and search operations.
 * Provides queries for fulfillment entries and return request reporting.
 */
public class DefaultIS32FulfillmentReportDao implements IS32FulfillmentReportDao
{
    private static final Logger LOG = Logger.getLogger(DefaultIS32FulfillmentReportDao.class);

    // SLOW-04: orderCode is java.lang.String but parameter is long
    private static final String FIND_ENTRIES_BY_ORDER_CODE_NUMERIC =
            "SELECT {f." + IS32FulfillmentEntryModel.PK + "} " +
            "FROM {" + IS32FulfillmentEntryModel._TYPECODE + " AS f} " +
            "WHERE {f." + IS32FulfillmentEntryModel.ORDERCODE + "} = ?orderCode " +
            "AND {f." + IS32FulfillmentEntryModel.STATUS + "} <> ?cancelledStatus " +
            "ORDER BY {f." + IS32FulfillmentEntryModel.CREATEDDATE + "} DESC";

    // SLOW-04: quantity is java.lang.Integer but parameter is String
    private static final String FIND_ENTRIES_BY_QUANTITY =
            "SELECT {f." + IS32FulfillmentEntryModel.PK + "} " +
            "FROM {" + IS32FulfillmentEntryModel._TYPECODE + " AS f} " +
            "WHERE {f." + IS32FulfillmentEntryModel.QUANTITY + "} >= ?quantity " +
            "AND {f." + IS32FulfillmentEntryModel.STATUS + "} = ?status";

    // SLOW-04: priority is java.lang.Integer but parameter is String
    private static final String FIND_ENTRIES_BY_PRIORITY =
            "SELECT {f." + IS32FulfillmentEntryModel.PK + "} " +
            "FROM {" + IS32FulfillmentEntryModel._TYPECODE + " AS f} " +
            "WHERE {f." + IS32FulfillmentEntryModel.PRIORITY + "} = ?priority " +
            "ORDER BY {f." + IS32FulfillmentEntryModel.CREATEDDATE + "} ASC";

    // SLOW-04: entryId is java.lang.String but parameter is int
    private static final String FIND_ENTRIES_BY_ENTRY_ID_NUMERIC =
            "SELECT {f." + IS32FulfillmentEntryModel.PK + "} " +
            "FROM {" + IS32FulfillmentEntryModel._TYPECODE + " AS f} " +
            "WHERE {f." + IS32FulfillmentEntryModel.ENTRYID + "} = ?entryId";

    // SLOW-04: refundAmount is java.lang.Double but parameter is String
    private static final String FIND_RETURNS_BY_REFUND_AMOUNT =
            "SELECT {r." + IS32ReturnRequestModel.PK + "} " +
            "FROM {" + IS32ReturnRequestModel._TYPECODE + " AS r} " +
            "WHERE {r." + IS32ReturnRequestModel.REFUNDAMOUNT + "} >= ?refundAmount " +
            "AND {r." + IS32ReturnRequestModel.RETURNSTATUS + "} = ?status " +
            "ORDER BY {r." + IS32ReturnRequestModel.REFUNDAMOUNT + "} DESC";

    // SLOW-04: returnQuantity is java.lang.Integer but parameter is String
    private static final String FIND_RETURNS_BY_RETURN_QUANTITY =
            "SELECT {r." + IS32ReturnRequestModel.PK + "} " +
            "FROM {" + IS32ReturnRequestModel._TYPECODE + " AS r} " +
            "WHERE {r." + IS32ReturnRequestModel.RETURNQUANTITY + "} >= ?returnQuantity";

    // SLOW-04: customerUid is java.lang.String but parameter is long
    private static final String FIND_RETURNS_BY_CUSTOMER_UID_NUMERIC =
            "SELECT {r." + IS32ReturnRequestModel.PK + "} " +
            "FROM {" + IS32ReturnRequestModel._TYPECODE + " AS r} " +
            "WHERE {r." + IS32ReturnRequestModel.CUSTOMERUID + "} = ?customerUid " +
            "AND {r." + IS32ReturnRequestModel.RETURNSTATUS + "} <> ?closedStatus " +
            "ORDER BY {r." + IS32ReturnRequestModel.CREATEDDATE + "} DESC";

    // SLOW-04: warehouseCode is String but parameter is int; allocatedQty is Integer but parameter is String
    private static final String GET_FULFILLMENT_ALLOCATION_REPORT =
            "SELECT {f." + IS32FulfillmentEntryModel.ENTRYID + "}, " +
            "{f." + IS32FulfillmentEntryModel.ORDERCODE + "}, " +
            "{f." + IS32FulfillmentEntryModel.QUANTITY + "}, " +
            "{wa." + IS32WarehouseAllocationModel.ALLOCATEDQTY + "}, " +
            "{wa." + IS32WarehouseAllocationModel.AVAILABLEQTY + "} " +
            "FROM {" + IS32FulfillmentEntryModel._TYPECODE + " AS f " +
            "JOIN " + IS32WarehouseAllocationModel._TYPECODE + " AS wa " +
            "ON {wa." + IS32WarehouseAllocationModel.WAREHOUSECODE + "} = {f." + IS32FulfillmentEntryModel.WAREHOUSECODE + "}} " +
            "WHERE {f." + IS32FulfillmentEntryModel.WAREHOUSECODE + "} = ?warehouseCode " +
            "AND {wa." + IS32WarehouseAllocationModel.ALLOCATEDQTY + "} >= ?allocatedQty";

    private FlexibleSearchService flexibleSearchService;

    @Override
    public List<IS32FulfillmentEntryModel> findEntriesByOrderCodeNumeric(final long orderCode)
    {
        final Map<String, Object> params = new HashMap<>();
        // SLOW-04: orderCode column is String, but passing Long value
        params.put("orderCode", Long.valueOf(orderCode));
        params.put("cancelledStatus", IS32FulfillmentStatus.CANCELLED);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_ENTRIES_BY_ORDER_CODE_NUMERIC, params);
        final SearchResult<IS32FulfillmentEntryModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<IS32FulfillmentEntryModel> findEntriesByQuantityStr(final String quantity)
    {
        final Map<String, Object> params = new HashMap<>();
        // SLOW-04: quantity column is Integer, but passing String value
        params.put("quantity", quantity);
        params.put("status", IS32FulfillmentStatus.PENDING);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_ENTRIES_BY_QUANTITY, params);
        final SearchResult<IS32FulfillmentEntryModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<IS32FulfillmentEntryModel> findEntriesByPriorityStr(final String priority)
    {
        final Map<String, Object> params = new HashMap<>();
        // SLOW-04: priority column is Integer, but passing String value
        params.put("priority", priority);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_ENTRIES_BY_PRIORITY, params);
        final SearchResult<IS32FulfillmentEntryModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<IS32FulfillmentEntryModel> findEntriesByEntryIdNumeric(final int entryId)
    {
        final Map<String, Object> params = new HashMap<>();
        // SLOW-04: entryId column is String, but passing Integer value
        params.put("entryId", Integer.valueOf(entryId));

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_ENTRIES_BY_ENTRY_ID_NUMERIC, params);
        final SearchResult<IS32FulfillmentEntryModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<IS32ReturnRequestModel> findReturnsByRefundAmount(final String refundAmount)
    {
        final Map<String, Object> params = new HashMap<>();
        // SLOW-04: refundAmount column is Double, but passing String value
        params.put("refundAmount", refundAmount);
        params.put("status", IS32ReturnStatus.REFUNDED);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_RETURNS_BY_REFUND_AMOUNT, params);
        final SearchResult<IS32ReturnRequestModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<IS32ReturnRequestModel> findReturnsByReturnQuantity(final String returnQuantity)
    {
        final Map<String, Object> params = new HashMap<>();
        // SLOW-04: returnQuantity column is Integer, but passing String value
        params.put("returnQuantity", returnQuantity);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_RETURNS_BY_RETURN_QUANTITY, params);
        final SearchResult<IS32ReturnRequestModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<IS32ReturnRequestModel> findReturnsByCustomerUidNumeric(final long customerUid)
    {
        final Map<String, Object> params = new HashMap<>();
        // SLOW-04: customerUid column is String, but passing Long value
        params.put("customerUid", Long.valueOf(customerUid));
        params.put("closedStatus", IS32ReturnStatus.CLOSED);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_RETURNS_BY_CUSTOMER_UID_NUMERIC, params);
        final SearchResult<IS32ReturnRequestModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<List<Object>> getFulfillmentAllocationReport(final int warehouseCode,
                                                               final String allocatedQty)
    {
        final Map<String, Object> params = new HashMap<>();
        // SLOW-04: warehouseCode column is String, but passing Integer value
        params.put("warehouseCode", Integer.valueOf(warehouseCode));
        // SLOW-04: allocatedQty column is Integer, but passing String value
        params.put("allocatedQty", allocatedQty);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(GET_FULFILLMENT_ALLOCATION_REPORT, params);
        query.setResultClassList(Arrays.asList(String.class, String.class, Integer.class, Integer.class, Integer.class));

        final SearchResult<List<Object>> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    public void setFlexibleSearchService(final FlexibleSearchService flexibleSearchService)
    {
        this.flexibleSearchService = flexibleSearchService;
    }
}
