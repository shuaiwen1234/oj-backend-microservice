package com.wen.ojbackendjudgeservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@ComponentScan("com.wen")
@EnableScheduling
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@SpringBootApplication
@EnableFeignClients(basePackages = "com.wen.ojbackendserviceclient.service")
@EnableDiscoveryClient
public class OjBackendJudgeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OjBackendJudgeServiceApplication.class, args);
    }

}
