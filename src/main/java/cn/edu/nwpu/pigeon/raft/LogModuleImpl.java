package cn.edu.nwpu.pigeon.raft;

import cn.edu.nwpu.pigeon.Log;
import cn.edu.nwpu.pigeon.message.Command;
import cn.edu.nwpu.pigeon.message.LogEntry;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * @author zzf
 */
@Slf4j
public class LogModuleImpl implements Log {

    private Deque<LogEntry> logDB = new LinkedList<>();
    private Map<Long, LogEntry> map = new HashMap<>();

    private ReentrantLock lock = new ReentrantLock();

    {
        logDB.addLast(new LogEntry(1L,1L,new Command("SET","raft","true")));
    }

    @Override
    public void write(LogEntry logEntry) {
        boolean result;
        try {
            result = lock.tryLock(3000, MILLISECONDS);
            if (!result) {
                throw new RuntimeException("write fail, tryLock fail.");
            }
            logEntry.setIndex(getLastIndex() + 1);
            map.put(logEntry.getIndex(), logEntry);
            logDB.addLast(logEntry);
            log.info("写日志成功, logEntry info : [{}]", logEntry);
        } catch (Exception e) {
            log.info("fail", e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public LogEntry read(Long index) {
        try {
            return map.get(index);
        } catch (Exception e) {
            log.info("fail", e);
        }
        return null;
    }

    @Override
    public void removeOnStartIndex(Long startIndex) {
        boolean tryLock;
        try {
            tryLock = lock.tryLock(3000, MILLISECONDS);
            if (!tryLock) {
                throw new RuntimeException("tryLock fail, removeOnStartIndex fail");
            }
            for (long i = startIndex; i <= getLastIndex(); i++) {

                LogEntry logEntry = map.get(i);
                logDB.remove(logEntry);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public LogEntry getLast() {
        if (logDB.isEmpty()) {
            return null;
        }
        return logDB.getLast();
    }

    @Override
    public Long getLastIndex() {
        return getLast().getIndex();
    }
}
