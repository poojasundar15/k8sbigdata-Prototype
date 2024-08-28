package ch.niceideas.bigdata.controlers;

import ch.niceideas.bigdata.services.TerminalService;
import ch.niceideas.bigdata.utils.ReturnStatusHelper;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;



@Controller
public class TerminalController {

    private static final Logger logger = Logger.getLogger(TerminalController.class);

    @Autowired
    private TerminalService terminalService;

    @GetMapping("/terminal-remove")
    @ResponseBody
    @PreAuthorize("hasAuthority('ADMIN')")
    public String removeTerminal(@RequestParam("session") String sessionId) {

        try {
            terminalService.removeTerminal(sessionId);

            return ReturnStatusHelper.createOKStatus();

        } catch (IOException e) {
            logger.error(e, e);
            return ReturnStatusHelper.createErrorStatus(e);
        }
    }

    @PostMapping("/terminal")
    @ResponseBody
    @PreAuthorize("hasAuthority('ADMIN')")
    public String postUpdate(@RequestBody String terminalBody, HttpServletResponse resp) {
        try {
            return terminalService.postUpdate(terminalBody).renderResponse(resp);
        } catch (IOException e) {
            logger.error(e, e);
            return e.getMessage();
        }
    }
}
