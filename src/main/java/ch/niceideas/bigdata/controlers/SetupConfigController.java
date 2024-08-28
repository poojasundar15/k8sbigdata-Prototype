package ch.niceideas.bigdata.controlers;

import ch.niceideas.common.json.JsonWrapper;
import ch.niceideas.common.utils.FileException;
import ch.niceideas.bigdata.model.SetupCommand;
import ch.niceideas.bigdata.services.*;
import ch.niceideas.bigdata.utils.ReturnStatusHelper;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;


@Controller
public class SetupConfigController extends AbstractOperationController {

    private static final Logger logger = Logger.getLogger(SetupConfigController.class);

    public static final String PENDING_SETUP_COMMAND = "PENDING_SETUP_COMMAND";

    @Value("${build.version}")
    private String buildVersion = "DEV-SNAPSHOT";

    @Autowired
    private SetupService setupService;

    @Autowired
    private ConfigurationService configurationService;

    @GetMapping("/load-setup")
    @ResponseBody
    public String loadSetupConfig() {

        try {
            JsonWrapper setupConfig = configurationService.loadSetupConfig();

            JsonWrapper configWrapper = new JsonWrapper(setupConfig.getFormattedValue());

            try {
                setupService.ensureSetupCompleted();
            } catch (SetupException e) {
                logger.debug (e, e);

                configWrapper.setValueForPath("clear", "setup");
                configWrapper.setValueForPath("message", e.getMessage());
            }

            configWrapper.setValueForPath("version", buildVersion);
            configWrapper.setValueForPath("isSnapshot", ApplicationStatusServiceImpl.isSnapshot(buildVersion));

            configWrapper.setValueForPath("processingPending", operationsMonitoringService.isProcessingPending());

            return configWrapper.getFormattedValue();

        } catch (FileException | JSONException e) {
            logger.error(e, e);
            return ReturnStatusHelper.createErrorStatus(e);

        } catch (SetupException e) {
            // this is OK. means application is not initialized
            logger.debug(e, e);
            return ReturnStatusHelper.createClearStatus("missing", operationsMonitoringService.isProcessingPending(), map -> {
                map.put("version", buildVersion);
                map.put("isSnapshot", ApplicationStatusServiceImpl.isSnapshot(buildVersion));
            });
        }
    }


    @PostMapping("/save-setup")
    @ResponseBody
    @PreAuthorize("hasAuthority('ADMIN')")
    public String saveSetup(@RequestBody String configAsString, HttpSession session) {

        logger.info("Got config : " + configAsString);

        try {

            // Create SetupCommand
            SetupCommand command = setupService.saveAndPrepareSetup(configAsString);

            // store command and config in HTTP Session
            session.setAttribute(PENDING_SETUP_COMMAND, command);

            return returnCommand (command);

        } catch (SetupException e) {
            logger.error(e, e);
            return ReturnStatusHelper.createEncodedErrorStatus(e);
        }
    }

    private String returnCommand(SetupCommand command) {
        return ReturnStatusHelper.createOKStatus(map -> map.put("command", command.toJSON()));
    }

    @PostMapping("/apply-setup")
    @ResponseBody
    @PreAuthorize("hasAuthority('ADMIN')")
    public String applySetup(HttpSession session) {

        try {
            JSONObject checkObject = checkOperations("Unfortunately, changing setup configuration is not possible in DEMO mode.");
            if (checkObject != null) {
                return checkObject.toString(2);
            }

            SetupCommand command = (SetupCommand) session.getAttribute(PENDING_SETUP_COMMAND);
            session.removeAttribute(PENDING_SETUP_COMMAND);

            setupService.applySetup(command);

            return ReturnStatusHelper.createOKStatus();

        } catch (SetupException e) {
            logger.error(e, e);
            return ReturnStatusHelper.createErrorStatus (e);
        }
    }

}
