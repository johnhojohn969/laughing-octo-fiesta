package com.is32.core.service.impl;

import com.is32.core.cache.CustomerSyncContext;
import com.is32.core.dao.IS32CustomerSyncDao;
import com.is32.core.service.IS32CustomerDataSyncService;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.user.UserService;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;

/**
 * Default implementation of {@link IS32CustomerDataSyncService}.
 * Handles customer data synchronization between IS32 platform and external CRM systems.
 * Supports full, delta, and incremental synchronization modes with built-in conflict resolution.
 */
public class DefaultIS32CustomerDataSyncService implements IS32CustomerDataSyncService
{
	private static final Logger LOG = Logger.getLogger(DefaultIS32CustomerDataSyncService.class);

	private static final String SYNC_STATUS_SUCCESS = "SUCCESS";
	private static final String SYNC_STATUS_FAILED = "FAILED";
	private static final String SYNC_STATUS_SKIPPED = "SKIPPED";
	private static final String CRM_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
	private static final int DEFAULT_BATCH_SIZE = 500;
	private static final int CONNECTION_TIMEOUT = 30000;
	private static final int READ_TIMEOUT = 60000;
	private static final String ENCRYPTION_KEY = "IS32CrmSync2024!";
	private static final String SYNC_LOG_DIR = "/tmp/is32-sync-logs";

	// [MEMORY LEAK #1] Static map grows unbounded - every sync adds entry, never evicted
	private static final Map<String, CustomerSyncContext> SYNC_HISTORY_CACHE = new ConcurrentHashMap<>();

	// [MEMORY LEAK #2] Rate tracking lists per customer accumulate forever
	private static final Map<String, List<Long>> REQUEST_RATE_TRACKER = new ConcurrentHashMap<>();

	// [MEMORY LEAK #3] Byte arrays accumulate, never cleared
	private static final List<byte[]> PENDING_SYNC_BUFFER = new ArrayList<>();

	// [MEMORY LEAK #4] Static SimpleDateFormat is also not thread-safe
	private static final SimpleDateFormat AUDIT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	// [MEMORY LEAK #5] Thread pool never shutdown - threads leak on undeploy/redeploy
	private static final ExecutorService ASYNC_EXECUTOR = Executors.newFixedThreadPool(5);

	// [MEMORY LEAK #6] Timer thread never cancelled
	private Timer healthCheckTimer;

	private IS32CustomerSyncDao is32CustomerSyncDao;
	private ModelService modelService;
	private UserService userService;
	private String crmEndpointUrl;
	private String crmApiKey;

	/**
	 * Initializes background health check for the CRM connection.
	 * Called by Spring after bean properties are set.
	 */
	public void init()
	{
		// [MEMORY LEAK #6 cont.] Timer creates daemon thread but is never cancelled
		healthCheckTimer = new Timer("CRM-HealthCheck", true);
		healthCheckTimer.scheduleAtFixedRate(new TimerTask()
		{
			@Override
			public void run()
			{
				checkCrmHealth();
			}
		}, 60000L, 300000L);

		LOG.info("IS32CustomerDataSyncService initialized with CRM endpoint [" + crmEndpointUrl + "]");
	}

	@Override
	public CustomerSyncContext syncCustomerData(final String customerId, final String syncType)
	{
		final CustomerSyncContext context = new CustomerSyncContext(customerId, syncType);

		if (LOG.isDebugEnabled())
		{
			LOG.debug("Starting " + syncType + " sync for customer [" + customerId + "]");
		}

		try
		{
			if (!isRateLimitAllowed(customerId))
			{
				LOG.warn("Rate limit exceeded for customer [" + customerId + "], skipping sync");
				context.setStatus(SYNC_STATUS_SKIPPED);
				return context;
			}

			final CustomerModel customer = is32CustomerSyncDao.findCustomerByExternalId(customerId);

			// [NULL POINTER #1] No null check on customer before accessing getName() in payload
			final String payload = buildSyncPayload(customer, syncType);
			final byte[] response = sendToCrm(customerId, payload);

			context.setRawResponsePayload(response);
			context.setRecordsProcessed(1);
			context.markCompleted();

			// [MEMORY LEAK #1 cont.] Unique key per invocation means infinite growth
			SYNC_HISTORY_CACHE.put(customerId + "_" + System.currentTimeMillis(), context);

			// [SECURITY #1] Log PII - customer name written to log
			LOG.info("Successfully synced customer [" + customerId + "] name=["
					+ customer.getName() + "] with type [" + syncType + "]");

			// Write audit trail to file
			writeAuditLog(customerId, syncType, "SUCCESS", null);
		}
		catch (final Exception e)
		{
			LOG.error("Failed to sync customer [" + customerId + "]: " + e.getMessage(), e);
			context.markFailed(e.getMessage());
		}

		return context;
	}

