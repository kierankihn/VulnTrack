# VulnTrack 漏洞追踪与工单管理系统

VulnTrack 是一个面向漏洞治理流程的前后端分离课程项目。系统围绕 CVE 漏洞数据、资产、开发组、用户角色和漏洞处理工单展开，帮助测试人员、组长、开发者和管理员协作完成漏洞发现、分派、跟踪、复核与关闭。

## 功能概览

- CVE 数据库：支持 CVE 列表检索、详情查看、统计和 NVD 数据同步。
- 工单管理：支持漏洞工单创建、分派、状态流转、关联 CVE 和处理记录追踪。
- 资产管理：维护项目资产、代码仓库、所属开发组和未关闭工单数量。
- 开发组管理：维护开发组、组长和成员关系。
- 用户与权限：内置管理员、组长、开发者、测试员四类角色，并按页面和操作控制访问权限。
- 系统设置：维护 NVD API Key 等系统级配置。
- 外部扫描接入：提供外部扫描报告提交接口，可生成对应漏洞工单。

## 技术栈

### 后端

- Java 21
- Spring Boot 3.4.5
- Spring Web / Spring Security / Spring Data JPA / Bean Validation
- PostgreSQL
- JWT 访问令牌与刷新令牌
- Maven

### 前端

- React 18
- TypeScript
- Vite
- React Router
- Tailwind CSS
- Radix UI
- Vitest

### 部署

- Docker
- Docker Compose
- Nginx 前端静态资源服务与 `/api/` 反向代理

## 项目结构

```text
.
├── backend/              # Spring Boot 后端服务
├── frontend/             # React + Vite 前端应用
├── ci-integration/       # CI 与依赖检查示例脚本
├── docs/                 # 课程报告、演示材料等文档
├── docker-compose.yml    # PostgreSQL、后端、前端编排
└── .env.example          # 环境变量示例
```

## 使用 Docker Compose 启动

1. 复制环境变量文件：

   ```bash
   cp .env.example .env
   ```

2. 修改 `.env` 中的敏感配置，至少应替换：

   ```text
   POSTGRES_PASSWORD=change-me
   JWT_SECRET=change-me-to-a-random-32-byte-secret
   ```

3. 构建并启动服务：

   ```bash
   docker compose up --build
   ```

4. 浏览器访问：

   ```text
   http://localhost:3000
   ```

Docker Compose 会启动 PostgreSQL、后端服务和前端 Nginx。前端容器暴露 `3000` 端口，并将 `/api/` 请求代理到后端容器的 `8080` 端口。

## 默认管理员账号

当数据库中不存在管理员用户时，后端会自动创建默认管理员：

```text
用户名：admin
密码：admin
```

首次登录后建议立即修改密码。

## 本地开发

### 后端

本地运行后端前，需要先准备 PostgreSQL，并保证 `application.yml` 中的数据源配置可用，或通过环境变量覆盖：

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/vulntrack
export SPRING_DATASOURCE_USERNAME=vulntrack
export SPRING_DATASOURCE_PASSWORD=your-password
export JWT_SECRET=your-random-32-byte-secret
```

启动后端：

```bash
cd backend
mvn spring-boot:run
```

后端默认监听：

```text
http://localhost:8080
```

### 前端

安装依赖并启动 Vite 开发服务器：

```bash
cd frontend
npm install
npm run dev
```

前端开发服务器会把 `/api` 请求代理到 `http://localhost:8080`。

## 测试

后端测试：

```bash
cd backend
mvn test
```

前端测试：

```bash
cd frontend
npm test
```

前端生产构建：

```bash
cd frontend
npm run build
```

## 主要 API 分组

- `POST /api/v1/auth/login`：登录
- `/api/v1/cves`：CVE 检索、统计、同步和详情
- `/api/v1/tickets`：工单检索、创建、分派、状态流转和 CVE 关联
- `/api/v1/assets`：资产管理
- `/api/v1/dev-groups`：开发组管理
- `/api/v1/users`：用户管理
- `/api/v1/settings`：系统设置
- `/api/v1/external/scan`：外部扫描报告接入

## 角色权限说明

- 管理员：拥有系统设置、用户、开发组、资产、CVE 同步和工单管理权限。
- 组长：可查看 CVE 和工单，管理本组相关分派流程，并访问资产信息。
- 开发者：可查看 CVE 和工单，并处理分派给自己的漏洞工单。
- 测试员：可查看 CVE 和工单，并创建漏洞工单。

