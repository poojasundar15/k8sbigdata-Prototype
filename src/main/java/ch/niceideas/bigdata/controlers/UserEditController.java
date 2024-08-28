package ch.niceideas.bigdata.controlers;

import ch.niceideas.common.json.JsonWrapper;
import ch.niceideas.common.utils.StringUtils;
import ch.niceideas.bigdata.security.MutableUser;
import ch.niceideas.bigdata.utils.ReturnStatusHelper;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;


@Controller
public class UserEditController extends AbstractOperationController {

    private static final Logger logger = Logger.getLogger(UserEditController.class);

    @Autowired
    private UserDetailsManager userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/edit-user")
    @ResponseBody
    public String editUser(@RequestBody String userDataString) {

        try {

            JsonWrapper userData = new JsonWrapper(userDataString);

            MutableUser user = new MutableUser(
                    userDetailsService.loadUserByUsername(userData.getValueForPathAsString("edit-user-user-id")));

            String currentPassword = userData.getValueForPathAsString("edit-user-current-password");
            if (StringUtils.isBlank(currentPassword)) {
                return ReturnStatusHelper.createErrorStatus("Current password is empty");
            }

            String newPassword = userData.getValueForPathAsString("edit-user-new-password");
            if (StringUtils.isBlank(newPassword)) {
                return ReturnStatusHelper.createErrorStatus("New password is empty");
            }
            String repeatPassword = userData.getValueForPathAsString("edit-user-repeat-password");
            if (StringUtils.isBlank(repeatPassword)) {
                return ReturnStatusHelper.createErrorStatus("Repeat password is empty");
            }

            if (!newPassword.equals(repeatPassword)) {
                return ReturnStatusHelper.createErrorStatus("Both password don't match");
            }

            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                return ReturnStatusHelper.createErrorStatus("Authentication failed. Perhaps wrong current password ?");
            }

            user.setPassword(passwordEncoder.encode(newPassword));

            userDetailsService.updateUser(user);

            return ReturnStatusHelper.createOKStatus(map -> map.put("message", "password successfully changed."));

        } catch (JSONException e) {
            logger.error(e, e);
            return ReturnStatusHelper.createErrorStatus(e);

        }
    }
}
