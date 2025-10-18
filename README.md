# DlabRNA-platform

RNA-小分子结合数据管理、预测与智能分析平台

## 项目架构

本项目采用 Spring Boot 3 + Spring Cloud 2023 + Maven 多模块结构，通过微服务架构实现RNA–小分子结合数据的管理、预测与智能分析。

### 模块说明

- **DlabRNA-common**: 通用模块（工具类、实体DTO、异常处理、通用配置）
- **DlabRNA-config-service**: 配置中心（Spring Cloud Config Server）
- **DlabRNA-registry-service**: 注册中心（Eureka）
- **DlabRNA-gateway-service**: API 网关（Spring Cloud Gateway）
- **DlabRNA-user-service**: 用户与权限服务（注册、登录、JWT认证）
- **DlabRNA-data-service**: 数据服务（RNA–小分子结合数据的查询、检索与展示）
- **DlabRNA-upload-service**: 上传服务（实验/预测数据上传与存储）
- **DlabRNA-ai-service**: AI 预测服务（深度学习模型接口、任务调度）
- **DlabRNA-agent-service**: 智能Agent服务（自然语言交互与AI工具协调）
- **DlabRNA-admin-service**: 管理与监控服务（Spring Boot Admin）

## 技术栈

- **基础框架**: Spring Boot 3.2.0, Spring Cloud 2023.0.0
- **服务注册与发现**: Spring Cloud Netflix Eureka
- **配置中心**: Spring Cloud Config
- **API网关**: Spring Cloud Gateway
- **安全认证**: Spring Security, JWT
- **数据库访问**: MyBatis-Plus
- **数据库**: MySQL
- **工具库**: Lombok, Hutool
- **API文档**: Knife4j (基于Swagger)
- **对象映射**: MapStruct

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+

### 构建与运行

1. 克隆项目
```bash
git clone https://github.com/yourusername/DlabRNA-platform.git
cd DlabRNA-platform
```

2. 编译项目
```bash
mvn clean package -DskipTests
```

3. 启动服务（按顺序）
```bash
# 1. 启动注册中心
java -jar DlabRNA-registry-service/target/DlabRNA-registry-service-1.0.0-SNAPSHOT.jar

# 2. 启动配置中心
java -jar DlabRNA-config-service/target/DlabRNA-config-service-1.0.0-SNAPSHOT.jar

# 3. 启动网关
java -jar DlabRNA-gateway-service/target/DlabRNA-gateway-service-1.0.0-SNAPSHOT.jar

# 4. 启动其他业务服务
java -jar DlabRNA-user-service/target/DlabRNA-user-service-1.0.0-SNAPSHOT.jar
java -jar DlabRNA-data-service/target/DlabRNA-data-service-1.0.0-SNAPSHOT.jar
# ... 启动其他服务
```

## 服务访问

- 注册中心: http://localhost:8761
- 配置中心: http://localhost:8888
- API网关: http://localhost:8080
- 管理控制台: http://localhost:8090

## 开发指南

### 添加新服务

1. 在父项目中添加新模块
2. 配置pom.xml，继承父项目
3. 添加必要的依赖
4. 实现业务逻辑
5. 在网关中配置路由

### 配置管理

所有服务的配置文件存储在Git仓库中，通过配置中心统一管理。

## 贡献指南

欢迎提交Issue和Pull Request。

## 许可证

[MIT License](LICENSE)