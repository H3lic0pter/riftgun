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
- [x] `DestinationDimensionPolicy`：当前同维度策略；为跨维度保留 seam。
- [x] `DestinationSafetyInspector`：lazy collision、support surface、hazard 检查。
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
- [x] portal 存活时为目的地 chunk 添加 ticking ticket。

## 4. Networking

- [x] 带 protocol version 的 NeoForge payload registration。
- [x] C2S GUI、mutation、selection、safety 与 portal action payload。
- [x] S2C snapshot、safety-result 与 portal-opened acknowledgement；validation 通过 action-bar 返回。
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
- [x] 地点/分组删除确认；玩家可独立关闭该确认；unsafe destination 始终保留开门确认。
- [x] lazy safety 状态：显示检查中；位置改变才失效，改名/换组/置顶不失效。
- [x] 关闭 safety-check 后不发送检查请求、server 不运行 inspector，GUI 不显示检查状态。
- [x] 地点行右侧常驻黄星置顶按钮，hover/focus 时显示红色删除按钮；长名称省略并提供 tooltip。
- [x] 自定义分组行 hover/focus 时显示重命名和删除按钮；drag handle 与 Alt+↑/↓ 均可排序。
- [x] 右侧详情整体滚动，目标变化时归顶；底部生成 portal 固定，不参与滚动。
- [x] 空详情与 server action-bar 的 missing-gun、limit、validation 状态。
- [x] Mouse 与 Tab/Shift+Tab/Enter/Esc navigation。
- [x] 分组展开/收起与置顶重排使用短时平滑位移动画；关闭 animations 后立即更新。
- [x] Vanilla UI sounds；个人声音开关。
- [x] English 与 Simplified Chinese localization。

## 6. Portal behavior and tooltip

- [x] Portal 出口使用保存的 feet position 与 yaw。
- [x] Safety 只警告；不偏移、不破坏、不拒绝开门。
- [x] Portal Gun tooltip：目标名、分组、维度、同维度距离。
- [x] Portal Gun 互动键遇到 unsafe 目标时 action-bar 警告但照常开门；GUI 开门使用确认弹窗。
- [x] 玩家、掉落物、允许的 vehicle/passenger tree 可传送；mob 不可传送。
- [x] 保留水平 momentum 与 riding relationship。

## 7. Verification

- [x] Domain/storage/network codec/lifecycle JUnit tests。
- [ ] 完整 access、CRUD、group、sort、safety、vehicle traversal GameTests。
- [x] `clean test build` 成功。
- [x] Dedicated server 启动到 `Done`，无 mod exception。
- [x] `runClient -PguiCapture=true` 自动打开真实 Screen、截图并正常退出，无 mod exception。
- [x] GUI scale 1 与当前窗口允许的自动上限 scale 2：主界面、坐标 modal、unsafe modal 无 clipping。
- [x] scale 2 详情区实际滚到底，Edit 可见且底部 Open Portal 保持固定。
- [ ] `runClient` 手工完整流程成功。
- [ ] 三种 GUI scale / resolution 检查。
- [ ] 截图检查 clipping、hierarchy、contrast、hover、focus、empty/error states。

## Future roadmap — 只记录，不暴露 disabled control

- [ ] 玩家可选关闭触发：打开后或首次穿过后。
- [ ] 玩家可配关闭延迟；默认 `3s`。
- [ ] 通过 `SafeDestinationResolver` 智能搜索安全位置。
- [ ] 跨维度 policy 与维度选择器。
- [ ] Portal upgrade module 组合不同实体 eligibility policy。
- [ ] 可选 accessory / inventory mod locator integration。
- [ ] 原创 UI 与 portal sound set。
- [ ] internal seam 稳定后再设计 versioned public addon API。
