# Agent-Scope Java 2.0

AI 知识库平台：基于 Spring Boot 3 后端 + Vue 3 前端，集成大模型对话（GLM）、RAG 知识库检索（Milvus 向量库）、本地 ONNX 模型推理（中文向量化 / OCR / 离线 TTS），支持文档上传、解析、分块、向量化与全文/语义检索。

## 功能总览

| 模块 | 功能 |
|------|------|
| 用户认证 | JWT 登录、用户/角色/菜单权限管理、快捷登录开关 |
| AI 对话 | 多会话管理、流式输出（SSE）、对话历史持久化（MongoDB） |
| 知识库 | 创建/删除知识库、文档上传（PDF/Word/TXT 等）、文本解析与分块 |
| 向量检索 | 文本向量化 → Milvus 入库 → 语义 Top-K 检索（RAG） |
| OCR 识别 | 基于 RapidOCR 的图片/PDF 页面文字识别 |
| TTS 朗读 | 文档内容朗读，支持 Edge-TTS 在线合成与 sherpa-onnx 本地离线合成 |
| 文档编辑 | 分页浏览、逐行编辑、行合并/删除/移动、页级编辑 |

## 技术栈

### 后端
- **框架**: Spring Boot 3.5.x + Java 21 + Maven
- **AI 编排**: [AgentScope Java](https://github.com/agentscope-ai)（agentscope-harness + OpenAI 兼容模型扩展）
- **数据库**: MongoDB（业务数据）
- **向量库**: Milvus 2.x（milvus-sdk-java 3.x）
- **本地推理**: DJL + ONNX Runtime（Embedding、Tokenizer）
- **OCR**: RapidOCR（ONNX Runtime 推理）
- **TTS**: sherpa-onnx（离线）/ edge-tts（在线命令行）
- **文档解析**: Apache PDFBox（PDF）、Apache POI（Word）
- **认证**: JJWT（JWT）

### 前端
- **框架**: Vue 3 + TypeScript + Vite 5
- **UI**: Element Plus + @element-plus/icons-vue
- **状态/路由**: Pinia、Vue Router 4
- **HTTP**: Axios（统一拦截器）
- **其他**: Sass

### 使用的模型

| 用途 | 模型 | 说明 |
|------|------|------|
| 对话 LLM | GLM `glm-4-flash`（智谱AI） | OpenAI 兼容接口，通过环境变量可换 base-url/model |
| 文本向量化 | `Xenova/bge-small-zh-v1.5` | 本地 ONNX 推理，512 维，归一化输出 |
| OCR | RapidOCR ONNX | 中英文图片文字识别 |
| 离线 TTS | sherpa-onnx `vits-melo-tts-zh_en`、`supertonic-3-int8` | 本地语音合成（中英混读） |
| 在线 TTS | Edge-TTS | 可选，依赖系统安装 `edge-tts` 命令 |

## 环境要求

| 组件 | 版本/要求 | 说明 |
|------|-----------|------|
| JDK | 21+ | 后端编译运行 |
| Maven | 3.8+ | 后端构建 |
| Node.js | 18+（建议 20/22） | 前端构建运行 |
| MongoDB | 4.4+，默认 `127.0.0.1:27017` | 业务数据存储，库名 `test` |
| Milvus | 2.x，默认 `localhost:19530` | 向量检索；不需要时可设 `milvus.enabled=false` |
| GLM API Key | 必需 | 环境变量 `GLM_API_KEY` |
| 本地模型目录 | `/home/software/AI/Xenova/bge-small-zh-v1.5/` | Embedding 模型（tokenizer.json + onnx/model.onnx） |
| 磁盘目录 | `/data/agent-scope/files`、`/data/agent-scope/tts-cache` | 文件存储与 TTS 缓存，需可写 |

> MongoDB / Milvus 均可用 Docker 快速启动：
> ```bash
> docker run -d -p 27017:27017 --name mongodb mongo:latest
> docker run -d -p 19530:19530 --name milvus milvusdb/milvus:latest milvus run standalone
> ```

## 部署与运行

### 1. 配置

后端配置集中在 `backend/src/main/resources/application.yml`，关键项：

```yaml
glm:
  api-key: ${GLM_API_KEY}        # 智谱AI API Key（必填）
  model: glm-4-flash             # 可换其他 OpenAI 兼容模型

embedding:
  tokenizer-uri: file:/home/software/AI/Xenova/bge-small-zh-v1.5/tokenizer.json
  onnx-model-uri: file:/home/software/AI/Xenova/bge-small-zh-v1.5/onnx/model.onnx

milvus:
  enabled: true                  # 不用向量检索时改为 false 即可整体禁用

file.storage.base-path: /data/agent-scope/files

app.quick-login.enabled: true   # 生产环境建议关闭
```

设置环境变量：

```bash
export GLM_API_KEY=你的智谱APIKey          # 必须
export OPENAI_API_KEY=dummy               # AgentScope harness 要求非空即可
```

### 2. 启动后端

```bash
cd backend
mvn compile                # 仅编译
mvn spring-boot:run        # 开发模式启动（8080 端口）

# 或打包运行
mvn clean package
java -jar target/agent-scope-java2-1.0.0-SNAPSHOT.jar
```

也可使用根目录脚本 `./start.sh`（自动检查 MongoDB 连接与 API Key 后启动）。

启动时 `DataInitConfig` 会自动创建 root 用户。

### 3. 启动前端

```bash
cd frontend
npm ci                     # 首次安装（已提交 package-lock.json）
npm run dev                # 开发模式（3000 端口，/api 自动代理到 8080）
```

生产构建：

```bash
npm run build              # vue-tsc 类型检查 + vite 打包到 dist/
```

### 4. 访问

- 前端页面：<http://localhost:3000>
- 后端 API：<http://localhost:8080/api/*>
- 登录：默认账号 `root / 123456`；或在登录页空白处双击触发快捷登录

## 项目结构

```
├── backend/                    # Spring Boot 后端
│   └── src/main/java/com/agentscope/
│       ├── controller/         # Auth/User/Menu/Role/Chat/KnowledgeBase/Tts
│       ├── service/            # 业务层（Embedding/Milvus/Ocr/Tts/FileStorage...）
│       ├── repository/         # MongoDB 数据访问
│       ├── config/             # 配置类（Milvus 条件装配、数据初始化等）
│       └── interceptor/        # JWT、TraceContext 过滤器
├── frontend/                   # Vue 3 前端
│   └── src/
│       ├── api/                # 全部接口封装
│       ├── views/              # login/dashboard/chat/user/knowledge-base
│       ├── components/         # TextViewer/PdfViewer/TtsToolbar 等
│       ├── store/              # Pinia
│       └── router/             # 路由
├── sherpa-onnx/                # 本地 TTS 运行库与模型
├── scripts/                    # 辅助脚本
└── start.sh                    # 一键启动脚本
```

## 常见问题

- **MongoDB 连接失败**：确认服务已启动且地址为 `127.0.0.1:27017`，URI 带 `authSource=admin`
- **Milvus 未启用**：`application.yml` 中 `milvus.enabled=false` 时向量功能整体关闭，纯对话仍可用
- **Embedding 加载失败**：检查模型目录路径是否正确（可在 yml 中修改指向自己的 bge 模型路径）
- **TTS 无声音**：离线模式依赖 `sherpa-onnx/` 下原生库；在线模式需系统安装 `edge-tts`（`pip install edge-tts`）
- **文件上传失败**：确认 `/data/agent-scope/files` 目录存在且应用有写权限
