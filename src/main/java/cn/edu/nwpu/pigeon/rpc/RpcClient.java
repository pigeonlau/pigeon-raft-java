package cn.edu.nwpu.pigeon.rpc;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;

/**
 * @author zzf
 */
@Slf4j
public class RpcClient {

    public JSONObject send(Request request) {
        HttpResponse httpResponse = HttpRequest.post(request.getUrl())
                .body(JSONObject.toJSONString(request))
                .timeout(100000)
                .execute();

        String responseBody = httpResponse.body();
        return JSONObject.parseObject(responseBody);

    }
}
