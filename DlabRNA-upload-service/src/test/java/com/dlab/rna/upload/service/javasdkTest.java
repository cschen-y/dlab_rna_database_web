package com.dlab.rna.upload.service;

import java.util.*;

import org.json.JSONArray;
import org.json.JSONObject;
import com.baidu.aip.ocr.AipOcr;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class javasdkTest {
    //设置APPID/AK/SK
    public static final String APP_ID = "120995884";
    public static final String API_KEY = "myjQe4b9E0ZMSRu60Srrrdqq";
    public static final String SECRET_KEY = "O4PD2Vcp5khrEwo9DukCGqZX8pxNeVW6";

    @Test
    public void test1() {
        // 初始化一个AipOcr
        AipOcr client = new AipOcr(APP_ID, API_KEY, SECRET_KEY);

        // 可选：设置网络连接参数
        client.setConnectionTimeoutInMillis(2000);
        client.setSocketTimeoutInMillis(60000);

//        // 可选：设置代理服务器地址, http和socket二选一，或者均不设置
//        client.setHttpProxy("proxy_host", proxy_port);  // 设置http代理
//        client.setSocketProxy("proxy_host", proxy_port);  // 设置socket代理

        // 可选：设置log4j日志输出格式，若不设置，则使用默认配置
        // 也可以直接通过jvm启动参数设置此环境变量
//        System.setProperty("aip.log4j.conf", "log4j.properties");

        // 调用接口
        String path = "D:\\Desktop\\web\\database\\RNADatabase1013\\DlabRNA-upload-service\\src\\test\\resources\\1.png";
        JSONObject res = client.basicGeneral(path, new HashMap<String, String>());
        System.out.println(res.toString(2));
    }

    private static void parseResult(JSONObject response) {
        if (response.has("words_result")) {
            JSONArray wordsArray = response.getJSONArray("words_result");
            System.out.println("识别结果：");
            for (int i = 0; i < wordsArray.length(); i++) {
                JSONObject wordObj = wordsArray.getJSONObject(i);
                System.out.println(wordObj.getString("words"));
            }
        } else {
            System.err.println("识别失败：" + response.toString());
        }
    }
}
