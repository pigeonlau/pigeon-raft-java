package cn.edu.nwpu.pigeon.run;

import cn.edu.nwpu.pigeon.message.ClientKVRequest;
import cn.edu.nwpu.pigeon.message.ClientKVResult;
import cn.edu.nwpu.pigeon.message.Command;
import cn.edu.nwpu.pigeon.message.LogEntry;
import cn.edu.nwpu.pigeon.rpc.Request;
import cn.edu.nwpu.pigeon.rpc.RpcClient;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;

@Slf4j
public class RaftClientPut {

    public static void main(String[] args) throws Throwable {

        RpcClient rpc = new RpcClient();

        try {
            String key = "key1";
            String value = "1111";

            Request request = new Request();
            request.setRequestType(Request.CLIENT_REQ);
            request.setUrl("localhost:10001");
            ClientKVRequest clientKVRequest = new ClientKVRequest();
            clientKVRequest.setType(ClientKVRequest.PUT);
            clientKVRequest.setKey(key);
            clientKVRequest.setValue(value);
            request.setObj(clientKVRequest);


            log.info("发送 put 指令, 目标 url:{}  key: {}, value: {}", "localhost:10001", key, value);

            ClientKVResult clientKVResult = rpc.send(request).to(ClientKVResult.class);

            log.info("返回 {}", clientKVResult);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }


    }

}
