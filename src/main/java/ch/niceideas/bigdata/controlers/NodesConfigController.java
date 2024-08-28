package ch.niceideas.bigdata.controlers;

import ch.niceideas.common.utils.FileException;
import ch.niceideas.bigdata.model.NodesConfigWrapper;
import ch.niceideas.bigdata.model.NodeServiceOperationsCommand;
import ch.niceideas.bigdata.model.ServicesInstallStatusWrapper;
import ch.niceideas.bigdata.services.*;
import ch.niceideas.bigdata.services.satellite.NodeRangeResolver;
import ch.niceideas.bigdata.services.satellite.NodesConfigurationChecker;
import ch.niceideas.bigdata.services.satellite.NodesConfigurationException;
import ch.niceideas.bigdata.utils.ReturnStatusHelper;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;


@Controller
public class NodesConfigController extends AbstractOperationController {

    private static final Logger logger = Logger.getLogger(NodesConfigController.class);

    public static final String PENDING_OPERATIONS_STATUS_OVERRIDE = "PENDING_KUBERNETES_OPERATIONS_STATUS_OVERRIDE";
    public static final String PENDING_OPERATIONS_COMMAND = "PENDING_KUBERNETES_OPERATIONS_COMMAND";

    @Autowired
    private SetupService setupService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private NodesConfigurationChecker nodesConfigChecker;

    @Autowired
    private ServicesDefinition servicesDefinition;

    @Autowired
    private NodeRangeResolver nodeRangeResolver;

    @Autowired
    private NodesConfigurationService nodesConfigurationService;

    @GetMapping("/load-nodes-config")
    @ResponseBody
    public String loadNodesConfig() {
        try {
            setupService.ensureSetupCompleted();
            NodesConfigWrapper nodesConfig = configurationService.loadNodesConfig();
            if (nodesConfig == null || nodesConfig.isEmpty()) {
                return ReturnStatusHelper.createClearStatus("missing", operationsMonitoringService.isProcessingPending());
            }
            return nodesConfig.getFormattedValue();

        } catch (SystemException | JSONException e) {
            logger.error(e, e);
            return ReturnStatusHelper.createErrorStatus(e.getMessage());

        } catch (SetupException e) {
            // this is OK. means application is not yet initialized
            logger.debug (e, e);
            return ReturnStatusHelper.createClearStatus("setup", operationsMonitoringService.isProcessingPending());
        }
    }

    @PostMapping("/reinstall-nodes-config")
    @ResponseBody
    @PreAuthorize("hasAuthority('ADMIN')")
    public String reinstallNodesConfig(@RequestBody String reinstallModel, HttpSession session) {

        logger.info ("Got model : " + reinstallModel);

        try {

            ServicesInstallStatusWrapper servicesInstallStatus = configurationService.loadServicesInstallationStatus();
            ServicesInstallStatusWrapper newServicesInstallStatus = ServicesInstallStatusWrapper.empty();

            NodesConfigWrapper reinstallModelConfig = new NodesConfigWrapper(reinstallModel);

            for (String serviceInstallStatusFlag : servicesInstallStatus.getInstalledServicesFlags()) {

                if (!reinstallModelConfig.hasServiceConfigured(servicesInstallStatus.getService(serviceInstallStatusFlag))) {
                    newServicesInstallStatus.copyFrom (serviceInstallStatusFlag, servicesInstallStatus);
                }
            }

            NodesConfigWrapper nodesConfig = configurationService.loadNodesConfig();
            if (nodesConfig == null || nodesConfig.isEmpty()) {
                return ReturnStatusHelper.createClearStatus("missing", operationsMonitoringService.isProcessingPending());
            }

            // Create OperationsCommand
            NodeServiceOperationsCommand command = NodeServiceOperationsCommand.create(servicesDefinition, nodeRangeResolver, newServicesInstallStatus, nodesConfig);

            // store command and config in HTTP Session
            session.setAttribute(PENDING_OPERATIONS_STATUS_OVERRIDE, newServicesInstallStatus);
            session.setAttribute(PENDING_OPERATIONS_COMMAND, command);

            return returnCommand (command);

        } catch (JSONException | FileException | NodesConfigurationException | SetupException | SystemException e) {
            logger.error(e, e);
            return ReturnStatusHelper.createEncodedErrorStatus(e);
        }
    }

    @PostMapping("/save-nodes-config")
    @ResponseBody
    @PreAuthorize("hasAuthority('ADMIN')")
    public String saveNodesConfig(@RequestBody String configAsString, HttpSession session) {

        logger.info ("Got config : " + configAsString);

        try {

            // first of all check nodes config
            NodesConfigWrapper nodesConfig = new NodesConfigWrapper(configAsString);
            nodesConfigChecker.checkNodesSetup(nodesConfig);

            ServicesInstallStatusWrapper serviceInstallStatus = configurationService.loadServicesInstallationStatus();

            // Create OperationsCommand
            NodeServiceOperationsCommand command = NodeServiceOperationsCommand.create(servicesDefinition, nodeRangeResolver, serviceInstallStatus, nodesConfig);

            // store command and config in HTTP Session
            session.removeAttribute(PENDING_OPERATIONS_STATUS_OVERRIDE);
            session.setAttribute(PENDING_OPERATIONS_COMMAND, command);

            return returnCommand (command);

        } catch (JSONException | SetupException | FileException | NodesConfigurationException e) {
            logger.error(e, e);
            notificationService.addError("Nodes installation failed !");
            return ReturnStatusHelper.createEncodedErrorStatus(e);
        }
    }

    @PostMapping("/apply-nodes-config")
    @ResponseBody
    @PreAuthorize("hasAuthority('ADMIN')")
    public String applyNodesConfig(HttpSession session) {

        try {

            JSONObject checkObject = checkOperations("Unfortunately, re-applying nodes configuration or changing nodes configuration is not possible in DEMO mode.");
            if (checkObject != null) {
                return checkObject.toString(2);
            }

            NodeServiceOperationsCommand command = (NodeServiceOperationsCommand) session.getAttribute(PENDING_OPERATIONS_COMMAND);
            session.removeAttribute(PENDING_OPERATIONS_COMMAND);

            ServicesInstallStatusWrapper newServicesInstallationStatus = (ServicesInstallStatusWrapper) session.getAttribute(PENDING_OPERATIONS_STATUS_OVERRIDE);
            session.removeAttribute(PENDING_OPERATIONS_STATUS_OVERRIDE);

            // newNodesStatus is null in case of nodes config change (as opposed to forced reinstall)
            if (newServicesInstallationStatus != null) {
                configurationService.saveServicesInstallationStatus(newServicesInstallationStatus);
            }

            configurationService.saveNodesConfig(command.getRawConfig());

            nodesConfigurationService.applyNodesConfig(command);

            return ReturnStatusHelper.createOKStatus();

        } catch (JSONException | SetupException | FileException | NodesConfigurationException  e) {
            logger.error(e, e);
            if (!(e.getCause() instanceof SystemException)) { // instanceof checks for null as well
                notificationService.addError("Nodes installation failed !");
            }
            return ReturnStatusHelper.createEncodedErrorStatus(e);
        }
    }
}
