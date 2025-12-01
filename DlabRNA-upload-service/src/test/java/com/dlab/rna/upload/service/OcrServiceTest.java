//package com.dlab.rna.upload.service;
//
//import lombok.extern.slf4j.Slf4j;
//import org.junit.jupiter.api.Assumptions;
//import org.junit.jupiter.api.Test;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.DynamicPropertyRegistry;
//import org.springframework.test.context.DynamicPropertySource;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.util.Base64;
//import java.util.Optional;
//
//@SpringBootTest
//@Slf4j
//public class OcrServiceTest {
//    @Autowired
//    private OcrService ocrService;
//
//    @DynamicPropertySource
//    static void props(DynamicPropertyRegistry r) {
//        r.add("ocr.baidu.endpoint", () -> Optional.ofNullable(System.getenv("OCR_BAIDU_ENDPOINT")).orElse(System.getProperty("OCR_BAIDU_ENDPOINT", "https://aip.baidubce.com/rest/2.0/ocr/v1/general_basic")));
//        r.add("ocr.baidu.access-token", () -> Optional.ofNullable(System.getenv("OCR_BAIDU_ACCESS_TOKEN")).orElse(System.getProperty("OCR_BAIDU_ACCESS_TOKEN", "24.d784a1fcd8f636dbeeba363cd05b1ccd.2592000.1766574668.282335-120995884")));
//        r.add("ocr.baidu.language-type", () -> Optional.ofNullable(System.getenv("OCR_BAIDU_LANGUAGE_TYPE")).orElse(System.getProperty("OCR_BAIDU_LANGUAGE_TYPE", "CHN_ENG")));
//        r.add("ocr.baidu.detect-direction", () -> Optional.ofNullable(System.getenv("OCR_BAIDU_DETECT_DIRECTION")).orElse(System.getProperty("OCR_BAIDU_DETECT_DIRECTION", "false")));
//        r.add("ocr.baidu.detect-language", () -> Optional.ofNullable(System.getenv("OCR_BAIDU_DETECT_LANGUAGE")).orElse(System.getProperty("OCR_BAIDU_DETECT_LANGUAGE", "false")));
//        r.add("ocr.baidu.paragraph", () -> Optional.ofNullable(System.getenv("OCR_BAIDU_PARAGRAPH")).orElse(System.getProperty("OCR_BAIDU_PARAGRAPH", "false")));
//        r.add("ocr.baidu.probability", () -> Optional.ofNullable(System.getenv("OCR_BAIDU_PROBABILITY")).orElse(System.getProperty("OCR_BAIDU_PROBABILITY", "false")));
//    }
//
//    @Test
//    void ocrByUrlPdfOrImage() throws Exception {
//        String url = Optional.ofNullable(System.getenv("OCR_TEST_URL")).orElse("http://127.0.0.1:9000/browser/uploads/RBind.pdf");
//        Assumptions.assumeTrue(url != null && !url.isEmpty());
//        String typeStr = Optional.ofNullable(System.getenv("OCR_TEST_TYPE")).orElse(System.getProperty("OCR_TEST_TYPE", "1"));
//        Integer fileType = Integer.valueOf(typeStr);
//        OcrService.ResultPages res = ocrService.ocrByUrl(url, fileType);
//        log.info("log" + res.toString());
//        Assumptions.assumeTrue(res.ok && res.pages != null && !res.pages.isEmpty());
//        String first = res.pages.get(0);
//        System.out.println(first);
//    }
//
//    @Test
//    void ocrByBase64Image() throws Exception {
//        String path = Optional.ofNullable(System.getenv("OCR_TEST_IMAGE_PATH")).orElse(System.getProperty("OCR_TEST_IMAGE_PATH"));
//        Assumptions.assumeTrue(path != null && !path.isEmpty());
//        byte[] bytes = Files.readAllBytes(Path.of(path));
//        String b64 = Base64.getEncoder().encodeToString(bytes);
//        OcrService.Result res = ocrService.ocrImageBase64(b64);
//        Assumptions.assumeTrue(res.ok && res.text != null && !res.text.isEmpty());
//        System.out.println(res.text);
//    }
//}