package cn.edu.nwpu.pigeon;

import cn.edu.nwpu.pigeon.message.LogEntry;

/**
 * @author zzf
 */
public interface Log {


    void write(LogEntry logEntry);

    LogEntry read(Long index);

    void removeOnStartIndex(Long startIndex);

    LogEntry getLast();

    Long getLastIndex();
}
