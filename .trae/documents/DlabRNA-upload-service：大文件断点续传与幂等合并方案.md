## 目标

* 在 `DlabRNA-upload-service` 内实现大文件分片上传、断点续传、幂等合并和最终一致性。

* 三段状态机：`上传中 → 合并中 → 已完成`，支持失败与取消旁路状态，保证幂等与可恢复。

* 当 MinIO 合并成功但 MySQL/Redis 更新失败时，通过接口重试 + 后台校准 + MQ 补偿实现最终一致。

* 引入 RabbitMQ 进行任务路由，使用多消费者 + 手动 ACK + DLQ，确保合并与校准任务稳定可控。

## 技术栈与依赖

* Spring Boot Web（已存在）。

* MinIO Java SDK（走 S3 Multipart 接口）。

* Redis（Lettuce，Spring Data Redis）。

* MySQL（Spring Data JPA 或 MyBatis，按平台规范选用）。

* RabbitMQ（spring-boot-starter-amqp）。

* Resilience4j（接口重试/熔断/隔离）。

## 领域模型

* `upload_task`（MySQL）：记录一次文件上传的主档案：`id/fileId、filename、size、chunkCount、chunkSize、sha256、uploadId、state、storagePath、created/updated、userId`。

* Redis：

  * `upload:{fileId}:meta`：JSON/Hash，保存 `uploadId、chunkCount、chunkSize、sha256、filename` 等。

  * `upload:{fileId}:parts`：存储分片完成清单（`Set`、`Bitmap` 或 `SortedSet`），含每个分片的 `ETag`。

  * `upload:{fileId}:state`：状态机当前值；用 Lua 原子校验并迁移状态。

  * `upload:{fileId}:lock`：分布式锁，防止并发合并。

* RabbitMQ 队列/交换机：

  * `upload.merge`（直连/主题）用于触发合并。

  * `upload.reconcile`（直连/主题）用于后台校准与补偿。

  * 配套 DLQ：`upload.merge.dlq`、`upload.reconcile.dlq`。

## 状态机设计

* 主状态：`UPLOADING → MERGING → COMPLETED`。

* 旁路状态：`FAILED`、`CANCELLED`。

* 迁移规则（Lua 保证原子）：

  * `INIT/UPLOADING` 仅当分片覆盖率达到 100% 才允许进入 `MERGING`。

  * `MERGING` 只能进入一次；重复触发直接幂等返回。

  * MinIO 完成后先落地“合并成功”的事实事件，再推动状态到 `COMPLETED`（见一致性）。

## 端到端流程

* 初始化：客户端 `POST /upload/init`，服务生成 `fileId` 与 MinIO `uploadId`，持久化到 MySQL 与 Redis。

* 分片上传：客户端 `PUT /upload/{fileId}/chunk/{index}`，服务器上传至 MinIO `UploadPart`，写入 `parts`（含 `ETag`），状态保持 `UPLOADING`。

* 触发合并：客户端/后台 `POST /upload/{fileId}/merge`，状态机从 `UPLOADING`→`MERGING`，投递 `upload.merge` 任务。

* 合并执行（消费者）：汇总 `ETag` 完成 `CompleteMultipartUpload`；随后更新 MySQL/Redis；失败进入补偿。

* 查询进度：`GET /upload/{fileId}/status` 返回状态与分片完成度。

* 取消/清理：`POST /upload/{fileId}/abort` 触发 `AbortMultipartUpload`，清理 Redis/MySQL 记录。

## 分片上传

* 推荐分片大小：`5–32MB`（S3 规格兼容）。

* 服务器直传 MinIO：避免落盘，使用 `UploadPart` 流式上传；保存返回 `ETag`。

* 进度标记：Redis `Bitmap`/`Set` 标记已完成分片，重复分片直接幂等覆盖（按 `index`）。

* 去重：若客户端提供 `sha256`，发现同名同校验文件可短路复用（秒传）。

## 合并与幂等