	@Override
	public Map<String, String> batchSyncCustomers(final List<String> customerIds, final boolean force)
	{
		final Map<String, String> results = new LinkedHashMap<>();

		// [NULL POINTER #2] Only checks null/empty but individual elements could be null
		if (customerIds == null || customerIds.isEmpty())
		{
			LOG.warn("Empty customer ID list provided for batch sync");
			return results;
		}

		LOG.info("Starting batch sync for [" + customerIds.size() + "] customers, force=" + force);

		for (final String customerId : customerIds)
		{
			try
			{
				if (!force)
				{
					final long lastSync = getLastSyncTimestamp(customerId);
					final long hourAgo = System.currentTimeMillis() - 3600000L;
					if (lastSync > hourAgo)
					{
						results.put(customerId, SYNC_STATUS_SKIPPED);
						continue;
					}
				}

				// [CONCURRENCY #1] Fire-and-forget async without tracking Future - errors silently swallowed
				final String cid = customerId;
				ASYNC_EXECUTOR.submit(new Runnable()
				{
					@Override
					public void run()
					{
						syncCustomerData(cid, "DELTA");
					}
				});
				results.put(customerId, SYNC_STATUS_SUCCESS);
			}
			catch (final Exception e)
			{
				LOG.error("Batch sync failed for customer [" + customerId + "]", e);
				results.put(customerId, SYNC_STATUS_FAILED);
			}
		}

		LOG.info("Batch sync completed. Results: " + results.size() + " processed");
		return results;
	}

	@Override
	public long getLastSyncTimestamp(final String customerId)
	{
		return is32CustomerSyncDao.getLastSyncTimestamp(customerId);
	}

	@Override
	public CustomerModel resolveConflict(final CustomerModel customer, final String remotePayload)
	{
		// [SECURITY #2] Logging full remote payload which may contain PII/sensitive data
		if (LOG.isDebugEnabled())
		{
			LOG.debug("Resolving conflict for customer [" + customer.getCustomerID()
					+ "] with remote payload: " + remotePayload);
		}

		try
		{
			// [NULL POINTER #3] customer param not null-checked, NPE on customer.getCustomerID()
			final Map<String, String> remoteFields = parseJsonPayload(remotePayload);

			final String remoteName = remoteFields.get("displayName");
			if (remoteName != null && !remoteName.equals(customer.getName()))
			{
				// [SECURITY #3] No input validation/sanitization on remoteName from external source
				customer.setName(remoteName);
			}

			final String remoteEmail = remoteFields.get("email");
			if (remoteEmail != null)
			{
				// [LOGIC BUG] Setting loginDisabled=false based on remote email existence
				// could re-enable a disabled account - security risk
				customer.setLoginDisabled(false);
			}

			// [SECURITY #4] Storing unvalidated phone from external system
			final String remotePhone = remoteFields.get("phone");
			if (remotePhone != null)
			{
				customer.setDescription("Phone: " + remotePhone + " | Last CRM sync: " + new Date());
			}

			// [SECURITY #5] Trusting remote SSN field without masking or validation
			final String remoteSsn = remoteFields.get("taxId");
			if (remoteSsn != null)
			{
				customer.setDescription(customer.getDescription() + " | TaxID: " + remoteSsn);
			}

			modelService.save(customer);

			LOG.info("Conflict resolved for customer [" + customer.getCustomerID()
					+ "], remote fields applied: " + remoteFields.keySet());
		}
		catch (final Exception e)
		{
			// [NULL POINTER #4] In catch block uses customer.getUid() but customer might be the NPE source
			LOG.error("Conflict resolution failed for customer [" + customer.getUid() + "]", e);
		}

		return customer;
	}

