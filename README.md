这是一个基于 Spring Boot 3、MyBatis-Plus、JWT、MySQL 和 Redis 的微服务项目，主要功能包括用户管理、预约管理、房间与座位管理、人脸识别、统计分析等模块。

## 仓库简介

智慧图书馆座位管理系统围绕“预约—签到—统计”的完整链路搭建微服务架构，为高校或公共图书馆提供可扩展的座位运营能力。

- **核心流程**：用户通过网关完成注册与登录，创建或取消座位预约，现场通过二维码或人脸识别完成签到；系统实时同步座位状态并产出运营数据。
- **服务拆分**：预约、房间/座位、人脸识别、统计分析、用户、网关、注册中心等服务按职责拆分，支持弹性部署与独立扩展。
- **实时与安全**：利用 WebSocket 推送座位状态，结合 JWT、Spring Security 与网关统一鉴权，同时依托 Redis 提供会话、验证码及热点数据缓存。
- **开放互通**：通过 Feign/OpenFeign 实现服务间调用，Nacos/Eureka 负责注册发现；对外暴露 RESTful API，便于前端与第三方系统接入。

下文包含详细的技术栈、模块说明、启动方式与配置指南，便于快速上手与二次开发。

## 技术栈

- **Spring Boot 3**
- **MyBatis-Plus**
- **JWT (JSON Web Token)**
- **MySQL**
- **Redis**
- **Nacos**
- **Eureka Server**
- **WebSocket**
- **Feign Client**
- **OpenFeign**
- **Spring Security**
- **Spring Cloud Gateway**
- **Spring Data REST**
- **Spring WebFlux**
- **Apache POI**
- **Gson**
- **Jackson**
- **OkHttp**
- **RedisTemplate**
- **JavaMailSender**
- **Excel 操作（用于导入导出）**

## 项目结构

该项目由多个模块组成，每个模块负责不同的功能：

### 1. `common-module`

- **功能**: 提供通用配置、工具类、异常处理、DTO、PO、VO、枚举等。
- **关键类**:
  - `CommonSecurityConfig`: 通用安全配置。
  - `GsonConfig`: Gson 序列化配置。
  - `MyBatisPlusConfig`: MyBatis Plus 分页配置。
  - `GlobalExceptionHandler`: 全局异常处理。
  - `JwtAuthenticationFilter`: JWT 认证过滤器。
  - `UserSession`: 用户会话信息。
  - `JwtUtil`: JWT 工具类。
  - `ResultView`: 统一返回结果。
  - `PageResponseVO`: 分页响应封装。
  - `UserLoginVO`: 登录返回封装。
  - `ReservationVO`: 预约信息封装。
  - `RoomDTO`: 房间信息 DTO。
  - `UpdateProfileDTO`: 用户信息更新 DTO。
  - `UserRegisterDTO`: 用户注册 DTO。
  - `UserLoginDTO`: 用户登录 DTO。
  - `ReservationStatus`: 预约状态枚举。
  - `TimeSlot`: 时间段枚举。
  - `ClassInfo`: 班级信息实体。
  - `College`: 学院信息实体。
  - `FaceFeature`: 人脸特征实体。
  - `FaceRecognitionLog`: 人脸识别日志实体。
  - `Reservation`: 预约信息实体。
  - `Room`: 房间信息实体。
  - `Seat`: 座位信息实体。
  - `User`: 用户信息实体。
  - `UserMapper`: 用户数据库操作接口。
  - `ReservationMapper`: 预约数据库操作接口。
  - `RoomMapper`: 房间数据库操作接口。
  - `SeatMapper`: 座位数据库操作接口。
  - `ClassInfoMapper`: 班级数据库操作接口。
  - `CollegeMapper`: 学院数据库操作接口。
  - `FaceFeatureMapper`: 人脸特征数据库操作接口。
  - `FaceRecognitionLogMapper`: 人脸识别日志数据库操作接口。

### 2. `eureka-server`

- **功能**: 提供服务注册与发现功能。
- **关键类**:
  - `EurekaServerApplication`: 启动类，启用 Eureka Server。

### 3. `face-service`

