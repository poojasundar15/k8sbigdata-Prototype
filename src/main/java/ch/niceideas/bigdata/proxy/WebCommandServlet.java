package ch.niceideas.bigdata.proxy;

import ch.niceideas.common.json.JsonWrapper;
import ch.niceideas.common.utils.FileException;
import ch.niceideas.common.utils.StringUtils;
import ch.niceideas.bigdata.model.ServicesInstallStatusWrapper;
import ch.niceideas.bigdata.model.service.ServiceDefinition;
import ch.niceideas.bigdata.model.service.proxy.WebCommand;
import ch.niceideas.bigdata.services.*;
import ch.niceideas.bigdata.types.Node;
import ch.niceideas.bigdata.utils.ReturnStatusHelper;
import org.apache.log4j.Logger;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.MimeTypeUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class WebCommandServlet extends HttpServlet {

    private static final Logger logger = Logger.getLogger(WebCommandServlet.class);

    private final ServicesDefinition servicesDefinition;
    private final SSHCommandService sshCommandService;
    private final ConfigurationService configurationService;

    public WebCommandServlet(
            ServicesDefinition servicesDefinition,
            SSHCommandService sshCommandService,
            ConfigurationService configurationService) {
        this.servicesDefinition = servicesDefinition;
        this.sshCommandService = sshCommandService;
        this.configurationService = configurationService;

    }

    @Override
    protected void service(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
            throws ServletException, IOException {

        servletResponse.setContentType(MimeTypeUtils.APPLICATION_JSON.toString());

        try {

            // 1. Identify command ID I need to execute from argument
            String reqURI = servletRequest.getRequestURI();
            String servletPath = servletRequest.getServletPath();

            String command = reqURI.substring(reqURI.indexOf(servletPath) + servletPath.length() + 1);

            // 2. Find matching command
            WebCommand webCommand = Arrays.stream(servicesDefinition.listUIServices())
                    .map(servicesDefinition::getServiceDefinition)
                    .map(ServiceDefinition::getWebCommands)
                    .flatMap(List::stream)
                    .filter(wc -> wc.getId().equals(command))
                    .findFirst().orElseThrow(() -> new IllegalStateException("No webCommand found with ID " + command));

            // 3. Find node running target service
            ServicesInstallStatusWrapper installStatus = configurationService.loadServicesInstallationStatus();
            Node serviceNode = Optional.ofNullable(installStatus.getFirstNode(webCommand.getTarget()))
                    .orElseThrow(() -> new IllegalStateException("Couldn't find any node running servuce " + webCommand.getTarget().getName()));

            // 6. Ensure user has required Role (if any is required)
            String requiredRole = webCommand.getRole();
            if (StringUtils.isNotBlank(requiredRole)) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth == null || auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals(requiredRole))) {
                    throw new IllegalStateException("User is lacking role " + requiredRole);
                }
            }

            // 5. Execute command
            String result = sshCommandService.runSSHCommand(serviceNode, webCommand.getCommand());

            // 6. Return result or error in expected json format
            JsonWrapper returnObject = JsonWrapper.empty();
            returnObject.setValueForPath("value", result);
            servletResponse.getOutputStream().write(returnObject.getFormattedValue().getBytes(StandardCharsets.UTF_8));

        } catch (FileException | SetupException | IllegalStateException | SSHCommandException e) {
            logger.error (e, e);
            servletResponse.getOutputStream().write(ReturnStatusHelper.createErrorStatus(e).getBytes(StandardCharsets.UTF_8));
        }
    }

}
