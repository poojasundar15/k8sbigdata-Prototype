package ch.niceideas.bigdata.controlers;

import ch.niceideas.common.json.JsonWrapper;
import ch.niceideas.bigdata.model.MasterStatusWrapper;
import ch.niceideas.bigdata.model.SystemStatusWrapper;
import ch.niceideas.bigdata.security.AuthorizationException;
import ch.niceideas.bigdata.security.SecurityHelper;
import ch.niceideas.bigdata.services.*;
import ch.niceideas.bigdata.utils.ReturnStatusHelper;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.stream.Collectors;


@Controller
public class SystemStatusController {

    private static final Logger logger = Logger.getLogger(SystemStatusController.class);
    public static final String MASTERS = "masters";

    @Autowired
    private SetupService setupService;

    @Autowired
    private SystemService systemService;

    @Autowired
    private ApplicationStatusService statusService;

    @Autowired
    private MasterService masterService;

    @Autowired
    protected OperationsMonitoringService operationsMonitoringService;

    @Value("${build.version}")
    private String buildVersion = "DEV-SNAPSHOT";

    @GetMapping("/get-last-operation-result")
    @ResponseBody
    public String getLastOperationResult() {
        return ReturnStatusHelper.createOKStatus(map -> map.put("success", operationsMonitoringService.getLastOperationSuccess()));
    }

    @GetMapping("/context")
    @ResponseBody
    public String getContext() {

        try {
            JsonWrapper retObject = new JsonWrapper(new JSONObject(new HashMap<>() {{
                put("status", "OK");
                put("version", buildVersion);
                put("user", SecurityHelper.getUserId());
                put("roles", new JSONArray(SecurityHelper.getuserRoles().stream()
                        .map(Enum::name)
                        .collect(Collectors.toList())
                ));
            }}));

            return retObject.getFormattedValue();

        } catch (AuthorizationException e) {
            return ReturnStatusHelper.createErrorStatus(e);
        }
    }

    @GetMapping("/get-status")
    @ResponseBody
    public String getStatus() {

        try {
            setupService.ensureSetupCompleted();

            SystemStatusWrapper nodeServicesStatus = systemService.getStatus();

            JsonWrapper systemStatus = statusService.getStatus();

            // 6. Inject identified masters
            MasterStatusWrapper masterStatus = masterService.getMasterStatus();

            return ReturnStatusHelper.createOKStatus(map -> {
                if (nodeServicesStatus == null || nodeServicesStatus.isEmpty()) {
                    map.put("clear", "nodes");
                } else {
                    map.put("nodeServicesStatus", nodeServicesStatus.getJSONObject());
                }
                if (masterStatus.hasPath(MASTERS)) {
                    map.put(MASTERS, masterStatus.getJSONObject().getJSONObject(MASTERS));
                }
                map.put("systemStatus", systemStatus.getJSONObject());
                map.put("processingPending", operationsMonitoringService.isProcessingPending());
            });

        } catch (SetupException e) {

            // this is OK. means application is not yet initialized
            logger.debug (e.getCause(), e.getCause());
            return ReturnStatusHelper.createClearStatus("setup", operationsMonitoringService.isProcessingPending());

        } catch (SystemService.StatusExceptionWrapperException | MasterService.MasterExceptionWrapperException e) {

            if (e.getCause() instanceof SetupException) {
                // this is OK. means application is not yet initialized
                logger.debug (e.getCause(), e.getCause());
                return ReturnStatusHelper.createClearStatus("setup", operationsMonitoringService.isProcessingPending());
            } else {
                logger.debug(e.getCause(), e.getCause());
                return ReturnStatusHelper.createErrorStatus ((Exception)e.getCause());
            }
        }
    }
}
