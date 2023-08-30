package cn.edu.nwpu.pigeon.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zzf
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppendEntryRequest {
    private Long term;

    private String serverId;

    private String leaderId;

    private Long prevLogIndex;

    private Long preLogTerm;

    private LogEntry[] entries;

    private Long leaderCommit;
}