* 合并触发使用 Redis Lua：

  * 校验 `state==UPLOADING` 且 `parts` 覆盖率 100%；原子地将 `state→MERGING` 并写入一次性 `mergeToken`。

* 合并执行：

  * 仅持有 `mergeToken` 的任务允许执行 `CompleteMultipartUpload`，避免并发重复。

  * 结果落地：写“合并成功事件”（MQ）+ 更新 MySQL/Redis；重复合并请求直接返回合并结果（读 Redis/MySQL）。

## 一致性与补偿

* 事实优先：以 MinIO 合并结果为事实源。

* 失败场景：MinIO 合并成功，但更新 MySQL/Redis 失败。

* 补偿策略：

  * 接口重试：合并消费者对 DB/Redis 更新使用 `@Retryable`/Resilience4j（指数退避、最大重试）。

  * 事务外盒（Outbox）：在本地事务记录事件表或用 MQ Publisher Confirm，保证事件可靠投递；消费者幂等更新（基于 `fileId` 唯一约束）。

  * 后台校准：周期性扫描 `MERGING` 且 MinIO 已存在合并对象的任务，投递 `upload.reconcile`，补全 MySQL/Redis 并将状态推进到 `COMPLETED`。

  * DLQ：无法处理的消息进入 DLQ，定时回放或人工介入。

## 消息与任务路由

* Exchange：`upload.ex`（`topic`）。

* RoutingKey：`upload.merge.{fileId}`、`upload.reconcile.{fileId}`。

* 多消费者：合并/校准分别使用并发容器（如 `concurrency=4–16`）。

* 手动 ACK：成功后 `basicAck`，失败 `basicNack(requeue=false)` 进入 DLQ。

* 幂等：消费者按 `fileId` 获取分布式锁或用去重表，避免重复更新。

## 负载均衡与扩展性

* API 层通过网关/负载均衡（如 Nginx/SCG）分发。

* Redis 作为中心态与进度缓存，多实例共享。

* MinIO 部署支持多节点；分片并行上传可水平扩展；合并由 MQ 控制速率。

* 防热点：路由键按 `fileId` 做一致性哈希分组，减少同一任务跨消费者迁移。

## 接口清单

* `POST /upload/init`：请求 `filename、size、chunkSize、sha256`，响应 `fileId、uploadId`。

* `PUT /upload/{fileId}/chunk/{index}`：请求体为分片数据；响应 `ETag`。

* `POST /upload/{fileId}/merge`：触发合并（可幂等调用）。

* `GET /upload/{fileId}/status`：返回 `state、doneChunks/totalChunks、percent`。

* `POST /upload/{fileId}/abort`：取消上传并清理。

## 配置项（application.yml 增补）

* MinIO：`endpoint、accessKey、secretKey、bucket`。

* Redis：`host/port/pool`。

* MySQL：`datasource`。

* RabbitMQ：`host/port/vhost/username/password`、消费者并发与重试参数、DLQ。

* 业务：分片大小上下限、合并并发、校准调度频率。

## 测试与验证

* 单元测试：状态机迁移（Lua 脚本）、分片完成度计算、幂等校验。

* 集成测试：MinIO Multipart 合并、消费者手动 ACK、DLQ 回退、补偿生效。

* 压测：大文件（>20GB）并发上传，观察 Redis/MinIO/QPS 与延迟。

* 演练：模拟 MinIO 成功但 DB/Redis 失败，验证最终一致性恢复。

## 风险与防护

* 大文件内存压力：流式上传，限制并发与分片大小。

* 重复分片：按 `index` 幂等覆盖，校验 `ETag/size`。

* 恶意/异常：签名校验、大小限制、速率限制、WAF/Gateway 限流。

* 清理：超时未完成的 `uploadId` 定时 `Abort` 与回收 Redis 键。

## 交付

* 我将按以上方案为该模块补齐依赖、配置、存储模型、控制器/服务、MQ 消费者与校准任务，并提供完整测试与验证脚本。请确认方案后开始实施。

