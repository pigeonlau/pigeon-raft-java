package cn.edu.nwpu.pigeon.config;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * @author zzf
 */
@Data
public class NodeConfig {

    private String selfAddress;


    private List<String> peerAddress;
}
