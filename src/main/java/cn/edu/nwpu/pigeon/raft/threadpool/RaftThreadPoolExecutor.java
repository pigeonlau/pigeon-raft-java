package cn.edu.nwpu.pigeon.raft.threadpool;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author zzf
 */
@Slf4j
public class RaftThreadPoolExecutor extends ThreadPoolExecutor {

    private static final ThreadLocal<Long> COST_TIME_WATCH = ThreadLocal.withInitial(System::currentTimeMillis);

    public RaftThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                                  BlockingQueue<Runnable> workQueue, RaftThreadPool.NameThreadFactory nameThreadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, nameThreadFactory);
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        COST_TIME_WATCH.get();
        log.debug("raft thread pool before Execute");
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        log.debug("raft thread pool after Execute, cost time : {}", System.currentTimeMillis() - COST_TIME_WATCH.get());
        COST_TIME_WATCH.remove();
    }

    @Override
    protected void terminated() {
        log.info("active count : {}, queueSize : {}, poolSize : {}", getActiveCount(), getQueue().size(), getPoolSize());
    }
}
