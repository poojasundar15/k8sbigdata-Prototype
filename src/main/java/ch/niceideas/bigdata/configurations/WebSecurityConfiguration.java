package ch.niceideas.bigdata.configurations;

import ch.niceideas.common.utils.FileException;
import ch.niceideas.common.utils.FileUtils;
import ch.niceideas.common.utils.StringUtils;
import ch.niceideas.bigdata.security.JSONBackedUserDetailsManager;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class WebSecurityConfiguration {

    private static final Logger logger = Logger.getLogger(WebSecurityConfiguration.class);
    public static final String LOGIN_PAGE_URL = "/login.html";

    @Value("${security.userJsonFile}")
    private String userJsonFilePath = "/tmp/eskimo-users.json";

    @Value("${server.servlet.context-path:#{null}}")
    private String configuredContextPath = "";

    @Value("${eskimo.demoMode}")
    private boolean demoMode = false;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        final String contextPath = getContextPath();

        http
            // authentication and authorization stuff
            .authorizeRequests()
                .antMatchers(LOGIN_PAGE_URL).permitAll()
                .antMatchers("/css/**").permitAll()
                .antMatchers("/scripts/**").permitAll()
                .antMatchers("/images/**").permitAll()
                .antMatchers("/fonts/**").permitAll()
                .antMatchers("/html/**").permitAll()
                .antMatchers("/index.html").authenticated()
                .anyRequest().authenticated()
                .and()
                .exceptionHandling()
                // way to avoid sending redirect to AJAX call (they tend not to like it)
                .authenticationEntryPoint((httpServletRequest, httpServletResponse, e) -> {
                    if (isAjax(httpServletRequest)) {
                        httpServletResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    } else {
                        httpServletResponse.sendRedirect(contextPath + LOGIN_PAGE_URL
                                + (demoMode ? "?demo=true" : ""));
                    }
                }).and()
            // own login stuff
            .formLogin()
                .loginPage(LOGIN_PAGE_URL).permitAll()
                .loginProcessingUrl("/login").permitAll()
                    .usernameParameter("eskimo-username")
                    .passwordParameter("eskimo-password")
                .defaultSuccessUrl("/index.html",true)
                .and()
            .userDetailsService(userDetailsService())
            .logout().permitAll()
                .and()
             // disabling CSRF security as long as not implemented backend side
            .csrf().disable()
            // disabling Same origin policy on iframes (eskimo uses this extensively)
            .headers().frameOptions().disable();

        return http.build();
    }

    private String getContextPath() {
        if (StringUtils.isBlank(configuredContextPath)) {
            return "";
        } else {
            return (FileUtils.slashStart(configuredContextPath));
        }
    }

    private boolean isAjax(HttpServletRequest request) {
        String acceptHeader = request.getHeader("accept");
        return acceptHeader != null && (
                 acceptHeader.contains("json") || acceptHeader.contains("javascript"));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(11);
    }

    @Bean
    @Profile("!test-security")
    public UserDetailsManager userDetailsService() {
        try {
            return new JSONBackedUserDetailsManager(userJsonFilePath, passwordEncoder());
        } catch (FileException | JSONException e) {
            logger.error (e, e);
            throw new ConfigurationException(e);
        }
    }
}
