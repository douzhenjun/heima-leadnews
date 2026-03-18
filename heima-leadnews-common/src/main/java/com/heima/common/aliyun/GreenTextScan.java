package com.heima.common.aliyun;

import com.aliyun.imageaudit20191230.models.ScanTextRequest;
import com.aliyun.imageaudit20191230.models.ScanTextResponse;
import com.aliyun.tea.TeaException;
import com.aliyun.tea.TeaModel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.*;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "aliyun")
public class GreenTextScan {

    private String accessKeyId;
    private String secret;
//    private String scenes;

//    public Map greeTextScan(String content) throws Exception {
//        System.out.println(accessKeyId);
//        IClientProfile profile = DefaultProfile
//                .getProfile("cn-shanghai", accessKeyId, secret);
//        DefaultProfile.addEndpoint("cn-shanghai", "cn-shanghai", "Green", "green.cn-shanghai.aliyuncs.com");
//        IAcsClient client = new DefaultAcsClient(profile);
//        TextScanRequest textScanRequest = new TextScanRequest();
//        textScanRequest.setAcceptFormat(FormatType.JSON); // 指定api返回格式
//        textScanRequest.setHttpContentType(FormatType.JSON);
//        textScanRequest.setMethod(com.aliyuncs.http.MethodType.POST); // 指定请求方法
//        textScanRequest.setEncoding("UTF-8");
//        textScanRequest.setRegionId("cn-shanghai");
//        List<Map<String, Object>> tasks = new ArrayList<Map<String, Object>>();
//        Map<String, Object> task1 = new LinkedHashMap<String, Object>();
//        task1.put("dataId", UUID.randomUUID().toString());
//        /**
//         * 待检测的文本，长度不超过10000个字符
//         */
//        task1.put("content", content);
//        tasks.add(task1);
//        JSONObject data = new JSONObject();
//
//        /**
//         * 检测场景，文本垃圾检测传递：antispam
//         **/
//        data.put("scenes", Arrays.asList("antispam"));
//        data.put("tasks", tasks);
//        System.out.println(JSON.toJSONString(data, true));
//        textScanRequest.setHttpContent(data.toJSONString().getBytes("UTF-8"), "UTF-8", FormatType.JSON);
//        // 请务必设置超时时间
//        textScanRequest.setConnectTimeout(3000);
//        textScanRequest.setReadTimeout(6000);
//
//        Map<String, String> resultMap = new HashMap<>();
//        try {
//            HttpResponse httpResponse = client.doAction(textScanRequest);
//            if (httpResponse.isSuccess()) {
//                JSONObject scrResponse = JSON.parseObject(new String(httpResponse.getHttpContent(), "UTF-8"));
//                System.out.println(JSON.toJSONString(scrResponse, true));
//                if (200 == scrResponse.getInteger("code")) {
//                    JSONArray taskResults = scrResponse.getJSONArray("data");
//                    for (Object taskResult : taskResults) {
//                        if (200 == ((JSONObject) taskResult).getInteger("code")) {
//                            JSONArray sceneResults = ((JSONObject) taskResult).getJSONArray("results");
//                            for (Object sceneResult : sceneResults) {
//                                String scene = ((JSONObject) sceneResult).getString("scene");
//                                String label = ((JSONObject) sceneResult).getString("label");
//                                String suggestion = ((JSONObject) sceneResult).getString("suggestion");
//                                System.out.println("suggestion = [" + label + "]");
//                                if (!suggestion.equals("pass")) {
//                                    resultMap.put("suggestion", suggestion);
//                                    resultMap.put("label", label);
//                                    return resultMap;
//                                }
//
//                            }
//                        } else {
//                            return null;
//                        }
//                    }
//                    resultMap.put("suggestion", "pass");
//                    return resultMap;
//                } else {
//                    return null;
//                }
//            } else {
//                return null;
//            }
//        } catch (ServerException e) {
//            e.printStackTrace();
//        } catch (ClientException e) {
//            e.printStackTrace();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return null;
//    }

