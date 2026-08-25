package com.agentscope.config;

import com.agentscope.common.enums.UserStatus;
import com.agentscope.model.entity.User;
import com.agentscope.repository.UserRepository;
import com.agentscope.util.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 数据初始化配置
 * <p>
 * 在应用启动时初始化root用户
 * </p>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitConfig {

    private final UserRepository userRepository;
    private final QuickLoginConfig quickLoginConfig;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            // 检查root用户是否存在
            if (!userRepository.existsByUsername(quickLoginConfig.getUsername())) {
                // 创建root用户
                User rootUser = User.builder()
                        .username(quickLoginConfig.getUsername())
                        .password(PasswordEncoder.encode(quickLoginConfig.getPassword()))
                        .nickname("管理员")
                        .email("admin@agentscope.com")
                        .status(UserStatus.ENABLED)
                        .root(true)
                        .build();

                userRepository.save(rootUser);
                log.info("已创建root用户: {}", quickLoginConfig.getUsername());
            } else {
                log.info("root用户已存在，跳过初始化");
            }
        };
    }
}
