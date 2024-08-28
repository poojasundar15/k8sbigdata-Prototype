package ch.niceideas.bigdata.controlers;

import ch.niceideas.bigdata.model.service.ServiceDefinition;
import ch.niceideas.bigdata.model.service.UIConfig;
import ch.niceideas.bigdata.services.ServicesDefinition;
import ch.niceideas.bigdata.types.Service;
import ch.niceideas.bigdata.utils.ReturnStatusHelper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;


@Controller
public class ServicesController {

    @Resource
    private ServicesDefinition servicesDefinition;

    /** for tests only ! */
    public void setServicesDefinition(ServicesDefinition servicesDefinition) {
        this.servicesDefinition = servicesDefinition;
    }

    @GetMapping("/list-services")
    @ResponseBody
    public String listServices() {
        return ReturnStatusHelper.createOKStatus(map ->
                map.put("services", new JSONArray(Arrays.stream(servicesDefinition.listServicesInOrder()).map(Service::getName).collect(Collectors.toList()))));
    }

    @GetMapping("/list-ui-services")
    @ResponseBody
    public String listUIServices() {
        return ReturnStatusHelper.createOKStatus(map ->
                map.put("uiServices", new JSONArray(Arrays.stream(servicesDefinition.listUIServices()).map(Service::getName).collect(Collectors.toList()))));
    }

    @GetMapping("/get-ui-services-config")
    @ResponseBody
    public String getUIServicesConfig() {
        return ReturnStatusHelper.createOKStatus(map -> {
            Map<String, Object> uiServicesConfig = new HashMap<>();
            Map<Service, UIConfig> uiConfigs = servicesDefinition.getUIServicesConfig();

            uiConfigs.keySet()
                    .forEach(service -> uiServicesConfig.put (service.getName(), uiConfigs.get(service).toJSON()));

            map.put("uiServicesConfig", new JSONObject(uiServicesConfig));
        });
    }

    @GetMapping("/get-ui-services-status-config")
    @ResponseBody
    public String getUIServicesStatusConfig() {
        return ReturnStatusHelper.createOKStatus(map -> {
            Map<String, Object> uiServicesStatusConfig = new HashMap<>();

            Arrays.stream(servicesDefinition.listAllServices())
                    .map(service -> servicesDefinition.getServiceDefinition(service))
                    .forEach(serviceDef -> uiServicesStatusConfig.put (serviceDef.getName(), serviceDef.toUiStatusConfigJSON()));

            map.put("uiServicesStatusConfig", new JSONObject(uiServicesStatusConfig));
        });
    }

    @GetMapping("/get-services-dependencies")
    @ResponseBody
    public String getServicesDependencies() {
        return ReturnStatusHelper.createOKStatus(map -> {
            Map<String, Object> servicesDependencies = new HashMap<>();

            Arrays.stream(servicesDefinition.listAllServices())
                    .map(service -> servicesDefinition.getServiceDefinition(service))
                    .forEach(serviceDef -> servicesDependencies.put (serviceDef.getName(), serviceDef.toDependenciesJSON()));

            map.put("servicesDependencies", new JSONObject(servicesDependencies));
        });
    }

    @GetMapping("/get-services-config")
    @ResponseBody
    public String getServicesConfigurations() {
        return ReturnStatusHelper.createOKStatus(map -> {
            Map<String, Object> servicesConfigurations = new HashMap<>();

            Arrays.stream(servicesDefinition.listAllServices())
                    .map(service -> servicesDefinition.getServiceDefinition(service))
                    .filter(serviceDef -> !serviceDef.isKubernetes())
                    .forEach(serviceDef -> servicesConfigurations.put (serviceDef.getName(), serviceDef.toConfigJSON()));

            map.put("servicesConfigurations", new JSONObject(servicesConfigurations));
        });
    }

    @GetMapping("/list-config-services")
    @ResponseBody
    public String listConfigServices() {
        return ReturnStatusHelper.createOKStatus(map -> {

            Map<String, Object> servicesConfigurations = new HashMap<>();

            Arrays.stream(servicesDefinition.listAllServices())
                    .map(service -> servicesDefinition.getServiceDefinition(service))
                    .forEach(serviceDef -> servicesConfigurations.put (serviceDef.getName(), serviceDef.toConfigJSON()));

            map.put("uniqueServices", new JSONArray(Arrays.stream(servicesDefinition.listUniqueServices()).map(Service::getName).collect(Collectors.toList())));
            map.put("multipleServices", new JSONArray(Arrays.stream(servicesDefinition.listMultipleServicesNonKubernetes()).map(Service::getName).collect(Collectors.toList())));
            map.put("mandatoryServices", new JSONArray(Arrays.stream(servicesDefinition.listMandatoryServices()).map(Service::getName).collect(Collectors.toList())));
            map.put("servicesConfigurations", new JSONObject(servicesConfigurations));
        });
    }

    @GetMapping("/get-kubernetes-services")
    @ResponseBody
    public String getKubernetesServices() {
        return ReturnStatusHelper.createOKStatus(map -> {

            Map<String, Object> kubeServicesConfig = new HashMap<>();

            Arrays.stream(servicesDefinition.listAllServices())
                    .map(service -> servicesDefinition.getServiceDefinition(service))
                    .filter(ServiceDefinition::isKubernetes)
                    .forEach(serviceDef -> kubeServicesConfig.put (serviceDef.getName(), serviceDef.toConfigJSON()));

            map.put("kubernetesServices", new JSONArray(Arrays.stream(servicesDefinition.listKubernetesServices()).map(Service::getName).collect(Collectors.toList())));
            map.put("kubernetesServicesConfigurations", new JSONObject(kubeServicesConfig));
        });
    }

}