    public static com.aliyun.imageaudit20191230.Client createClient(String accessKeyId, String accessKeySecret) throws Exception {
        /*
          初始化配置对象com.aliyun.teaopenapi.models.Config
          Config对象存放 AccessKeyId、AccessKeySecret、endpoint等配置
         */
        com.aliyun.teaopenapi.models.Config config = new com.aliyun.teaopenapi.models.Config()
                .setAccessKeyId(accessKeyId)
                .setAccessKeySecret(accessKeySecret);
        // 访问的域名
        config.endpoint = "imageaudit.cn-shanghai.aliyuncs.com";
        return new com.aliyun.imageaudit20191230.Client(config);
    }

    public Map<String, Object> textScan(String content) throws Exception {

        Map<String, Object> resultMap = new HashMap<>();
        // 创建AccessKey ID和AccessKey Secret，请参考https://help.aliyun.com/document_detail/175144.html。
        // 如果您使用的是RAM用户的AccessKey，还需要为RAM用户授予权限AliyunVIAPIFullAccess，请参考https://help.aliyun.com/document_detail/145025.html。
        // 从环境变量读取配置的AccessKey ID和AccessKey Secret。运行代码示例前必须先配置环境变量。
        com.aliyun.imageaudit20191230.Client client = GreenTextScan.createClient(accessKeyId, secret);
        ScanTextRequest.ScanTextRequestTasks tasks = new ScanTextRequest.ScanTextRequestTasks().setContent(content);
        ScanTextRequest.ScanTextRequestLabels spam = new ScanTextRequest.ScanTextRequestLabels().setLabel("spam");
        ScanTextRequest.ScanTextRequestLabels politics = new ScanTextRequest.ScanTextRequestLabels().setLabel("politics");
        ScanTextRequest.ScanTextRequestLabels abuse = new ScanTextRequest.ScanTextRequestLabels().setLabel("abuse");
        ScanTextRequest.ScanTextRequestLabels terrorism = new ScanTextRequest.ScanTextRequestLabels().setLabel("terrorism");
        ScanTextRequest.ScanTextRequestLabels porn = new ScanTextRequest.ScanTextRequestLabels().setLabel("porn");
        ScanTextRequest.ScanTextRequestLabels flood = new ScanTextRequest.ScanTextRequestLabels().setLabel("flood");
        ScanTextRequest.ScanTextRequestLabels contraband = new ScanTextRequest.ScanTextRequestLabels().setLabel("contraband");
        ScanTextRequest.ScanTextRequestLabels ad = new ScanTextRequest.ScanTextRequestLabels().setLabel("ad");
        com.aliyun.imageaudit20191230.models.ScanTextRequest scanTextRequest = new com.aliyun.imageaudit20191230.models.ScanTextRequest()
                .setLabels(java.util.Arrays.asList(
                        spam, politics, abuse, terrorism, porn, flood, contraband, ad
                ))
                .setTasks(java.util.Arrays.asList(
                        tasks
                ));
        com.aliyun.teautil.models.RuntimeOptions runtime = new com.aliyun.teautil.models.RuntimeOptions();
        try {
            // 复制代码运行请自行打印API的返回值
            ScanTextResponse response = client.scanTextWithOptions(scanTextRequest, runtime);
            return TeaModel.buildMap(response);
//            System.out.println(com.aliyun.teautil.Common.toJSONString(TeaModel.buildMap(response)));
        } catch (TeaException error) {
            // 获取整体报错信息
//            System.out.println(com.aliyun.teautil.Common.toJSONString(error));
//            // 获取单个字段
//            System.out.println(error.getCode());
            resultMap.put("code", error.getCode());
            resultMap.put("message", error.getMessage());
            resultMap.put("data", null);
            return resultMap;
        } catch (Exception e) {
            resultMap.put("code", -1);
            resultMap.put("message", e.getMessage());
            return resultMap;
        }
    }
}