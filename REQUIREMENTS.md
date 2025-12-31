# WMC-Deploy 项目需求文档

## 1. 项目概述

### 1.1 项目名称
WMC-Deploy - Ansible部署管理系统

### 1.2 项目简介
WMC-Deploy是一个基于Vaadin Flow和Quarkus框架开发的Web应用程序，用于管理和执行Ansible自动化部署任务。系统提供可视化的界面来管理主机、清单、项目、模板，并监控任务执行状态。

### 1.3 技术栈
- **后端框架**：Quarkus 3.15.3
- **前端框架**：Vaadin Flow
- **数据库**：SQLite
- **ORM**：Hibernate Panache
- **自动化工具**：Ansible
- **Java版本**：JDK 17+
- **测试框架**：JUnit 5

### 1.4 应用端口
- 开发模式：8080
- 生产模式：可配置

---

## 2. 功能模块

### 2.1 用户认证与会话管理

#### 2.1.1 功能描述
提供完整的用户登录状态管理和会话控制机制。

#### 2.1.2 功能需求
- **用户登录**
  - 支持用户名密码登录
  - 表单居中显示，自动聚焦用户名输入框
  - 输入框带清除按钮
  - 登录成功后跳转到资源预览页面

- **会话管理**
  - 默认会话超时时间：30分钟
  - 记录用户登录时间和最后活动时间
  - 无操作超过30分钟自动过期
  - 登出时完整清理会话数据

- **登录界面**
  - 居中布局，固定宽度
  - 支持查询参数处理：
    - `?logout` - 显示登出成功消息
    - `?expired` - 显示会话过期提示
  - 即时反馈通知

#### 2.1.3 安全特性
- 防止长时间不活动导致的未授权访问
- 输入验证（空用户名/密码检测）
- 详细日志记录认证操作
- 统一异常处理机制

#### 2.1.4 默认用户
- 用户名：admin
- 密码：admin

---

### 2.2 主机管理

#### 2.2.1 功能描述
管理服务器主机信息，通过SSH连接验证服务器可用性。

#### 2.2.2 功能需求
- **添加主机**
  - 必填字段：名称、主机/IP地址、端口、用户名、密码
  - 可选字段：描述
  - 端口默认值：22

- **主机列表**
  - 表格展示所有主机
  - 显示字段：名称、主机地址、端口、描述、连接状态
  - 支持按连接状态筛选

- **主机操作**
  - 测试连接：通过SSH检测主机可用性
  - 删除主机：移除主机记录

- **连接状态检测**
  - 自动检测：主机列表加载时自动检测
  - 手动检测：点击测试连接按钮
  - 状态显示：已连接（绿色）/ 未连接（红色）

#### 2.2.3 技术实现
- 后端：InventoryHost实体、InventoryHostRepository、SSHConnectionService
- 前端：HostManagementView
- 数据存储：SQLite数据库

---

### 2.3 清单管理

#### 2.3.1 功能描述
组织和Ansible管理服务器清单，支持层级分组和变量配置。

#### 2.3.2 功能需求
- **创建清单**
  - 必填字段：名称
  - 可选字段：描述

- **清单分组**
  - 支持嵌套组结构
  - 最多支持3层嵌套
  - 组名称唯一性验证

- **主机管理**
  - 将主机添加到清单或组
  - 主机可以在多个组中存在
  - 支持从组中移除主机

- **变量配置**
  - 清单级别变量（全局变量）
  - 组级别变量
  - 主机级别变量
  - 变量格式：YAML格式

- **层级管理**
  - 计算组的层级深度
  - 超过3层抛出异常

#### 2.3.3 异常处理
- DuplicateResourceException：创建重复名称清单/主机已存在时抛出
- IllegalArgumentException：组嵌套超过3层或参数无效时抛出
- ResourceNotFoundException：操作不存在的清单/主机时抛出

#### 2.3.4 技术实现
- 后端：Inventory系列实体、InventoryRepository、InventoryService
- 前端：InventoryManagementView
- 数据存储：SQLite数据库

