package com.is32.core.dao.impl;

import com.is32.core.enums.IS32PromotionDisplayType;
import com.is32.core.enums.IS32PromotionStatus;
import com.is32.core.enums.IS32RewardType;
import de.hybris.bootstrap.annotations.IntegrationTest;
import de.hybris.platform.catalog.CatalogVersionService;
import de.hybris.platform.catalog.model.CatalogVersionModel;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.impex.jalo.ImpExException;
import de.hybris.platform.servicelayer.ServicelayerTransactionalTest;
import de.hybris.platform.servicelayer.user.UserService;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@IntegrationTest
public class DefaultIS32AccountQuotaDaoTest extends ServicelayerTransactionalTest
{
    private static final String TEST_CUSTOMER_UID = "is32testcustomer@test.com";
    private static final String TEST_CATALOG_ID = "is32TestCatalog";
    private static final String TEST_CATALOG_VERSION = "Online";

    @Resource
    private DefaultIS32AccountQuotaDao defaultIS32AccountQuotaDao;

    @Resource
    private UserService userService;

    @Resource
    private CatalogVersionService catalogVersionService;

    @Before
    public void setUp() throws ImpExException
    {
        importCsv("/test/testdata-is32-account-quota.impex", "utf-8");
    }

    @Test
    public void testFindAccountQuotaRawDataReturnsResults()
    {
        final CustomerModel customer = (CustomerModel) userService.getUserForUID(TEST_CUSTOMER_UID);
        final CatalogVersionModel catalogVersion =
                catalogVersionService.getCatalogVersion(TEST_CATALOG_ID, TEST_CATALOG_VERSION);

        final List<List<Object>> results =
                defaultIS32AccountQuotaDao.findAccountQuotaRawData(customer, catalogVersion, new Date());

        assertNotNull("Result should not be null", results);
    }

    @Test
    public void testFindAccountQuotaRawDataRowStructure()
    {
        final CustomerModel customer = (CustomerModel) userService.getUserForUID(TEST_CUSTOMER_UID);
        final CatalogVersionModel catalogVersion =
                catalogVersionService.getCatalogVersion(TEST_CATALOG_ID, TEST_CATALOG_VERSION);

        final List<List<Object>> results =
                defaultIS32AccountQuotaDao.findAccountQuotaRawData(customer, catalogVersion, new Date());

        if (!results.isEmpty())
        {
            final List<Object> firstRow = results.get(0);
            assertEquals("Each row should contain 4 fields", 4, firstRow.size());
            assertTrue("accountId should be a String", firstRow.get(0) instanceof String);
            assertTrue("siebelAcctId should be a String", firstRow.get(1) instanceof String);
            assertTrue("threshold should be an Integer", firstRow.get(2) instanceof Integer);
        }
    }

    @Test
    public void testGetRedemptionCountForCustomer()
    {
        final CustomerModel customer = (CustomerModel) userService.getUserForUID(TEST_CUSTOMER_UID);
        final int count = defaultIS32AccountQuotaDao.getRedemptionCountForCustomer("PROMO_TEST_001", customer);
        assertTrue("Redemption count should be >= 0", count >= 0);
    }

    @Test
    public void testFindAccountQuotaRawDataWithNoMatchingPromotions()
    {
        final CustomerModel customer = (CustomerModel) userService.getUserForUID(TEST_CUSTOMER_UID);
        final CatalogVersionModel catalogVersion =
                catalogVersionService.getCatalogVersion(TEST_CATALOG_ID, TEST_CATALOG_VERSION);

        // Using a date far in the past to ensure no active promotions match
        final Date pastDate = new Date(0L);
        final List<List<Object>> results =
                defaultIS32AccountQuotaDao.findAccountQuotaRawData(customer, catalogVersion, pastDate);

        assertNotNull("Result should not be null even with no matches", results);
        assertTrue("Should return empty list for past date", results.isEmpty());
    }
}
