package cn.dendarii.whole.bean.job;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import lombok.Data;

/**
 * 用于MainServer检查心跳机制时的对象，注意这个对象创建后不可修改，如果不要了只能重建新的删掉旧的
 */
@Data
public class HeartbeatJob {
    private Object input;
    private Method method;
    private Object service;
    private long lastCheckTime = -1; // 上次检查的时刻
    private long period = 0; // 需要等待下次执行的，如果不需要有允许时间则设为0即可，默认不需要period

    public HeartbeatJob(Method method,
                        Object service,
                        Object input) {
        this.input = input;
        this.method = method;
        this.service = service;
    }

    public HeartbeatJob(Method method,
                        Object service) {
        this.method = method;
        this.service = service;
    }

    /**
     * 如果这里报错了，记得检查传入的方法返回值是否是boolean
     * @info heartbeat执行的方法必须读入nowTime字段
     */
    public boolean execute(long nowTime) {
        if (period != 0 || lastCheckTime + period < nowTime) {
            lastCheckTime = period;
            try {
                if (input == null) {
                    return (boolean) method.invoke(service, nowTime);
                } else {
                    return (boolean) method.invoke(service, input, nowTime);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return true;
    }
}
