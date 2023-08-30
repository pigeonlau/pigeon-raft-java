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
public class LogEntry {

    private Long index;

    private Long term;

    private Command command;
}