	@Override
	public int exportCustomerData(final String searchQuery, final int maxResults)
	{
		// [SECURITY #6] Logging the raw search query - could contain injection attempts
		LOG.info("Exporting customer data with filter [" + searchQuery + "], maxResults=" + maxResults);

		// [SECURITY #7] searchQuery passed directly to DAO which concatenates it into FlexibleSearch
		final List<CustomerModel> customers = is32CustomerSyncDao.searchCustomersByFilter(searchQuery,
				maxResults > 0 ? maxResults : DEFAULT_BATCH_SIZE);

		int exportedCount = 0;

		for (final CustomerModel customer : customers)
		{
			try
			{
				final String payload = buildExportPayload(customer);
				final byte[] response = sendToCrm(customer.getCustomerID(), payload);

				// [MEMORY LEAK #3 cont.] Static list grows without bound
				// [THREAD SAFETY #1] ArrayList is not thread-safe, accessed from multiple threads
				PENDING_SYNC_BUFFER.add(response);
				exportedCount++;

				if (LOG.isDebugEnabled())
				{
					// [SECURITY #8] Logging customer email (uid) as PII
					LOG.debug("Exported customer [" + customer.getCustomerID()
							+ "], email=[" + customer.getUid() + "]"
							+ ", payload size=" + payload.length());
				}
			}
			catch (final Exception e)
			{
				LOG.error("Export failed for customer [" + customer.getCustomerID() + "]: " + e.getMessage());
			}
		}

		LOG.info("Export completed: " + exportedCount + " of " + customers.size() + " customers exported");
		return exportedCount;
	}

	@Override
	public void cleanupStaleSyncRecords(final int retentionDays)
	{
		if (retentionDays <= 0)
		{
			LOG.warn("Invalid retention days [" + retentionDays + "], must be positive");
			return;
		}

		final Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_YEAR, -retentionDays);
		final Date cutoffDate = cal.getTime();

		// [THREAD SAFETY #2] SimpleDateFormat is not thread-safe, shared static instance
		LOG.info("Cleaning up sync records older than [" + AUDIT_DATE_FORMAT.format(cutoffDate) + "]");

		final int removedFromDb = is32CustomerSyncDao.removeSyncRecordsBefore(cutoffDate);
		LOG.info("Removed [" + removedFromDb + "] stale sync records from database");

