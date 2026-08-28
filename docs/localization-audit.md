# RiftGun 双语文本审计

审计对象：`src/main/resources/assets/riftgun/lang/en_us.json` 与 `zh_cn.json`，并对照共享代码及两个版本节点中的实际调用。

修复后的 471 项双语对照见 [localization-catalog.md](localization-catalog.md)。

## 修复结果

- `en_us`：471 项；`zh_cn`：471 项。
- 两边 key 集一致，没有单边缺项。
- 所有格式占位符一致，没有 `%s`、`%1$s` 等参数错位。
- P0 的 3 个 REMOTE 错误 key 已补齐；测试中的虚构 key 已改为生产代码实际使用的 key。
- P1 已按审计建议修复；第 10 项 `Duration Module` 命名按要求保留现状。
- 43 个 P2 旧界面/旧流程残留 key 已从双语文件删除。
- 模块 Shift 详情新增 Remote 滚轮操作与配对传送门的简短用法。
- 显示文本的列表分隔符统一为 ASCII ` - `，不再使用 `·` 或 `—`。
- 新增本地化 contract test，检查双语 key、占位符、运行时消息 key 和分隔符。
- 未发现需要本地化的硬编码单词；`X`、`!`、`A↔B` 是控件符号。

以下章节保留修复前的问题与采用方案，作为变更记录。

## P0：代码引用但双语缺失（已修复）

这三项由 `VanillaPortalPlacementResolver` 的 REMOTE 路径直接返回。触发时客户端会显示原始 key。

| 缺失 key | 建议 English | 建议中文 |
|---|---|---|
| `message.riftgun.remote_invalid` | Remote portal placement is no longer valid | 远端传送门位置已失效 |
| `message.riftgun.remote_obstructed` | Not enough room to open a remote portal | 远端位置空间不足，无法开启传送门 |
| `message.riftgun.remote_out_of_range` | The remote portal position is out of range | 远端传送门位置超出射程 |

另有 `message.riftgun.portal_gun_required` 只出现在 `PortalOpenContractTest` 构造的测试数据中，生产代码使用的是已有的 `message.riftgun.no_portal_gun`。应修改测试复用现有 key，而不是新增一条永远不会在游戏中使用的翻译。

## P1：在用文本不准确或不全面（除第 10 项外已修复）

### 1. Motion prediction 被误译为“动量预测”

- Key：`screen.riftgun.motion_prediction_tooltip`
- 现有：`Motion prediction: %s` / `动量预测：%s`
- 问题：motion prediction 预测的是运动状态或轨迹，不是物理量 momentum。
- 建议：`Motion prediction: %s` / `运动预测：%s`

### 2. Prediction 的三段英文把 portal 写成 door

- Keys：`screen.riftgun.prediction.*.description`
- 问题：`door distance`、`Leads the door` 与全项目的 Portal 术语冲突，也容易被理解成普通门。
- 建议：
  - Off：`Fixed portal distance; no motion lead` / `传送门距离固定，不进行运动预判`
  - Projection：`Portal distance increases with your forward speed` / `传送门距离随视线方向速度增加`
  - Trajectory：`Places the portal along your predicted trajectory` / `沿预测的运动轨迹提前放置传送门`

### 3. 配对模块描述仍有已要求移除的“这把传送枪”

- Key：`tooltip.riftgun.module.portal_pairing_module.description`
- 现有：`Lets this Portal Gun...` / `允许这把传送枪……`
- 建议：`Allows direct placement of both ends of a linked portal pair.` / `允许直接放置一对相互连接的传送门。`

### 4. 配对快速键没有说明实际操作

- Key：`key.riftgun.portal_pairing_operation`
- 现有：`Quick Action: Use Pairing Mode` / `快捷操作：使用配对模式`
- 问题：没有说明普通按键与 `Shift + 按键` 分别操作哪一端，也没有说明实体转移时 `Shift` 用于设置固定目标。
- 建议至少改成“快速操作：放置配对传送门”；完整手势说明应出现在按键 tooltip 或模块说明中。

### 5. 配对设置页标题和说明没有覆盖 Remote 拆分后的职责

