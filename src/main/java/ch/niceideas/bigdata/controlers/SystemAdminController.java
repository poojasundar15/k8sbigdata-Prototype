package ch.niceideas.bigdata.controlers;

import ch.niceideas.common.utils.FileException;
import ch.niceideas.bigdata.model.*;
import ch.niceideas.bigdata.model.service.ServiceDefinition;
import ch.niceideas.bigdata.services.*;
import ch.niceideas.bigdata.services.satellite.NodeRangeResolver;
import ch.niceideas.bigdata.services.satellite.NodesConfigurationException;
import ch.niceideas.bigdata.types.Node;
import ch.niceideas.bigdata.types.Service;
import ch.niceideas.bigdata.utils.ReturnStatusHelper;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;


@Controller
public class SystemAdminController extends AbstractOperationController {

    private static final Logger logger = Logger.getLogger(SystemAdminController.class);

    @Autowired
    private KubernetesService kubernetesService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private ServicesDefinition servicesDefinition;

    @Autowired
    private NodeRangeResolver nodeRangeResolver;

    @GetMapping("/interrupt-processing")
    @ResponseBody
    @PreAuthorize("hasAuthority('ADMIN')")
    public String interruptProcessing() {
        try {
            operationsMonitoringService.interruptProcessing();
            return ReturnStatusHelper.createOKStatus();
        } catch (JSONException e) {
            logger.error(e, e);
            return ReturnStatusHelper.createErrorStatus(e);
        }
    }

    private String performKubernetesOperation(KubernetesOperation operation, String message) {
        try {
            operation.performOperation(kubernetesService);
            return ReturnStatusHelper.createOKStatus(map -> map.put("messages", message));
        } catch (KubernetesException | SystemException e) {
            logger.error(e, e);
            return ReturnStatusHelper.createErrorStatus(e);
        }
    }

    private String performSystemOperation(SystemOperation operation, String message) {
        try {
            operation.performOperation(systemService);
            return ReturnStatusHelper.createOKStatus(map -> map.put("messages", message));
        } catch (NodesConfigurationException | SystemException e) {
            logger.error(e, e);
            return ReturnStatusHelper.createErrorStatus(e);
        }
    }

    @GetMapping("/show-journal")
    @ResponseBody
    public String showJournal(@RequestParam(name="service") String serviceName, @RequestParam(name="nodeAddress") String nodeAddress) {
        ServiceDefinition serviceDef = servicesDefinition.getServiceDefinition(Service.from(serviceName));
        if (serviceDef.isKubernetes()) {
            return performKubernetesOperation(
                    kubernetesService -> kubernetesService.showJournal(serviceDef, Node.fromAddress(nodeAddress)),
                    "Successfully shown journal of " +  serviceName + ".");

        } else {
            return performSystemOperation(
                    sysService -> sysService.showJournal(serviceDef, Node.fromAddress(nodeAddress)),
                    serviceName + " journal display from " + nodeAddress + ".");
        }

    }

    @GetMapping("/start-service")
    @ResponseBody
    public String startService(@RequestParam(name="service") String serviceName, @RequestParam(name="nodeAddress") String nodeAddress) {
        ServiceDefinition serviceDef = servicesDefinition.getServiceDefinition(Service.from(serviceName));
        if (serviceDef.isKubernetes()) {
            return performKubernetesOperation(
                    kubernetesService -> kubernetesService.startService(serviceDef, Node.fromAddress(nodeAddress)),
                    serviceName + " has been started successfully on kubernetes.");

        } else {
            return performSystemOperation(
                    sysService -> sysService.startService(serviceDef, Node.fromAddress(nodeAddress)),
                    serviceName + " has been started successfully on " + nodeAddress + ".");
        }
    }

    @GetMapping("/stop-service")
    @ResponseBody
    @PreAuthorize("hasAuthority('ADMIN')")
    public String stopService(@RequestParam(name="service") String serviceName, @RequestParam(name="nodeAddress") String nodeAddress) {
        ServiceDefinition serviceDef = servicesDefinition.getServiceDefinition(Service.from(serviceName));
        if (serviceDef.isKubernetes()) {
            return performKubernetesOperation(
                    kubernetesService -> kubernetesService.stopService(serviceDef, Node.fromAddress(nodeAddress)),
                    serviceName + " has been stopped successfully on kubernetes.");

        } else {
            return performSystemOperation(
                    sysService -> sysService.stopService(serviceDef, Node.fromAddress(nodeAddress)),
                    serviceName + " has been stopped successfully on " + nodeAddress + ".");
        }
    }