---

### 2.4 项目管理

#### 2.4.1 功能描述
管理和配置Ansible项目，用于存储YAML配置文件。

#### 2.4.2 功能需求
- **创建项目**
  - 必填字段：名称、YAML配置内容
  - 可选字段：描述
  - YAML内容Base64编码存储

- **项目列表**
  - 表格展示所有项目
  - 显示字段：名称、描述、创建时间
  - 支持查看YAML内容

- **项目操作**
  - 编辑项目：修改项目信息和配置
  - 删除项目：移除项目记录

- **资源管理**
  - 管理项目关联的资源和模板

#### 2.4.3 异常处理
- DuplicateResourceException：创建重复名称项目时抛出
- ResourceNotFoundException：更新/删除不存在项目时抛出

#### 2.4.4 技术实现
- 后端：Project实体、ProjectRepository、ProjectService
- 前端：ProjectManagementView
- 数据存储：SQLite数据库

---

### 2.5 模板管理

#### 2.5.1 功能描述
创建和管理任务模板，关联项目和清单，方便快速创建任务。

#### 2.5.2 功能需求
- **创建模板**
  - 必填字段：名称、项目、清单
  - 可选字段：描述、变量配置
  - 模板名称唯一性验证

- **模板列表**
  - 表格展示所有模板
  - 显示字段：名称、关联项目、关联清单、描述

- **模板操作**
  - 编辑模板：修改模板配置
  - 删除模板：移除模板记录

- **变量管理**
  - 添加模板变量
  - 编辑模板变量
  - 删除模板变量

#### 2.5.3 技术实现
- 后端：Template实体、TemplateVariable实体、TemplateRepository、TemplateService
- 前端：TemplateManagementView
- 数据存储：SQLite数据库

---

### 2.6 任务管理

#### 2.6.1 功能描述
管理和执行Ansible任务，支持任务创建、监控、重启、取消和删除。

#### 2.6.2 功能需求
- **创建任务**
  - 必填字段：任务名称、选择模板
  - 创建后自动启动执行

- **任务列表**
  - 表格展示所有任务（按创建时间倒序）
  - 显示字段：
    - 任务名称
    - 关联模板
    - 任务状态（等待中/执行中/成功/失败/已取消）
    - 创建时间
    - 开始时间
    - 完成时间
    - 持续时间
  - 操作按钮：查看日志、重启、取消、删除

- **任务状态**
  - PENDING：等待执行
  - RUNNING：执行中
  - SUCCESS：成功完成
  - FAILED：执行失败
  - CANCELLED：已取消

- **任务操作**
  - 查看日志：打开日志对话框显示执行日志
  - 重新启动：重置任务状态并重新执行
  - 取消任务：终止正在运行的任务
  - 删除任务：删除任务记录和临时文件

- **日志管理**
  - 实时查看任务执行日志
  - 运行中的任务每2秒自动刷新日志
  - 支持手动刷新
  - 日志文件保存在临时目录

- **重启任务特性**
  - 重用现有临时目录和日志文件
  - 日志文件使用追加模式（保留历史记录）
  - 重新生成playbook和inventory文件
  - 保留任务历史执行记录

#### 2.6.3 任务执行流程
1. 创建任务，状态为PENDING
2. 生成临时目录
3. 解码项目YAML内容，生成playbook.yml
4. 根据清单生成inventory.yml（YAML格式）
5. 创建日志文件execution.log
6. 更新任务状态为RUNNING
7. 执行ansible-playbook命令
8. 实时捕获输出并写入日志
9. 等待进程完成
10. 根据退出码更新状态为SUCCESS或FAILED
11. 广播更新通知

#### 2.6.4 技术实现
- 后端：Task实体、TaskRepository、TaskService
- 前端：TaskManagementView
- 执行引擎：Ansible-playbook
- 临时文件：tmp/wmc-deploy-tasks目录

