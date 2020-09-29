package cn.dendarii.whole.dao.l1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;

import cn.dendarii.whole.server.MainServer;

/**
 * 最最基础的dao，不对上层做任何限制
 */
@DependsOn("sysConfService")
public abstract class BaseDao {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    protected MainServer mainServer;
}
