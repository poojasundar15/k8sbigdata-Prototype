package ch.niceideas.bigdata.controlers;

import ch.niceideas.common.utils.Pair;
import ch.niceideas.bigdata.services.NotificationService;
import ch.niceideas.bigdata.utils.ReturnStatusHelper;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.List;


@Controller
public class NotificationController extends AbstractInformationController<JSONObject, List<JSONObject>> {

    private static final Logger logger = Logger.getLogger(NotificationController.class);

    @Resource
    private NotificationService notificationService;

    @GetMapping("/fetch-notifications")
    @ResponseBody
    public String fetchNotifications(@RequestParam(name="last_line") Integer lastLine) {

        try {
            Pair<Integer, List<JSONObject>> newLines = notificationService.fetchElements (lastLine);

            return ReturnStatusHelper.createOKStatus(map -> {
                map.put("lastLine", "" + newLines.getKey());
                map.put("notifications", new JSONArray(newLines.getValue().toArray()));
            });

        } catch (JSONException e) {
            logger.error (e, e);
            throw new IllegalStateException(e);
        }
    }

    @GetMapping("/get-lastline-notification")
    @ResponseBody
    public String getLastlineMontoring() {
        return getLastlineElement(notificationService);
    }

    @GetMapping("/clear-notifications")
    @ResponseBody
    public String clearNotifications() {
        return clear(notificationService);
    }
}
