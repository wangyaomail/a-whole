package cn.dendarii.whole.service;

import java.lang.reflect.ParameterizedType;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

import cn.dendarii.whole.bean.mongo.BaseIdBean;
import cn.dendarii.whole.dao.l2.BaseCRUDDao;
import cn.dendarii.whole.dao.l3.BaseMongoDao;
import cn.dendarii.whole.server.MainServer;

/**
 * 高于BaseCRUDService的一层，能自动封装基础版的dao，不带缓存
 */
public abstract class FullBaseCRUDService<T extends BaseIdBean> extends BaseCRUDService<T> {
    @Resource
    protected MongoTemplate mongoTemplate;
    @Autowired
    protected MainServer mainServer;
    @Autowired
    protected TimerService timerService;
    protected BaseCRUDDao<T> baseDao;

    public FullBaseCRUDService() {}

    public FullBaseCRUDService(MongoTemplate mongoTemplate,
                               MainServer mainServer,
                               TimerService timerService,
                               BaseCRUDDao<T> baseDao) {
        this.mongoTemplate = mongoTemplate;
        this.mainServer = mainServer;
        this.timerService = timerService;
        this.baseDao = baseDao;
    }

    @Override
    public BaseCRUDDao<T> dao() {
        return baseDao;
    }

    @SuppressWarnings("unchecked")
    @PostConstruct
    public void fullAutoInit() {
        if (baseDao == null) {
            ParameterizedType pt = (ParameterizedType) getClass().getGenericSuperclass();
            baseDao = new BaseMongoDao<T>(mainServer,
                                          mongoTemplate,
                                          (Class<T>) pt.getActualTypeArguments()[0]) {};
        }
    }
}
