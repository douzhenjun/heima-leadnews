package com.heima.wemedia.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.apis.article.IArticleClient;
import com.heima.common.aliyun.GreenImageScan;
import com.heima.common.aliyun.GreenTextScan;
import com.heima.common.tess4j.Tess4jClient;
import com.heima.file.service.FileStorageService;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.pojos.WmChannel;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.pojos.WmSensitive;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.utils.common.SensitiveWordUtil;
import com.heima.wemedia.mapper.WmChannelMapper;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.mapper.WmSensitiveMapper;
import com.heima.wemedia.mapper.WmUserMapper;
import com.heima.wemedia.service.WmNewsAutoScanService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;


@Service
@Slf4j
@Transactional
public class WmNewsAutoScanServiceImpl implements WmNewsAutoScanService {

    @Autowired
    private WmNewsMapper wmNewsMapper;

    /**
     * 自媒体文章审核
     *
     * @param id 自媒体文章id
     * @return
     */
    @Override
    @Async  //标明当前方法是一个异步方法
    public CompletableFuture<Void> autoScanWmNews(Integer id) {

//        int a = 1/0;
        // 创建一个空的 CompletableFuture，用于承载结果
        CompletableFuture<Void> result = new CompletableFuture<>();

        try {
            //1.查询自媒体文章
            WmNews wmNews = wmNewsMapper.selectById(id);
            if (wmNews == null) {
                throw new RuntimeException("WmNewsAutoScanServiceImpl-文章不存在");
            }

            if (wmNews.getStatus().equals(WmNews.Status.SUBMIT.getCode())) {
                //从内容中提取纯文本内容和图片
                Map<String, Object> textAndImages = handleTextAndImages(wmNews);

                //自管理的敏感词过滤
                boolean isSensitive = handleSensitiveScan((String) textAndImages.get("content"), wmNews);
                if (!isSensitive) return CompletableFuture.completedFuture(null);

                //2.审核文本内容  阿里云接口
                boolean isTextScan = handleTextScan((String) textAndImages.get("content"), wmNews);
                if (!isTextScan) return CompletableFuture.completedFuture(null);

                //3.审核图片  阿里云接口
                boolean isImageScan = handleImageScan((List<String>) textAndImages.get("images"), wmNews);
                if (!isImageScan) return CompletableFuture.completedFuture(null);

                //4.审核成功，保存app端的相关的文章数据
                ResponseResult responseResult = saveAppArticle(wmNews);
                if (!responseResult.getCode().equals(200)) {
                    throw new RuntimeException("WmNewsAutoScanServiceImpl-文章审核，保存app端相关文章数据失败");
                }
                //回填article_id
                wmNews.setArticleId((Long) responseResult.getData());
                updateWmNews(wmNews, (short) 9, "审核成功");
            }
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            // 关键：打印异常并完成 Future
            log.error("异步方法异常", e);
            result.completeExceptionally(e);
        }
        return result;
    }

    @Autowired
    private WmSensitiveMapper wmSensitiveMapper;

    /**
     * 自管理的敏感词审核
     *
     * @param content
     * @param wmNews
     * @return
     */
    private boolean handleSensitiveScan(String content, WmNews wmNews) {

        boolean flag = true;

        //获取所有的敏感词
        List<WmSensitive> wmSensitives = wmSensitiveMapper.selectList(Wrappers.<WmSensitive>lambdaQuery().select(WmSensitive::getSensitives));
        List<String> sensitiveList = wmSensitives.stream().map(WmSensitive::getSensitives).collect(Collectors.toList());

        //初始化敏感词库
        SensitiveWordUtil.initMap(sensitiveList);

        //查看文章中是否包含敏感词
        Map<String, Integer> map = SensitiveWordUtil.matchWords(content);
        if (!map.isEmpty()) {
            updateWmNews(wmNews, (short) 2, "当前文章中存在违规内容" + map);
            flag = false;
        }

        return flag;
    }

    @Autowired
    private IArticleClient articleClient;

    @Autowired
    private WmChannelMapper wmChannelMapper;

