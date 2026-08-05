# 同步开门、懒出口与 GUI 修复清单

> 旧异步预加载方案已完整移除。本文件保留原路径，避免丢失开发记录。

## 开门路由

- [x] GUI“生成传送门”和传送枪互动键共用唯一 server-side 开门入口。
- [x] GUI 始终使用 `FRONT`；互动键继续读取玩家保存的 placement mode。
- [x] 同维度沿用同步双门行为，不引入新的首实体直达逻辑。
- [x] 跨维度且目标处于 `entity-ticking` 时同步生成双门。
- [x] 跨维度且目标未处于 `entity-ticking` 时只生成入口，不预加载目标区块。
- [x] 未加载目标的首个合法实体树进入后，通过 `DimensionTransition` 直接抵达保存坐标；随后在目标侧检测并生成出口。
- [x] 首个实体使用保存 yaw、保留当前 pitch，并清零速度。
- [x] 出口生成后，入口和出口获得完整 3 秒开放时间。
- [x] 出口 transit gate 预登记首个实体树；实体必须离开触发区后才能再次传送。
- [x] 玩家、掉落物、空载具及载人载具共用该路径。
- [x] horizontal 出口在目标加载后检查支撑和空间；不满足时回退 2×1 vertical 出口。
- [x] 入口生成成功时立即扣燃料；出口失败不退款，也不回滚已完成的实体传送。
- [x] lazy 入口将地点 ID、维度、坐标和 yaw 写入 NBT；重载后可继续剩余生命周期。
- [x] 仅已生成的 portal entity 持有 chunk ticket；不存在 prepare ticket、pending request 或 loading timeout。

## 已删除的异步机制

- [x] 删除 `CompletableFuture` / background executor chunk load。
- [x] 删除 `PENDING`、request token、latest-wins 和 server tick 轮询。
- [x] 删除 `DestinationLoadPolicy`、`DestinationLoadTimeline` 及其测试。
- [x] 删除 `CANCEL_PORTAL_OPEN`、loading response 和灰色 pending button 状态。
- [x] 删除“正在加载目的地”、超时和异步加载失败文案。

## Safety 与 GUI

- [x] 详情面板只显示名称、分组、维度和坐标。
- [x] 删除 unsafe 二次确认；不安全仅发 action-bar，仍然开门。
- [x] 同维度只在真实开门时执行 Safety inspector。
- [x] 跨维度完全跳过 Safety inspector，并清除该地点的旧结果。
- [x] 关闭 Safety 后完全跳过 inspector 并隐藏 `!`。
- [x] 保留分组左右键切换、下拉列表遮罩和“生成传送门”按钮修复。

## 自动验证

- [x] `PortalOpenRouteTest`：只有未加载的跨维度目标使用 lazy 出口。
- [x] `PortalExitTargetTest`：lazy 目标 NBT round-trip。
- [x] `PortalPairClockTest`：出口完成后从 open tick 0 重新获得完整时长。
- [x] `PortalTransitGateTest`：抵达后禁止立即返回，失败后允许重试。
- [x] `gradle clean test build`。
- [ ] 真机：未加载 Overworld chunk 的跨维度首实体直达。
- [ ] 真机：Overworld → Nether 与 Nether → Overworld。
- [ ] 真机：玩家、掉落物、空载具、载人载具跨维度。
- [ ] 真机：服务器在 lazy 入口存活期间保存并重载。

> Splash effect 已冻结。本轮未修改粒子数量、边缘采样、时序、速度、重力、寿命、方向、贴图、renderer 或 RGB provider。
