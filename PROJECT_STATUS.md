# Vaadin Flow 和 Quarkus 项目 - WMC Deploy

本项目是一个基于 Vaadin Flow 和 Quarkus 的 Web 应用程序，用于部署管理和清单管理。

Quarkus 3.0+ 需要 Java 17。

## 项目结构

项目采用分层架构，按功能职责将类分离到不同的包中：

```
com.example.starter/
├── config/                          # 配置层
│   └── AppConfig.java
├── entity/                           # 实体层 - 数据模型
│   ├── Inventory.java                  # 清单实体
│   ├── InventoryGroup.java             # 清单组实体
│   ├── InventoryGroupHost.java         # 清单组-主机关联
│   ├── InventoryGroupVariable.java      # 清单组变量
│   ├── InventoryHost.java             # 主机实体
│   ├── InventoryHostInventory.java      # 主机-清单关联
│   ├── InventoryHostVariable.java      # 主机变量
│   ├── InventoryVariable.java          # 清单变量
│   ├── Project.java                   # 项目实体
│   └── User.java                    # 用户实体
├── exception/                        # 异常处理层
│   ├── DuplicateResourceException.java   # 资源重复异常
│   └── ResourceNotFoundException.java  # 资源未找到异常
├── repository/                       # 数据访问层
│   ├── InventoryHostRepository.java
│   ├── InventoryRepository.java
│   └── ProjectRepository.java
├── service/                          # 业务逻辑层
│   ├── GreetService.java             # 问候服务
│   ├── ProjectService.java            # 项目服务
│   ├── auth/                        # 认证模块
│   │   ├── AuthService.java
│   │   └── UserService.java
│   ├── host/                        # 主机管理模块
│   │   └── SSHConnectionService.java
│   └── inventory/                   # 清单管理模块
│       └── InventoryService.java
├── util/                             # 工具类
│   └── UIBroadcaster.java            # UI广播器
└── view/                             # 视图层 - UI组件
    ├── MainLayout.java                # 主布局
    ├── ResourcePreviewView.java        # 资源预览视图
    ├── auth/                        # 认证视图
    │   ├── LoginView.java
    │   └── LogoutView.java
    └── admin/                       # 管理视图
        ├── AdminView.java
        ├── HostManagementView.java      # 主机管理视图
        ├── InventoryManagementView.java # 清单管理视图
        └── ProjectManagementView.java  # 项目管理视图
```

## 单元测试

测试文件位于 `src/test/java/com/example/starter/` 目录下：

### 测试文件列表
```
src/test/java/com/example/starter/
├── repository/
│   ├── InventoryHostRepositoryTest.java   (12个测试用例)
│   ├── InventoryRepositoryTest.java       (8个测试用例)
│   └── ProjectRepositoryTest.java        (11个测试用例)
└── service/
    ├── GreetServiceTest.java              (6个测试用例)
    ├── ProjectServiceTest.java             (21个测试用例)
    └── inventory/
        └── InventoryServiceTest.java     (19个测试用例)
```

### 测试覆盖情况
- **总测试文件**：6个
- **总测试用例**：77个
- **测试状态**：全部通过 ✅

### 已测试模块
- ✅ ProjectService - 项目管理业务逻辑
- ✅ InventoryService - 清单管理业务逻辑
- ✅ GreetService - 问候服务
- ✅ ProjectRepository - 项目数据访问
- ✅ InventoryRepository - 清单数据访问
- ✅ InventoryHostRepository - 主机数据访问

### 缺失测试（建议补充）
- ⏳ AuthServiceTest - 认证服务测试
- ⏳ UserServiceTest - 用户服务测试
- ⏳ SSHConnectionServiceTest - SSH连接服务测试

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

### 主机管理功能

主机管理模块允许用户添加、管理和监控服务器，通过SSH连接检测验证服务器可用性。

#### 系统布局
- **顶部导航栏**：应用名称、版本号，用户欢迎信息和退出登录按钮
- **左侧菜单栏**：深色背景，包含"资源预览"和"主机管理"菜单项

#### 主要功能
1. **添加主机**：填写名称、主机/IP、端口、用户名、密码、描述
2. **SSH连接检测**：自动检测和手动检测服务器连接状态
3. **主机列表**：显示所有主机的详细信息和连接状态
4. **主机操作**：测试连接、删除主机

