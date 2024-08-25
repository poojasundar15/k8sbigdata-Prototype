package ch.niceideas.bigdata.controlers;

import ch.niceideas.bigdata.services.ApplicationStatusServiceImpl;
import ch.niceideas.bigdata.utils.ReturnStatusHelper;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;


@Controller
public class AboutController extends AbstractInformationController<JSONObject, List<JSONObject>> {

    private static final Logger logger = Logger.getLogger(AboutController.class);

    @Value("${build.version}")
    private String buildVersion;

    @Value("${build.timestamp}")
    private String buildTimestamp;

    @Value("${eskimo.enableKubernetesSubsystem}")
    private Boolean enableKubernetesSubsystem;

    @Value("${eskimo.demoMode}")
    private Boolean demoMode;

    @Value("${setup.packagesDownloadUrlRoot}")
    private String packagesDownloadUrlRoot;

    @GetMapping("/fetch-about-info")
    @ResponseBody
    public String fetchAboutInfo() {

        try {

            return ReturnStatusHelper.createOKStatus(map -> {

                map.put("about-eskimo-version", buildVersion);

                map.put("about-eskimo-build-timestamp", buildTimestamp);
                map.put("about-eskimo-runtime-timestamp", ApplicationStatusServiceImpl.getRuntimeStartDate());
                map.put("about-eskimo-kube-enabled", enableKubernetesSubsystem);
                map.put("about-eskimo-demo-mode", demoMode);
                map.put("about-eskimo-packages-url", packagesDownloadUrlRoot);

                map.put("about-eskimo-java-home", System.getProperty("java.home"));
                map.put("about-eskimo-java-version", System.getProperty("java.version"));
                map.put("about-eskimo-working-dir", System.getProperty("user.dir"));
                map.put("about-eskimo-os-name", System.getProperty("os.name"));
                map.put("about-eskimo-os-version", System.getProperty("os.version"));
            });

        } catch (JSONException e) {
            logger.error (e, e);
            throw new IllegalStateException(e);
        }
    }

}
