package cn.dendarii.whole.dao.l2;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.consumer.rebalance.AllocateMessageQueueAveragely;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.RPCHook;
import org.springframework.beans.factory.annotation.Autowired;

import cn.dendarii.whole.bean.job.QueueJob;
import cn.dendarii.whole.dao.l1.BaseKVDao;
import cn.dendarii.whole.server.QueueServer;
import cn.dendarii.whole.util.set.HBCollectionUtil;

/**
 * rocketmq的消费者基类
 */
public abstract class BaseRocketMQConsumer extends BaseKVDao<String, byte[]> {
    @Autowired
    protected QueueServer queueServer; // 保证性能，消费也使用通用任务队列
    protected DefaultMQPushConsumer consumer = null;
    protected Object lock = new Object();

    @PostConstruct
    public boolean init() {
        boolean initState = mainServer.conf().getSwitchOnRocketmqConsumer();
        if (initState) {
            synchronized (lock) {
                try {
                    if (consumer == null) {
                        RPCHook hook = new AclClientRPCHook(new SessionCredentials(mainServer.conf()
                                                                                             .getRocketmqaccessKey(),
                                                                                   mainServer.conf()
                                                                                             .getRocketmqsecretKey()));
                        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(mainServer.conf()
                                                                                             .getRocketmqGroupName(),
                                                                                   hook,
                                                                                   new AllocateMessageQueueAveragely());
                        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
                        consumer.subscribe(topic(), "*"); // 这里没有对数据做任何限制，今后根据情况可以考虑
                        consumer.setNamesrvAddr(mainServer.conf().getRocketmqUrl() + ":"
                                + mainServer.conf().getRocketmqPort());
                        BaseRocketMQConsumer parentService = this;
                        consumer.setMessageListener(new MessageListenerConcurrently() {
                            @Override
                            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs,
                                                                            ConsumeConcurrentlyContext context) {
                                if (HBCollectionUtil.isNotEmpty(msgs)) {
                                    for (MessageExt msg : msgs) {
                                        QueueJob job;
                                        try {
                                            job = new QueueJob(msg,
                                                               parentService.getClass()
                                                                            .getMethod("consumeMessage",
                                                                                       MessageExt.class),
                                                               parentService);
                                            queueServer.addAJob(job);
                                        } catch (NoSuchMethodException | SecurityException e) {
                                            logger.error("msg写入queueserver失败", e);
                                        }
                                    }
                                }
                                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                            }
                        });
                        consumer.start();
                    }
                } catch (Exception e) {
                    logger.error("rocketmq初始化失败 init error", e);
                    mainServer.shutdown(-1);
                    initState = false;
                }
            }
        }
        return initState;
    }

    public abstract String topic();

    // 模板方法，继承的类应该覆盖这个方法
    public boolean consumeMessage(MessageExt msg) {
        logger.info("consumer收到：" + new String(msg.getBody()));
        return true;
    }

    @PreDestroy
    public void onDestroy() {
        if (consumer != null) {
            try {
                consumer.shutdown();
                consumer = null;
            } catch (Exception e) {
                logger.error("rocketmq consumer关闭失败", e);
            }
        }
    }
}
