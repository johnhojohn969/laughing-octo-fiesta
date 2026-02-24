package com.is32.core.setup;

import com.is32.core.constants.IS32CoreConstants;
import de.hybris.platform.commerceservices.setup.AbstractSystemSetup;
import de.hybris.platform.core.initialization.SystemSetup;
import de.hybris.platform.core.initialization.SystemSetupContext;
import de.hybris.platform.core.initialization.SystemSetupParameter;
import de.hybris.platform.core.initialization.SystemSetupParameterMethod;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

@SystemSetup(extension = IS32CoreConstants.EXTENSIONNAME)
public class IS32CoreSystemSetup extends AbstractSystemSetup
{
    private static final Logger LOG = Logger.getLogger(IS32CoreSystemSetup.class);

    private static final String IMPORT_CORE_DATA = "importCoreData";
    private static final String IMPORT_SAMPLE_DATA = "importSampleData";

    @SystemSetupParameterMethod
    @Override
    public List<SystemSetupParameter> getInitializationOptions()
    {
        final List<SystemSetupParameter> params = new ArrayList<>();

        params.add(createBooleanSystemSetupParameter(IMPORT_CORE_DATA, "Import Core Data", true));
        params.add(createBooleanSystemSetupParameter(IMPORT_SAMPLE_DATA, "Import Sample Data", true));

        return params;
    }

    @SystemSetup(type = SystemSetup.Type.PROJECT, process = SystemSetup.Process.ALL)
    public void createProjectData(final SystemSetupContext context)
    {
        LOG.info("Starting IS32 Core system setup...");

        if (getBooleanSystemSetupParameter(context, IMPORT_CORE_DATA))
        {
            importImpexFile(context, "/is32core/import/coredata/common/essential-data.impex");
            LOG.info("Imported IS32 core essential data");
        }

        if (getBooleanSystemSetupParameter(context, IMPORT_SAMPLE_DATA))
        {
            importImpexFile(context, "/is32core/import/sampledata/promotions/sample-promotions.impex");
            importImpexFile(context, "/is32core/import/sampledata/promotions/sample-estamptiers.impex");
            LOG.info("Imported IS32 sample data");
        }

        LOG.info("IS32 Core system setup completed");
    }

    @SystemSetup(type = SystemSetup.Type.ESSENTIAL, process = SystemSetup.Process.ALL)
    public void createEssentialData(final SystemSetupContext context)
    {
        LOG.info("Creating IS32 essential data...");
        importImpexFile(context, "/impex/essentialdata-is32core.impex");
        LOG.info("IS32 essential data created");
    }
}
