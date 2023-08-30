package cn.edu.nwpu.pigeon.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zzf
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class VoteResult {

    private Long term;

    private boolean voteGranted;
}