- **功能**: 提供人脸识别功能，支持百度 AI 和 Face++。
- **关键类**:
  - `FaceServiceApplication`: 启动类。
  - `BaiduFaceClient`: 百度人脸识别客户端。
  - `FaceppClient`: Face++ 人脸识别客户端。
  - `BaiduFaceProperties`: 百度人脸识别配置。
  - `FaceppProperties`: Face++ 人脸识别配置。
  - `SecurityConfig`: 安全配置。
  - `WebSocketConfig`: WebSocket 配置。
  - `WebSocketContainerConfig`: WebSocket 容器配置。
  - `FaceController`: 人脸识别接口。
  - `FaceRegisterDTO`: 人脸注册 DTO。
  - `RecognitionResult`: 识别结果封装。
  - `FaceRecognitionService`: 人脸识别服务接口。
  - `FaceRecognitionServiceImpl`: 人脸识别服务实现。
  - `RecognizeWebSocketHandler`: WebSocket 处理器。

### 4. `gateway-service`

- **功能**: 提供 API 网关功能，负责路由、认证、限流等。
- **关键类**:
  - `GatewayApplication`: 启动类。
  - `SecurityConfig`: 安全配置。

### 5. `reservation-service`

- **功能**: 提供预约管理功能，包括预约、取消、签到等。
- **关键类**:
  - `ReservationServiceApplication`: 启动类。
  - `SecurityConfig`: 安全配置。
  - `WebSocketConfig`: WebSocket 配置。
  - `WebSocketContainerConfig`: WebSocket 容器配置。
  - `ReservationController`: 预约接口。
  - `ReservationService`: 预约服务接口。
  - `ReservationServiceImpl`: 预约服务实现。
  - `SeatWebSocketHandler`: WebSocket 处理器。
  - `SeatStatusMessage`: 座位状态消息封装。

### 6. `room-service`

- **功能**: 提供房间与座位管理功能，包括房间信息、座位信息、二维码生成等。
- **关键类**:
  - `RoomServiceApplication`: 启动类。
  - `SecurityConfig`: 安全配置。
  - `RoomController`: 房间接口。
  - `SeatController`: 座位接口。
  - `RoomService`: 房间服务接口。
  - `SeatService`: 座位服务接口。
  - `RoomServiceImpl`: 房间服务实现。
  - `SeatServiceImpl`: 座位服务实现。

### 7. `statistics-service`

- **功能**: 提供统计分析功能，包括预约趋势、房间使用率、签到率等。
- **关键类**:
  - `StatisticsServiceApplication`: 启动类。
  - `StatisticsController`: 统计接口。
  - `ReservationMapper`: 预约数据库操作接口。
  - `UserMapper`: 用户数据库操作接口。
  - `CheckinConversionDTO`: 签到率 DTO。
  - `DeptClassStatisticsDTO`: 部门班级统计 DTO。
  - `OverviewStatisticsDTO`: 概览统计 DTO。
  - `RoomTimeSlotHeatDTO`: 房间时间段热度 DTO。
  - `RoomUsageRankDTO`: 房间使用排名 DTO。
  - `SeatUsageDTO`: 座位使用 DTO。
  - `TrendPointDTO`: 趋势点 DTO。
  - `ViolationCancelTrendDTO`: 违规取消趋势 DTO。
  - `StatisticsService`: 统计服务接口。
  - `StatisticsServiceImpl`: 统计服务实现。

### 8. `user-service`

- **功能**: 提供用户管理功能，包括注册、登录、信息更新、密码修改等。
- **关键类**:
  - `UserServiceApplication`: 启动类。
  - `SecurityConfig`: 安全配置。
  - `ClassInfoController`: 班级信息接口。
  - `CollegeController`: 学院信息接口。
  - `UserController`: 用户接口。
  - `ClassInfoService`: 班级服务接口。
  - `CollegeService`: 学院服务接口.
  - `UserService`: 用户服务接口.
  - `ClassInfoServiceImpl`: 班级服务实现.
  - `CollegeServiceImpl`: 学院服务实现.
  - `UserServiceImpl`: 用户服务实现.
  - `EmailCodeService`: 邮箱验证码服务.
  - `ClassImportDTO`: 班级导入 DTO.
  - `CollegeImportDTO`: 学院导入 DTO.
  - `StudentImportDTO`: 学生导入 DTO.
  - `TeacherImportDTO`: 教师导入 DTO.

