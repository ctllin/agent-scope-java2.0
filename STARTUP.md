# Agent-Scope Java 2.0 启动说明

## 前置条件

### 1. 安装MongoDB
```bash
# 使用Docker（推荐）
docker run -d -p 27017:27017 --name mongodb mongo:latest

# 或者安装MongoDB Community Edition
# 参考: https://www.mongodb.com/docs/manual/installation/
```

### 2. 配置GLM API密钥
```bash
# 设置环境变量
export GLM_API_KEY=your-glm-api-key

# 或者在application.yml中配置
```

## 启动应用

### 方式一：使用启动脚本
```bash
cd /home/gitee/ai/agent-scope-java2
./start.sh
```

### 方式二：手动启动
```bash
# 启动后端
cd backend
mvn spring-boot:run

# 启动前端（新终端）
cd frontend
npm install
npm run dev
```

## 访问应用

- 前端: http://localhost:3000
- 后端API: http://localhost:8080
- API文档: http://localhost:8080/swagger-ui.html

## 快捷登录

在登录页面双击空白区域可使用快捷登录（root账号）

## 常见问题

### 1. MongoDB连接失败
```
MongoSocketOpenException: Exception opening socket
```
**解决方案**: 确保MongoDB服务已启动

### 2. GLM模型配置错误
```
Cannot resolve model: "openai:null"
```
**解决方案**: 设置GLM_API_KEY环境变量

### 3. 端口被占用
```
WebServerException: Port already in use
```
**解决方案**: 
```bash
# 查找占用端口的进程
lsof -i :8080
# 终止进程
kill -9 <PID>
```
