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
public class AppendEntryResult {

    private Long term;

    private boolean success;

}
