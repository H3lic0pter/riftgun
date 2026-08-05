# Portal configuration GUI implementation checklist

> Minecraft 1.21.1 / NeoForge / Java 21。只有完成实现并得到相应验证后才勾选。

## 1. Domain and persistence

- [x] 统一 `Destination`：UUID、名称、分组、维度、double 坐标、yaw、时间戳、置顶状态。
- [x] 单层 `DestinationGroup`；内置且不可变的 `Default` 分组。
- [x] 玩家 NBT 持久化；logout、death、respawn 时保留。
- [x] 持久化当前目标、上次查看、排序、分组顺序、展开状态和个人设置。
- [x] 全局单调递增 `Location{number}` 默认名；删除后不复用编号。
- [x] Server config：256 个地点、32 个自定义分组、地点名 48 字符、分组名 32 字符。
- [x] 数据版本字段与 migration 入口。

## 2. Independent service seams

- [x] `PortalGunLocator`：vanilla inventory 实现；为饰品/扩展物品栏保留 registry。
- [x] `DestinationDimensionPolicy`：允许 server 已加载维度；跨维度能力由 fuel profile 决定。
- [x] `DestinationSafetyInspector`：仅为同维度真实开门执行 collision、support surface、hazard 检查；跨维度暂时完全跳过。
- [x] `SafeDestinationResolver`：identity 实现；为未来智能搜索保留 seam。
- [x] `PortalEntityEligibilityPolicy`：玩家、掉落物、载具；为升级模块保留组合 seam。
- [x] `PortalClosePolicy`：完全开启后固定 3 秒；为玩家策略保留 seam。

## 3. Server-authoritative operations

- [x] `G` 请求 GUI；server 验证 eligible gun 与非 Spectator。
- [x] 没有传送枪时 action-bar 提示。
- [x] 每次 mutation 与 portal action 都在 server 重新验证权限。
- [x] 从当前位置创建地点。
- [x] absolute 或 `~` relative 坐标创建；拒绝 `^`。
- [x] 重命名、编辑、换组、置顶、删除地点。
- [x] 创建、重命名、drag/↑↓ 排序、删除分组；删除后地点移入 `Default`。
- [x] 选择目标；relog 后保留，删除目标时清空。
- [x] GUI 生成 portal，同时选择该目标。
- [x] 右键 Portal Gun 打开当前目标；不存在 raycast 模式。
- [x] 新 portal 关闭旧 portal；完全打开 3 秒后关闭。
- [x] portal 存活时为入口和出口 chunk 各持有临时 ticket；关闭或异常移除时释放。
- [x] 开门 placement 与双实体创建成功后才原子扣除一次燃料；失败时燃料和旧 portal 均保持不变。

## 4. Networking

- [x] 带 protocol version 的 NeoForge payload registration。
- [x] C2S GUI、mutation、selection 与统一 portal action payload；不再发送浏览 Safety 请求。
- [x] S2C snapshot 与 portal-opened acknowledgement；validation 与 Safety 警告通过 action-bar 返回。
- [x] NBT codec packet 上限及 server-side 名称/坐标字段长度校验。

## 5. GUI structure and visual quality

