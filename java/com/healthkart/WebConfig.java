package com.healthkart;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import com.healthkart.dao.AppointmentDao;
import com.healthkart.dao.MedicineDao;
import com.healthkart.dao.UserDao;
import com.healthkart.servlet.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web Configuration:
 * - CORS mapping
 * - Comprehensive Static Resource Locations (bundled classpath + filesystem)
 * - View Controllers and URL forward mappings for all frontend routes
 * - Servlets & Web Integration: Registering standard Jakarta HttpServlets and Filters
 */
@org.springframework.context.annotation.Configuration
public class WebConfig implements WebMvcConfigurer {

    /** app.use(cors()) */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**").allowedOrigins("*").allowedMethods("*");
    }

    /** Map clean frontend routes and ensure root "/" renders index.html */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.html");
        registry.addViewController("/login").setViewName("forward:/pages/login.html");
        registry.addViewController("/search").setViewName("forward:/pages/search.html");
        registry.addViewController("/compare").setViewName("forward:/pages/compare.html");
        registry.addViewController("/doctors").setViewName("forward:/pages/doctors.html");
        registry.addViewController("/cart").setViewName("forward:/pages/cart.html");
        registry.addViewController("/orders").setViewName("forward:/pages/cart.html");
        registry.addViewController("/my-health").setViewName("forward:/pages/my-health.html");
        registry.addViewController("/doctor-dashboard").setViewName("forward:/pages/doctor-dashboard.html");
        registry.addViewController("/database").setViewName("forward:/pages/database.html");
        registry.addViewController("/call").setViewName("forward:/pages/call.html");
    }

    /** Ensure static frontend assets are served correctly regardless of working directory */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations(
                        "classpath:/static/",
                        "classpath:/public/",
                        "file:./public/",
                        "file:public/",
                        "file:healthkart-java/public/",
                        "file:./healthkart-java/public/",
                        "file:../public/"
                );
    }

    // =========================================================================
    // SERVLETS & WEB INTEGRATION (Jakarta HttpServlet & Filter Registration)
    // =========================================================================

    @Bean
    public ServletRegistrationBean<AuthServlet> authServletRegistration(UserDao userDao) {
        ServletRegistrationBean<AuthServlet> reg = new ServletRegistrationBean<>(
                new AuthServlet(userDao), "/servlets/auth", "/servlets/auth/*"
        );
        reg.setLoadOnStartup(1);
        reg.setName("AuthServlet");
        return reg;
    }

    @Bean
    public ServletRegistrationBean<AppointmentServlet> appointmentServletRegistration(AppointmentDao appointmentDao) {
        ServletRegistrationBean<AppointmentServlet> reg = new ServletRegistrationBean<>(
                new AppointmentServlet(appointmentDao), "/servlets/appointments", "/servlets/appointments/*"
        );
        reg.setLoadOnStartup(1);
        reg.setName("AppointmentServlet");
        return reg;
    }

    @Bean
    public ServletRegistrationBean<MedicineCatalogServlet> medicineCatalogServletRegistration(MedicineDao medicineDao) {
        ServletRegistrationBean<MedicineCatalogServlet> reg = new ServletRegistrationBean<>(
                new MedicineCatalogServlet(medicineDao),
                "/servlets/medicines", "/servlets/medicines/*",
                "/servlets/catalog", "/servlets/catalog/*"
        );
        reg.setLoadOnStartup(1);
        reg.setName("MedicineCatalogServlet");
        return reg;
    }

    @Bean
    public ServletRegistrationBean<HealthCheckServlet> healthCheckServletRegistration() {
        ServletRegistrationBean<HealthCheckServlet> reg = new ServletRegistrationBean<>(
                new HealthCheckServlet(), "/servlets/health", "/servlets/health/*"
        );
        reg.setLoadOnStartup(1);
        reg.setName("HealthCheckServlet");
        return reg;
    }

    @Bean
    public FilterRegistrationBean<SecurityAndLoggingFilter> securityFilterRegistration() {
        FilterRegistrationBean<SecurityAndLoggingFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new SecurityAndLoggingFilter());
        reg.addUrlPatterns("/*");
        reg.setOrder(1);
        reg.setName("SecurityAndLoggingFilter");
        return reg;
    }

    /** Socket.IO server (started/stopped with the Spring context). */
    @Bean(initMethod = "start", destroyMethod = "stop")
    public SocketIOServer socketIOServer(@Value("${healthkart.socket.port:9092}") int port) {
        Configuration config = new Configuration();
        config.setHostname("0.0.0.0");
        config.setPort(port);
        config.setOrigin("*");
        return new SocketIOServer(config);
    }

    @RestController
    static class RedirectController {
        @GetMapping("/pages/index.html")
        ResponseEntity<Void> legacyIndex() {
            return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                    .header(HttpHeaders.LOCATION, "/index.html").build();
        }
    }
}
