package cn.dendarii.whole.bean.mongo;

import java.lang.reflect.Method;

import lombok.Data;

@Data
public class BaseStreamBean {
    private Method returnMethod; // 回调函数
}
