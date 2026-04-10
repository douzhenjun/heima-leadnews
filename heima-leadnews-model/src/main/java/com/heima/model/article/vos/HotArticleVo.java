package com.heima.model.article.vos;

import com.heima.model.article.pojos.ApArticle;
import lombok.Data;

/**
 * 热点文章前端模板类
 */
@Data
public class HotArticleVo extends ApArticle {

    /**
     * 文章分值
     */
    private Integer score;
}