- Keys：`screen.riftgun.pairing.settings`、`screen.riftgun.pairing.settings_hint`
- 现有标题只说 Portal Pairing，但该页在仅安装 Remote Module 时也会出现，并管理 Coordinate SMART 的 FRONT/REMOTE 回退。
- 说明使用“fallback”却没有告诉玩家可选值是 FRONT 与 REMOTE。
- 建议标题：`SMART Fallbacks` / `智能回退`。
- 建议说明：`Choose whether Coordinate and Pairing SMART use Front or Remote for floating placement.` / `设置坐标与配对模式的智能浮空放置使用面前还是远端。`

### 6. Player Target 设置说明像从通行类别页复制而来

- Key：`screen.riftgun.player_target_hint`
- 现有：`Click an installed switch to allow or block it` / `点击已安装的开关以允许或禁止`
- 问题：页面实际包含“启用玩家目标”和“玩家排除范围”两个不同控件，不是 installed category switch。
- 建议：`Enable player targets and choose where the targeted player is excluded.` / `启用玩家目标，并设置目标玩家在入口或出口处的排除规则。`

### 7. “传送时机”不是该页面的实际含义

- Key：`screen.riftgun.portal_duration`
- 现有：`Portal timing` / `传送时机`
- 页面管理开启时长与通行冷却；“时机”表示何时发生，不表示 duration/cooldown。
- 建议：`Portal Timing` / `传送时长与冷却`

### 8. 权限英文有语法错误，中文另有明显笔误

- `screen.riftgun.permission.foreign_exit_transit`：`Other player's exit portal carry you` 应为 `Other players' exit portals carry you`。
- `screen.riftgun.privacy_transit_off_hint`：`开门口玩家` 应为 `开门玩家`。

### 9. 中文第二人称在同一权限流程中混用

- 28 个 key 使用“您”，27 个 key 使用“你”。
- 同一组 `chat.riftgun.privacy_*` 的基础请求使用“您”，新加的 `.entity_relocation_destination` 使用“你”。
- 建议统一为 Minecraft UI 更常用的“你”，否则同一轮请求会出现语气跳变。

### 10. Duration Module 名称与自己的说明不一致

- 状态：按要求不修改，保留现有名称。
- `item.riftgun.duration_extension_module`：`Duration Module` / `持续时长模块`
- 其他文本称它为 `Duration Extension Module` / `持续时长扩展模块`。
- 建议物品名统一为 `Duration Extension Module` / `持续时长扩展模块`。

### 11. End Frame 英文说明遗漏旋转，双语信息不对等

- Key：`screen.riftgun.visual.endframe_description`
- English 只写 static star 和 fluid ring；中文明确写了外圈可旋转。
- 建议：`Static end-portal star framed by a rotating portal-fluid ring` / 保留现有中文含义并适当精简。

### 12. “Open portal”被中文写成“生成传送门”

- Key：`screen.riftgun.generate`
- 现有：`Open portal` / `生成传送门`
- 这是玩家操作按钮，项目其余文案统一使用“开启传送门”。
- 建议：`Open Portal` / `开启传送门`

### 13. Placement tooltip 的范围过宽

- Key：`screen.riftgun.placement_mode_tooltip`
- 现有：`Portal Gun mode: %s` / `传送枪模式：%s`
- 当前已有独立的配对模式、Prediction 和 Placement 层级，“传送枪模式”无法说明这里显示的是 Placement。
- 建议：`Placement: %s` / `放置模式：%s`

### 14. 合并后的射程页面没有解释两个滑块的边界

- Key：`screen.riftgun.placement_ranges_hint`
- 现有只说 `Current capability limit: %s blocks`。
- 页面同时管理 SMART distance 与玩家设定的 maximum surface range；并且 SMART 只受 capability maximum 约束，不受当前 surface range 设定约束。现有说明没有传达这个关键差异。
- 建议增加第二行说明，或改为：`SMART and Surface ranges are adjusted independently; capability limit: %s blocks.` / `智能射程与最大贴面射程相互独立；能力上限：%s 格。`

### 15. 地图坐标上限说明对象含糊

- Key：`screen.riftgun.map.maximum_waypoints`
- 现有：`Maximum Waypoints per Map: %s` / `每个地图最多显示：%s`
- 中文缺少“坐标点”，`per Map` 也容易被理解成每张游戏地图物品，而不是每个地图模组来源。
- 建议：`Maximum waypoints per map mod: %s` / `每个地图模组最多读取 %s 个坐标点`

### 16. Entity Relocation 模块说明没有覆盖固定目标模式

