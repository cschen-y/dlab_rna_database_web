//package com.dlab.rna.upload.service;
//
//import org.apache.hc.client5.http.entity.mime.FileBody;
//import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
//import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
//import org.apache.hc.core5.http.HttpEntity;
//import org.apache.hc.core5.http.ParseException;
//import org.apache.hc.core5.http.io.entity.EntityUtils;
//import org.junit.jupiter.api.Test;
//import org.springframework.boot.test.context.SpringBootTest;
//import com.alibaba.fastjson.JSONObject;
//import java.io.File;
//import java.io.IOException;
//import java.net.URI;
//import java.net.URISyntaxException;
//import java.net.http.HttpClient;
//import java.net.http.HttpRequest;
//import java.net.http.HttpResponse;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.UUID;
//
//import static java.lang.Thread.sleep;
//
//@SpringBootTest
//public class keda {
//
//    @Test
//    public void test1() throws URISyntaxException, IOException, InterruptedException, ParseException {
////        String APPID="91c076dd";
////        String APISecret="OGM5YjAxMTg0ZDk1YjQzZDU4ZTUyMTdi";
////        String baseUrl = "https://iocr.xfyun.cn/ocrzdq";
////
////        // 获取请求头中需要携带的参数 appId（控制台获取）, timestamp（时间戳，单位：秒，与服务端时间相差五分钟之内）, signature（签名）
////        long timestamp = System.currentTimeMillis() / 1000;
////        String ts = String.valueOf(timestamp);
////
////        String signature = ApiAuthAlgorithm.getSignature(APPID, APISecret, timestamp);
////
////        // 建立链接
////        ApiClient client = new ApiClient(baseUrl);
////        //导出类型：word
////        String exportFormat = "word";
////
////
////        //方案1：有公网的pdf地址
////        //String pdfUrl = "https://xxxx.com/xxx.pdf";
////        //String result = client.startWithUrl(APPID, ts, signature, pdfUrl, exportFormat);
////
////        //方案2：上传本地的文件
////        File file = new File("D:\\Desktop\\web\\database\\RNADatabase1013\\DlabRNA-upload-service\\src\\test\\resources\\RBind.pdf");
////        String result = client.startWithFile(APPID, ts, signature, file, exportFormat);
////        JSONObject jsonObject = JSONObject.parseObject(result);
////        if ((Integer) jsonObject.get("code") != 0) {
////            System.out.println(jsonObject.get("desc"));
////            return;
////        }
////        JSONObject data = (JSONObject) jsonObject.get("data");
////        String taskNo = data.get("taskNo").toString();
////        //{"flag":true,"code":0,"desc":"成功","data":{"taskNo":"25082638726633","status":"CREATE","downUrl":null,"tip":"任务创建成功","pageList":null}}
////        System.out.println(result);
////
////        while (true) {
////            String statusResult = client.status(APPID, ts, signature, taskNo);
////            System.out.println(statusResult);
////            JSONObject statusObject = JSONObject.parseObject(statusResult);
////            if ((Integer) statusObject.get("code") != 0) {
////                System.out.println(jsonObject.get("desc"));
////            } else {
////                JSONObject statusData = (JSONObject) statusObject.get("data");
////
////                String status = statusData.get("status").toString();
////                if ("FINISH".equals(status)) {
////                    //{"flag":true,"code":0,"desc":"成功","data":{"taskNo":"25082638726633","status":"FINISH","downUrl":"http://bjcdn.openstorage.cn/oxxxx","tip":"已完成","pageList":[]}}
////                    break;
////                }
////            }
////
////            //等待10s再查一次状态
////            sleep(10000);
//
//
//        String host = "https://pdfanalyze.market.alicloudapi.com";
//        //请求解析、状态查询使用的path："/api/predict/ocr_pdf_parse"；图片下载："/api/predict/ocr_pdf_parse/binary";
//        String path = "/api/predict/ocr_pdf_parse";
//        String method = "POST";
//        String appcode = "你自己的AppCode";
//        Map<String, String> headers = new HashMap<String, String>();
//        //最后在header中的格式(中间是英文空格)为Authorization:APPCODE 83359fd73fe94948385f570e3c139105
//        headers.put("Authorization", "APPCODE " + appcode);
//        //根据API的要求，定义相对应的Content-Type
//        headers.put("Content-Type", "application/json; charset=UTF-8");
//        Map<String, String> querys = new HashMap<String, String>();
//        String bodys = "#请求解析{\"pdf\":\"pdf文件url\",\"async\":false,#是否异步解析，异步解析立即返回request_id\"callback_url\":\"http://...\"#解析完毕之后，调用callback_url}#状态查询{\"request_id\":\"\"}#获取解析结果中的图片文件{\"request_id\":\"\",\"file_path\":\"\"}";
//
//
//        try {
//            /**
//             * 重要提示如下:
//             * HttpUtils请从
//             * https://github.com/aliyun/api-gateway-demo-sign-java/blob/master/src/main/java/com/aliyun/api/gateway/demo/util/HttpUtils.java
//             * 下载
//             *
//             * 相应的依赖请参照
//             * https://github.com/aliyun/api-gateway-demo-sign-java/blob/master/pom.xml
//             */
//            HttpResponse response = HttpUtils.doPost(host, path, method, headers, querys, bodys);
//            System.out.println(response.toString());
//            //获取response的body
//            //System.out.println(EntityUtils.toString(response.getEntity()));
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        }
//
//    }
//
//}
