package com.is32.core.job;

import com.is32.core.model.IS32PromotionModel;
import com.is32.core.model.IS32PromotionCleanupCronJobModel;
import com.is32.core.enums.IS32PromotionStatus;
import com.is32.core.service.IS32PromotionService;
import de.hybris.platform.cronjob.enums.CronJobResult;
import de.hybris.platform.cronjob.enums.CronJobStatus;
import de.hybris.platform.servicelayer.config.ConfigurationService;
import de.hybris.platform.servicelayer.cronjob.AbstractJobPerformable;
import de.hybris.platform.servicelayer.cronjob.PerformResult;
import de.hybris.platform.servicelayer.model.ModelService;
import org.apache.log4j.Logger;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * CronJob that finds expired IS32 promotions and marks them with EXPIRED status.
 * Optionally removes promotions that have been expired for longer than the configured
 * retention period.
 */
public class IS32PromotionCleanupJob extends AbstractJobPerformable<IS32PromotionCleanupCronJobModel>
{
    private static final Logger LOG = Logger.getLogger(IS32PromotionCleanupJob.class);

    private static final String CONFIG_CLEANUP_DAYS = "is32core.promotion.cleanup.days";
    private static final String CONFIG_BATCH_SIZE = "is32core.promotion.cleanup.batchsize";
    private static final int DEFAULT_CLEANUP_DAYS = 30;
    private static final int DEFAULT_BATCH_SIZE = 100;

    private IS32PromotionService is32PromotionService;
    private ModelService modelService;
    private ConfigurationService configurationService;

    @Override
    public PerformResult perform(final IS32PromotionCleanupCronJobModel cronJob)
    {
        LOG.info("Starting IS32 Promotion cleanup job");

        final int daysBeforeExpiry = cronJob.getDaysBeforeExpiry() != null
                ? cronJob.getDaysBeforeExpiry()
                : configurationService.getConfiguration().getInt(CONFIG_CLEANUP_DAYS, DEFAULT_CLEANUP_DAYS);

        final int batchSize = cronJob.getCleanupBatchSize() != null
                ? cronJob.getCleanupBatchSize()
                : configurationService.getConfiguration().getInt(CONFIG_BATCH_SIZE, DEFAULT_BATCH_SIZE);

        int markedCount = 0;
        int removedCount = 0;

        try
        {
            // Mark expired promotions
            final List<IS32PromotionModel> expiredPromotions =
                    is32PromotionService.getExpiredPromotions(batchSize);

            for (final IS32PromotionModel promotion : expiredPromotions)
            {
                if (clearAbortRequestedIfNeeded(cronJob))
                {
                    LOG.info("Cleanup job aborted by request");
                    return new PerformResult(CronJobResult.UNKNOWN, CronJobStatus.ABORTED);
                }

                if (!IS32PromotionStatus.EXPIRED.equals(promotion.getStatus()))
                {
                    is32PromotionService.updatePromotionStatus(promotion, IS32PromotionStatus.EXPIRED);
                    markedCount++;
                }
            }

            // Remove promotions expired beyond the retention period
            final Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, -daysBeforeExpiry);
            final Date retentionCutoff = calendar.getTime();

            final List<IS32PromotionModel> promotionsToRemove =
                    is32PromotionService.getPromotionsByStatus(IS32PromotionStatus.EXPIRED);

            for (final IS32PromotionModel promotion : promotionsToRemove)
            {
                if (clearAbortRequestedIfNeeded(cronJob))
                {
                    LOG.info("Cleanup job aborted by request during removal phase");
                    return new PerformResult(CronJobResult.UNKNOWN, CronJobStatus.ABORTED);
                }

                if (promotion.getEndDate() != null && promotion.getEndDate().before(retentionCutoff))
                {
                    modelService.remove(promotion);
                    removedCount++;

                    if (removedCount >= batchSize)
                    {
                        LOG.info("Reached batch size limit of [" + batchSize + "] for removals");
                        break;
                    }
                }
            }

            LOG.info("IS32 Promotion cleanup completed. Marked [" + markedCount
                    + "] as expired, removed [" + removedCount + "] past retention period");

            return new PerformResult(CronJobResult.SUCCESS, CronJobStatus.FINISHED);
        }
        catch (final Exception e)
        {
            LOG.error("Error during IS32 Promotion cleanup job", e);
            return new PerformResult(CronJobResult.ERROR, CronJobStatus.FINISHED);
        }
    }

    @Override
    public boolean isAbortable()
    {
        return true;
    }

    public void setIs32PromotionService(final IS32PromotionService is32PromotionService)
    {
        this.is32PromotionService = is32PromotionService;
    }

    @Override
    public void setModelService(final ModelService modelService)
    {
        this.modelService = modelService;
    }

    public void setConfigurationService(final ConfigurationService configurationService)
    {
        this.configurationService = configurationService;
    }
}