#### 2.6.5 配置选项
- ansible.path：ansible-playbook可执行文件路径（默认：/usr/bin/ansible-playbook）
- python.path：Python解释器路径（默认：/usr/bin/python3）
- task.temp.directory：任务临时目录（默认：tmp/wmc-deploy-tasks）
- ansible.env.*：自定义环境变量配置

---

### 2.7 资源预览（Dashboard）

#### 2.7.1 功能描述
提供系统资源统计和任务执行情况的概览视图。

#### 2.7.2 功能需求
- **统计卡片**
  - 主机总数
  - 清单总数
  - 项目总数
  - 模板总数
  - 任务总数
  - 成功任务数
  - 失败任务数
  - 任务成功率

- **图表展示**
  - 主机连接状态分布（已连接/未连接）
  - 任务状态分布（成功/失败/运行中/等待中）

- **最近任务**
  - 显示最近5个任务
  - 任务名称、模板名称、创建时间、状态

- **数据刷新**
  - 手动刷新按钮
  - WebSocket自动刷新（任务状态变更时）

#### 2.7.3 技术实现
- 后端：StatisticsService
- 前端：ResourcePreviewView
- 实时更新：WebSocket连接

---

### 2.8 WebSocket实时更新

#### 2.8.1 功能描述
使用WebSocket技术实现前端页面的实时数据更新。

#### 2.8.2 功能需求
- **WebSocket端点**
  - 路径：/ws/broadcast/{viewType}
  - 支持的viewType：
    - hosts：主机管理页面
    - tasks：任务管理页面
    - dashboard：资源预览页面

- **自动刷新**
  - 任务状态变更时广播到相关页面
  - 前端接收到刷新消息后自动重新加载页面
  - 支持nginx和haproxy代理

- **连接管理**
  - 页面加载时自动建立WebSocket连接
  - 页面关闭时自动断开连接
  - 连接断开后自动重连（3秒延迟）

#### 2.8.3 技术实现
- 后端：BroadcastWebSocket、UIBroadcaster
- 依赖：quarkus-websockets-next
- 协议：ws://或wss://（根据HTTPS自动选择）

---

## 3. 数据模型

### 3.1 实体关系图

```
User (用户)
  ↓
LoginView → UserService

Project (项目)
  ↓ (1:N)
Template (模板)
  ↓ (1:N)
Task (任务)
  ↓ (N:1)
Inventory (清单)
  ↓ (1:N)
InventoryGroup (清单组)
  ↓ (1:N)
InventoryGroupHost (组-主机关联)
  ↓ (N:1)
InventoryHost (主机)
```

### 3.2 核心实体

#### 3.2.1 User
- 字段：username（主键）、password、loginTime、lastActivityTime
- 功能：存储用户信息和会话状态

#### 3.2.2 Project
- 字段：id、name、description、yamlContent（Base64）、createdAt、updatedAt
- 功能：存储Ansible项目YAML配置

#### 3.2.3 Template
- 字段：id、name、description、project（外键）、inventory（外键）、variables
- 功能：任务模板，关联项目和清单

#### 3.2.4 Inventory
- 字段：id、name、description、variables、groups、hosts
- 功能：Ansible清单，包含组和主机

#### 3.2.5 InventoryGroup
- 字段：id、name、description、variables、groupHosts
- 功能：清单分组，支持嵌套

#### 3.2.6 InventoryHost
- 字段：id、name、host、port、username、password、description、variables
- 功能：服务器主机信息，包含SSH认证信息

#### 3.2.7 Task
- 字段：id、name、template（外键）、status、tempDirectory、logFilePath、startedAt、finishedAt、errorMessage、createdAt、updatedAt
- 功能：任务执行记录

---

## 4. 接口设计

### 4.1 REST API（预留）
虽然当前使用Vaadin Flow，但未来可扩展为RESTful API：

- GET /api/tasks - 获取任务列表
- POST /api/tasks - 创建任务
- GET /api/tasks/{id} - 获取任务详情
- DELETE /api/tasks/{id} - 删除任务
- GET /api/tasks/{id}/logs - 获取任务日志

