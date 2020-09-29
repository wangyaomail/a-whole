package cn.dendarii.whole.dao.l2;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.RPCHook;
import org.springframework.beans.factory.annotation.Autowired;

import cn.dendarii.whole.bean.job.QueueJob;
import cn.dendarii.whole.dao.l1.BaseKVDao;
import cn.dendarii.whole.server.QueueServer;

/**
 * rocketmq的发送者，需要使用的地方请继承类中关于
 */
public abstract class BaseRocketMQProducer extends BaseKVDao<String, byte[]> {
    @Autowired
    protected QueueServer queueServer; // 保证性能，所有对象解析都放在这里执行
    protected DefaultMQProducer producer = null;

    @PostConstruct
    public boolean init() {
        boolean initState = mainServer.conf().getSwitchOnRocketmqProducer();
        if (initState) {
            try {
                if (producer == null) {
                    RPCHook hook = new AclClientRPCHook(new SessionCredentials(mainServer.conf()
                                                                                         .getRocketmqaccessKey(),
                                                                               mainServer.conf()
                                                                                         .getRocketmqsecretKey()));
                    producer = new DefaultMQProducer(rocketMQGroupName(), hook);
                    producer.setNamesrvAddr(mainServer.conf().getRocketmqUrl() + ":"
                            + mainServer.conf().getRocketmqPort());
                    producer.start();
                }
            } catch (Exception e) {
                logger.error("rocketmq初始化失败 init error", e);
                mainServer.shutdown(-1);
                initState = false;
            }
        }
        return initState;
    }

    // 返回配置时候的rocketmq队列名
    public abstract String rocketMQGroupName();

    // 一般一个服务对应一个topic
    public abstract String topic();

    @PreDestroy
    public void onDestroy() {
        if (producer != null) {
            try {
                producer.shutdown();
                producer = null;
            } catch (Exception e) {
                logger.error("rocketmq producer关闭失败", e);
            }
        }
    }

    public void sendMessage(String tag,
                            byte[] body) {
        sendMessage(topic(), tag, body);
    }

    public void sendMessage(String topic,
                            String tags,
                            byte[] body) {
        if (body != null) {
            try {
                Message msg = new Message(topic, tags, body);
                QueueJob job = new QueueJob(msg,
                                            this.getClass()
                                                .getMethod("realSendMessage", Message.class),
                                            this);
                queueServer.addAJob(job);
            } catch (Exception e) {
                logger.error("rocketmq 发送数据写队列失败", e);
            }
        }
    }

    public boolean realSendMessage(Message msg) {
        try {
            // Message msg = (Message) msgObj;
            SendResult sendResult = producer.send(msg);
            logger.info("rocketmq发送数据：%s%n", sendResult);
            return true;
        } catch (Exception e) {
            logger.error("rocketmq输出数据出错", e);
            return false;
        }
    }
}
