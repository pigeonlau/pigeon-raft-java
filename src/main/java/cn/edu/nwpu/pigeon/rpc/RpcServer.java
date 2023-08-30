package cn.edu.nwpu.pigeon.rpc;

import cn.edu.nwpu.pigeon.config.NodeConfig;
import cn.edu.nwpu.pigeon.message.*;
import cn.edu.nwpu.pigeon.raft.Node;
import com.alibaba.fastjson2.JSONObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Random;

/**
 * @author zzf
 */
@Slf4j
@AllArgsConstructor
public class RpcServer {

    private Node node;

    public void init(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new MyHandler(node));
        server.setExecutor(null); // 使用默认的线程池

        server.start();
        log.info("Server is listening on port {}", port);
    }

    @AllArgsConstructor
    static class MyHandler implements HttpHandler {

        private Node node;

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            byte[] bytes = exchange.getRequestBody().readAllBytes();
            JSONObject requestObject = JSONObject.parseObject(new String(bytes));

            log.info("get request , request is {}", requestObject);



            int requestType = requestObject.getIntValue("requestType");
            requestObject = requestObject.getJSONObject("obj");
            String responseBody = "";

            if (requestType == Request.REQUEST_VOTE) {
                VoteResult voteResult = node.handlerRequestVote(requestObject.to(VoteRequest.class));
                responseBody = JSONObject.toJSONString(voteResult);
                log.info("投票请求 REQUEST_VOTE , body is {}",requestObject);
            } else if (requestType == Request.APPEND_ENTRIES) {

                AppendEntryResult appendEntryResult = node.handlerAppendEntries(requestObject.to(AppendEntryRequest.class));
                responseBody = JSONObject.toJSONString(appendEntryResult);
                log.info("日志复制请求 APPEND_ENTRIES , body is {}",requestObject);
            } else if (requestType == Request.CLIENT_REQ) {
                ClientKVResult clientKVResult = node.handlerClientRequest(requestObject.to(ClientKVRequest.class));
                responseBody = JSONObject.toJSONString(clientKVResult);
                log.info("客户端请求 CLIENT_REQ , body is {}",requestObject);
            }


            exchange.sendResponseHeaders(200, responseBody.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(responseBody.getBytes());
            os.close();
        }
    }
}