    @GetMapping("/restart-service")
    @ResponseBody
    @PreAuthorize("hasAuthority('ADMIN')")
    public String restartService(@RequestParam(name="service") String serviceName, @RequestParam(name="nodeAddress") String nodeAddress) {
        ServiceDefinition serviceDef = servicesDefinition.getServiceDefinition(Service.from(serviceName));
        if (serviceDef.isKubernetes()) {
            return performKubernetesOperation(
                    kubernetesService -> kubernetesService.restartService(serviceDef, Node.fromAddress(nodeAddress)),
                    serviceName + " has been restarted successfully on kubernetes.");

        } else {
            return performSystemOperation(
                    sysService -> sysService.restartService(serviceDef, Node.fromAddress(nodeAddress)),
                    serviceName + " has been restarted successfully on " + nodeAddress + ".");
        }
    }

    @GetMapping("service-custom-action")
    @ResponseBody
    public String serviceActionCustom(
            @RequestParam(name="action") String commandId,
            @RequestParam(name="service") String serviceName,
            @RequestParam(name="nodeAddress") String nodeAddress) {
        return performSystemOperation(
                sysService -> sysService.callCommand(commandId, Service.from(serviceName), Node.fromAddress(nodeAddress)),
                "command " + commandId + " for " + serviceName + " has been executed successfully on " + nodeAddress + ".");
    }

    @GetMapping("/reinstall-service")
    @ResponseBody
    @PreAuthorize("hasAuthority('ADMIN')")
    public String reinstallService(@RequestParam(name="service") String serviceName, @RequestParam(name="nodeAddress") String nodeAddress) {

        try {

            JSONObject checkObject = checkOperations("Unfortunately, re-installing a service is not possible in DEMO mode.");
            if (checkObject != null) {
                return checkObject.toString(2);
            }

            Service service = Service.from(serviceName);
            ServiceDefinition serviceDef = servicesDefinition.getServiceDefinition(service);

            Node node;
            if (serviceDef.isKubernetes()) {
                node = Node.KUBERNETES_NODE;
            } else {
                node = Node.fromAddress(nodeAddress);
            }

            ServicesInstallStatusWrapper formerServicesInstallationStatus = configurationService.loadServicesInstallationStatus();

            ServicesInstallStatusWrapper newServicesInstallationStatus = ServicesInstallStatusWrapper.empty();

            for (String is : formerServicesInstallationStatus.getAllInstallStatusesExceptServiceOnNode(service, node)) {
                newServicesInstallationStatus.setValueForPath(is, formerServicesInstallationStatus.getValueForPath(is));
            }

            configurationService.saveServicesInstallationStatus(newServicesInstallationStatus);

            if (serviceDef.isKubernetes()) {

                KubernetesServicesConfigWrapper kubeServicesConfig = configurationService.loadKubernetesServicesConfig();
                if (kubeServicesConfig == null || kubeServicesConfig.isEmpty()) {
                    return ReturnStatusHelper.createClearStatus("missing", operationsMonitoringService.isProcessingPending());
                }

                KubernetesOperationsCommand operationsCommand = KubernetesOperationsCommand.create(
                        servicesDefinition, systemService, kubeServicesConfig, newServicesInstallationStatus, kubeServicesConfig);

                return performKubernetesOperation(
                        kubernetesService -> kubernetesService.applyServicesConfig(operationsCommand),
                        serviceName + " has been reinstalled successfully on kubernetes.");

            } else {

                NodesConfigWrapper nodesConfig = configurationService.loadNodesConfig();
                if (nodesConfig == null || nodesConfig.isEmpty()) {
                    return ReturnStatusHelper.createClearStatus("missing", operationsMonitoringService.isProcessingPending());
                }

                NodeServiceOperationsCommand operationsCommand = NodeServiceOperationsCommand.create(
                        servicesDefinition, nodeRangeResolver, newServicesInstallationStatus, nodesConfig);

                return performSystemOperation(
                        sysService -> sysService.delegateApplyNodesConfig(operationsCommand),
                        serviceName + " has been reinstalled successfully on " + node + ".");
            }
        } catch (SetupException | SystemException | FileException | JSONException | NodesConfigurationException e) {
            logger.error(e, e);
            notificationService.addError("Nodes installation failed !");
            return ReturnStatusHelper.createErrorStatus(e);
        }
    }

    private interface SystemOperation {
        void performOperation (SystemService systemService) throws NodesConfigurationException, SystemException;
    }

    private interface KubernetesOperation {
        void performOperation (KubernetesService kubernetesService) throws KubernetesException, SystemException;
    }
}
