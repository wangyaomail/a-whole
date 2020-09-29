package cn.dendarii.whole.service;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import cn.dendarii.whole.bean.job.HeartbeatJob;
import cn.dendarii.whole.server.MainServer;
import cn.dendarii.whole.util.time.TimeUtil;
import lombok.Getter;

/**
 * 系统的标准化时间服务
 */
@Getter
@Service
public class TimerService extends BaseService {
    // 以2018-08-02-09-40时间节点为示例的起始时间
    public static String now_to_day; // 今日天，2018-08-02
    public static String las_to_day; // 昨日天，2018-08-01
    public static String la3_to_day; // 三天前，2018-07-31
    public static String la4_to_day; // 四天前，2018-07-30
    public static String nxt_to_day; // 明日天，2018-08-03
    public static String now_to_hour; // 当前小时，2018-08-02-09
    public static String las_to_hour; // 上一小时，2018-08-02-08
    public static String nxt_to_hour; // 下一小时，2018-08-02-10
    public static String now_to_min; // 当前分钟，2018-08-02-09-40
    public static String las_to_min; // 上一分钟，2018-08-02-09-39
    public static String nxt_to_min; // 下一分钟，2018-08-02-09-41
    public static long now_to_timestamp; // 当前时间戳，long
    @Autowired
    private MainServer mainServer;

    public TimerService() {
        resetMinClock();
        resetHourClock();
        resetDayClock();
    }

    @PostConstruct
    public void init() throws NoSuchMethodException, SecurityException {
        // 将分钟任务注入mainserver的检测器，降低时间调度器的损耗
        HeartbeatJob job = new HeartbeatJob(TimerService.class.getMethod("resetMinClock",
                                                                         long.class),
                                            this);
        job.setPeriod(0);
        mainServer.addHearbeatJob(job);
        for (;;) {
            try {
                Thread.sleep(10000);
                System.out.println(now_to_min);
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
    }

    /**
     * 每分钟更新一次分钟时间
     */
    public boolean resetMinClock() {
        long nowTime = System.currentTimeMillis();
        now_to_min = TimeUtil.getStringFromFreq(nowTime, "minute");
        las_to_min = TimeUtil.getStringFromFreq(nowTime - 60l * 1000, "minute");
        nxt_to_min = TimeUtil.getStringFromFreq(nowTime + 60l * 1000, "minute");
        now_to_timestamp = nowTime;
        return true;
    }

    public boolean resetMinClock(long nowTime) {
        now_to_min = TimeUtil.getStringFromFreq(nowTime, "minute");
        las_to_min = TimeUtil.getStringFromFreq(nowTime - 60l * 1000, "minute");
        nxt_to_min = TimeUtil.getStringFromFreq(nowTime + 60l * 1000, "minute");
        now_to_timestamp = nowTime;
        return true;
    }

    /**
     * 每小时更新一次小时级时间
     */
    @Scheduled(cron = "0 0 * * * *")
    public void resetHourClock() {
        long nowTime = System.currentTimeMillis();
        now_to_hour = TimeUtil.getStringFromFreq(nowTime, "hour");
        las_to_hour = TimeUtil.getStringFromFreq(nowTime - 60l * 60 * 1000, "hour");
        nxt_to_hour = TimeUtil.getStringFromFreq(nowTime + 60l * 60 * 1000, "hour");
        now_to_timestamp = nowTime;
    }

    /**
     * 每天更新一次天级时间
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void resetDayClock() {
        long nowTime = System.currentTimeMillis();
        now_to_day = TimeUtil.getStringFromFreq(nowTime, "day");
        las_to_day = TimeUtil.getStringFromFreq(nowTime - 24l * 3600 * 1000, "day");
        la3_to_day = TimeUtil.getStringFromFreq(nowTime - 24l * 3600 * 1000 * 2, "day");
        la4_to_day = TimeUtil.getStringFromFreq(nowTime - 24l * 3600 * 1000 * 3, "day");
        nxt_to_day = TimeUtil.getStringFromFreq(nowTime + 24l * 3600 * 1000, "day");
        now_to_timestamp = nowTime;
    }
}
