# s报错处理知识库

一个采用「边使用边收集边更新」机制的报错知识库系统，用于记录、搜索和持续优化常见问题的处理方案。

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 前端 | Vue | 2.6.x |
| 后端 | Spring Boot | 2.7.18 |
| JDK | Zulu OpenJDK | 1.8 |
| 数据库 | MySQL | 8.0 |
| 构建工具 | Maven | 3.9 |
| 编码规范 | 阿里巴巴 Java 开发手册 | — |

## 功能特性

- **智能日志匹配** — 整段报错日志直接 Ctrl+V 粘贴即自动匹配：系统反向用每条记录的关键字/标题/内容特征在日志中检索打分（错误码、异常类名、errno、脚本名、中文报错短语等），无需用户自己读日志提炼关键词；未命中可一键登记为待处理
- **模糊搜索** — 支持按报错标题、报错内容、关键字、处理步骤、分类进行多字段模糊检索，结果按 `update_time` 降序展示；短关键词自动与智能匹配合并
- **批量导入** — 首页「批量导入」下载动态收集模板，线下按行收集报错后一键上传入库；示例行/重复标题自动跳过、逐行报告结果，无处理步骤的记录自动进「待更新」，缺分类的行自动归入「其他」
- **动态收集模板** — 模板由后端按当前分类配置实时生成：分类列带下拉选项（与分类配置模块同步）、必填列与示例行带颜色标识，模板永远不会与系统配置脱节
- **报错录入** — 记录报错示例（截图 + 文字）、处理步骤、登记人、登记时间；「从报错内容提取」按钮可自动识别错误码、异常类名、文件名、中文报错短语等作为关键字
- **截图支持** — 支持上传报错截图，详情页点击截图可放大预览
- **文档管理** — 支持上传任意格式文档（单文件最大 50MB），在线按格式预览：图片、PDF、Word/Excel/PPT（新旧格式均可）、文本、音视频等；Word 由 Apache POI 转换预览（图片以 base64 内嵌保留），Excel 保留字体/填充/边框/合并单元格/列宽并支持多 Sheet 切换，PPT 由服务器 LibreOffice 转 PDF 预览（页数完整、保真度高），上传时自动按文件头纠正真实扩展名（如 WPS 的 .doc 起名 .docx），文件元数据入库、文件本体独立存放，可随时下载或删除
- **信息反馈** — 对存量记录可更新处理步骤，防止老旧方案过时
- **待更新机制** — 搜索未命中的报错或未填处理步骤的记录自动标记为「待更新」状态
- **分类配置模块** — 独立的所属分类管理（新增/编辑/删除/排序/启停），记录录入下拉框、收集模板下拉与分类配置实时同步；被记录引用的分类不可删除

## 项目结构

```
报错处理知识库/
├── backend/                         # Spring Boot 后端
│   ├── pom.xml                      # Maven 配置（MySQL 驱动、POI 等）
│   └── src/main/
│       ├── java/com/kb/error/
│       │   ├── ErrorKnowledgeBaseApplication.java   # 启动类
│       │   ├── config/               # CORS、异常处理、数据初始化、上传目录权限自适应
│       │   ├── controller/           # REST 控制器（记录/分类配置/模板下载）
│       │   ├── entity/               # 实体（ErrorRecord、CategoryConfig）
│       │   ├── repository/           # JPA 数据访问
│       │   ├── service/              # 业务逻辑（匹配打分、批量导入、特征提取）
│       │   └── dto/                  # 匹配结果 DTO
│       └── resources/
│           ├── application.yml       # 应用配置（MySQL 连接等）
│           └── static/               # 前端构建产物（Vue 打包后）
│
└── frontend/                         # Vue 2.6 前端
    ├── package.json
    ├── vue.config.js                 # 开发代理配置
    └── src/
        ├── main.js                   # 入口（含全局 Toast）
        ├── App.vue                   # 根组件（顶部导航）
        ├── router/index.js           # 路由（首页/新增/编辑/详情/分类配置）
        ├── api/index.js              # API 封装
        ├── components/               # 通用组件（Toast、EmptyState、LoadingSkeleton）
        └── views/
            ├── Home.vue              # 首页（搜索 + 列表 + 批量导入入口）
            ├── AddRecord.vue         # 新增报错记录（含关键字自动提取）
            ├── EditRecord.vue        # 编辑记录（含关键字自动提取）
            ├── Detail.vue            # 详情页
            └── CategoryManage.vue    # 分类配置管理页
```

## 快速启动

### 环境要求

- JDK 1.8
- Maven 3.9+
- Node.js (推荐 v16+)
- MySQL 8.0

### 数据库准备

```sql
CREATE DATABASE errorkb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'errorkb'@'localhost' IDENTIFIED BY '你的密码';
GRANT ALL PRIVILEGES ON errorkb.* TO 'errorkb'@'localhost';
FLUSH PRIVILEGES;
```

在 `backend/src/main/resources/application.yml` 中修改连接地址、账号密码。

### 后端

```bash
cd backend
mvn spring-boot:run
```

后端启动后访问：
- API 服务：`http://localhost:8080`

