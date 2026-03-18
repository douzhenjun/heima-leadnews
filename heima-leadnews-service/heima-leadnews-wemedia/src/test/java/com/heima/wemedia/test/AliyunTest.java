package com.heima.wemedia.test;


import com.alibaba.fastjson.JSON;
import com.heima.common.aliyun.GreenImageScan;
import com.heima.common.aliyun.GreenTextScan;
import com.heima.file.service.FileStorageService;
import com.heima.wemedia.WemediaApplication;
import org.json.JSONArray;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SpringBootTest(classes = WemediaApplication.class)
@RunWith(SpringRunner.class)
public class AliyunTest {

    @Autowired
    private GreenTextScan greenTextScan;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private GreenImageScan greenImageScan;

    /**
     * 测试文本内容审核
     */
    @Test
    public void testScanText() throws Exception {
        Map<String, Object> map = greenTextScan.textScan("我爱祖国");
        System.out.println(JSON.toJSONString(map));

    }

    /**
     * 测试图片审核
     */
    @Test
    public void testScanImage() throws Exception {
        //正常图片
        List<String> imageList = new ArrayList<>();
        String url = "http://192.168.88.130:9000/leadnews/2021/04/26/a73f5b60c0d84c32bfe175055aaaac40.jpg";
        imageList.add(url);

        Map<String, Object> resultMap = greenImageScan.imageScan(imageList);
        System.out.println(JSON.toJSONString(resultMap));
    }

    /**
     * 测试图片审核
     */
    @Test
    public void testScanImage2() throws Exception {
        //违规图片
        List<String> imageList = new ArrayList<>();
        String url = "http://192.168.88.130:9000/leadnews/2026/03/18/sex.jpg";
        imageList.add(url);

        greenImageScan.imageScan(imageList);
    }
}
