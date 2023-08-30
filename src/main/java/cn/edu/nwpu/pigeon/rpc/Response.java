package cn.edu.nwpu.pigeon.rpc;

import lombok.Data;

/**
 * @author zzf
 */
@Data
public class Response<T> {
    private T result;
}
