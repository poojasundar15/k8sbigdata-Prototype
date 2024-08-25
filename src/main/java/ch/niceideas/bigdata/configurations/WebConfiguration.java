package ch.niceideas.bigdata.configurations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.TimeUnit;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {

    @Value("${eskimo.externalLogoAndIconFolder}")
    private String externalLogoAndIconFolder = "";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry
                .addResourceHandler("/"+externalLogoAndIconFolder+"/**")
                .addResourceLocations("file:./"+externalLogoAndIconFolder+"/")
                .setCacheControl(CacheControl.maxAge(1, TimeUnit.HOURS));

        registry
                .addResourceHandler("/images/**")
                .addResourceLocations("/images/")
                .setCacheControl(CacheControl.maxAge(24, TimeUnit.HOURS));
    }

}
