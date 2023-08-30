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
@AllArgsConstructor
@NoArgsConstructor
public class ClientKVResult {
    String result;

    public static ClientKVResult success() {
        return new ClientKVResult("ok");
    }

    public static ClientKVResult fail() {
        return new ClientKVResult("fail");
    }

}
