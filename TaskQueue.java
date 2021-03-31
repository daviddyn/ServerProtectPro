package com.davidsoft.serverprotect.libs;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class TaskQueue {

    private static final class ScheduledTask implements Comparable<ScheduledTask> {

        private final long when;
        private Runnable task;

        private ScheduledTask(long when, Runnable task) {
            this.when = when;
            this.task = task;
        }

        @Override
        public int compareTo(ScheduledTask o) {
            return Long.compare(when, o.when);
        }

    }

    private final class DispatchTask extends TimerTask {

        @Override
        public void run() {
            //取出要执行的任务
            ScheduledTask scheduledTask;
            try {
                lock.lockInterruptibly();
            } catch (InterruptedException e) {
                return;
            }
            scheduledTask = taskQueue.poll();
            assert scheduledTask != null;
            //执行调度任务
            if (scheduledTask.task != null) {
                scheduledTask.task.run();
                timer = null;
            }
            insertTasksAndDispatch(null);
            lock.unlock();
        }
    }

    private final ReentrantLock lock;
    private final HashMap<Runnable, ScheduledTask> taskIndex;
    private final PriorityQueue<ScheduledTask> taskQueue;
    private Timer timer;

    public TaskQueue() {
        lock = new ReentrantLock(true);
        taskIndex = new HashMap<>();
        taskQueue = new PriorityQueue<>();
    }

    //不带同步控制
    private void insertTasksAndDispatch(ScheduledTask scheduledTask) {
        long currentMinTimestamp = Long.MAX_VALUE;
        if (!taskQueue.isEmpty()) {
            currentMinTimestamp = taskQueue.peek().when;
        }
        if (scheduledTask != null) {
            taskQueue.add(scheduledTask);
        }
        long newMinTimestamp = Long.MAX_VALUE;
        if (!taskQueue.isEmpty()) {
            newMinTimestamp = taskQueue.peek().when;
        }
        if (timer == null) {
            if (newMinTimestamp < Long.MAX_VALUE) {
                timer = new Timer();
                timer.schedule(new DispatchTask(), new Date(newMinTimestamp));
            }
            else {
                //TODO: 任务清空时
            }
        }
        else {
            if (newMinTimestamp < currentMinTimestamp) {
                timer.cancel();
                timer = new Timer();
                timer.schedule(new DispatchTask(), new Date(newMinTimestamp));
            }
        }
    }

    public void scheduleTask(Runnable task, long when) {

    }

    public void cancelTask(Runnable task) {

    }

}