- [x] Graphite / warm gray / ice blue theme tokens；绿色只用于 portal action。
- [x] 响应式 panel；宽布局和低分辨率收缩布局。
- [x] 分组与地点处于同一个 expandable tree；`Default` 固定第一。
- [x] 分组 drag reorder，并提供可聚焦的 ↑/↓ keyboard fallback。
- [x] 搜索名称、分组和坐标文本。
- [x] pinned-first；recent/name/created/distance 排序；选择由玩家数据记忆。
- [x] 右侧只读详情：名称、分组、友好维度名、简洁坐标；raw dimension ID tooltip。
- [x] 单击地点行即选中目标；底部只保留生成 portal，server 成功回执后关闭 GUI。
- [x] 独立 create-current、create-coordinate、edit modal；地点可在 modal 中换组。
- [x] 地点 modal 的名称/坐标输入增加纵向留白；X/Y/Z/Yaw 使用带独立标签的 2×2 布局。
- [x] 地点 modal 的分组选择支持键盘 ←/→ 前后切换，以及独立 ▼ 下拉列表。
- [x] dirty modal 关闭确认；玩家可独立关闭该确认。
- [x] 地点/分组删除确认；玩家可独立关闭该确认；unsafe destination 不再使用二次确认。
- [x] 地点列表 `!` 只读取上次真实开门的持久化 Safety 结果；位置改变才失效，改名/换组/置顶不失效。
- [x] 关闭 safety-check 后 server 完全跳过 inspector 并隐藏 `!`；历史结果保留，详情区始终不显示 Safety 文本。
- [x] 地点行右侧常驻黄星置顶按钮，hover/focus 时显示红色删除按钮；长名称省略并提供 tooltip。
- [x] 自定义分组行 hover/focus 时显示重命名和删除按钮；drag handle 与 Alt+↑/↓ 均可排序。
- [x] 地点名称左侧低干扰拖拽点；拖到分组标题或地点行即可换组，Default 可作为目标；换组只修改 `groupId` 并保留 Safety 历史与地点状态。
- [x] 右侧详情整体滚动，目标变化时归顶；底部生成 portal 固定，不参与滚动。
- [x] 打开任意二级 modal 后完全屏蔽背景主界面的 hover tooltip 与交互。
- [x] 空详情与 server action-bar 的 missing-gun、limit、validation 状态。
- [x] Mouse 与 Tab/Shift+Tab/Enter/Esc navigation。
- [x] 分组展开/收起与置顶重排使用短时平滑位移动画；关闭 animations 后立即更新。
- [x] Vanilla UI sounds；个人声音开关。
- [x] English 与 Simplified Chinese localization。
- [x] 底部紧凑 fuel gauge、bucket-mode 图标与清空图标；hover 显示 fluid、液量、溢出和模式。
- [x] 清空 fluid 使用独立二次确认；玩家可单独关闭该确认。
- [x] GUI 请求绑定打开界面时的具体枪槽位；枪被移动后拒绝操作，避免误改另一把枪。

## 6. Portal behavior and tooltip

- [x] Portal 出口使用保存的 feet position 与 yaw。
- [x] Safety 只警告；不偏移、不破坏、不拒绝开门。
- [x] Portal Gun tooltip：目标名、分组与维度；不再显示距离。
- [x] Portal Gun tooltip 显示 fluid 类型与液量；bucket mode 仅在开启时显示“开”。
- [x] Portal Gun 互动键和 GUI 遇到 unsafe 目标时只发 action-bar 警告并照常开门。
- [x] 玩家、掉落物、允许的 vehicle/passenger tree 可传送；mob 不可传送。
- [x] 保留水平 momentum 与 riding relationship。
- [x] `CHARGING` 前摇缩短为 6 ticks；opening/closing 各 5 ticks，完全开启后仍维持 3 秒。
- [x] `SMART` / `FRONT` / `SURFACE` 三种持久化 placement mode；`V` 默认循环切换。
- [x] GUI 底部 Open Portal 固定使用 `FRONT`，不受当前 placement mode 影响。
- [x] `FRONT` 最终路由支持 Downshot：pitch ≥ 78° 时在预测 feet 下方 2 blocks 生成浮空 1×1 TOP 门；入口按 TOP→BOTTOM 规则匹配出口。
- [x] 一级 GUI 提供个人持久化的动量预测图标开关（默认关）；关闭时不采样、不扫描物品栏、不保留 transient history。
- [x] 开启预测且携枪时保留 4 ticks server-side 位置历史；每 20 ticks 低频复查持枪状态，teleport、换维度、respawn 与 portal transit 后清空。
- [x] 预测到完全 OPEN 的 11 ticks：地面与普通空中侧向 FRONT 只使用近期 X/Z，当前 feet Y 固定；跳跃和普通坠落不会让侧向门升降。
- [x] Downshot 使用完整 X/Y/Z 空中轨迹；创造飞行、受控移动与 Levitation 的侧向 FRONT 也保留 Y；Elytra 按固定当前朝向模拟原版 glide 物理。
- [x] Slow Falling 与 Levitation 使用原版垂直公式；水平预测上限 16 blocks，位置跳变阈值 8 blocks/tick，时间校准保留 code-level seam。
- [x] 动量预测仅用于 `FRONT` 与 `SMART` 的 FRONT fallback；预测点受阻时普通 FRONT 回退静态点，Downshot 不回退，超出世界高度直接失败。
- [x] `FRONT` / `SURFACE` 各有默认未绑定的直接开门快捷键，且不修改持久化 mode。
- [x] `SMART` 默认阈值 8 blocks；二级设置页提供 1–32 slider，`Esc` 返回 Settings。
- [x] server-authoritative block collider raycast；忽略 fluids/entities；surface 最大射程为 code seam，默认 32 blocks。
- [x] 侧面贴附按 3×1 周围空间枚举 2×1 候选；优先 backing blocks 更完整者，支撑相同时才选择更靠近玩家的一侧；都不成立时允许 1×1 compact portal。
- [x] TOP/BOTTOM 使用 world-aligned 1×1 水平 portal，入口与出口反向配对；只检查 portal slab，出口不可用时静默回退到侧向 2×1。
- [x] 入口、出口采用统一 3D basis transform，保留 velocity/look 并提供最小向外速度。
- [x] Portal-local occupancy gate 取代 tick cooldown；实体完全离开出口 trigger 后才允许再次进入。
- [x] 贴面 portal 每 5 ticks 复验 anchor 与占用空间；失效时成对关闭。
- [x] 新 placement 验证成功后才关闭旧 portal pair；失败时保留旧门。
- [x] 开门与关门沿 portal 边缘生成 vanilla-behavior `SPLASH` 粒子；保持原版水平运动，支持侧向/TOP/BOTTOM，默认淡绿色 `#A8F0B6`，仅暴露 per-portal code-level RGB provider。
- [x] **Splash freeze：** 水花的数量、边缘采样、时序、速度、重力、寿命、side/TOP/BOTTOM 行为均已冻结；后续不得修改。唯一允许的扩展是通过 `PortalVisualStyleProvider` 更换 24-bit RGB，禁止控制 alpha。
- [x] 三种完整 fluid（source/flowing/block/bucket）与单-fluid 8000 mB tank；标准 capability 严格限容。
- [x] bucket mode 只抽取允许的完整 source；不放液、不回退开门；特殊整桶溢出隔离为可替换 policy。
- [x] 灰/蓝 fuel 仅同维度，绿色 fuel 支持跨维度；门体与冻结水花使用开门时的 fuel RGB snapshot。
- [x] 跨维度目标已 entity-ticking 时同步生成双门；未加载时先生成入口，首个实体抵达后再检测并生成出口。
- [x] 跨维度 lazy 出口不使用异步任务、目标预加载 ticket、pending request、轮询或 timeout。

