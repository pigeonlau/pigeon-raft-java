package cn.edu.nwpu.pigeon.run;

import cn.edu.nwpu.pigeon.message.ClientKVRequest;
import cn.edu.nwpu.pigeon.message.ClientKVResult;
import cn.edu.nwpu.pigeon.message.Command;
import cn.edu.nwpu.pigeon.message.LogEntry;
import cn.edu.nwpu.pigeon.rpc.Request;
import cn.edu.nwpu.pigeon.rpc.RpcClient;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RaftClient3 {

    public static void main(String[] args) throws Throwable {


        RpcClient rpc = new RpcClient();

        try {
            String key = "raft";
            String value = "node1";

            Request request = new Request();
            request.setRequestType(Request.CLIENT_REQ);
            request.setUrl("localhost:10003");
            ClientKVRequest clientKVRequest = new ClientKVRequest();
            clientKVRequest.setType(ClientKVRequest.GET);
            clientKVRequest.setKey(key);
            clientKVRequest.setValue(value);
            request.setObj(clientKVRequest);

            log.info("发送 get 指令, 目标 url:{}  key: {}, value{}", "localhost:10003", key, value);

            ClientKVResult clientKVResult = rpc.send(request).to(ClientKVResult.class);

            log.info("返回 {}", clientKVResult);


            key = "key1";

             request = new Request();
            request.setRequestType(Request.CLIENT_REQ);
            request.setUrl("localhost:10003");
             clientKVRequest = new ClientKVRequest();
            clientKVRequest.setType(ClientKVRequest.GET);
            clientKVRequest.setKey(key);
            clientKVRequest.setValue(value);
            request.setObj(clientKVRequest);

            log.info("发送 get 指令, 目标 url:{}  key: {}, value{}", "localhost:10003", key, value);

            clientKVResult = rpc.send(request).to(ClientKVResult.class);

            log.info("返回 {}", clientKVResult);

            key = "key2";

            request = new Request();
            request.setRequestType(Request.CLIENT_REQ);
            request.setUrl("localhost:10003");
            clientKVRequest = new ClientKVRequest();
            clientKVRequest.setType(ClientKVRequest.GET);
            clientKVRequest.setKey(key);
            clientKVRequest.setValue(value);
            request.setObj(clientKVRequest);

            log.info("发送 get 指令, 目标 url:{}  key: {}, value{}", "localhost:10003", key, value);

            clientKVResult = rpc.send(request).to(ClientKVResult.class);

            log.info("返回 {}", clientKVResult);

            key = "key3";

            request = new Request();
            request.setRequestType(Request.CLIENT_REQ);
            request.setUrl("localhost:10003");
            clientKVRequest = new ClientKVRequest();
            clientKVRequest.setType(ClientKVRequest.GET);
            clientKVRequest.setKey(key);
            clientKVRequest.setValue(value);
            request.setObj(clientKVRequest);

            log.info("发送 get 指令, 目标 url:{}  key: {}, value{}", "localhost:10003", key, value);

            clientKVResult = rpc.send(request).to(ClientKVResult.class);

            log.info("返回 {}", clientKVResult);

            key = "key4";

            request = new Request();
            request.setRequestType(Request.CLIENT_REQ);
            request.setUrl("localhost:10003");
            clientKVRequest = new ClientKVRequest();
            clientKVRequest.setType(ClientKVRequest.GET);
            clientKVRequest.setKey(key);
            clientKVRequest.setValue(value);
            request.setObj(clientKVRequest);

            log.info("发送 get 指令, 目标 url:{}  key: {}, value{}", "localhost:10003", key, value);

            clientKVResult = rpc.send(request).to(ClientKVResult.class);

            log.info("返回 {}", clientKVResult);


        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }


    }

}
