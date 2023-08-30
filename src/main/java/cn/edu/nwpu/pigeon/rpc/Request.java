package cn.edu.nwpu.pigeon.rpc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zzf
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Request {

    public static final int REQUEST_VOTE = 1;

    public static final int APPEND_ENTRIES = 2;

    public static final int CLIENT_REQ = 3;



    private Integer requestType;

    private String url;

    private Object obj;

}
