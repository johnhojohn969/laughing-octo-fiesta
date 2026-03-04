package com.is32.fulfillment.dao.impl;

import com.is32.fulfillment.dao.IS32FulfillmentEntryDao;
import com.is32.fulfillment.model.IS32FulfillmentEntryModel;
import com.is32.fulfillment.model.IS32ShipmentTrackingModel;
import com.is32.fulfillment.model.IS32ReturnRequestModel;
import com.is32.fulfillment.model.IS32WarehouseAllocationModel;
import com.is32.fulfillment.enums.IS32FulfillmentStatus;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;
import de.hybris.platform.servicelayer.model.ModelService;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultIS32FulfillmentEntryDao implements IS32FulfillmentEntryDao
{
    private static final Logger LOG = Logger.getLogger(DefaultIS32FulfillmentEntryDao.class);

    private static final String FIND_BY_ENTRY_ID =
            "SELECT {f." + IS32FulfillmentEntryModel.PK + "} " +
            "FROM {" + IS32FulfillmentEntryModel._TYPECODE + " AS f} " +
            "WHERE {f." + IS32FulfillmentEntryModel.ENTRYID + "} = ?entryId";

    private static final String FIND_BY_ORDER_CODE =
            "SELECT {f." + IS32FulfillmentEntryModel.PK + "} " +
            "FROM {" + IS32FulfillmentEntryModel._TYPECODE + " AS f} " +
            "WHERE {f." + IS32FulfillmentEntryModel.ORDERCODE + "} = ?orderCode";

    private static final String FIND_BY_STATUS =
            "SELECT {f." + IS32FulfillmentEntryModel.PK + "} " +
            "FROM {" + IS32FulfillmentEntryModel._TYPECODE + " AS f} " +
            "WHERE {f." + IS32FulfillmentEntryModel.STATUS + "} = ?status";

    private static final String FIND_BY_ORDER_AND_STATUS =
            "SELECT {f." + IS32FulfillmentEntryModel.PK + "} " +
            "FROM {" + IS32FulfillmentEntryModel._TYPECODE + " AS f} " +
            "WHERE {f." + IS32FulfillmentEntryModel.ORDERCODE + "} = ?orderCode " +
            "AND {f." + IS32FulfillmentEntryModel.STATUS + "} = ?status";

    private static final String FIND_BY_WAREHOUSE =
            "SELECT {f." + IS32FulfillmentEntryModel.PK + "} " +
            "FROM {" + IS32FulfillmentEntryModel._TYPECODE + " AS f} " +
            "WHERE {f." + IS32FulfillmentEntryModel.WAREHOUSECODE + "} = ?warehouseCode";

    private static final String FIND_WITH_SHIPMENTS =
            "SELECT {f." + IS32FulfillmentEntryModel.PK + "}, " +
            "{s." + IS32ShipmentTrackingModel.TRACKINGNUMBER + "}, " +
            "{s." + IS32ShipmentTrackingModel.CARRIER + "}, " +
            "{s." + IS32ShipmentTrackingModel.SHIPMENTSTATUS + "} " +
            "FROM {" + IS32FulfillmentEntryModel._TYPECODE + " AS f " +
            "LEFT JOIN " + IS32ShipmentTrackingModel._TYPECODE + " AS s " +
            "ON {f." + IS32FulfillmentEntryModel.ENTRYID + "} = {s." + IS32ShipmentTrackingModel.ENTRYID + "}} " +
            "WHERE {f." + IS32FulfillmentEntryModel.STATUS + "} = ?status " +
            "AND {f." + IS32FulfillmentEntryModel.CREATEDDATE + "} >= ?fromDate";

    private static final String GET_ORDER_FULFILLMENT_REPORT =
            "SELECT {f." + IS32FulfillmentEntryModel.ORDERCODE + "}, " +
            "{f." + IS32FulfillmentEntryModel.PRODUCTCODE + "}, " +
            "{f." + IS32FulfillmentEntryModel.STATUS + "}, " +
            "{f." + IS32FulfillmentEntryModel.QUANTITY + "}, " +
            "{s." + IS32ShipmentTrackingModel.TRACKINGNUMBER + "}, " +
            "{s." + IS32ShipmentTrackingModel.CARRIER + "}, " +
            "{s." + IS32ShipmentTrackingModel.SHIPMENTSTATUS + "}, " +
            "{wa." + IS32WarehouseAllocationModel.ALLOCATEDQTY + "}, " +
            "{wa." + IS32WarehouseAllocationModel.AVAILABLEQTY + "}, " +
            "{r." + IS32ReturnRequestModel.REQUESTID + "}, " +
            "{r." + IS32ReturnRequestModel.RETURNSTATUS + "} " +
            "FROM {" + IS32FulfillmentEntryModel._TYPECODE + " AS f " +
            "LEFT JOIN " + IS32ShipmentTrackingModel._TYPECODE + " AS s " +
            "ON {f." + IS32FulfillmentEntryModel.ENTRYID + "} = {s." + IS32ShipmentTrackingModel.ENTRYID + "} " +
            "LEFT JOIN " + IS32WarehouseAllocationModel._TYPECODE + " AS wa " +
            "ON {wa." + IS32WarehouseAllocationModel.ENTRYID + "} = {f." + IS32FulfillmentEntryModel.ENTRYID + "} " +
            "LEFT JOIN " + IS32ReturnRequestModel._TYPECODE + " AS r " +
            "ON {r." + IS32ReturnRequestModel.ORDERCODE + "} = {f." + IS32FulfillmentEntryModel.ORDERCODE + "}} " +
            "WHERE {f." + IS32FulfillmentEntryModel.WAREHOUSECODE + "} = ?warehouseCode " +
            "AND {f." + IS32FulfillmentEntryModel.CREATEDDATE + "} >= ?startDate " +
            "AND {f." + IS32FulfillmentEntryModel.CREATEDDATE + "} <= ?endDate";

    private static final String FIND_BY_CUSTOMER_UID =
            "SELECT {f." + IS32FulfillmentEntryModel.PK + "} " +
            "FROM {" + IS32FulfillmentEntryModel._TYPECODE + " AS f} " +
            "WHERE {f." + IS32FulfillmentEntryModel.CUSTOMERUID + "} = ?customerUid " +
            "ORDER BY {f." + IS32FulfillmentEntryModel.CREATEDDATE + "} DESC";

    private static final String FIND_PENDING_BY_WAREHOUSE =
            "SELECT {f." + IS32FulfillmentEntryModel.PK + "} " +
            "FROM {" + IS32FulfillmentEntryModel._TYPECODE + " AS f} " +
            "WHERE {f." + IS32FulfillmentEntryModel.WAREHOUSECODE + "} = ?warehouseCode " +
            "AND {f." + IS32FulfillmentEntryModel.STATUS + "} = ?status " +
            "AND {f." + IS32FulfillmentEntryModel.CREATEDDATE + "} < ?createdBefore " +
            "ORDER BY {f." + IS32FulfillmentEntryModel.PRIORITY + "} DESC, " +
            "{f." + IS32FulfillmentEntryModel.CREATEDDATE + "} ASC";

    private static final String FIND_BY_PRODUCT_CODE =
            "SELECT {f." + IS32FulfillmentEntryModel.PK + "} " +
            "FROM {" + IS32FulfillmentEntryModel._TYPECODE + " AS f} " +
            "WHERE {f." + IS32FulfillmentEntryModel.PRODUCTCODE + "} = ?productCode";

    private FlexibleSearchService flexibleSearchService;
    private ModelService modelService;

    @Override
    public IS32FulfillmentEntryModel findByEntryId(final String entryId)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("entryId", entryId);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BY_ENTRY_ID, params);
        query.setResultClassList(Collections.singletonList(IS32FulfillmentEntryModel.class));

        final SearchResult<IS32FulfillmentEntryModel> result = flexibleSearchService.search(query);
        return result.getResult().isEmpty() ? null : result.getResult().get(0);
    }

    @Override
    public List<IS32FulfillmentEntryModel> findByOrderCode(final String orderCode)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("orderCode", orderCode);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BY_ORDER_CODE, params);
        final SearchResult<IS32FulfillmentEntryModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<IS32FulfillmentEntryModel> findByStatus(final IS32FulfillmentStatus status)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("status", status);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BY_STATUS, params);
        final SearchResult<IS32FulfillmentEntryModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<IS32FulfillmentEntryModel> findByOrderCodeAndStatus(final String orderCode,
                                                                      final IS32FulfillmentStatus status)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("orderCode", orderCode);
        params.put("status", status);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BY_ORDER_AND_STATUS, params);
        final SearchResult<IS32FulfillmentEntryModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<IS32FulfillmentEntryModel> findByWarehouseCode(final String warehouseCode)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("warehouseCode", warehouseCode);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BY_WAREHOUSE, params);
        final SearchResult<IS32FulfillmentEntryModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<IS32FulfillmentEntryModel> findEntriesWithShipments(final IS32FulfillmentStatus status,
                                                                      final Date fromDate)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("status", status);
        params.put("fromDate", fromDate);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_WITH_SHIPMENTS, params);
        query.setResultClassList(Arrays.asList(IS32FulfillmentEntryModel.class, String.class, String.class, String.class));

        final SearchResult<IS32FulfillmentEntryModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<List<Object>> getOrderFulfillmentReport(final String warehouseCode,
                                                          final Date startDate,
                                                          final Date endDate)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("warehouseCode", warehouseCode);
        params.put("startDate", startDate);
        params.put("endDate", endDate);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(GET_ORDER_FULFILLMENT_REPORT, params);
        query.setResultClassList(Arrays.asList(
                String.class, String.class, String.class, Integer.class,
                String.class, String.class, String.class,
                Integer.class, Integer.class,
                String.class, String.class
        ));

        final SearchResult<List<Object>> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<IS32FulfillmentEntryModel> findByCustomerUid(final String customerUid)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("customerUid", customerUid);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BY_CUSTOMER_UID, params);
        final SearchResult<IS32FulfillmentEntryModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<IS32FulfillmentEntryModel> findPendingEntriesByWarehouse(final String warehouseCode,
                                                                           final Date createdBefore)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("warehouseCode", warehouseCode);
        params.put("status", IS32FulfillmentStatus.PENDING);
        params.put("createdBefore", createdBefore);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_PENDING_BY_WAREHOUSE, params);
        final SearchResult<IS32FulfillmentEntryModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    @Override
    public List<IS32FulfillmentEntryModel> findByProductCode(final String productCode)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("productCode", productCode);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_BY_PRODUCT_CODE, params);
        final SearchResult<IS32FulfillmentEntryModel> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    public void setFlexibleSearchService(final FlexibleSearchService flexibleSearchService)
    {
        this.flexibleSearchService = flexibleSearchService;
    }

    public void setModelService(final ModelService modelService)
    {
        this.modelService = modelService;
    }
}