#### 技术实现
- **后端**：InventoryHost实体、InventoryHostRepository、SSHConnectionService
- **前端**：MainLayout、HostManagementView
- **数据库**：SQLite数据库存储主机信息

### 项目管理功能

项目管理模块用于管理和部署配置项目。

#### 主要功能
1. **创建项目**：输入项目名称、描述和YAML配置
2. **项目列表**：显示所有已创建的项目
3. **编辑项目**：修改项目信息和配置
4. **删除项目**：移除不再需要的项目
5. **资源管理**：管理项目关联的资源

#### 异常处理
- **DuplicateResourceException**：创建重复名称项目时抛出
- **ResourceNotFoundException**：更新/删除不存在项目时抛出

#### 技术实现
- **后端**：Project实体、ProjectRepository、ProjectService
- **前端**：ProjectManagementView
- **数据库**：SQLite数据库存储项目信息

### 清单管理功能

清单管理模块用于组织和管理服务器清单，支持层级分组和变量配置。

#### 主要功能
1. **创建清单**：创建新的服务器清单
2. **清单分组**：支持最多3层嵌套的组结构
3. **主机管理**：将主机添加到清单或组
4. **变量配置**：为清单或主机添加变量
5. **层级管理**：计算和管理组的层级深度

#### 异常处理
- **DuplicateResourceException**：创建重复名称清单/主机已存在时抛出
- **IllegalArgumentException**：组嵌套超过3层或参数无效时抛出

#### 技术实现
- **后端**：Inventory系列实体、InventoryRepository、InventoryHostRepository、InventoryService
- **前端**：InventoryManagementView
- **数据库**：SQLite数据库存储清单信息

## 运行应用程序

将项目作为 Maven 项目导入到您选择的 IDE 中。

使用 `mvnw` (Windows) 或 `./mvnw` (Mac & Linux) 运行应用程序。

在浏览器中打开 [http://localhost:8081/](http://localhost:8081/)。

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
- 端口配置：8081

### 应用端口
- 开发模式：8081
- 生产模式：可配置

## 安全特性

1. **会话超时**：防止长时间不活动导致的未授权访问
2. **输入验证**：防止空用户名/密码的无效请求
3. **详细日志**：记录所有认证相关操作，便于审计
4. **会话清理**：登出时完整清理会话数据
5. **状态检查**：每次访问都检查会话有效性
6. **异常处理**：统一的异常处理机制，提供清晰的错误信息

## 依赖项

### 主要依赖
- Quarkus 3.15.3
- Vaadin Flow
- Hibernate Panache ORM
- SQLite JDBC
- JSch (用于SSH连接)
- JUnit 5 (测试框架)

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

## 运行单元测试

```bash
mvn test
```

当前测试结果：
- Tests run: 77
- Failures: 0
- Errors: 0
- Skipped: 0

## 生产环境部署

```bash
mvn clean package -Pproduction
java -jar target/quarkus-app/quarkus-run.jar
```

## 默认用户

系统默认创建管理员用户：
- 用户名：`admin`
- 密码：`admin`

## 异常处理体系

项目使用自定义异常类进行错误处理：

### DuplicateResourceException
用于表示尝试创建已存在的资源：
- 创建重复名称的项目
- 创建重复名称的清单
- 添加已存在的主机到清单/组

### ResourceNotFoundException
用于表示请求的资源不存在：
- 更新/删除不存在的项目
- 更新/删除不存在的清单
- 操作不存在的主机/组

## 代码规范

### 测试规范
- 使用 `@DisplayName` 提供清晰的测试名称
- 使用 `@Order` 控制测试执行顺序
- 使用 `@BeforeEach` 清理测试数据
- 使用 `@Transactional` 确保数据一致性

### 事务管理
- Service 层方法使用 `@Transactional` 注解
- Repository 层保持简单
- 确保数据一致性

### 依赖注入
- 使用 `@Inject` 进行依赖注入
- 使用 `@ApplicationScoped` 定义作用域

## 项目文档

- `README.md` - 项目基本说明
- `PROJECT_STATUS.md` - 本文档，项目当前状态
- `PROJECT_STRUCTURE_OPTIMIZATION.md` - 项目结构优化说明
- `INVENTORY_DESIGN.md` - 清单模块设计文档
