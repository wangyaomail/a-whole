package cn.dendarii.whole.schedule;

import java.lang.reflect.Method;

import org.springframework.scheduling.support.CronSequenceGenerator;
import org.springframework.scheduling.support.CronTrigger;

import cn.dendarii.whole.bean.job.QueueJob;
import cn.dendarii.whole.util.set.HBStringUtil;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 这个task从WTask来，转为内部可以运行的task
 */
@Data
@AllArgsConstructor
public class ScheduledTask {

    private String id;
    private String expression;
    private CronTrigger trigger;
    private Object input;
    private Method method;
    private Object service;

    public boolean check() {
        if (HBStringUtil.isBlank(id)) {
            return false;
        }
        if (HBStringUtil.isBlank(expression)
                || !CronSequenceGenerator.isValidExpression(expression)) {
            return false;
        }
        return true;
    }

    public boolean prepareBean() {
        if (check() && trigger == null) {
            trigger = new CronTrigger(expression);
            return true;
        } else {
            return false;
        }
    }

    /**
     * 以此定时任务为模板创造一个具体的执行任务
     */
    public QueueJob genQueueJob() {
        return new QueueJob(input, method, service);
    }

}
