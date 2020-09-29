package cn.dendarii.whole.schedule;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = { "cn.dendarii.whole" })
@SuppressWarnings("unused")
public class TestScheduledService {
    @Autowired
    private ScheduledService scheduledService;

    public static boolean sayhi(String name) {
        System.out.println("hi " + name + " " + Thread.currentThread().getName());
        return true;
    }

    @PostConstruct
    public void test() throws InterruptedException, NoSuchMethodException, SecurityException {
        scheduledService.addATask(new ScheduledTask("a",
                                                    "*/1 * * * * ?",
                                                    null,
                                                    "a",
                                                    TestScheduledService.class.getMethod("sayhi",
                                                                                         String.class),
                                                    null));
        scheduledService.addATask(new ScheduledTask("b",
                                                    "*/1 * * * * ?",
                                                    null,
                                                    "b",
                                                    TestScheduledService.class.getMethod("sayhi",
                                                                                         String.class),
                                                    null));
        Thread.sleep(3000);
        scheduledService.removeTask("a");
    }

    public static void main(String[] args)
            throws InterruptedException, NoSuchMethodException, SecurityException {
        ApplicationContext applicationContext = SpringApplication.run(TestScheduledService.class,
                                                                      args);
    }
}