> 首次启动由 Hibernate 自动建表（`ddl-auto: update`），并自动写入初始化数据与默认分类配置。

### 前端（开发模式）

```bash
cd frontend
npm install
npm run serve
```

前端启动后访问：`http://localhost:3000`（开发模式通过 vue.config.js 代理 `/api` 到后端）

### 前端构建（部署模式）

```bash
cd frontend
npm run build
# 构建产物在 frontend/dist/，需同步到 backend/src/main/resources/static/ 后重新打包后端
cd ../backend
mvn package -DskipTests
```

## 部署与权限说明

- 应用启动时会自动创建并修正 `uploads`（截图、`uploads/documents` 文档）与 `logs`（日志）目录的读写权限（POSIX 环境）。
- 部署目录属主（如通过 `chown` 变更为其他用户后）与运行用户不一致时：
  - 若部署目录对运行用户仍可写（属主为运行用户，或属组/其他含写权限），应用会自动适配，上传截图无需额外处理；
  - 若部署目录对运行用户只读（如 `drwxr-xr-x` 且属主为其他用户），需将服务切换到目录属主用户运行，或执行
    `chmod g+rwx` 相关目录。

## API 接口

### 报错记录 `/api/error-record`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/error-record/match` | 智能日志匹配（body: `{"logText": "..."}`，返回识别特征、根因行、按匹配度排序的记录） |
| POST | `/api/error-record/extract-keywords` | 从报错文本提取特征关键字（body: `{"text": "..."}`） |
| POST | `/api/error-record/import` | xlsx 批量导入（multipart `file`，返回逐行导入结果） |
| GET | `/api/error-record/search` | 模糊搜索（分页，按 update_time 降序） |
| GET | `/api/error-record/{id}` | 查询详情 |
| POST | `/api/error-record` | 新增记录 |
| PUT | `/api/error-record/{id}` | 更新记录 |
| DELETE | `/api/error-record/{id}` | 删除记录 |
| GET | `/api/error-record/pending` | 待更新列表 |
| GET | `/api/error-record/categories` | 分类下拉（配置中启用的分类 + 记录中实际存在的分类） |
| POST | `/api/error-record/upload-screenshot` | 上传截图 |

### 分类配置 `/api/category-config`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/category-config` | 分类列表（含每个分类下的记录数） |
| POST | `/api/category-config` | 新增分类 |
| PUT | `/api/category-config/{id}` | 修改分类（名称/描述/排序/启停） |
| DELETE | `/api/category-config/{id}` | 删除分类（被记录引用时拒绝） |

### 收集模板 `/api/template`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/template/error-kb-template` | 下载动态收集模板（分类下拉与颜色标识随当前配置生成） |

### 文档管理 `/api/documents`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/documents` | 文档列表（按上传时间倒序） |
| POST | `/api/documents` | 上传文档（multipart `file`，格式不限，单文件最大 50MB） |
| DELETE | `/api/documents/{id}` | 删除文档（同时删除元数据与磁盘文件） |

预览文件直接访问返回的 `url`（如 `/uploads/documents/xxx.pdf`），前端按扩展名选择对应预览器。

## 数据模型

### error_record（报错记录）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| error_title | String | 报错标题 |
| error_content | Text | 报错内容描述 |
| error_screenshot | String | 报错截图路径 |
| solution_steps | Text | 处理步骤 |
| category | String | 所属分类（与 category_config.name 对应） |
| keywords | String | 关键字，逗号分隔 |
| status | String | 状态：RECORDED / PENDING |
| registrar | String | 登记人 |
| register_time | DateTime | 登记时间 |
| updater | String | 更新人 |
| update_time | DateTime | 更新时间 |

### category_config（分类配置）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| name | String | 分类名称（唯一） |
| description | String | 分类描述 |
| sort_order | Integer | 排序号（越小越靠前） |
| enabled | Boolean | 是否启用（停用后不出现在下拉与模板中） |
| create_time | DateTime | 创建时间 |
| update_time | DateTime | 更新时间 |

### document_file（文档元数据）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| original_name | String | 原始文件名（上传时清理路径分隔符） |
| stored_name | String | 存储文件名（UUID + 原扩展名，唯一） |
| content_type | String | MIME 类型 |
| file_size | Long | 文件大小（字节） |
| upload_time | DateTime | 上传时间 |

文档文件本体存放在 `uploads/documents/`，由 `/uploads/**` 静态映射对外提供预览与下载。

## 智能匹配规则

匹配接口对每条记录独立打分，得分大于 0 才返回，按得分降序取前 10 条：

- **关键字命中**：记录 keywords 中的词在日志中出现即得分（代码型词如 `ORA-12505`、含 `-`/`_`/`.` 的词权重更高），命中 ERROR/Caused by 关键行额外加权
- **标题整体命中**：记录标题完整出现在日志中 +40 分
- **内容特征交集**：记录错误内容与日志提取出的共同特征（错误码、异常类名、文件名等）每个 +20 分
- **短输入兜底**：单行 ≤100 字符的输入额外执行模糊搜索合并，命中 +12 分
- **匹配度分档**：≥45 分 HIGH，≥22 分 MEDIUM，否则 LOW
