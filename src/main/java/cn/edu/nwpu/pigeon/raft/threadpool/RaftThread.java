package cn.edu.nwpu.pigeon.raft.threadpool;

import lombok.extern.slf4j.Slf4j;

/**
 * @author zzf
 */
@Slf4j
public class RaftThread extends Thread {

    private static final UncaughtExceptionHandler UNCAUGHT_EXCEPTION_HANDLER = (t, e)
            -> log.warn("Exception occurred from thread {}", t.getName(), e);

    public RaftThread(String threadName, Runnable r) {
        super(r, threadName);
        setUncaughtExceptionHandler(UNCAUGHT_EXCEPTION_HANDLER);
    }

}
