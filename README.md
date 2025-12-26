# Vaadin Flow 和 Quarkus 项目基础模板

base-starter-flow-quarkus-24

本项目可以作为创建您自己的 Vaadin Flow 和 Quarkus 应用程序的起点。它包含了所有必要的配置和一些占位文件，以帮助您开始开发。

Quarkus 3.0+ 需要 Java 17。

此启动器也提供 [Gradle](https://github.com/vaadin/base-starter-flow-quarkus/tree/gradle) 版本

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