package cn.dendarii.whole.server;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import cn.dendarii.whole.bean.job.HeartbeatJob;
import cn.dendarii.whole.service.BaseService;
import cn.dendarii.whole.service.SysConfService;
import cn.dendarii.whole.util.set.DuplicateArraylist;
import sun.misc.Signal;
import sun.misc.SignalHandler;

/**
 * 完成一些系统初始化的工作
 * 
 * @INCLUDE 定线程数量式线程池
 * @INCLUDE 需要异步启动不能阻塞开放web请求的资源的启动
 * @INCLUDE 监听LINUX KILL信号，注意这种方法会导致启动的时候报出警告信息说有些方法不再使用了，暂时没有更好的解决策略
 * @INCLUDE 管理一些资源的柔性关闭
 */
@Service
@DependsOn("sysConfService")
@SuppressWarnings("restriction")
public class MainServer extends BaseService implements Runnable {
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private SysConfService sysConfService;
    @Autowired
    private QueueServer queueServer;
    // 心跳线程
    private Thread heartbeat;
    private AtomicBoolean live = new AtomicBoolean(true);
    private DuplicateArraylist<HeartbeatJob> checkList = new DuplicateArraylist<>();

    @PostConstruct
    public void init() {
        logger.info("start init huaban cu.");
        // 配置柔性关闭，用于当tomcat关闭时，必须清空队列才能彻底退出，不在内部队列留线程
        initSystemSoftExit();
        // 启动queue开始执行
        queueServer.start();
        // 开始使用异步线程进入死循环
        heartbeat = new Thread(this);
        heartbeat.setDaemon(true);
        heartbeat.setName("heartbeat");
        heartbeat.setPriority(Thread.MAX_PRIORITY);
        heartbeat.start();
        logger.info("init huaban cu success.");
    }

    @Override
    public void run() {
        while (live.get()) { // 每10s运行一次，高频且短速计算可以放在这里，避免线程过多切换
            long startTime = System.currentTimeMillis();
            List<HeartbeatJob> joblist = checkList.getList();
            for (HeartbeatJob job : joblist) {
                try { // 注意这里是在一个线程做的，这个线程只会阻塞，不会释放
                    job.execute(startTime);
                } catch (Exception e) {
                    logger.error("cu心跳任务执行失败:" + job, e);
                }
            }
            long endTime = System.currentTimeMillis();
            try {
                Thread.sleep(10l * 1000 - (endTime - startTime));
            } catch (Exception e) {
                logger.error("cu休止期故障");
            }
        }
    }

    public void addHearbeatJob(HeartbeatJob job) { // 在数据结构中已经完成阻塞
        checkList.add(job);
    }

    public SysConfService conf() {
        return sysConfService;
    }

    private AtomicBoolean isShuttingDown = new AtomicBoolean(false);

    public void initSystemSoftExit() {
        if (ifRunOnLinux()) {
            logger.info("添加关闭钩子，监听 -15(TERM)信号");
            Signal.handle(new Signal("TERM"), new CuSignalHandler());
        }
        logger.info("在JVM上监听关闭信号");
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());
    }

    public void shutdown(int returnCode) {
        try {
            logger.info("系统开始关闭");
            live.set(false);
            queueServer.shutdown();
        } catch (Exception e) {
            logger.error("系统关闭失败!", e);
            System.exit(returnCode);
        } finally {
            SpringApplication.exit(applicationContext);
            logger.info("系统关闭成功");
            // System.exit(returnCode);
        }
    }

    @PreDestroy
    private void softShutdown() {
        if (!isShuttingDown.getAndSet(true)) {
            logger.info("系统柔性关闭启动");
            shutdown(0);
            logger.info("柔性关闭成功");
            // 阻塞Runtime关闭问题，否则会影响tomcat重启
            // Runtime.getRuntime().exit(0);
        }
    }

    class ShutdownHook extends Thread {
        public void run() {
            softShutdown();
        };
    }

    class CuSignalHandler implements SignalHandler {
        @Override
        public void handle(Signal signal) {
            logger.info("检测到-15关闭信号，开始关闭系统！");
            softShutdown();
            logger.info("系统关闭完成！");
        }
    }

    /**
     * 判断系统运行的平台
     */
    public boolean ifRunOnLinux() {
        String os = System.getProperty("os.name");
        return !os.toLowerCase().startsWith("win");
    }

    public static boolean sayhi() {
        System.out.println("hi");
        return true;
    }

}
