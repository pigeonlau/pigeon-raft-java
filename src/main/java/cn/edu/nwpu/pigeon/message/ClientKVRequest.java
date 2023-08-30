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
public class ClientKVRequest {
    public static int PUT = 0;
    public static int GET = 1;

    int type;

    String key;

    String value;

    public enum Type {
        PUT(0), GET(1);
        int code;

        Type(int code) {
            this.code = code;
        }

        public static Type value(int code) {
            for (Type type : values()) {
                if (type.code == code) {
                    return type;
                }
            }
            return null;
        }
    }
}
