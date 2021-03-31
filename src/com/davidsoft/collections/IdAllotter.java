package com.davidsoft.collections;

public class IdAllotter {

    private int[] ids;
    private int[] idPositions;
    private int allocatePointer;
    private int minId;
    private int maxId;

    /**
     * @param minId 可分配的最小id值，必须 >= 0.
     * @param maxId 可分配的最大id值，必须 >= minId. 由于内存为懒开辟，所以它可以为Integer.MAX_VALUE.
     */
    public IdAllotter(int minId, int maxId) {
        if (maxId - minId == Integer.MAX_VALUE) {
            maxId--;
        }
        int length = Math.min(16, maxId - minId + 1);
        ids = new int[length];
        idPositions = new int[length];
        allocatePointer = 0;
        this.minId = minId;
        this.maxId = maxId;
        for (int i = 0; i < length; i++) {
            ids[i] = i;
            idPositions[i] = -1;
        }
    }

    private void resize() {
        int newLength = Math.min(ids.length << 1, maxId - minId + 1);
        int[] newIds = new int[newLength];
        int[] newIdPositions = new int[newLength];
        System.arraycopy(ids, 0, newIds, 0, ids.length);
        System.arraycopy(idPositions, 0, newIdPositions, 0, idPositions.length);
        for (int i = allocatePointer; i < newIds.length; i++) {
            newIds[i] = i;
            newIdPositions[i] = -1;
        }
        ids = newIds;
        idPositions = newIdPositions;
    }

    /**
     * 分配一个新的id.
     *
     * @return 新的id值. 如果所有id均已分配，则返回-1。
     */
    public int allocate() {
        if (allocatePointer == ids.length) {
            if (minId + ids.length - 1 == maxId) {
                return -1;
            }
            resize();
        }
        int id = ids[allocatePointer];
        idPositions[id] = allocatePointer;
        allocatePointer++;
        return id + minId;
    }

    public int peekId() {
        if (allocatePointer == ids.length) {
            if (minId + ids.length - 1 == maxId) {
                return -1;
            }
            return minId + allocatePointer;
        }
        return minId + ids[allocatePointer];
    }

    /**
     * 归还一个已分配的id.
     *
     * @param id 要归还的id.
     *
     * @return 当此id不合法(超出[minId .. maxId]范围、或尚未被分配)时返回false.
     */
    public boolean recycle(int id) {
        id -= minId;
        if (id < 0 || id >= idPositions.length || idPositions[id] < 0) {
            return false;
        }
        allocatePointer--;
        int movingId = ids[allocatePointer];
        int positionOfRemovingId = idPositions[id];
        ids[positionOfRemovingId] = movingId;
        idPositions[movingId] = positionOfRemovingId;
        ids[allocatePointer] = id;
        idPositions[id] = -1;
        return true;
    }

    /**
     * 所有id全部收回.
     */
    public void recycleAll() {
        for (int i = 0; i < allocatePointer; i++) {
            idPositions[ids[i]] = -1;
        }
        allocatePointer = 0;
    }

    /**
     * 判断指定id是否已被分配.
     *
     * @param id 待判断的id
     * @return 当此id尚未被分配，或超出[minId .. maxId]范围时返回false，否则返回true.
     */
    public boolean allocated(int id) {
        id -= minId;
        return id >= 0 && id < idPositions.length && idPositions[id] >= 0;
    }

    /**
     * 获取已经分配id的个数.
     *
     * @return 返回当前已经分配了几个id.
     */
    public int size() {
        return allocatePointer;
    }

    /**
     * 获取剩几个id未分配.
     */
    public int remain() {
        return maxId - minId + 1 - allocatePointer;
    }

    /**
     * 获取已经分配的id.
     *
     * @param position 第几个.
     *
     * @return id.
     */
    public int getId(int position) {
        if (position < 0 || position >= allocatePointer) {
            throw new ArrayIndexOutOfBoundsException("总长度：" + allocatePointer + "，企图访问的位置：" + position);
        }
        return ids[position];
    }
}