# Vaadin Flow 和 Quarkus 项目基础模板

本项目可以作为创建您自己的 Vaadin Flow 和 Quarkus 应用程序的起点。它包含了所有必要的配置和一些占位文件，以帮助您开始开发。

Quarkus 3.0+ 需要 Java 17。

此启动器也提供 [Gradle](https://github.com/vaadin/base-starter-flow-quarkus/tree/gradle) 版本

## 项目结构

项目采用分层架构，按功能职责将类分离到不同的包中：

```
com.example.starter/
├── entity/                    # 实体层 - 数据模型
│   ├── User.java
│   ├── Server.java
│   ├── Role.java
│   └── RoleVariable.java
├── repository/                # 数据访问层
│   ├── ServerRepository.java
│   └── RoleRepository.java
├── service/                   # 业务逻辑层
│   ├── GreetService.java      # 通用服务
│   ├── auth/                 # 认证模块
│   │   ├── AuthService.java
│   │   └── UserService.java
│   └── server/              # 服务器管理模块
│       └── SSHConnectionService.java
├── view/                      # 视图层 - UI组件
│   ├── MainLayout.java        # 主布局
│   ├── ResourcePreviewView.java
│   ├── auth/                 # 认证视图
│   │   ├── LoginView.java
│   │   └── LogoutView.java
│   └── admin/               # 管理视图
│       ├── AdminView.java
│       ├── ServerManagementView.java
│       └── RoleManagementView.java
└── config/                    # 配置层
    └── AppConfig.java
```

## 主要功能

### 用户认证和会话管理

系统提供完整的用户登录状态管理：

#### UserService 会话管理
- **会话超时管理**：30分钟无活动自动过期
- **登录时间追踪**：记录用户登录时间
- **最后活动时间**：记录用户最后活动时间，用于会话超时判断
- **会话状态检查**：提供多种会话状态查询方法
- **会话信息展示**：提供详细的会话信息字符串

#### 主要方法：
```java
public LocalDateTime getLoginTime()                    // 获取登录时间
public LocalDateTime getLastActivityTime()            // 获取最后活动时间
public boolean isSessionExpired()                     // 检查会话是否过期
public long getSessionTimeoutMinutes()                // 获取会话超时时间
public long getRemainingSessionTime()                // 获取剩余会话时间
public String getSessionInfo()                       // 获取会话详细信息
```

#### 登录界面
- **表单布局**：居中显示，固定宽度
- **输入框增强**：添加清除按钮，自动聚焦
- **即时反馈**：使用不同颜色和样式的通知提示
- **查询参数处理**：
  - `?logout` - 显示登出成功消息
  - `?expired` - 显示会话过期提示

#### 主页面会话管理
- **实时会话信息**：显示用户名、角色和剩余会话时间
- **会话过期警告**：会话剩余时间 <= 5 分钟时显示橙色警告
- **登出按钮**：使用按钮替代链接，提供更好的用户体验

### 服务器管理功能

主机管理模块允许用户添加、管理和监控服务器，通过SSH连接检测验证服务器可用性。

#### 系统布局
- **顶部导航栏**：应用名称、版本号，用户欢迎信息和退出登录按钮
- **左侧菜单栏**：深色背景，包含"资源预览"和"主机管理"菜单项

#### 主要功能
1. **添加服务器**：填写名称、主机/IP、端口、用户名、密码、描述
2. **SSH连接检测**：自动检测和手动检测服务器连接状态
3. **服务器列表**：显示所有服务器的详细信息和连接状态
4. **服务器操作**：测试连接、删除服务器

#### 技术实现
- **后端**：Server实体、ServerRepository、SSHConnectionService
- **前端**：MainLayout、ResourcePreviewView、ServerManagementView
- **数据库**：SQLite数据库存储服务器信息

## 运行应用程序

将项目作为 Maven 项目导入到您选择的 IDE 中。

使用 `mvnw` (Windows) 或 `./mvnw` (Mac & Linux) 运行应用程序。

在浏览器中打开 [http://localhost:8080/](http://localhost:8080/)。

如果您想在本地以生产模式运行应用程序，请执行 `mvnw package -Pproduction` (Windows) 或 `./mvnw package -Pproduction` (Mac & Linux)
然后
```
java -jar target/quarkus-app/quarkus-run.jar
```

### 为 Pro 组件包含 vaadin-jandex
如果您使用 Pro 组件，例如 GridPro，您也需要为它们提供 Jandex 索引。
虽然可以通过在 `application.properties` 中逐个添加它们的名称来实现，如下例所示：
```properties
quarkus.index-dependency.vaadin-grid-pro.group-id=com.vaadin
quarkus.index-dependency.vaadin-grid-pro.artifact-id=vaadin-grid-pro-flow
```
Vaadin 建议使用作为平台一部分发布的 Pro 组件的官方 Jandex 索引：
```xml
<dependency>
    <groupId>com.vaadin</groupId>
    <artifactId>vaadin-jandex</artifactId>
</dependency>
```
上述依赖项已经添加到 `pom.xml` 中，您只需要在需要时取消注释即可。

## 配置说明

### 会话超时配置
在 `UserService.java` 中修改：
```java
private static final long SESSION_TIMEOUT_MINUTES = 30;  // 修改这个值调整超时时间
```

### 数据库配置
- 使用SQLite数据库，文件名为 `app.db`
- 数据库模式自动更新：`quarkus.hibernate-orm.database.generation=update`
- 端口配置：8082

## 安全特性

1. **会话超时**：防止长时间不活动导致的未授权访问
2. **输入验证**：防止空用户名/密码的无效请求
3. **详细日志**：记录所有认证相关操作，便于审计
4. **会话清理**：登出时完整清理会话数据
5. **状态检查**：每次访问都检查会话有效性

## 依赖项

### 主要依赖
- Quarkus 3.0+
- Vaadin Flow
- Hibernate Panache ORM
- SQLite
- JSch (用于SSH连接)

### 新增依赖
```xml
<dependency>
    <groupId>com.github.mwiede</groupId>
    <artifactId>jsch</artifactId>
    <version>0.2.18</version>
</dependency>
```

## 开发模式运行

```bash
mvn quarkus:dev
```

## 生产环境部署

```bash
mvn clean package -Pproduction
java -jar target/quarkus-app/quarkus-run.jar
```

## 默认用户

系统默认创建管理员用户：
- 用户名：`admin`
- 密码：`admin`