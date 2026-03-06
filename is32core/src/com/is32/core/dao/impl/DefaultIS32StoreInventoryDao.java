package com.is32.core.dao.impl;

import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultIS32StoreInventoryDao
{
    private static final Logger LOG = Logger.getLogger(DefaultIS32StoreInventoryDao.class);

    private static final String AVG_STOCK_LEVEL_BY_WAREHOUSE =
            "SELECT avg({si.stockLevel}) FROM {IS32StoreInventory AS si} " +
            "WHERE {si.warehouse} = ?warehouse " +
            "AND ({si.discontinued} = ?discontinued OR {si.discontinued} IS NULL) " +
            "AND {si.productStatus} != ?excludedStatus";

    private static final String MAX_PRICE_BY_CATEGORY =
            "SELECT max({pp.price}) FROM {IS32ProductPrice AS pp " +
            "JOIN Category AS cat ON {pp.category} = {cat.pk}} " +
            "WHERE {cat.code} = ?categoryCode " +
            "AND {pp.currency} = ?currency " +
            "AND ({pp.validUntil} IS NULL OR {pp.validUntil} >= ?currentDate)";

    private static final String SUM_ALLOCATED_QUANTITY =
            "SELECT SUM({wa.allocatedQty}) FROM {IS32WarehouseAllocation AS wa " +
            "JOIN IS32FulfillmentEntry AS fe ON {fe.allocationId} = {wa.allocationId}} " +
            "WHERE {fe.status} = ?fulfillmentStatus " +
            "AND {wa.warehouseCode} = ?warehouseCode";

    private static final String FIND_LOW_STOCK_ITEMS =
            "SELECT {si.pk} FROM {IS32StoreInventory AS si} " +
            "WHERE {si.warehouse} = ?warehouse " +
            "AND {si.stockLevel} < ?threshold " +
            "AND {si.productStatus} = ?activeStatus " +
            "ORDER BY {si.stockLevel} ASC";

    private FlexibleSearchService flexibleSearchService;

    public Double getAverageStockLevel(final Object warehouse)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("warehouse", warehouse);
        params.put("discontinued", Boolean.FALSE);
        params.put("excludedStatus", "REMOVED");

        final FlexibleSearchQuery query = new FlexibleSearchQuery(AVG_STOCK_LEVEL_BY_WAREHOUSE, params);
        final SearchResult<List<Double>> result = flexibleSearchService.search(query);
        final List<List<Double>> rows = result.getResult();
        if (rows != null && !rows.isEmpty())
        {
            return (Double) ((List) rows.get(0)).get(0);
        }
        return null;
    }

    public Double getMaxPriceByCategory(final String categoryCode, final String currency,
                                         final java.util.Date currentDate)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("categoryCode", categoryCode);
        params.put("currency", currency);
        params.put("currentDate", currentDate);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(MAX_PRICE_BY_CATEGORY, params);
        final SearchResult<List<Double>> result = flexibleSearchService.search(query);
        final List<List<Double>> rows = result.getResult();
        if (rows != null && !rows.isEmpty())
        {
            return (Double) ((List) rows.get(0)).get(0);
        }
        return null;
    }

    public Double getSumAllocatedQuantity(final String fulfillmentStatus, final String warehouseCode)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("fulfillmentStatus", fulfillmentStatus);
        params.put("warehouseCode", warehouseCode);

        final FlexibleSearchQuery query = new FlexibleSearchQuery(SUM_ALLOCATED_QUANTITY, params);
        final SearchResult<List<Double>> result = flexibleSearchService.search(query);
        final List<List<Double>> rows = result.getResult();
        if (rows != null && !rows.isEmpty())
        {
            return (Double) ((List) rows.get(0)).get(0);
        }
        return 0.0;
    }

    public List<Object> findLowStockItems(final Object warehouse, final int threshold)
    {
        final Map<String, Object> params = new HashMap<>();
        params.put("warehouse", warehouse);
        params.put("threshold", threshold);
        params.put("activeStatus", "ACTIVE");

        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_LOW_STOCK_ITEMS, params);
        final SearchResult<Object> result = flexibleSearchService.search(query);
        return result.getResult();
    }

    public void setFlexibleSearchService(final FlexibleSearchService flexibleSearchService)
    {
        this.flexibleSearchService = flexibleSearchService;
    }
}