- Key：`tooltip.riftgun.module.entity_relocation_module.description`
- 现有只描述转移到 selected destination；配对模式下还可通过 `Shift + 快速键` 设置固定目标。
- 中文“演出传送门”也不自然。
- 建议：`Opens a visual gate beneath the targeted entity and relocates it to the selected or fixed destination.` / `在目标实体脚下显示传送门，并将其转移到所选地点或固定目标点。`

### 17. Crisis 管理命令中的 armed/武装不自然

- Keys：`commands.riftgun.crisis.force.success`、`commands.riftgun.crisis.status.armed`
- 这是“已为下一次穿门预设危机”，不是武器武装状态。
- 建议使用 `queued/set` / `已预设、待触发`。

### 18. 分隔符风格仍未完全统一

修复前含 `·` 的 key：

- `message.riftgun.scoop_success`（在用）
- `screen.riftgun.zero_point_fuel_active`（在用，中文）
- `screen.riftgun.bucket_mode_tooltip`（旧 key）
- `screen.riftgun.player_section_tooltip`（旧 key）

若项目规则是统一使用 `-`，前两项仍需修改；后两项应直接随旧 key 清理。

## P2：高度可信的旧 key（43 项，已删除）

这些 key 没有静态引用，也不属于当前确认过的动态家族（PlacementMode、PredictionMode、Sort、Privacy policy/profile 等）。建议删除前增加引用完整性测试，然后一次性清理。

### 旧错误消息（4）

- `message.riftgun.destination_selected`
- `message.riftgun.entity_relocation_target_busy`
- `message.riftgun.front_outside_world`
- `message.riftgun.surface_range_module_required`

### 旧目的地安全/选择流程（7）

- `screen.riftgun.checking`
- `screen.riftgun.open_anyway`
- `screen.riftgun.pin`
- `screen.riftgun.safe`
- `screen.riftgun.select`
- `screen.riftgun.unsafe`
- `screen.riftgun.unsafe_body`

### 旧分组与详情文本（4）

- `screen.riftgun.group_reorder_hint`
- `screen.riftgun.group_selected`
- `screen.riftgun.player_section_tooltip`
- `screen.riftgun.xyz`

### 旧模块摘要文本（7）

- `screen.riftgun.bucket_mode_tooltip`
- `screen.riftgun.modules.capacity`
- `screen.riftgun.modules.coordinate_locked`
- `screen.riftgun.modules.coordinate_unlocked`
- `screen.riftgun.modules.entity_access`
- `screen.riftgun.modules.surface_range`
- `screen.riftgun.nominal`

### 合并射程页面前的旧文本（6）

- `screen.riftgun.maximum_surface_range`
- `screen.riftgun.placement_settings`
- `screen.riftgun.smart_distance`
- `screen.riftgun.surface_range`
- `screen.riftgun.surface_range_modules`
- `screen.riftgun.surface_range_value`

### 旧隐私界面文本（13）

- `screen.riftgun.player_exclude`
- `screen.riftgun.privacy_target`
- `screen.riftgun.privacy_target_hint`
- `screen.riftgun.privacy_transit_off`
- `screen.riftgun.privacy_transit_off_hint`
- `screen.riftgun.privacy_transit_on`
- `screen.riftgun.privacy_transit_on_hint`
- `screen.riftgun.privacy.mode.allow`
- `screen.riftgun.privacy.mode.default`
- `screen.riftgun.privacy.mode.deny`
- `screen.riftgun.privacy.private`
- `screen.riftgun.privacy.public`
- `screen.riftgun.privacy.request`

### 其他被替代文本（2）

- `screen.riftgun.pairing_mode`（当前使用 `pairing_mode_tooltip` 与轮盘专用 key）
- `screen.riftgun.portal_duration_hint`（当前使用 `portal_timing_hint`）

## 执行记录

1. 已补齐 3 个 REMOTE 运行时错误，避免原始 key 暴露给玩家。
2. 已修正术语、语法、笔误和既有明确要求；Duration Module 名称按要求保留。
3. 已重写 Pairing/Remote、Placement Ranges、Player Target 三组说明，使其覆盖当前功能边界。
4. 已增加本地化契约测试：双语 key/占位符一致、production message key 存在、分隔符统一。
5. 已在测试保护下删除 43 个旧 key。
