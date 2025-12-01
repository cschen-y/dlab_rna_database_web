package com.dlab.rna.upload.service;

import okhttp3.*;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ApiClient {

    private static final String MEDIA_TYPE_JSON = "application/json; charset=utf-8";
    private final static OkHttpClient client = new OkHttpClient().newBuilder()
            .connectionPool(new ConnectionPool(100, 5, TimeUnit.MINUTES))
            .readTimeout(60 * 10, TimeUnit.SECONDS)
            .build();

    private static final String ERROR_MESSAGE = "Unexpected code: ";
    private final String baseUrl;

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * 文件上传
     */
    public String startWithFile(String appId, String timestamp, String signature, File file, String exportFormat) throws IOException {
        validateParameters(appId, timestamp, signature);

        MultipartBody.Builder body = new MultipartBody.Builder().setType(MultipartBody.FORM);
        body.addFormDataPart("file", file.getName(), RequestBody.create(file, MediaType.parse("application/pdf")));
        body.addFormDataPart("fileName", file.getName());
        body.addFormDataPart("exportFormat", exportFormat);
        RequestBody requestBody = body.build();
        Request request = buildPostRequest(baseUrl + "/v1/pdfOcr/start", appId, timestamp, signature, requestBody);
        return executeRequest(request);
    }

    public String startWithUrl(String appId, String timestamp, String signature, String pdfUrl, String exportFormat) throws IOException {
        validateParameters(appId, timestamp, signature);

        MultipartBody.Builder body = new MultipartBody.Builder().setType(MultipartBody.FORM);
        body.addFormDataPart("pdfUrl", pdfUrl);
        body.addFormDataPart("exportFormat", exportFormat);
        RequestBody requestBody = body.build();

        Request request = buildPostRequest(baseUrl + "/v1/pdfOcr/start", appId, timestamp, signature, requestBody);
        return executeRequest(request);
    }

    public String status(String appId, String timestamp, String signature, String taskNo) throws IOException {
        HttpUrl url = HttpUrl.parse(baseUrl + "/v1/pdfOcr/status").newBuilder()
                .addQueryParameter("taskNo", taskNo)
                .build();

        Request request = buildGetRequest(url.toString(), appId, timestamp, signature);
        return executeRequest(request);
    }

    private Request buildPostRequest(String url, String appId, String timestamp, String signature, RequestBody body) {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("appId", appId)
                .addHeader("timestamp", timestamp)
                .addHeader("signature", signature)
                .post(body)
                .build();
        return request;
    }

    private Request buildGetRequest(String url, String appId, String timestamp, String signature) {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("appId", appId)
                .addHeader("timestamp", timestamp)
                .addHeader("signature", signature)
                .get()
                .build();
        return request;
    }

    private String executeRequest(Request request) throws IOException {
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.out.println(response.body().string());
                throw new IOException(ERROR_MESSAGE + response);
            }
            return response.body().string();
        }
    }

    private void validateParameters(String... params) {
        for (String param : params) {
            if (param == null || param.isEmpty()) {
                throw new IllegalArgumentException("Parameter cannot be null or empty");
            }
        }
    }

}
