package cn.edu.nwpu.pigeon.raft;

import cn.edu.nwpu.pigeon.StateMachine;
import cn.edu.nwpu.pigeon.message.Command;
import cn.edu.nwpu.pigeon.message.LogEntry;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author zzf
 */
@Slf4j
public class StateMachineImpl implements StateMachine {

    private Map<String, String> kvStore = new HashMap<>(16);

    @Override
    public void apply(LogEntry logEntry) {

        try {

            Command command = logEntry.getCommand();
            if (command == null) {
                log.warn("空 log, log entry:{}", logEntry);
                return;
            }

            if (Objects.equals(command.getMethod(), Command.SET)) {
                kvStore.put(command.getKey(), command.getValue());
            }

        } catch (Exception e) {
            log.error("复制状态机失败", e);
        }
    }

    @Override
    public String get(String key) {
        return kvStore.get(key) == null ? "null" : kvStore.get(key);
    }
}
