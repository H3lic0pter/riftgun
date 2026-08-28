# 发布后依赖与功能加固清单

审查范围：`cfc26e47d95d7f7abf1800a225f05987800d8f07..53d4c57`

发布标签：`mc1.21.1-v0.1.1-r1`、`mc26.1.2-v0.1.1-r1`

本清单保留 Standards 与 Spec 两条审查轴。重复问题仍分别列出，但共享同一份实现与验收记录。

状态：`[ ]` 待处理，`[x]` 已实现并验证。

## Standards

- [x] **S1 P1 — 坐标分享状态跨 server session 残留。**
  - 风险：chat share 与 cooldown 使用 server tick 计时，却存放在 process-global Map 中。
  - 处理：分享和导入时同时清理过期 share/cooldown；server stop 时清空两张 Map。
  - 验收：`CoordinateSharingLifecycleSourceTest`、双版本完整构建。
- [x] **S2 P1 — optional 地图 adapter 的 linkage failure 可逃逸兼容边界。**
  - 风险：adapter 构造原先位于保护区之外。
  - 处理：adapter 构造、读取与 catalog normalization 共用一个 `LinkageError | RuntimeException` 边界；不兼容来源自动隐藏且每次会话只警告一次；JourneyMap metadata 限制到使用 API v2 的 JourneyMap 6 系列。
  - 验收：`MapIntegrationHardeningSourceTest`、双版本完整构建。
- [x] **S3 P2 — JourneyMap clean state 每个 client tick 都产生分配。**
  - 风险：没有变化时仍以 20 Hz 构建 dimension Set 并扫描 selection。
  - 处理：两个版本均在任何分配和 selection reconciliation 之前检查 dirty bit。
  - 验收：`MapIntegrationHardeningSourceTest`、源码检查。
- [x] **S4 P2 — 轮盘射程滑块以 10 Hz 回传完整 snapshot。**
  - 风险：每个中间值都会序列化全部玩家坐标与 dimension labels。
  - 处理：中间值改用仅包含当前传送枪状态的 `GunSnapshot`；完整 snapshot 额外对 dimension ID 去重。
  - 验收：`RadialRangeSnapshotSourceTest`、双版本完整构建。
- [x] **S5 P2 — REMOTE 图标透明边界不居中。**
  - 风险：不对称 padding 会导致图标产生视觉偏移。
  - 处理：并行重绘后的 alpha bounds 为居中的 `x=1..14`、`y=3..12`；箭头与目标框构图本身允许比 SMART 更宽。
  - 验收：`PlacementModeSpriteAlignmentTest`、26.1.2 GUI scale 2/4 radial capture。
- [x] **S6 P3 — 画笔 optical correction 使用未说明的 magic offset。**
  - 风险：渲染坐标契约不清晰，两个版本容易漂移。
  - 处理：两个版本统一使用具备注释的 `EDIT_OPTICAL_X/Y` 常量。
  - 验收：源码检查、双版本完整构建。
- [x] **S7 P3 — 版本专属 GUI 副本存在 shotgun-surgery 风险。**
  - 风险：`PortalConfigScreen` 与 `ModeRadialScreen` 的行为修复容易只改一个版本。
  - 处理：延迟敏感的轮盘状态提取为共享 `RadialRequestState`；`VersionedScreenParityTest` 强制两个具体 Screen 的 network action 和 translation contract 保持一致。Minecraft GUI method signatures 不同的渲染代码继续留在版本 adapter 中。
  - 验收：`RadialRequestStateTest`、`VersionedScreenParityTest`、双版本完整构建。

## Spec

- [x] **P1 P1 — 卸下 Remote Module 会破坏保存的 REMOTE 偏好。**
  - 要求：卸载时只在运行时回退 FRONT，重装后恢复原偏好。
  - 处理：卸载不再写玩家数据；坐标传送和配对传送均在运行时把不可用 REMOTE 解析为 FRONT。
  - 验收：`PortalGunCapabilitiesTest`、`RemoteModulePreferenceSourceTest`。
- [x] **P2 P1 — 长按键在 server response 前松开会丢失操作。**
  - 要求：长按原模式键必须可靠进入轮盘。
  - 处理：达到长按阈值后本地立即打开轮盘；交互等待带 request ID 的 server acknowledgement；响应前松键会在确认后提交当前悬浮项。拒绝响应会关闭轮盘且不发送陈旧设置。
  - 验收：`RadialRequestStateTest`、版本 parity test、双版本完整构建。
- [x] **P3 P1 — JourneyMap 缺少 primary dimension 时可能在排序阶段 NPE。**
  - 要求：无法解析的维度应禁用条目并显示原因，而不是崩溃。
  - 处理：nullable ID/name/group 在排序前统一 normalization；缺失维度变为空字符串并标记为 unavailable。
  - 验收：`ClientExternalDestinationCatalogTest.missingDimensionsRemainVisibleAndSortWithoutCrashing`。
- [x] **P4 P1 — JourneyMap compatibility failure 未始终 graceful degradation。**
  - 要求：隐藏对应分组并且每次会话只警告一次。
  - 共享处理：S2。
  - 验收：S2 compatibility boundary 与 metadata 检查。
- [x] **P5 P1 — 配置的 chat-share 有效期没有限定在 server session 内。**
  - 要求：token 有效期只使用签发它的 server session 时钟。
  - 共享处理：S1。
  - 验收：S1 lifecycle reset 与 pruning 检查。

## 构建验收

- [x] `git diff --check`
- [x] `./gradlew.bat :1.21.1:test :26.1.2:test`
- [x] `./gradlew.bat build`
- [x] 26.1.2 GUI scale 2 radial capture
- [x] 26.1.2 GUI scale 4 radial capture

## 环境备注

- 1.21.1 GUI capture profile 中的 Sodium 要求 NeoForge `21.1.219+`，而项目运行配置是 `21.1.140`，因此该 profile 在 RiftGun 初始化前被 loader 拒绝。没有擅自移动 `run/mods` 中的用户文件；实际图标 scale 验收改由依赖匹配的 26.1.2 profile 完成。