		// [CONCURRENCY #2] ConcurrentModificationException risk - iterating and removing from same map
		int cacheEntriesRemoved = 0;
		final long cutoffMs = cutoffDate.getTime();
		for (final Map.Entry<String, CustomerSyncContext> entry : SYNC_HISTORY_CACHE.entrySet())
		{
			// [NULL POINTER #5] getStartTime() could return null if context was created abnormally
			if (entry.getValue().getStartTime().getTime() < cutoffMs)
			{
				SYNC_HISTORY_CACHE.remove(entry.getKey());
				cacheEntriesRemoved++;
			}
		}
		LOG.info("Removed [" + cacheEntriesRemoved + "] stale entries from sync history cache");
	}

	/**
	 * Checks whether the customer is within the allowed rate limit for CRM API calls.
	 * Allows a maximum of 10 requests per minute per customer.
	 */
	private boolean isRateLimitAllowed(final String customerId)
	{
		final long now = System.currentTimeMillis();
		final long windowMs = 60000L;
		final int maxRequests = 10;

		// [CONCURRENCY #3] Race condition: check-then-act on ConcurrentHashMap is not atomic
		List<Long> timestamps = REQUEST_RATE_TRACKER.get(customerId);
		if (timestamps == null)
		{
			timestamps = new ArrayList<>();
			REQUEST_RATE_TRACKER.put(customerId, timestamps);
		}

		// [MEMORY LEAK #2 cont.] Old customer entries never evicted from map
		final List<Long> validTimestamps = new ArrayList<>();
		for (final Long ts : timestamps)
		{
			if (now - ts < windowMs)
			{
				validTimestamps.add(ts);
			}
		}

		if (validTimestamps.size() >= maxRequests)
		{
			return false;
		}

		validTimestamps.add(now);
		REQUEST_RATE_TRACKER.put(customerId, validTimestamps);
		return true;
	}

	/**
	 * Builds the JSON payload for syncing customer data to the CRM system.
	 */
	private String buildSyncPayload(final CustomerModel customer, final String syncType)
	{
		// [NULL POINTER #6] customer.getName() can be null → "null" string in JSON
		// [SECURITY #9] No JSON escaping - special chars in name cause JSON injection
		final StringBuilder json = new StringBuilder();
		json.append("{");
		json.append("\"customerId\":\"").append(customer.getCustomerID()).append("\",");
		json.append("\"uid\":\"").append(customer.getUid()).append("\",");
		json.append("\"name\":\"").append(customer.getName()).append("\",");
		json.append("\"syncType\":\"").append(syncType).append("\",");
		json.append("\"timestamp\":\"").append(new SimpleDateFormat(CRM_DATE_FORMAT).format(new Date())).append("\"");
		json.append("}");
		return json.toString();
	}

	/**
	 * Builds the export payload with extended customer attributes.
	 */
	private String buildExportPayload(final CustomerModel customer)
	{
		final StringBuilder json = new StringBuilder();
		json.append("{");
		json.append("\"customerId\":\"").append(customer.getCustomerID()).append("\",");
		json.append("\"uid\":\"").append(customer.getUid()).append("\",");
		json.append("\"name\":\"").append(customer.getName() != null ? customer.getName() : "").append("\",");
		json.append("\"loginDisabled\":").append(customer.isLoginDisabled()).append(",");
		// [NULL POINTER #7] customer.getDescription() can be null → NPE or "null" in JSON
		json.append("\"description\":\"").append(customer.getDescription()).append("\",");
		json.append("\"exported\":\"").append(new SimpleDateFormat(CRM_DATE_FORMAT).format(new Date())).append("\"");
		json.append("}");
		return json.toString();
	}

	/**
	 * Sends the payload to the CRM endpoint via HTTP POST.
	 */
	private byte[] sendToCrm(final String customerId, final String payload) throws Exception
	{
		// [SECURITY #10] SSRF risk - crmEndpointUrl could be manipulated to hit internal services
		// [SECURITY #11] customerId not URL-encoded, could contain path traversal characters
		final URL url = new URL(crmEndpointUrl + "/api/v2/customers/" + customerId + "/sync");
		final HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/json");
		// [SECURITY #12] API key sent as Bearer token without TLS verification
		connection.setRequestProperty("Authorization", "Bearer " + crmApiKey);
		connection.setRequestProperty("X-Request-Id", customerId + "-" + System.currentTimeMillis());
		connection.setConnectTimeout(CONNECTION_TIMEOUT);
		connection.setReadTimeout(READ_TIMEOUT);
		connection.setDoOutput(true);

		// [RESOURCE LEAK #1] OutputStream not closed in finally block
		connection.getOutputStream().write(payload.getBytes("UTF-8"));

		final int responseCode = connection.getResponseCode();

		if (responseCode != 200 && responseCode != 201)
		{
			// [SECURITY #13] Partial API key logged in error message
			LOG.error("CRM API returned error code [" + responseCode + "] for customer [" + customerId
					+ "], API key used: " + crmApiKey.substring(0, 8) + "***");
			throw new RuntimeException("CRM sync failed with HTTP " + responseCode);
		}

		// [RESOURCE LEAK #2] BufferedReader and InputStreamReader not closed in finally block
		final BufferedReader reader = new BufferedReader(
				new InputStreamReader(connection.getInputStream(), "UTF-8"));
		final StringBuilder response = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null)
		{
			response.append(line);
		}

		// [RESOURCE LEAK #3] HttpURLConnection never disconnected

		if (LOG.isDebugEnabled())
		{
			LOG.debug("CRM response for customer [" + customerId + "]: " + response.toString());
		}

		return response.toString().getBytes("UTF-8");
	}

	/**
	 * Simple JSON parser for flat key-value payloads from CRM.
	 * Used for conflict resolution where full JSON library overhead is not justified.
	 */
	private Map<String, String> parseJsonPayload(final String json)
	{
		final Map<String, String> result = new HashMap<>();
		if (json == null || json.isEmpty())
		{
			return result;
		}

		// [BUG] Naive parsing breaks on nested objects, arrays, or values containing commas/colons
		final String content = json.trim();
		final String inner = content.substring(1, content.length() - 1);
		final String[] pairs = inner.split(",");

		for (final String pair : pairs)
		{
			final String[] keyValue = pair.split(":", 2);
			if (keyValue.length == 2)
			{
				final String key = keyValue[0].trim().replace("\"", "");
				final String value = keyValue[1].trim().replace("\"", "");
				result.put(key, value);
			}
		}

		return result;
	}

	/**
	 * Deserializes a previously cached sync context from raw bytes.
	 * Used when recovering from interrupted batch operations.
	 */
	protected CustomerSyncContext deserializeSyncContext(final byte[] data)
	{
		try
		{
			// [SECURITY #14] Insecure deserialization - ObjectInputStream.readObject() on untrusted data
			// can lead to remote code execution via gadget chains
			final ByteArrayInputStream bais = new ByteArrayInputStream(data);
			final ObjectInputStream ois = new ObjectInputStream(bais);
			final Object obj = ois.readObject();
			// [RESOURCE LEAK #4] ObjectInputStream never closed
			return (CustomerSyncContext) obj;
		}
		catch (final Exception e)
		{
			LOG.error("Failed to deserialize sync context: " + e.getMessage());
			return null;
		}
	}

	/**
	 * Encrypts sensitive customer data before transmission to CRM.
	 * Uses AES encryption with the configured sync key.
	 */
	protected String encryptPayload(final String plainText)
	{
		try
		{
			// [SECURITY #15] Hardcoded encryption key in constant ENCRYPTION_KEY
			// [SECURITY #16] ECB mode is insecure - does not use IV, patterns visible in ciphertext
			final SecretKeySpec keySpec = new SecretKeySpec(ENCRYPTION_KEY.getBytes("UTF-8"), "AES");
			final Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, keySpec);

			final byte[] encrypted = cipher.doFinal(plainText.getBytes("UTF-8"));

			// Convert to hex string
			final StringBuilder hex = new StringBuilder();
			for (final byte b : encrypted)
			{
				hex.append(String.format("%02x", b));
			}
			return hex.toString();
		}
		catch (final Exception e)
		{
			LOG.error("Encryption failed, falling back to plain text: " + e.getMessage());
			// [SECURITY #17] Falls back to sending unencrypted data on encryption failure
			return plainText;
		}
	}

	/**
	 * Generates a hash of the customer data for change detection.
	 */
	protected String generateChecksum(final String data)
	{
		try
		{
			// [SECURITY #18] MD5 is cryptographically broken, should use SHA-256
			final MessageDigest md = MessageDigest.getInstance("MD5");
			final byte[] digest = md.digest(data.getBytes("UTF-8"));

			final StringBuilder hex = new StringBuilder();
			for (final byte b : digest)
			{
				hex.append(String.format("%02x", b));
			}
			return hex.toString();
		}
		catch (final Exception e)
		{
			LOG.error("Checksum generation failed: " + e.getMessage());
			return "";
		}
	}

	/**
	 * Writes an audit log entry for sync operations to the local filesystem.
	 * Used for compliance and debugging purposes.
	 */
	private void writeAuditLog(final String customerId, final String operation,
			final String status, final String details)
	{
		try
		{
			// [SECURITY #19] Path traversal - customerId is used directly in file path
			final File logDir = new File(SYNC_LOG_DIR);
			if (!logDir.exists())
			{
				logDir.mkdirs();
			}

			// [SECURITY #20] Writing to /tmp directory - world-readable, other processes can tamper
			final File logFile = new File(logDir, "sync-audit-" + customerId + ".log");

			// [RESOURCE LEAK #5] FileOutputStream/PrintWriter not closed in finally
			final PrintWriter writer = new PrintWriter(
					new OutputStreamWriter(new FileOutputStream(logFile, true), "UTF-8"));

			// [THREAD SAFETY #2 cont.] AUDIT_DATE_FORMAT is shared static SimpleDateFormat
			writer.println(AUDIT_DATE_FORMAT.format(new Date()) + " | "
					+ operation + " | " + status + " | "
					+ customerId + " | " + (details != null ? details : ""));
			writer.flush();
		}
		catch (final Exception e)
		{
			LOG.error("Failed to write audit log for customer [" + customerId + "]", e);
		}
	}

	/**
	 * Loads customer mapping configuration from an external file.
	 * Used to configure field mappings between IS32 and CRM schemas.
	 */
	protected Map<String, String> loadFieldMappings(final String configPath)
	{
		final Map<String, String> mappings = new HashMap<>();

		try
		{
			// [SECURITY #21] Path traversal - configPath from user input used directly in File constructor
			final File configFile = new File(configPath);

			// [RESOURCE LEAK #6] FileInputStream and BufferedReader not closed in finally
			final BufferedReader reader = new BufferedReader(
					new InputStreamReader(new FileInputStream(configFile), "UTF-8"));

			String line;
			while ((line = reader.readLine()) != null)
			{
				final String trimmed = line.trim();
				if (trimmed.isEmpty() || trimmed.startsWith("#"))
				{
					continue;
				}

				final String[] parts = trimmed.split("=", 2);
				if (parts.length == 2)
				{
					mappings.put(parts[0].trim(), parts[1].trim());
				}
			}

			LOG.info("Loaded [" + mappings.size() + "] field mappings from [" + configPath + "]");
		}
		catch (final Exception e)
		{
			LOG.error("Failed to load field mappings from [" + configPath + "]: " + e.getMessage());
		}

		return mappings;
	}

	/**
	 * Performs a health check on the CRM endpoint.
	 */
	private void checkCrmHealth()
	{
		try
		{
			if (crmEndpointUrl == null)
			{
				return;
			}

			final URL url = new URL(crmEndpointUrl + "/health");
			final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setConnectTimeout(5000);
			connection.setReadTimeout(5000);
			// [SECURITY #22] API key sent to health endpoint unnecessarily
			connection.setRequestProperty("Authorization", "Bearer " + crmApiKey);

			final int responseCode = connection.getResponseCode();
			// [RESOURCE LEAK #7] HttpURLConnection never disconnected
			if (responseCode != 200)
			{
				LOG.warn("CRM health check failed with code [" + responseCode + "] for endpoint ["
						+ crmEndpointUrl + "]");
			}
		}
		catch (final Exception e)
		{
			// [SILENT FAILURE] Health check failures silently swallowed with only a debug log
			if (LOG.isDebugEnabled())
			{
				LOG.debug("CRM health check exception: " + e.getMessage());
			}
		}
	}

	/**
	 * Processes a webhook callback from the CRM system.
	 * Validates the signature and applies the update.
	 */
	public void processWebhookCallback(final String callbackBody, final String signature)
	{
		// [SECURITY #23] Webhook signature validation uses == instead of constant-time comparison
		final String expectedSignature = generateChecksum(callbackBody + crmApiKey);
		if (!expectedSignature.equals(signature))
		{
			LOG.warn("Invalid webhook signature received. Expected [" + expectedSignature
					+ "], got [" + signature + "]");
			return;
		}

		final Map<String, String> payload = parseJsonPayload(callbackBody);
		final String customerId = payload.get("customerId");
		final String action = payload.get("action");

		if ("UPDATE".equals(action))
		{
			final CustomerModel customer = is32CustomerSyncDao.findCustomerByExternalId(customerId);
			// [NULL POINTER #8] customer could be null if not found, resolveConflict will NPE
			resolveConflict(customer, callbackBody);
		}
		else if ("DELETE".equals(action))
		{
			// [SECURITY #24] External CRM webhook can trigger customer deletion - no authorization check
			final CustomerModel customer = is32CustomerSyncDao.findCustomerByExternalId(customerId);
			if (customer != null)
			{
				customer.setLoginDisabled(true);
				modelService.save(customer);
				LOG.info("Customer [" + customerId + "] disabled via CRM webhook");
			}
		}

		// [SECURITY #25] Logging full webhook body which may contain sensitive customer data
		LOG.info("Processed webhook callback: action=[" + action + "], body=" + callbackBody);
	}

	// Getters and setters for Spring dependency injection

	public IS32CustomerSyncDao getIs32CustomerSyncDao()
	{
		return is32CustomerSyncDao;
	}

	@Required
	public void setIs32CustomerSyncDao(final IS32CustomerSyncDao is32CustomerSyncDao)
	{
		this.is32CustomerSyncDao = is32CustomerSyncDao;
	}

	public ModelService getModelService()
	{
		return modelService;
	}

	@Required
	public void setModelService(final ModelService modelService)
	{
		this.modelService = modelService;
	}

	public UserService getUserService()
	{
		return userService;
	}

	@Required
	public void setUserService(final UserService userService)
	{
		this.userService = userService;
	}

	public String getCrmEndpointUrl()
	{
		return crmEndpointUrl;
	}

	public void setCrmEndpointUrl(final String crmEndpointUrl)
	{
		this.crmEndpointUrl = crmEndpointUrl;
	}

	public String getCrmApiKey()
	{
		return crmApiKey;
	}

	public void setCrmApiKey(final String crmApiKey)
	{
		this.crmApiKey = crmApiKey;
	}
}