## 7. Verification

- [x] Domain/storage/network codec/lifecycle JUnit tests。
- [ ] 完整 access、CRUD、group、sort、safety、vehicle traversal GameTests。
- [x] `clean test build` 成功。
- [x] Dedicated server 启动到 `Done`，无 mod exception。
- [x] `runClient -PguiCapture=true` 自动打开真实 Screen、截图并正常退出，无 mod exception。
- [x] GUI scale 4 自动 QA：主界面、详情滚底、坐标 modal、Safety 历史图标、placement settings 均无 clipping。
- [x] GUI scale 1 与当前窗口允许的自动上限 scale 2：主界面、坐标 modal、Safety 历史图标无 clipping。
- [x] scale 2 详情区实际滚到底，Edit 可见且底部 Open Portal 保持固定。
- [x] scale 2 placement settings 二级页无 clipping，slider、说明与返回按钮均完整可见。
- [ ] `runClient` 手工完整流程成功。
- [ ] 三种 GUI scale / resolution 检查。
- [ ] 截图检查 clipping、hierarchy、contrast、hover、focus、empty/error states。

## Future roadmap — 只记录，不暴露 disabled control

- [ ] 玩家可选关闭触发：打开后或首次穿过后。
- [ ] 玩家可配关闭延迟；默认 `3s`。
- [ ] 通过 `SafeDestinationResolver` 智能搜索安全位置。
- [ ] 独立维度选择器（跨维度传送与数据路径已完成）。
- [ ] 三种 fluid 的 recipes、worldgen 与 progression。
- [ ] 不稳定 fluid 的失控机制。
- [ ] Portal Gun tank 容量升级与 multi-fluid 模块。
- [ ] Portal upgrade module 组合不同实体 eligibility policy。
- [ ] Portal geometry upgrade：根据空间在 3×3、2×1、1×1 之间动态选择。
- [ ] Portal range upgrade：通过 `PortalPlacementCapabilities` 扩展 surface raycast 射程。
- [ ] 可选 accessory / inventory mod locator integration。
- [ ] 原创 UI 与 portal sound set。
- [ ] internal seam 稳定后再设计 versioned public addon API。
