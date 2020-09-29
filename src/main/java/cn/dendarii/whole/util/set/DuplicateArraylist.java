package cn.dendarii.whole.util.set;

import java.util.ArrayList;

/**
 * 复写list，读取是某一个时刻的全量镜像，写入是阻塞的写入
 */
public class DuplicateArraylist<E> {
    private ArrayList<E> array = new ArrayList<>();

    public synchronized ArrayList<E> getList() {
        return new ArrayList<>(array);
    }

    public synchronized void add(E e) {
        array.add(e);
    }
}
