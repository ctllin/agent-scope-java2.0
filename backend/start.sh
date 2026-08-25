#!/bin/bash
cd /home/gitee/ai/agent-scope-java2/backend

export OPENAI_API_KEY="4aa2c5736fe74af4b7811cb82b111a0f.PQmDGxzrkGkDdEf6"
export OPENAI_BASE_URL="https://open.bigmodel.cn/api/paas/v4"

# 使用setsid创建新会话，确保进程不受父进程影响
exec setsid mvn spring-boot:run > /tmp/app.log 2>&1 &
echo $!