## 启动方式

1. **启动 Nacos**: 确保 Nacos 服务已启动。
2. **启动 Eureka Server**: 运行 `eureka-server` 模块。
3. **启动各个微服务**: 依次运行 `face-service`, `gateway-service`, `reservation-service`, `room-service`, `statistics-service`, `user-service` 模块。
4. **访问 API**: 通过网关访问各个服务的 API。

## 依赖

- **MySQL**: 用于存储用户、预约、房间、座位等数据。
- **Redis**: 用于缓存 JWT Token、验证码等。
- **Nacos**: 用于服务注册与发现。
- **Eureka Server**: 用于服务注册与发现。
- **JavaMailSender**: 用于发送邮件验证码。
- **OkHttp**: 用于调用第三方人脸识别 API。
- **Gson**: 用于 JSON 序列化与反序列化。
- **Jackson**: 用于 JSON 序列化与反序列化。
- **RedisTemplate**: 用于 Redis 操作。
- **Apache POI**: 用于 Excel 导入导出。
- **JavaMailSender**: 用于发送邮件验证码。
- **Spring Security**: 用于安全认证与授权。
- **Spring WebFlux**: 用于响应式编程。
- **Spring Data REST**: 用于 RESTful API 生成。
- **Spring Cloud Gateway**: 用于 API 网关。
- **OpenFeign**: 用于服务间通信。
- **Feign Client**: 用于服务间通信。

## 配置

- **application.yml**: 各个模块的配置文件，包括数据库连接、Redis 连接、Nacos 配置等。
- **SecurityConfig**: 安全配置，包括 JWT 认证、CORS 配置等。
- **WebSocketConfig**: WebSocket 配置。
- **WebSocketContainerConfig**: WebSocket 容器配置。
- **BaiduFaceProperties**: 百度人脸识别配置。
- **FaceppProperties**: Face++ 人脸识别配置。
- **EmailCodeService**: 邮箱验证码服务配置。

## 使用说明

- **用户管理**: 用户可以注册、登录、更新信息、修改密码等。
- **预约管理**: 用户可以预约座位、取消预约、签到等。
- **房间与座位管理**: 管理员可以管理房间与座位信息。
- **人脸识别**: 支持通过人脸识别进行签到。
- **统计分析**: 提供预约趋势、房间使用率、签到率等统计信息。
- **WebSocket**: 实时更新座位状态。

## 注意事项

- **JWT Token**: 用户登录后会返回 JWT Token，后续请求需要携带该 Token。
- **Redis**: 用于缓存 JWT Token、验证码等。
- **Nacos**: 用于服务注册与发现。
- **Eureka Server**: 用于服务注册与发现。
- **JavaMailSender**: 用于发送邮件验证码。
- **OkHttp**: 用于调用第三方人脸识别 API。
- **Gson**: 用于 JSON 序列化与反序列化。
- **Jackson**: 用于 JSON 序列化与反序列化。
- **RedisTemplate**: 用于 Redis 操作。
- **Apache POI**: 用于 Excel 导入导出。
- **JavaMailSender**: 用于发送邮件验证码。
- **Spring Security**: 用于安全认证与授权。
- **Spring WebFlux**: 用于响应式编程。
- **Spring Data REST**: 用于 RESTful API 生成。
- **Spring Cloud Gateway**: 用于 API 网关。
- **OpenFeign**: 用于服务间通信。
- **Feign Client**: 用于服务间通信。

## 版本

- **Spring Boot**: 3.x
- **MyBatis-Plus**: 3.x
- **JWT**: 0.9.x
- **MySQL**: 8.x
- **Redis**: 6.x
- **Nacos**: 2.x
- **Eureka Server**: 3.x
- **WebSocket**: Spring WebSocket
- **OpenFeign**: 3.x
- **Feign Client**: 3.x
- **Spring Security**: 5.x
- **Spring WebFlux**: 5.x
- **Spring Data REST**: 5.x
- **Apache POI**: 5.x
- **Gson**: 2.8.x
- **Jackson**: 2.13.x
- **OkHttp**: 4.x
- **RedisTemplate**: Spring Data Redis
- **JavaMailSender**: Spring Mail

## 许可证

该项目使用 MIT 许可证。