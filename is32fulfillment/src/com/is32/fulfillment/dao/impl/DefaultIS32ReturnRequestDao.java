package com.is32.fulfillment.dao.impl;

import com.is32.fulfillment.dao.IS32ReturnRequestDao;
import com.is32.fulfillment.model.IS32ReturnRequestModel;
import com.is32.fulfillment.model.IS32FulfillmentEntryModel;
import com.is32.fulfillment.model.IS32ShipmentTrackingModel;
import com.is32.fulfillment.model.IS32WarehouseAllocationModel;
import com.is32.fulfillment.enums.IS32ReturnStatus;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultIS32ReturnRequestDao implements IS32ReturnRequestDao
{
    private static final Logger LOG = Logger.getLogger(DefaultIS32ReturnRequestDao.class);

    private static final String FIND_BY_REQUEST_ID =
            "SELECT {r." + IS32ReturnRequestModel.PK + "} " +
            "FROM {" + IS32ReturnRequestModel._TYPECODE + " AS r} " +
            "WHERE {r." + IS32ReturnRequestModel.REQUESTID + "} = ?requestId";

    private static final String FIND_BY_ORDER_CODE =
            "SELECT {r." + IS32ReturnRequestModel.PK + "} " +
            "FROM {" + IS32ReturnRequestModel._TYPECODE + " AS r} " +
            "WHERE {r." + IS32ReturnRequestModel.ORDERCODE + "} = ?orderCode";

    private static final String FIND_BY_CUSTOMER_UID =
            "SELECT {r." + IS32ReturnRequestModel.PK + "} " +
            "FROM {" + IS32ReturnRequestModel._TYPECODE + " AS r} " +
            "WHERE {r." + IS32ReturnRequestModel.CUSTOMERUID + "} = ?customerUid " +
            "ORDER BY {r." + IS32ReturnRequestModel.CREATEDDATE + "} DESC";

    private static final String FIND_BY_STATUS =
            "SELECT {r." + IS32ReturnRequestModel.PK + "} " +
            "FROM {" + IS32ReturnRequestModel._TYPECODE + " AS r} " +
            "WHERE {r." + IS32ReturnRequestModel.RETURNSTATUS + "} = ?returnStatus";

    private static final String FIND_BY_CREATED_DATE_FORMATTED =
            "SELECT {r." + IS32ReturnRequestModel.PK + "} " +
            "FROM {" + IS32ReturnRequestModel._TYPECODE + " AS r} " +
            "WHERE FORMAT({r." + IS32ReturnRequestModel.CREATEDDATE + "}, 'yyyy-MM-dd') = ?dateString";

    private static final String FIND_BY_ORDER_AND_STATUS =
            "SELECT {r." + IS32ReturnRequestModel.PK + "} " +
            "FROM {" + IS32ReturnRequestModel._TYPECODE + " AS r} " +
            "WHERE {r." + IS32ReturnRequestModel.ORDERCODE + "} = ?orderCode " +
            "AND {r." + IS32ReturnRequestModel.RETURNSTATUS + "} = ?returnStatus";

    private static final String FIND_RETURN_WITH_FULFILLMENT =
            "SELECT {r." + IS32ReturnRequestModel.PK + "}, " +
            "{f." + IS32FulfillmentEntryModel.ENTRYID + "}, " +
            "{f." + IS32FulfillmentEntryModel.STATUS + "}, " +
            "{f." + IS32FulfillmentEntryModel.WAREHOUSECODE + "}, " +
            "{s." + IS32ShipmentTrackingModel.TRACKINGNUMBER + "}, " +
            "{s." + IS32ShipmentTrackingModel.CARRIER + "} " +
            "FROM {" + IS32ReturnRequestModel._TYPECODE + " AS r " +
            "JOIN " + IS32FulfillmentEntryModel._TYPECODE + " AS f " +
            "ON {r." + IS32ReturnRequestModel.ORDERCODE + "} = {f." + IS32FulfillmentEntryModel.ORDERCODE + "} " +
            "LEFT JOIN " + IS32ShipmentTrackingModel._TYPECODE + " AS s " +
            "ON {s." + IS32ShipmentTrackingModel.ENTRYID + "} = {f." + IS32FulfillmentEntryModel.ENTRYID + "}} " +
            "WHERE {r." + IS32ReturnRequestModel.ORDERCODE + "} = ?orderCode";

    private static final String GET_RETURN_ANALYTICS =
            "SELECT {r." + IS32ReturnRequestModel.REASON + "}, " +
            "{r." + IS32ReturnRequestModel.RETURNSTATUS + "}, " +
            "{r." + IS32ReturnRequestModel.REFUNDAMOUNT + "}, " +
            "{r." + IS32ReturnRequestModel.PRODUCTCODE + "}, " +
            "{f." + IS32FulfillmentEntryModel.WAREHOUSECODE + "}, " +
            "{f." + IS32FulfillmentEntryModel.QUANTITY + "}, " +
            "{wa." + IS32WarehouseAllocationModel.ALLOCATEDQTY + "}, " +
            "{wa." + IS32WarehouseAllocationModel.AVAILABLEQTY + "} " +
            "FROM {" + IS32ReturnRequestModel._TYPECODE + " AS r " +
            "LEFT JOIN " + IS32FulfillmentEntryModel._TYPECODE + " AS f " +
            "ON {r." + IS32ReturnRequestModel.ORDERCODE + "} = {f." + IS32FulfillmentEntryModel.ORDERCODE + "} " +
            "LEFT JOIN " + IS32WarehouseAllocationModel._TYPECODE + " AS wa " +
            "ON {wa." + IS32WarehouseAllocationModel.WAREHOUSECODE + "} = {f." + IS32FulfillmentEntryModel.WAREHOUSECODE + "} " +
            "AND {wa." + IS32WarehouseAllocationModel.PRODUCTCODE + "} = {f." + IS32FulfillmentEntryModel.PRODUCTCODE + "}} " +
            "WHERE {r." + IS32ReturnRequestModel.CREATEDDATE + "} >= ?startDate " +
            "AND {r." + IS32ReturnRequestModel.CREATEDDATE + "} <= ?endDate";

    private static final String FIND_PENDING_BY_CUSTOMER =
            "SELECT {r." + IS32ReturnRequestModel.PK + "} " +
            "FROM {" + IS32ReturnRequestModel._TYPECODE + " AS r} " +
            "WHERE {r." + IS32ReturnRequestModel.CUSTOMERUID + "} = ?customerUid " +
            "AND {r." + IS32ReturnRequestModel.RETURNSTATUS + "} = ?returnStatus " +
            "ORDER BY {r." + IS32ReturnRequestModel.CREATEDDATE + "} ASC";

    private static final String FIND_BY_PRODUCT_CODE =
            "SELECT {r." + IS32ReturnRequestModel.PK + "} " +
            "FROM {" + IS32ReturnRequestModel._TYPECODE + " AS r} " +
            "WHERE {r." + IS32ReturnRequestModel.PRODUCTCODE + "} = ?productCode";

    private FlexibleSearchService flexibleSearchService;

    @Override
    public IS32ReturnRequestModel findByRequestId(final String requestId)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("requestId", requestId);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BY_REQUEST_ID, params);
        query.setResultClassList(Collections.singletonList(IS32ReturnRequestModel.class));

        final SearchResult<IS32ReturnRequestModel> result = flexibleSearchService.search(query);
        return result.getResult().isEmpty() ? null : result.getResult().get(0);
    }

    @Override
    public List<IS32ReturnRequestModel> findByOrderCode(final String orderCode)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("orderCode", orderCode);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BY_ORDER_CODE, params);
        final SearchResult<IS32ReturnRequestModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<IS32ReturnRequestModel> findByCustomerUid(final String customerUid)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("customerUid", customerUid);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BY_CUSTOMER_UID, params);
        final SearchResult<IS32ReturnRequestModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<IS32ReturnRequestModel> findByStatus(final IS32ReturnStatus status)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("returnStatus", status);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BY_STATUS, params);
        final SearchResult<IS32ReturnRequestModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<IS32ReturnRequestModel> findByCreatedDateFormatted(final String dateString)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("dateString", dateString);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BY_CREATED_DATE_FORMATTED, params);
        final SearchResult<IS32ReturnRequestModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<IS32ReturnRequestModel> findByOrderCodeAndStatus(final String orderCode,
                                                                    final IS32ReturnStatus status)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("orderCode", orderCode);
        params.put("returnStatus", status);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BY_ORDER_AND_STATUS, params);
        final SearchResult<IS32ReturnRequestModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<IS32ReturnRequestModel> findReturnWithFulfillmentDetails(final String orderCode)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("orderCode", orderCode);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_RETURN_WITH_FULFILLMENT, params);
        query.setResultClassList(Arrays.asList(
                IS32ReturnRequestModel.class, String.class, String.class, String.class,
                String.class, String.class
        ));

        final SearchResult<IS32ReturnRequestModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<List<Object>> getReturnAnalyticsReport(final Date startDate, final Date endDate)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("startDate", startDate);
        params.put("endDate", endDate);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(GET_RETURN_ANALYTICS, params);
        query.setResultClassList(Arrays.asList(
                String.class, String.class, Double.class, String.class,
                String.class, Integer.class, Integer.class, Integer.class
        ));

        final SearchResult<List<Object>> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<IS32ReturnRequestModel> findPendingReturnsByCustomer(final String customerUid)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("customerUid", customerUid);
        params.put("returnStatus", IS32ReturnStatus.REQUESTED);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_PENDING_BY_CUSTOMER, params);
        final SearchResult<IS32ReturnRequestModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<IS32ReturnRequestModel> findByProductCode(final String productCode)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("productCode", productCode);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BY_PRODUCT_CODE, params);
        final SearchResult<IS32ReturnRequestModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    public void setFlexibleSearchService(final FlexibleSearchService flexibleSearchService)
    {
        this.flexibleSearchService = flexibleSearchService;
    }
}
