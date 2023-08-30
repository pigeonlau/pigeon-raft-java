package cn.edu.nwpu.pigeon.run;


import cn.edu.nwpu.pigeon.config.NodeConfig;
import cn.edu.nwpu.pigeon.raft.Node;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;


@Slf4j
public class RaftNode2 {

    public static final String[] DEFAULT_PROCESS = new String[]{"localhost:10001", "localhost:10002", "localhost:10003"};

    private static final String SELF_PORT = "10002";

    public static void main(String[] args) throws Throwable {
        run();
    }

    public static void run() throws Throwable {


        NodeConfig config = new NodeConfig();
        config.setSelfAddress("localhost:" + SELF_PORT);
        config.setPeerAddress(Arrays.asList(DEFAULT_PROCESS));


        Node node = new Node();
        node.setConfig(config);

        node.init();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            synchronized (node) {
                node.notifyAll();
            }
        }));

        synchronized (node) {
            node.wait();
        }



    }

}
