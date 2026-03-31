package com.heima.wemedia.service;

import java.util.concurrent.CompletableFuture;

public interface WmNewsAutoScanService {

    /**
     * 自媒体文章审核
     *
     * @param id 自媒体文章id
     * @return
     */
    public CompletableFuture<Void> autoScanWmNews(Integer id);
}
