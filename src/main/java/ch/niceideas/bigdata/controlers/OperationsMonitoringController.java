package ch.niceideas.bigdata.controlers;

import ch.niceideas.common.json.JsonWrapper;
import ch.niceideas.common.utils.StringUtils;
import ch.niceideas.bigdata.model.OperationsMonitoringStatusWrapper;
import ch.niceideas.bigdata.services.OperationsMonitoringService;
import ch.niceideas.bigdata.utils.ReturnStatusHelper;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.Map;
import java.util.stream.Collectors;


@Controller
public class OperationsMonitoringController extends AbstractInformationController<String, String> {

    private static final Logger logger = Logger.getLogger(OperationsMonitoringController.class);

    @Resource
    private OperationsMonitoringService operationsMonitoringService;

    @GetMapping("/fetch-operations-status")
    @ResponseBody
    public String fetchOperationsStatus(@RequestParam(name="last-lines") String lastLinesJsonString) {

        try {

            JsonWrapper lastLinesJson = StringUtils.isBlank(lastLinesJsonString) ?
                    JsonWrapper.empty() :
                    new JsonWrapper(lastLinesJsonString);

            Map<String, Integer> lastLinesMap = lastLinesJson.toMap().keySet().stream()
                    .collect(Collectors.toMap(opId -> opId, opId -> (Integer) lastLinesJson.toMap().get(opId)));

            OperationsMonitoringStatusWrapper status = operationsMonitoringService.getOperationsMonitoringStatus (lastLinesMap);

            return status.getFormattedValue();

        } catch (JSONException e) {
            logger.error(e, e);
            return ReturnStatusHelper.createErrorStatus(e);
        }
    }

}
