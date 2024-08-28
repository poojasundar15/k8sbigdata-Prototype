package ch.niceideas.bigdata.controlers;

import ch.niceideas.common.utils.FileException;
import ch.niceideas.bigdata.model.ServicesSettingsWrapper;
import ch.niceideas.bigdata.model.SettingsOperationsCommand;
import ch.niceideas.bigdata.services.*;
import ch.niceideas.bigdata.utils.ReturnStatusHelper;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;


@Controller
public class ServicesSettingsController extends AbstractOperationController{

    private static final Logger logger = Logger.getLogger(ServicesSettingsController.class);

    public static final String PENDING_SETTINGS_OPERATIONS_COMMAND = "PENDING_SETTINGS_OPERATIONS_COMMAND";

    @Autowired
    private ServicesSettingsService servicesSettingsService;

    @Autowired
    private ConfigurationService configurationService;

    @GetMapping("/load-services-settings")
    @ResponseBody
    public String loadServicesSettings() {
        try {

            ServicesSettingsWrapper wrapper = configurationService.loadServicesSettings();
            wrapper.setValueForPath("status", "OK");

            return wrapper.getFormattedValue();
        } catch (JSONException | FileException | SetupException e) {
            logger.error(e, e);
            return ReturnStatusHelper.createErrorStatus(e.getMessage());
        }
    }

    @PostMapping("/save-services-settings")
    @ResponseBody
    @PreAuthorize("hasAuthority('ADMIN')")
    public String prepareSaveServicesSettings(@RequestBody String settingsFormAsString, HttpSession session) {

        logger.info("Got config : " + settingsFormAsString);

        try {

            // Create OperationsCommand
            SettingsOperationsCommand command = SettingsOperationsCommand.create(settingsFormAsString, servicesSettingsService);

            // store command and config in HTTP Session
            session.setAttribute(PENDING_SETTINGS_OPERATIONS_COMMAND, command);

            return ReturnStatusHelper.createOKStatus(map -> map.put("command", command.toJSON()));

        } catch (SetupException | FileException | ServicesSettingsException e) {
            logger.error(e, e);
            notificationService.addError("Service Settings Application preparation failed ! " + e.getMessage());
            return ReturnStatusHelper.createEncodedErrorStatus(e);
        }
    }

    @PostMapping("/apply-services-settings")
    @ResponseBody
    @PreAuthorize("hasAuthority('ADMIN')")
    public String saveServicesSettings(HttpSession session) {

        try {

            if (operationsMonitoringService.isProcessingPending()) {
                return ReturnStatusHelper.createOKStatus(map ->
                        map.put("messages", "Some backend operations are currently running. Please retry after they are completed.."));
            }

            SettingsOperationsCommand command = (SettingsOperationsCommand) session.getAttribute(PENDING_SETTINGS_OPERATIONS_COMMAND);
            session.removeAttribute(PENDING_SETTINGS_OPERATIONS_COMMAND);

            servicesSettingsService.applyServicesSettings(command);

            return ReturnStatusHelper.createOKStatus();

        } catch (SystemException | SetupException | FileException e) {
            logger.error(e, e);
            notificationService.addError("Setting application failed ! " + e.getMessage());
            return ReturnStatusHelper.createEncodedErrorStatus(e);
        }
    }
}
