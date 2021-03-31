package com.davidsoft.collections;

@SuppressWarnings("unchecked")
public class IdSet<T> implements Iterable<T> {

    private IdAllotter idAllotter;
    private Object[] values;
    private int maxSize;
    private int minId;

    /**
     * @param minId 可分配的最小id值，必须 >= 0.
     * @param maxId 可分配的最大id值，必须 >= minId. 由于内存为懒开辟，所以它可以为Integer.MAX_VALUE.
     */
    public IdSet(int minId, int maxId) {
        maxSize = maxId - minId;
        if (maxSize < Integer.MAX_VALUE) {
            maxSize++;
        }
        idAllotter = new IdAllotter(0, maxSize - 1);
        values = new Object[Math.min(16, maxSize)];
        this.minId = minId;
    }

    public IdSet(int minId) {
        this(minId, Integer.MAX_VALUE);
    }

    public IdSet() {
        this(0, Integer.MAX_VALUE);
    }

    private void resize() {
        Object[] newValues = new Object[Math.min(values.length << 1, maxSize)];
        System.arraycopy(values, 0, newValues, 0, values.length);
        values = newValues;
    }

    /**
     * 向集合中添加一个元素，返回为其分配的id.
     *
     * @param x 待添加元素. 不接受null值，否则抛出NullPointerException异常.
     *
     * @return 为x分配的id. 如果所有id均已分配，则无法添加，返回-1.
     * @throws NullPointerException 当待添加元素为null时.
     */
    public int add(T x) {
        /*
        if (x == null) {
            throw new NullPointerException("企图向" + getClass().getName() + "中添加null元素.");
        }
        */
        int id = idAllotter.allocate();
        if (id == -1) {
            return -1;
        }
        if (id >= values.length) {
            resize();
        }
        values[id] = x;
        return id + minId;
    }

    public int peekId() {
        return idAllotter.peekId();
    }

    /**
     * 删除集合中指定id的元素，并返回.
     *
     * @param id 要删除的元素的id. 出于性能考虑，本方法不会检查此id是否合法，请在调用时确保此id一定在
     *           [minId .. maxId]范围内且已经被分配出去.
     *
     * @return 刚刚被删除的元素.
     */
    public T remove(int id) {
        id -= minId;
        if (idAllotter.recycle(id)) {
            Object x = values[id];
            values[id] = null;
            return (T) x;
        }
        return null;
    }

    /**
     * 查找指定元素在本集合中的id.
     *
     * @param x 要查找的元素. 通过equals查找.
     *
     * @return x的id. 若未找到，则返回-1. 特别地，如果x为null，则始终返回-1.
     */
    public int idOf(T x) {
        if (x == null) {
            return -1;
        }
        for (int i = 0; i < idAllotter.size(); i++) {
            int id = idAllotter.getId(i);
            if (x.equals(values[id])) {
                return id + minId;
            }
        }
        return -1;
    }

    /**
     * 通过指定id获得元素.
     *
     * @param id 要获得元素的id.
     *
     * @return id对应的元素. 若id未分配，或超过了[minId .. maxId]范围，则返回null.
     */
    public T get(int id) {
        id -= minId;
        if (id < 0 || id >= values.length) {
            return null;
        }
        return (T) values[id];
    }

    /**
     * 将指定id的元素改变成指定元素.
     *
     * @param id 要改变元素的id.
     * @param x  新的元素.
     *
     * @return 此id原先的元素. 若id未分配，或超过了[minId .. maxId]范围，则操作失败，且返回null.
     */
    public T set(int id, T x) {
        id -= minId;
        if (id < 0 || id >= values.length) {
            return null;
        }
        Object o = values[id];
        if (o == null) {
            return null;
        }
        values[id] = x;
        return (T) o;
    }

    /**
     * 删除所有元素.
     */
    public void clear() {
        for (int i = 0; i < idAllotter.size(); i++) {
            values[idAllotter.getId(i)] = null;
        }
        idAllotter.recycleAll();
    }

    /**
     * 获得元素个数.
     *
     * @return 元素个数.
     */
    public int size() {
        return idAllotter.size();
    }

    public boolean isEmpty() {
        return idAllotter.size() == 0;
    }

    /**
     * 获取已经分配的id.
     *
     * @param position 第几个.
     *
     * @return id.
     */
    public int getId(int position) {
        return idAllotter.getId(position) + minId;
    }

    /**
     * 获取剩几个id未分配.
     */
    public int remain() {
        return idAllotter.remain();
    }

    public boolean full() {
        return remain() == 0;
    }

    private class Iterator implements java.util.Iterator<T> {

        private int cursor = 0;

        @Override
        public boolean hasNext() {
            return cursor < idAllotter.size();
        }

        @Override
        public T next() {
            return (T) values[idAllotter.getId(cursor++)];
        }

        @Override
        public void remove() {
            cursor--;
            IdSet.this.remove(idAllotter.getId(cursor));
        }
    }

    @Override
    public java.util.Iterator<T> iterator() {
        return new Iterator();
    }
}
