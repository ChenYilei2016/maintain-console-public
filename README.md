# 数据库准备

# manager启动 修改点

## 环境

manager.pom active profiles  
prod: nacos、mysql
local:  sqllite

## 配置

application-local.properties : 本地调用环境  
application-prod.properties : 生产配置

## 数据库

1. 表生成 , sqllite or mysql 等
2. 配置修改: spring.datasource.url=jdbc:sqlite:manager/src/main/resources/sqlite/maintain-manager.sqlite

## 登录态接入

cn.chenyilei.maintain.manager.controller.LoginController.getLoginInfo
cn.chenyilei.maintain.manager.context.LoginUserContext

# 客户端接入修改点

## 依赖

```
<dependency>
    <groupId>cn.chenyilei</groupId>
    <artifactId>maintain-console-client-http-starter</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>

   <!--    上报客户端信息    -->
        <dependency>
            <groupId>cn.chenyilei</groupId>
            <artifactId>maintain-console-client-registry-starter</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
```

## 配置

maintain.console.enabled=true

# Maintain Console 脚本远程调用平台

## 项目概述

Maintain Console 是一个基于Spring Boot和Spring
Cloud的分布式运维管理平台，为企业提供统一的远程脚本执行和系统维护能力。通过可视化Web界面，运维人员可以对多个分布式应用进行统一管理和脚本执行，极大提升运维效率和系统可维护性。

### 项目定位

- **分布式系统运维管理平台**：统一管理企业内多个微服务应用
- **远程脚本执行引擎**：支持Groovy脚本和通用命令的远程执行
- **可视化运维控制台**：提供直观的Web界面进行运维操作

## 技术架构

### 系统架构图

```mermaid
graph TB
    subgraph "Web层"
        A[Web管理界面<br/>Vue3 + Thymeleaf]
    end

    subgraph "Manager应用"
        B[管理控制器<br/>ManagerController]
        C[脚本管理服务<br/>ScriptService]
        D[目录管理服务<br/>DirectoryService]
        E[客户端调用器<br/>ClientCaller]
        F[服务发现<br/>RegistryDiscovery]
    end

    subgraph "数据层"
        G[SQLite数据库<br/>脚本/历史/目录]
    end

    subgraph "注册中心"
        H[Nacos<br/>服务注册与发现]
    end

    subgraph "客户端应用群"
        I[应用A<br/>Client SDK]
        J[应用B<br/>Client SDK]
        K[应用C<br/>Client SDK]
    end

    A --> B
    B --> C
    B --> D
    B --> E
    C --> G
    D --> G
    E --> F
    F --> H
    E --> I
    E --> J
    E --> K
    I --> H
    J --> H
    K --> H
    style A fill: #e1f5fe
    style B fill: #f3e5f5
    style C fill: #f3e5f5
    style D fill: #f3e5f5
    style E fill: #f3e5f5
    style F fill: #f3e5f5
    style G fill: #fff3e0
    style H fill: #e8f5e8
    style I fill: #fff9c4
    style J fill: #fff9c4
    style K fill: #fff9c4
```

### 技术栈

- **后端框架**：Spring Boot 2.3.12, Spring Cloud Hoxton.SR12
- **数据库**：SQLite (嵌入式数据库), MYSQL
- **ORM框架**：MyBatis-Plus
- **服务发现**：Nacos Discovery
- **前端技术**：HTML, Thymeleaf
- **通信协议**：HTTP, Retrofit2
- **脚本引擎**：Groovy
- **安全机制**：RSA数字签名

### 模块设计

#### 1. Manager模块（管理端）

- **Web控制层**：提供RESTful API和页面控制
- **业务服务层**：脚本管理、目录管理、执行历史等业务逻辑
- **数据访问层**：基于MyBatis-Plus的数据持久化
- **客户端调用层**：封装对客户端应用的远程调用

#### 2. Client SDK模块（客户端）

- **Common模块**：公共API接口和DTO定义
- **Registry Starter**：服务注册集成组件
- **HTTP Starter**：HTTP通信支持组件
- **Groovy Support Starter**：Groovy脚本执行组件

#### 3. 通信机制

- **服务发现**：基于Nacos的自动服务发现
- **负载均衡**：Spring Cloud LoadBalancer
- **安全认证**：RSA签名验证机制
- **协议支持**：HTTP RESTful API

## 部署方案

### 环境要求

- **JDK版本**：1.8
- **数据库**：SQLite（内置）, MYSQL
- **注册中心**：Nacos 1.4.0+
- **内存要求**：Manager应用 512MB+，Client应用 256MB+

### 部署架构

```mermaid
graph TB
    subgraph "生产环境"
        subgraph "Nacos集群"
            N1[Nacos-1]
            N2[Nacos-2]
            N3[Nacos-3]
        end

        subgraph "Manager集群"
            M1[Manager-1<br/>主节点]
            M2[Manager-2<br/>备用节点]
        end

        subgraph "业务应用集群"
            A1[应用A-实例1]
            A2[应用A-实例2]
            B1[应用B-实例1]
            B2[应用B-实例2]
        end

        subgraph "负载均衡"
            LB[Nginx/SLB]
        end
    end

    LB --> M1
    LB --> M2
    M1 --> N1
    M2 --> N1
    A1 --> N1
    A2 --> N1
    B1 --> N1
    B2 --> N1
    style N1 fill: #e8f5e8
    style N2 fill: #e8f5e8
    style N3 fill: #e8f5e8
    style M1 fill: #e1f5fe
    style M2 fill: #e1f5fe
    style LB fill: #fff3e0
```
