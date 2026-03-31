package com.heima.wemedia.service;

import com.alibaba.fastjson.JSONArray;
import com.heima.file.service.FileStorageService;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.utils.common.Tess4jUtils;
import com.heima.wemedia.WemediaApplication;
import com.heima.wemedia.mapper.WmNewsMapper;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


@SpringBootTest(classes = WemediaApplication.class)
@RunWith(SpringRunner.class)
public class WmNewsAutoScanServiceTest {

    @Autowired
    private WmNewsAutoScanService wmNewsAutoScanService;

    @Autowired
    private WmNewsMapper wmNewsMapper;

    @Test
    public void autoScanWmNewsAsync() throws ExecutionException, InterruptedException, TimeoutException {
        try {
            wmNewsAutoScanService.autoScanWmNews(6667).get(60, TimeUnit.SECONDS);
            // 成功后再查库验证
            WmNews news = wmNewsMapper.selectById(6667);
            Assert.assertEquals(9, (short) news.getStatus());
        } catch (ExecutionException e) {
            // 异步方法内部抛异常会包装在这里
            Assert.fail("异步任务执行失败: " + e.getCause().getMessage());
        } catch (TimeoutException e) {
            Assert.fail("异步任务超时（10秒未完成）");
        }
    }

    @Test
    public void autoScanWmNewsSync() throws ExecutionException, InterruptedException, TimeoutException {
        CompletableFuture<Void> future = wmNewsAutoScanService.autoScanWmNews(6667);
        // 到这里，异步任务肯定已完成，验证数据库
        WmNews news = wmNewsMapper.selectById(6667);
        Assert.assertEquals(9, (short) news.getStatus());
    }

    @Autowired
    private FileStorageService fileStorageService;

    @SneakyThrows
    @Test
    public void testTess4j() {
        //下载文件
        byte[] bytes = fileStorageService.downLoadFile("http://192.168.200.130:9000/leadnews/2021/05/21/8bde311fa21448b18a8ca2378610b16d.png");
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        BufferedImage imageFile = ImageIO.read(in);
        //识别文件
        String result = Tess4jUtils.doOCR(imageFile);
        System.out.println("识别内容：------------------");
        System.out.println(result);
    }


    @Test
    public void testJsonArray() {
        String testjson = "[{\"type\":\"text\",\"value\":\"Kafka文件的存储机制Kafka文件的存储机制Kafka文件的存储机制Kafka文件的存储机制Kafka文件的存储机制Kafka文件的存储机制Kafka文件的存储机制Kafka文件的存储机制Kafka文件的存储机制Kafka文件的存储机制\"},{\"type\":\"image\",\"value\":\"http://192.168.200.130:9000/leadnews/2021/4/20210418/4a498d9cf3614570ac0cb2da3e51c164.jpg\"},{\"type\":\"text\",\"value\":\"请在这里输入正文\"}]";
        List<Map> maps = JSONArray.parseArray(testjson, Map.class);
        System.out.println(maps);
    }
}