package cn.edu.nwpu.pigeon.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zzf
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VoteRequest {

    private Long term;

    private String serverId;

    private String candidateId;

    private Long lastLogIndex;

    private Long lastLogTerm;
}
