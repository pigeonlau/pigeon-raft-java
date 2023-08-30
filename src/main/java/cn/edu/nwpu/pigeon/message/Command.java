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
public class Command {

    public static final String SET = "SET";

    public static final String GET = "GET";


    private String method;

    private String key;

    private String value;
}
