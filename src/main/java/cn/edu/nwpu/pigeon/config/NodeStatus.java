package cn.edu.nwpu.pigeon.config;

import lombok.Data;

/**
 * @author zzf
 */

public enum NodeStatus {
    FOLLOWER(0),
    CANDIDATE(1),
    LEADER(2);

    NodeStatus(int code) {
        this.code = code;
    }

    private final int code;

    public int getCode() {
        return code;
    }

    public static NodeStatus value(int i) {
        for (NodeStatus value : NodeStatus.values()) {
            if (value.code == i) {
                return value;
            }
        }
        return null;
    }

}
