package cn.dendarii.whole.schedule;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import cn.dendarii.whole.server.MainServer;
import cn.dendarii.whole.server.QueueServer;
import cn.dendarii.whole.service.BaseService;
import cn.dendarii.whole.util.set.HBStringUtil;
import lombok.Getter;

/**
 * 处理定时任务，具体的任务不在这里处理，而是放到queueserver集中处理
 */
@Getter
@Service
public class ScheduledService extends BaseService {
    @Autowired
    private MainServer mainServer;
    @Autowired
    private QueueServer queueServer;
    @Autowired
    public ThreadPoolTaskScheduler threadPoolTaskScheduler;
    private ConcurrentHashMap<String, ScheduledFuture<?>> scheduledFutureMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, ScheduledTask> scheduledTaskMap = new ConcurrentHashMap<>();

    public void addATask(ScheduledTask task) {
        if (task.prepareBean()) {
            ScheduledFuture<?> future = threadPoolTaskScheduler.schedule(new Runnable() {
                @Override
                public void run() {
                    queueServer.addAJob(task.genQueueJob());
                }
            }, task.getTrigger());
            scheduledFutureMap.put(task.getId(), future);
            scheduledTaskMap.put(task.getId(), task);
        } else {
            logger.error("添加定时任务失败：" + task);
        }
    }

    public void removeTask(String taskId) {
        if (HBStringUtil.isNotBlank(taskId)) {
            scheduledFutureMap.remove(taskId).cancel(false);
            scheduledTaskMap.remove(taskId);
        }
    }


}
