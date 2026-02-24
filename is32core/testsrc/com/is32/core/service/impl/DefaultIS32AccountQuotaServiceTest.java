package com.is32.core.service.impl;

import com.is32.core.dao.IS32AccountQuotaDao;
import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.catalog.CatalogVersionService;
import de.hybris.platform.catalog.model.CatalogVersionModel;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.servicelayer.user.UserService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class DefaultIS32AccountQuotaServiceTest
{
    @InjectMocks
    private DefaultIS32AccountQuotaService accountQuotaService;

    @Mock
    private IS32AccountQuotaDao is32AccountQuotaDao;

    @Mock
    private UserService userService;

    @Mock
    private CatalogVersionService catalogVersionService;

    @Mock
    private CustomerModel customer;

    @Mock
    private CatalogVersionModel catalogVersion;

    @Before
    public void setUp()
    {
        when(customer.getUid()).thenReturn("testcustomer@test.com");
    }

    @Test
    public void testGetAccountQuotasAggregation()
    {
        final List<List<Object>> rawData = new ArrayList<>();
        // Two rows for same account - one with userPk (redeemed), one without
        rawData.add(Arrays.asList("ACCT_001", "SBL_001", 10, "somePk"));
        rawData.add(Arrays.asList("ACCT_001", "SBL_001", 10, null));
        rawData.add(Arrays.asList("ACCT_002", "SBL_002", 20, null));

        when(is32AccountQuotaDao.findAccountQuotaRawData(eq(customer), eq(catalogVersion), any(Date.class)))
                .thenReturn(rawData);

        final List<Map<String, Object>> result = accountQuotaService.getAccountQuotas(customer, catalogVersion);

        assertNotNull(result);
        assertEquals("Should have 2 aggregated accounts", 2, result.size());

        // Find ACCT_001
        final Map<String, Object> acct1 = result.stream()
                .filter(m -> "ACCT_001".equals(m.get("accountId")))
                .findFirst()
                .orElse(null);

        assertNotNull("ACCT_001 should exist", acct1);
        assertEquals("ACCT_001 orderedAmt should be 1 (one redemption)", 1, acct1.get("orderedAmt"));

        // Find ACCT_002
        final Map<String, Object> acct2 = result.stream()
                .filter(m -> "ACCT_002".equals(m.get("accountId")))
                .findFirst()
                .orElse(null);

        assertNotNull("ACCT_002 should exist", acct2);
        assertEquals("ACCT_002 orderedAmt should be 0 (no redemption)", 0, acct2.get("orderedAmt"));
    }

    @Test
    public void testGetAccountQuotasEmptyResult()
    {
        when(is32AccountQuotaDao.findAccountQuotaRawData(eq(customer), eq(catalogVersion), any(Date.class)))
                .thenReturn(Collections.emptyList());

        final List<Map<String, Object>> result = accountQuotaService.getAccountQuotas(customer, catalogVersion);

        assertNotNull(result);
        assertTrue("Should be empty when no raw data", result.isEmpty());
    }

    @Test
    public void testGetAccountQuotasMultipleRedemptions()
    {
        final List<List<Object>> rawData = new ArrayList<>();
        rawData.add(Arrays.asList("ACCT_001", "SBL_001", 5, "pk1"));
        rawData.add(Arrays.asList("ACCT_001", "SBL_001", 5, "pk2"));
        rawData.add(Arrays.asList("ACCT_001", "SBL_001", 5, "pk3"));

        when(is32AccountQuotaDao.findAccountQuotaRawData(eq(customer), eq(catalogVersion), any(Date.class)))
                .thenReturn(rawData);

        final List<Map<String, Object>> result = accountQuotaService.getAccountQuotas(customer, catalogVersion);

        assertEquals(1, result.size());
        assertEquals("Should sum 3 redemptions", 3, result.get(0).get("orderedAmt"));
    }

    @Test
    public void testGetRedemptionCount()
    {
        when(is32AccountQuotaDao.getRedemptionCountForCustomer("PROMO_001", customer)).thenReturn(5);

        final int count = accountQuotaService.getRedemptionCount("PROMO_001", customer);

        assertEquals(5, count);
    }

    @Test
    public void testGetAccountQuotasForCurrentUserNonCustomer()
    {
        final de.hybris.platform.core.model.user.UserModel anonymousUser =
                mock(de.hybris.platform.core.model.user.UserModel.class);
        when(anonymousUser.getUid()).thenReturn("anonymous");
        when(userService.getCurrentUser()).thenReturn(anonymousUser);

        final List<Map<String, Object>> result = accountQuotaService.getAccountQuotasForCurrentUser();

        assertNotNull(result);
        assertTrue("Should return empty for non-customer user", result.isEmpty());
    }
}
