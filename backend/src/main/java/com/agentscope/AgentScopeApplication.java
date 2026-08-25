package com.agentscope;

import org.dromara.x.file.storage.spring.EnableFileStorage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableFileStorage
@EnableAsync
@EnableScheduling
public class AgentScopeApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentScopeApplication.class, args);
    }
}
