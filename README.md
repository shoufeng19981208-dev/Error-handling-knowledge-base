# 报错处理知识库

一个采用「边使用边收集边更新」机制的报错知识库系统，用于记录、搜索和持续优化常见问题的处理方案。

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 前端 | Vue | 2.6.x |
| 后端 | Spring Boot | 2.7.18 |
| JDK | Zulu OpenJDK | 1.8 |
| 数据库 | H2（内存数据库） | — |
| 构建工具 | Maven | 3.9 |
| 编码规范 | 阿里巴巴 Java 开发手册 | — |

## 功能特性

- **模糊搜索** — 支持按报错标题、报错内容、关键字、处理步骤、分类进行多字段模糊检索
- **报错录入** — 记录报错示例（截图 + 文字）、处理步骤、登记人、登记时间
- **截图支持** — 支持上传报错截图，详情页点击截图可放大预览
- **信息反馈** — 对存量记录可更新处理步骤，防止老旧方案过时
- **待更新机制** — 搜索未命中的报错或未填处理步骤的记录自动标记为「待更新」状态
- **分类管理** — 所属分类支持动态编辑，无需提前预设

## 项目结构

```
报错处理知识库/
├── backend/                         # Spring Boot 后端
│   ├── pom.xml                      # Maven 配置
│   └── src/main/
│       ├── java/com/kb/error/
│       │   ├── ErrorKnowledgeBaseApplication.java   # 启动类
│       │   ├── config/               # 配置（CORS、异常处理、静态资源、数据初始化）
│       │   ├── entity/               # 实体（ErrorRecord）
│       │   ├── repository/           # JPA 数据访问
│       │   ├── service/              # 业务逻辑
│       │   └── controller/           # REST 控制器
│       └── resources/
│           ├── application.yml       # 应用配置
│           └── data.sql              # 初始化数据（54 条报错记录）
│
└── frontend/                         # Vue 2.6 前端
    ├── package.json
    ├── vue.config.js                 # 代理配置
    └── src/
        ├── main.js                   # 入口
        ├── App.vue                   # 根组件
        ├── router/index.js           # 路由
        ├── api/index.js              # API 封装
        └── views/
            ├── Home.vue              # 首页（搜索 + 列表）
            ├── AddRecord.vue         # 新增报错记录
            ├── EditRecord.vue        # 编辑记录
            └── Detail.vue            # 详情页
```

## 快速启动

### 环境要求

- JDK 1.8
- Maven 3.9+
- Node.js (推荐 v16+)

### 后端

```bash
cd backend
mvn spring-boot:run
```

后端启动后访问：
- API 服务：`http://localhost:8080`
- H2 控制台：`http://localhost:8080/h2-console`（JDBC URL: `jdbc:h2:file:./data/errorkb`，用户名 `sa`，密码为空）

### 前端

```bash
cd frontend
npm install
npm run serve
```

前端启动后访问：`http://localhost:3000`

## API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/error-record/search` | 模糊搜索（分页） |
| GET | `/api/error-record/{id}` | 查询详情 |
| POST | `/api/error-record` | 新增记录 |
| PUT | `/api/error-record/{id}` | 更新记录 |
| DELETE | `/api/error-record/{id}` | 删除记录 |
| GET | `/api/error-record/pending` | 待更新列表 |
| GET | `/api/error-record/categories` | 所有分类 |
| POST | `/api/error-record/upload-screenshot` | 上传截图 |

## 数据模型

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| errorTitle | String | 报错标题 |
| errorContent | String | 报错内容描述 |
| errorScreenshot | String | 报错截图路径 |
| solutionSteps | String | 处理步骤 |
| category | String | 所属分类（可动态编辑） |
| keywords | String | 关键字，逗号分隔 |
| status | RecordStatus | 状态：RECORDED / PENDING |
| registrar | String | 登记人 |
| registerTime | LocalDateTime | 登记时间 |
| updater | String | 更新人 |
| updateTime | LocalDateTime | 更新时间 |
