package com.heima.minio;

import com.heima.file.service.FileStorageService;
import com.heima.file.service.impl.MinIOFileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.FileInputStream;


@SpringBootApplication
public class MinIOApplication {

    @Autowired
    private FileStorageService fileStorageService;

    public static void main(String[] args) {
        // 检查classpath中是否有FileStorageService
        try {
            Class.forName("com.heima.file.service.FileStorageService");
            System.out.println("✅ FileStorageService 存在");
        } catch (ClassNotFoundException e) {
            System.out.println("❌ FileStorageService 不存在");
        }
        // 检查classpath中是否有MinIOConfig
        try {
            Class.forName("com.heima.file.config.MinIOConfig");
            System.out.println("✅ MinIOConfig 存在");
        } catch (ClassNotFoundException e) {
            System.out.println("❌ MinIOConfig 不存在");
        }
        ConfigurableApplicationContext context = SpringApplication.run(MinIOApplication.class,args);

        // 阶段2：run()之后 - 可以检查Bean是否存在
        checkBeanExistence(context, "minIOConfig");  // 注意：方法名首字母小写
        checkBeanExistence(context, "fileStorageService");

//        FileInputStream fileInputStream = new FileInputStream("D:\\index.html");
//        String path = fileStorageService.uploadHtmlFile("", "index.html", fileInputStream);
    }


    // 检查Bean是否在Spring容器中
    private static void checkBeanExistence(ConfigurableApplicationContext context, String beanName) {
        if (context.containsBean(beanName)) {
            System.out.println("✅ [Bean存在] " + beanName + " 已注册到Spring容器");
            // 甚至可以获取实例验证
            Object bean = context.getBean(beanName);
            System.out.println("   实例类型: " + bean.getClass().getName());
        } else {
            System.out.println("❌ [Bean不存在] " + beanName + " 未注册（条件可能未满足）");
        }
    }
}