    @Autowired
    private WmUserMapper wmUserMapper;

    /**
     * 保存 app 端相关的文章数据
     *
     * @param wmNews
     */
    private ResponseResult saveAppArticle(WmNews wmNews) {
        ArticleDto dto = new ArticleDto();
        BeanUtils.copyProperties(wmNews, dto);
        dto.setLayout(wmNews.getType());

        //频道
        WmChannel wmChannel = wmChannelMapper.selectById(wmNews.getChannelId());
        if (wmChannel != null) {
            dto.setChannelName(wmChannel.getName());
        }

        //作者
        dto.setAuthorId(wmNews.getUserId().longValue());
        WmUser wmUser = wmUserMapper.selectById(wmNews.getUserId());
        if (wmUser != null) {
            dto.setAuthorName(wmUser.getName());
        }

        //设置文章 id
        if (wmNews.getArticleId() != null) {
            dto.setId(wmNews.getArticleId());
        }
        dto.setCreatedTime(new Date());

        ResponseResult responseResult = articleClient.saveArticle(dto);
        return responseResult;
    }


    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private GreenImageScan greenImageScan;

    @Autowired
    private Tess4jClient tess4jClient;

    /**
     * 审核图片
     *
     * @param images
     * @param wmNews
     * @return
     */
    private boolean handleImageScan(List<String> images, WmNews wmNews) {

        boolean flag = true;

        if (images == null || images.isEmpty()) {
            return flag;
        }

        //下载图片 minIO
        //图片去重
        images = images.stream().distinct().collect(Collectors.toList());

        List<byte[]> imageList = new ArrayList<>();

        try {
            for (String image : images) {
                byte[] bytes = fileStorageService.downLoadFile(image);

                //byte[] 转换为bufferedImage
                ByteArrayInputStream in = new ByteArrayInputStream(bytes);
                BufferedImage bufferedImage = ImageIO.read(in);

                //图片识别
                String result = tess4jClient.doOCR(bufferedImage);
                //过滤文字
                boolean isSensitive = handleSensitiveScan(result, wmNews);
                if (!isSensitive) {
                    return isSensitive;
                }
                imageList.add(bytes);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        //审核图片
        try {
            Map map = greenImageScan.imageScan(images);
            System.out.println(JSONArray.toJSONString(map));
            if (map != null) {
                // 从嵌套结构中获取 suggestion: body.Data.Results[0].SubResults[].Suggestion
                String suggestion = getImageSuggestionFromResult(map);

                //审核失败
                if ("block".equals(suggestion)) {
                    flag = false;
                    updateWmNews(wmNews, (short) 2, "当前文章中存在违规内容");
                }

                //不确定信息  需要人工审核
                if ("review".equals(suggestion)) {
                    flag = false;
                    updateWmNews(wmNews, (short) 3, "当前文章中存在不确定内容");
                }
            }

        } catch (Exception e) {
            flag = false;
            e.printStackTrace();
        }
        return flag;
    }

    @Autowired
    private GreenTextScan greenTextScan;

    /**
     * 审核纯文本内容
     *
     * @param content
     * @param wmNews
     * @return
     */
    private boolean handleTextScan(String content, WmNews wmNews) {

        boolean flag = true;

        try {
            Map map = greenTextScan.textScan((wmNews.getTitle() + "-" + content));
            if (map != null) {
                // 从嵌套结构中获取 suggestion: body.Data.Elements[0].Results[0].Suggestion
                String suggestion = getSuggestionFromResult(map);

                //审核失败
                if ("block".equals(suggestion)) {
                    flag = false;
                    updateWmNews(wmNews, (short) 2, "当前文章中存在违规内容");
                }

                //不确定信息  需要人工审核
                if ("review".equals(suggestion)) {
                    flag = false;
                    updateWmNews(wmNews, (short) 3, "当前文章中存在不确定内容");
                }
            }
        } catch (Exception e) {
            flag = false;
            e.printStackTrace();
        }

        return flag;

    }

    /**
     * 从阿里云返回结果中提取 suggestion（文本审核）
     * 结构：body.Data.Elements[0].Results[0].Suggestion
     */
    private String getSuggestionFromResult(Map map) {
        try {
            if (map == null || !map.containsKey("body")) {
                return null;
            }

            Map body = (Map) map.get("body");
            if (body == null || !body.containsKey("Data")) {
                return null;
            }

            Map data = (Map) body.get("Data");
            if (data == null || !data.containsKey("Elements")) {
                return null;
            }

            List elements = (List) data.get("Elements");
            if (elements == null || elements.isEmpty()) {
                return null;
            }

            Map element = (Map) elements.get(0);
            if (element == null || !element.containsKey("Results")) {
                return null;
            }

            List results = (List) element.get("Results");
            if (results == null || results.isEmpty()) {
                return null;
            }

            Map result = (Map) results.get(0);
            if (result == null || !result.containsKey("Suggestion")) {
                return null;
            }

            return result.get("Suggestion").toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 从阿里云返回结果中提取 suggestion（图片审核）
     * 结构：body.Data.Results[0].SubResults[].Suggestion
     * 需要检查所有子结果，只要有一个 block 或 review 就返回对应值
     */
    private String getImageSuggestionFromResult(Map map) {
        try {
            if (map == null || !map.containsKey("body")) {
                return null;
            }

            Map body = (Map) map.get("body");
            if (body == null || !body.containsKey("Data")) {
                return null;
            }

            Map data = (Map) body.get("Data");
            if (data == null || !data.containsKey("Results")) {
                return null;
            }

            List results = (List) data.get("Results");
            if (results == null || results.isEmpty()) {
                return null;
            }

            Map result = (Map) results.get(0);
            if (result == null || !result.containsKey("SubResults")) {
                return null;
            }

            List subResults = (List) result.get("SubResults");
            if (subResults == null || subResults.isEmpty()) {
                return null;
            }

            // 遍历所有子结果，检查是否有违规或需审核的情况
            // 优先级：block > review > pass
            boolean hasReview = false;
            for (Object subResultObj : subResults) {
                Map subResult = (Map) subResultObj;
                if (subResult == null || !subResult.containsKey("Suggestion")) {
                    continue;
                }

                String suggestion = subResult.get("Suggestion").toString();

                // 如果发现 block，直接返回
                if ("block".equals(suggestion)) {
                    return "block";
                }

                // 如果发现 review，先标记，继续检查是否有 block
                if ("review".equals(suggestion)) {
                    hasReview = true;
                }
            }

            // 如果没有 block，但有 review，返回 review
            if (hasReview) {
                return "review";
            }

            // 否则返回 pass
            return "pass";
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 修改文章内容
     *
     * @param wmNews
     * @param status
     * @param reason
     */
    private void updateWmNews(WmNews wmNews, short status, String reason) {
        wmNews.setStatus(status);
        wmNews.setReason(reason);
        wmNewsMapper.updateById(wmNews);
    }

    /**
     * 1。从自媒体文章的内容中提取文本和图片
     * 2.提取文章的封面图片
     *
     * @param wmNews
     * @return
     */
    private Map<String, Object> handleTextAndImages(WmNews wmNews) {

        //存储纯文本内容
        StringBuilder stringBuilder = new StringBuilder();

        List<String> images = new ArrayList<>();

        //1。从自媒体文章的内容中提取文本和图片
        if (StringUtils.isNotBlank(wmNews.getContent())) {
            List<Map> maps = JSONArray.parseArray(wmNews.getContent(), Map.class);
            for (Map map : maps) {
                if (map.get("type").equals("text")) {
                    stringBuilder.append(map.get("value"));
                }

                if (map.get("type").equals("image")) {
                    images.add((String) map.get("value"));
                }
            }
        }
        //2.提取文章的封面图片
        if (StringUtils.isNotBlank(wmNews.getImages())) {
            String[] split = wmNews.getImages().split(",");
            images.addAll(Arrays.asList(split));
        }

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("content", stringBuilder.toString());
        resultMap.put("images", images);
        return resultMap;

    }
}
