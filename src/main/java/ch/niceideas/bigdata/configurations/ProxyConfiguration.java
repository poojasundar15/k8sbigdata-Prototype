package ch.niceideas.bigdata.configurations;

import ch.niceideas.bigdata.proxy.*;
import ch.niceideas.bigdata.services.ConfigurationService;
import ch.niceideas.bigdata.services.SSHCommandService;
import ch.niceideas.bigdata.services.ServicesDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import java.util.Arrays;

@Configuration
@EnableWebSocket
public class ProxyConfiguration implements WebSocketConfigurer {

    public static final String ESKIMO_WEB_SOCKET_URL_PREFIX = "/ws";

    @Autowired
    private ProxyManagerService proxyManagerService;

    @Autowired
    private ServicesDefinition servicesDefinition;

    @Autowired
    private SSHCommandService sshCommandService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private WebSocketProxyServer webSocketProxyServer;

    @Autowired
    private Environment env;

    @Value("${server.servlet.context-path:#{null}}")
    private String configuredContextPath = "";

    @Value ("${proxy.maxConnections:50}")
    private int maxConnections = 50;

    @Value ("${proxy.readTimeout:90000}")
    private int readTimeout = 90000;

    @Value ("${proxy.connectTimeout:10000}")
    private int connectTimeout = 12000;

    @Value ("${proxy.connectionRequestTimeout:20000}")
    private int connectionRequestTimeout = 20000;

    /**
     * This is to avoid following problem with REST requests passed by grafana
     *
     * <code>
     *     2019-08-28T14:42:32,123 INFO  [http-nio-9090-exec-8] o.a.j.l.DirectJDKLog: Error parsing HTTP request header
     * Note: further occurrences of HTTP request parsing errors will be logged at DEBUG level.
     * java.lang.IllegalArgumentException: Invalid character found in the request target. The valid characters are defined in RFC 7230 and RFC 3986
     * at org.apache.coyote.http11.Http11InputBuffer.parseRequestLine(Http11InputBuffer.java:467)
     * at org.apache.coyote.http11.Http11Processor.service(Http11Processor.java:294)
     * at org.apache.coyote.AbstractProcessorLight.process(AbstractProcessorLight.java:66)
     * at org.apache.coyote.AbstractProtocol$ConnectionHandler.process(AbstractProtocol.java:834)
     * at org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.doRun(NioEndpoint.java:1415)
     * at org.apache.tomcat.util.net.SocketProcessorBase.run(SocketProcessorBase.java:49)
     * at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)
     * at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
     * at org.apache.tomcat.util.threads.TaskThread$WrappingRunnable.run(TaskThread.java:61)
     * at java.lang.Thread.run(Thread.java:748)
     * </code>
     */
    @Bean
    @Profile("!no-web-stack")
    public ConfigurableServletWebServerFactory webServerFactory() {
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
        factory.addConnectorCustomizers(connector -> connector.setProperty("relaxedQueryChars", "|{}[]"));
        return factory;
    }

    @Bean
    @Profile("!no-web-stack")
    public ServletRegistrationBean<ServicesProxyServlet> proxyServletRegistrationBean(){

        ServletRegistrationBean<ServicesProxyServlet> servletRegistrationBean = new ServletRegistrationBean<>(
                new ServicesProxyServlet(
                        proxyManagerService,
                        servicesDefinition,
                        configuredContextPath,
                        maxConnections,
                        readTimeout,
                        connectTimeout,
                        connectionRequestTimeout),
                Arrays.stream(servicesDefinition.listProxiedServices())
                        .map(serviceName -> servicesDefinition.getServiceDefinition(serviceName))
                        .map(service -> "/" + service.getName() + "/*")
                        .toArray(String[]::new));

        servletRegistrationBean.addInitParameter(ProxyServlet.P_LOG, env.getProperty("logging_enabled", "false"));

        servletRegistrationBean.setName("eskimo-proxy");
        return servletRegistrationBean;
    }

    @Bean
    @Profile("!no-web-stack")
    public ServletRegistrationBean<WebCommandServlet> commandServletRegistrationBean(){

        ServletRegistrationBean<WebCommandServlet> servletRegistrationBean = new ServletRegistrationBean<>(
                new WebCommandServlet(
                        servicesDefinition, sshCommandService, configurationService),
                "/eskimo-command/*");

        servletRegistrationBean.setName("login-handler-command");
        return servletRegistrationBean;
    }

    @Bean
    @Profile("!no-web-stack")
    public ServletListenerRegistrationBean<ProxyServlet.ProxySessionListener> sessionListenerWithMetrics() {
        ServletListenerRegistrationBean<ProxyServlet.ProxySessionListener> listenerRegBean =
                new ServletListenerRegistrationBean<>();

        listenerRegBean.setListener(new ProxyServlet.ProxySessionListener());
        return listenerRegBean;
    }

    @Override
    @Profile("!no-web-stack")
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String[] allWsUrls = Arrays.stream(servicesDefinition.listProxiedServices())
                .map(serviceName -> servicesDefinition.getServiceDefinition(serviceName))
                .map(service -> ESKIMO_WEB_SOCKET_URL_PREFIX + "/" + service.getName() + "/**")
                .toArray(String[]::new);
        registry.addHandler(webSocketProxyServer, allWsUrls);
    }

    @Bean
    @Profile("!no-web-stack")
    public ServletServerContainerFactoryBean createServletServerContainerFactoryBean() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(WebSocketProxyForwarder.MESSAGE_SIZE_LIMIT);
        container.setMaxBinaryMessageBufferSize(WebSocketProxyForwarder.MESSAGE_SIZE_LIMIT);
        return container;
    }
}
