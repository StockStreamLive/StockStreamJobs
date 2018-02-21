package application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

@Slf4j
public class Application {

    public static AnnotationConfigWebApplicationContext initApplicationContext() throws Throwable {
        final AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.register(AppContext.class);
        context.refresh();

        return context;
    }

    public static void main(final String[] args) throws Throwable {
        log.info("Application initialized");
        System.setProperty("user.timezone", "GMT");
        final AnnotationConfigWebApplicationContext context = initApplicationContext();
    }

}