### 4.2 WebSocket API
- 连接：ws://host/ws/broadcast/{viewType}
- 消息格式：JSON
- 消息类型：refresh

```json
{
  "type": "refresh",
  "viewType": "tasks"
}
```

---

## 5. 安全需求

### 5.1 认证
- 用户名密码认证
- 会话超时机制（30分钟）
- 登出清理会话数据

### 5.2 授权
- 所有管理页面需要登录访问
- 未登录用户自动跳转到登录页
- 会话过期提示

### 5.3 数据安全
- SSH密码加密存储
- YAML配置Base64编码存储
- 日志文件限制在临时目录

### 5.4 输入验证
- 防止SQL注入（使用参数化查询）
- 防止XSS攻击（Vaadin自动转义）
- 表单字段验证（必填项检查）

---

## 6. 性能需求

### 6.1 响应时间
- 页面加载时间 < 2秒
- 任务列表加载 < 1秒
- 日志查看 < 1秒

### 6.2 并发处理
- 支持多任务并行执行
- 使用线程池管理异步任务
- 数据库连接池：2-10个连接

### 6.3 资源使用
- 内存占用 < 512MB（空闲状态）
- 临时文件自动清理
- 日志文件限制大小

---

## 7. 部署需求

### 7.1 环境要求
- JDK 17+
- Ansible 2.x+
- Python 3+
- SQLite支持

### 7.2 配置要求
- 应用端口：8080（可配置）
- 数据库文件：app.db
- 临时目录：tmp/wmc-deploy-tasks

### 7.3 部署方式
```bash
# 开发模式
./mvnw quarkus:dev

# 生产模式
./mvnw clean package -Pproduction
java -jar target/quarkus-app/quarkus-run.jar
```

---

## 8. 测试需求

---

## 9. 用户体验需求

### 9.1 界面设计
- 响应式布局，支持不同屏幕尺寸
- 统一的色彩方案
- 清晰的导航结构
- 友好的错误提示

### 9.2 交互设计
- 表单自动聚焦
- 即时反馈通知
- 加载状态提示
- 确认对话框

### 9.3 可访问性
- 键盘导航支持
- 清晰的标签和提示
- 足够的对比度

---

## 10. 未来扩展

### 10.1 功能扩展
- [ ] 用户角色管理
- [ ] 任务调度（定时任务）
- [ ] 任务依赖管理
- [ ] 任务模板市场
- [ ] 主机标签管理
- [ ] 更多图表和统计

### 10.2 技术扩展
- [ ] RESTful API支持
- [ ] 集群部署支持
- [ ] 容器化部署（Docker/K8s）
- [ ] 日志集中收集（ELK）
- [ ] 监控告警（Prometheus）

---

## 11. 维护与支持

### 11.1 日志
- 应用日志：INFO级别
- SQL日志：关闭（生产环境）
- 日志格式：时间戳 + 级别 + 类名 + 消息

### 11.2 备份
- 数据库定期备份
- 临时文件清理策略
- 配置文件版本管理

### 11.3 更新
- 支持热更新（开发模式）
- 数据库自动升级（Hibernate ORM）
- 零停机部署

---

## 12. 附录

### 12.1 术语表
- **Inventory**：Ansible清单，包含目标主机列表
- **Playbook**：Ansible剧本，定义自动化任务
- **Template**：任务模板，关联项目和清单
- **Task**：任务，模板的实例化执行
- **Dashboard**：资源预览页面，显示统计信息

### 12.2 参考资料
- [Vaadin Flow文档](https://vaadin.com/docs/flow)
- [Quarkus文档](https://quarkus.io/guides/)
- [Ansible文档](https://docs.ansible.com/)
- [Hibernate Panache](https://quarkus.io/guides/hibernate-orm-panache)

---

## 文档版本历史

| 版本 | 日期       | 作者  | 说明                   |
| ---- | ---------- | ----- | ---------------------- |
| 1.0  | 2025-12-31 | Cline | 初始版本，完整需求文档 |
