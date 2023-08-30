package cn.edu.nwpu.pigeon;

import cn.edu.nwpu.pigeon.message.LogEntry;

/**
 * @author zzf
 */
public interface StateMachine {

    void apply(LogEntry logEntry);

    String get(String key);
}
