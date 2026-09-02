# Rift Gun 下一版本发布准备审查

- 审查日期：2026-09-03
- 审查分支：`dev`
- 审查终点：`c207f9a` + 当前 `0.2.0-rc.1` 准备工作树（尚未提交）
- 上次发布基线：`cfc26e47d95d7f7abf1800a225f05987800d8f07`
- 基线 tag：`mc1.21.1-v0.1.1-r1`、`mc26.1.2-v0.1.1-r1`
- 审查范围：基线之后 69 个 commit、434 个文件
- 变更规模：新增 25,632 行、删除 3,528 行
- 当前结论：**RC 产物已生成；公开发布仍为 No-Go，等待最终游戏内矩阵**

## 1. 执行摘要

自 `0.1.1-r1` 发布以来，Rift Gun 已经从一次常规修补演变为包含新玩法、
新 GUI、新扩展 API、新第三方集成和新渲染兼容层的大版本更新。主要功能已具备，
两条版本线的自动化测试也全部通过。当前工作树已经收束 release identity、网络协议、
REMOTE/Pairing 产品边界、recipe 文档、公共 callback、API artifact 和 optional client
runtime 边界，并生成两份 `0.2.0-rc.1` 候选主 JAR。

原 API artifact 阻断已在当前工作树修复：binary artifact 包含
`RiftGunApiBootstrap`，binary/source artifacts 均包含 `META-INF/LICENSE`，并由最终
artifact 内容检查、隔离 classloader 链接 smoke test 和最小 Addon 编译 fixture 覆盖。

剩余阻断主要是无法由 unit/build 代替的游戏内验证：Pair marker 在最终 JAR 下的
shaders off/on、远坐标、遮挡和性能；最低 optional mod 组合；新旧 protocol 实际混连；
旧世界/旧枪/旧 config；以及 GUI scale。两节点 dedicated server 的无 client mod 启动
已经到达 `Done`。

本轮候选版本已确定为 **`0.2.0-rc.1`**。通过最终矩阵后再决定是否保留此候选、制作
下一 RC，或进入正式 `0.2.0`；当前不创建 tag、不对外发布。

## 2. 审查方法和证据

本审查同时从两个轴检查变更：

- **Standards**：代码边界、artifact 完整性、运行时安全、性能、维护性、仓库规范。
- **Spec**：实现是否符合设计文档、README、已确认用户需求和对外兼容声明。

本次已确认：

- `git diff --check cfc26e4..HEAD` 通过。
- Minecraft 1.21.1：558 项测试，0 failure、0 error、0 skipped。
- Minecraft 26.1.2：554 项测试，0 failure、0 error、0 skipped。
- 两节点完整 `build` 已通过。
- 跨版本 GUI/preview 重复状态已在 `76ed68a` 抽到 shared 层，并加入 architecture
  source test 防止业务逻辑重新漂回节点 adapter。
- 当前源码没有遗留试错/逐帧 debug log。
- 普通 placement preview 保持原有 batch；Pair I/II marker 使用一个独立的
  post-composite line batch，不会为任意 I/II 标记添加动态光源。
- LambDynamicLights 保持 client-only、optional、`compileOnly`，未嵌入发布 JAR。
- 两节点 dedicated server 在临时移除 run 目录中的 Sodium/Iris 后，仅加载 Minecraft、
  NeoForge 和 Rift Gun `0.2.0-rc.1`，并分别到达 `Done`。测试后四个 client mod 文件已
  原样恢复，临时目录已删除。
- dedicated smoke 还发现并修复 `optionalClientRuntimeOnly` 被全局并入
  `runtimeOnly` 的构建边界问题；JEI 等 client runtime 现在只进入 client run。
- 当前工作树包含本轮 RC 准备修改，尚未提交。最终 tag 前仍须提交并确认
  `git status --short` 为空。

自动化测试不能证明以下事项，因此不能用“build 通过”替代发布 smoke test：

- 第三方 optional mod 的真实 classloading。
- Iris/Complementary 实际 shader pipeline。
- dedicated server 上 client-only 类的隔离。
- 新旧客户端之间的协议拒绝行为。
- 旧世界、旧枪 NBT 和旧 config 的升级结果。
- Minecraft GUI scale 下的视觉对齐和遮挡顺序。

## 3. 自上次发布以来的主要修改

### 3.1 模块与玩法

- 新增 Portal Pairing 流程及 A/B 端点管理。
- 新增 REMOTE、SMART 和 Precision Placement 相关能力与预览。
- 新增 Dimensional Traversal 和维度选择流程。
- 新增模块堆叠规则、容量和持久化逻辑。
- 调整多个模块的 crafting recipe。
- 新增 1.21.1 Create Mechanical Mixer 的传送门流体配方。
- 强化枪定位、门户打开、燃料、跨维度及传送稳定性。

### 3.2 GUI 与交互

- 大幅扩展 Portal Config Screen。
- 新增/扩展 radial menu、二级页面、滑条及 per-gun 设置。
- 新增玩家、维度、外部地图目标等列表和分组。
- 将 GUI page/session/layout/rows/settings/presentation/player-target state 抽到 shared
  层；两个 Minecraft 节点保留各自 widget/render adapter。
- 将 placement preview 的 tick、cache 和 frame state 抽到共享
  `PortalPlacementPreviewEngine`。
- 坐标输入不再直接拒绝大数值，而是在提交后 clamp：X/Z 使用 Minecraft
  world bounds，Y 使用目标维度 build height。
- GUI 中关闭传送门的含义调整为“清除传送门”。

### 3.3 外部集成和公共 API

- 新增 Rift Gun public addon API，当前 API version 为 `1.2.0`。
- 新增 dimension label、portal-open policy、transit context、coordinate note 等扩展点。
- 新增 JourneyMap/Xaero 只读 waypoint 集成。
- 新增外部目的地 request 的长度、维度、有限数值和 server-side trust-boundary 校验。
- 动态光源从 RyoamicLights 迁移到 LambDynamicLights v4，同时覆盖 1.21.1 和
  26.1.2。

### 3.4 渲染与光影

- 为 Complementary Reimagined/Unbound 添加末地环中央盘面支持。
- 未注册光影继续使用默认“中央留空”表现。
- 修复中央盘面与外圈的遮挡顺序，使外圈覆盖中央盘面。
- 修复 1.21.1 Portal Pairing I/II preview 随玩家移动产生的闪烁、遮挡和周期性
  跳回问题。
- 两节点的 Pair I/II marker 均改为 opaque、unlit、unfogged 的固定宽度线框，保持
  原本朝向、颜色、I/II 字形和外框，并使用 read-only world depth 逐像素遮挡。
- 移除 line shader 中的 `VIEW_SHRINK`，避免以 view-space depth 偏移换取可见性而导致
  远距离遮挡误差。
- 26.1.2 不再使用 GUI overlay、中心 raycast 或逐帧 world-to-screen 投影；Pair marker
  在 shader composition 后、world depth 清除前以一个专用 line batch 绘制。
- 26.1.2 的 `LevelRenderState` 会在 `AfterLevel` 发布前 reset。Pair marker 必须使用
  extraction 阶段单独保留的 immutable render state，不能在 `AfterLevel` 中重新读取
  已清空的 render data；该生命周期约束已有 source regression test。
- 预览 marker 不发光；只有真实传送门可由 optional dynamic-light provider 提供
  附近方块光照。
- 清理 shader 试错日志并保留一次性 compatibility warning。

## 4. Standards 审查发现

### S1 — 已修复：standalone API JAR 缺失类引用

**解决结果**

- `apiJar` 和 `apiSourcesJar` 不再排除 `RiftGunApiBootstrap`。
- [`PublicApiArtifactTest`](../src/test/java/dev/riftgun/architecture/PublicApiArtifactTest.java)
  直接检查最终 JAR 内容，并使用 parent 为 platform classloader 的隔离 classloader
  调用 `RiftGunTransitContext.currentAuthorization()`。
- 同一测试使用仅含 binary API JAR 的 classpath 编译最小 Addon fixture，避免主工程
  test classpath 掩盖缺失类。

### S2 — 已修复：mod 版本和网络协议随 wire schema 推进

**解决结果**

- Tree 默认属性与两节点均使用 `mod_version=0.2.0-rc.1`，最终构建的文件名与
  `neoforge.mods.toml` 内 version 一致。
- 两节点 `NeoForgeNetworkAdapter` registrar 均从 `"1"` 推进到 `"2"`；不提供旧
  snapshot compatibility codec，旧 protocol-1 peer 应在握手阶段被拒绝。
- [`ReleaseCandidateIdentitySourceTest`](../src/test/java/dev/riftgun/architecture/ReleaseCandidateIdentitySourceTest.java)
  固定两节点版本、protocol、release-note 文件名和 26.1.2 JEI 下界。
- 两份 release notes 明确要求 client/server 安装同一 `0.2.0-rc.1` 构建。

**剩余人工验证**

- N02/N03/N05/N06 仍需用旧 `0.1.1-r1` 与候选 JAR 实际混连，记录 NeoForge 拒绝信息；
  该项不再需要代码决策，但仍是公开发布 gate。

### S3 — 已修复：API artifacts 缺少 MIT LICENSE

**解决结果**

- `apiJar` 和 `apiSourcesJar` 都从仓库根目录复制 `LICENSE` 到
  `META-INF/LICENSE`。
- artifact-content test 同时检查 binary 和 sources JAR，防止 task 重构再次遗漏。

### S4 — 已修复：公共 provider callback 故障隔离

**解决结果**

- Dimension label provider 的 `RuntimeException`/null 返回被单 provider 隔离；失败后继续
  后续 provider，最终仍可使用内置 fallback。
- Portal-open policy 的同类失败按已确认策略 fail-closed，返回本地化拒绝原因。
- warning 包含 provider ID，并用 process-wide concurrent set 保证每个 ID 只记录一次；
  callback 仍可在后续请求重试，不会因一次失败被永久摘除。
- [`RiftGunCallbackIsolationTest`](../src/test/java/dev/riftgun/api/RiftGunCallbackIsolationTest.java)
  覆盖首个 label provider 失败后后续 provider 工作，以及 policy 重复失败始终拒绝。

### S5 — 已修复：README 补齐 1.21.1 common config

- README 和 1.21.1 release notes 均说明 `config/riftgun-common.toml` 只用于 1.21.1
  Create Mechanical Mixer recipe switch，并要求 game restart。
- 26.1.2 release notes 明确该 node 不包含 Create，也不生成此 common config。

### S6 — 已修复：未接通 destination-provider draft 不再公开

- 已删除 `RiftGunDestinationProvider`、`RiftGunDestinationProviders` 和
  `ProvidedPortalDestination`，不对 `1.2.0` Addon API 固化无生产消费点的契约。
- `PublicApiArtifactTest` 对 binary/source API JAR 都加入 absence assertion，防止这些
  draft 类型被旧 build output 或后续重构重新带入。

### S7 — 已关闭（原 P3）：跨版本 GUI/preview 状态已完成共享重构

**完成证据**

- commit `76ed68a` 新增以下 shared seam：
  - [`PortalConfigPage`](../src/main/java/dev/riftgun/ui/PortalConfigPage.java)
  - [`PortalConfigSession`](../src/main/java/dev/riftgun/ui/PortalConfigSession.java)
  - [`PortalConfigLayout`](../src/main/java/dev/riftgun/ui/PortalConfigLayout.java)
  - [`PortalConfigRows`](../src/main/java/dev/riftgun/ui/PortalConfigRows.java)
  - [`PortalConfigSettings`](../src/main/java/dev/riftgun/ui/PortalConfigSettings.java)
  - [`PortalConfigPresentation`](../src/main/java/dev/riftgun/ui/PortalConfigPresentation.java)
  - [`PortalPlayerTargetSession`](../src/main/java/dev/riftgun/ui/PortalPlayerTargetSession.java)
- 两节点 `PortalConfigScreen` 均委托上述 shared page、session、layout、rows 和 settings。
- 两节点 `PortalPlacementPreview` 均将 tick/cache/frame state 委托给
  [`PortalPlacementPreviewEngine`](../src/main/java/dev/riftgun/portal/PortalPlacementPreviewEngine.java)，
  节点内只保留 Minecraft API 解析和实际 renderer adapter。
- [`PortalConfigSharedSeamsSourceTest`](../src/test/java/dev/riftgun/architecture/PortalConfigSharedSeamsSourceTest.java)
  会阻止 modal/row/player-target/preview business state 重新复制回版本节点。
- 新增 shared component 单元测试；重构后 1.21.1 共 540 项、26.1.2 共 536 项测试，
  全部通过。

**评价**

- 原 finding 已解决，不再计入开放发布问题或发布后技术债。
- 两份 screen 仍保留大量版本相关 widget/render glue，这是当前跨 Minecraft API adapter
  的有意边界；除非以后出现新的真实漂移证据，不要求为本次发布继续抽象。
- GUI normal/max scale 的视觉验收仍然保留，因为 shared state test 不能代替像素级检查。

### S8 — 代码已修复：26.1.2 JEI dependency range

- 26.1.2 metadata 下界已改为 `[29.29.0.76,)`，与编译版本、README 和 release notes
  一致；最终 JAR 内容审计也确认该值。
- 最低版本真实 client smoke（I02）仍需执行，完成前不能把“声明一致”扩大为“运行兼容已证实”。

### S9 — 已修复：optional client runtime 污染 dedicated server run

- dedicated smoke 首次发现 JEI/Sodium client runtime 曾通过全局 `runtimeOnly` 进入
  `runServer` classpath；构建脚本现仅将 `optionalClientRuntimeOnly` 接到 client run 的
  `additionalRuntimeClasspathConfiguration`。
- source guard 固定该边界；清空 node `run/mods` 后两节点 server 都只发现 Minecraft、
  NeoForge 和 Rift Gun，并成功到达 `Done`。

## 5. Spec 审查发现

### P1 — 已修复：REMOTE capability 产品定义统一

- REMOTE 只由独立 Remote Module 提供；Portal Pairing Module 只提供 A/B placement 与
  Coordinate Travel / Portal Pairing function switch。
- Pairing SMART 的已保存 `REMOTE` fallback 仅在 Pairing + Remote 同时安装时生效；
  缺任一模块都临时解析为 `FRONT`，不覆盖持久化偏好。
- README、Pairing 设计和两份 release notes 已统一。
- `PortalGunCapabilitiesTest` 覆盖无模块、Pairing only、Remote only、两者同时四种组合；
  GUI/radial/server validation 继续消费同一 resolved capability。


### P2 — RC identity 已完成，最终发布 identity 待定

- 两节点、主/API artifact、metadata、标题和文档均使用 `0.2.0-rc.1`。
- 草稿已重命名为
  [`1.21.1-v0.2.0-rc.1.md`](release-notes/1.21.1-v0.2.0-rc.1.md) 与
  [`26.1.2-v0.2.0-rc.1.md`](release-notes/26.1.2-v0.2.0-rc.1.md)；旧 `r1` 草稿已移除。
- 图片 placeholder 按产品决定保留；发布日期与最终 SHA 仍为 `TBD`。本节第 9 章只记录
  当前候选哈希，不把它冒充最终发布哈希。
- 当前不创建 tag、不发布；游戏内矩阵后再确认最终 identity。

### P3 — 已修复：recipe 文档以现有 JSON 为权威

- Dimensional Traversal 设计现记录 `ECE / OAO / ELE` 及实际材料。
- Portal Pairing 设计现记录 `KEO / TMC / OEK` 及实际材料。
- recipe JSON 未改动；两节点继续共享同一资源基线。

### P4 — 已修复：README 覆盖完整玩家与 Addon 边界

- 模块表已补 Dimensional Traversal、Remote、Precision Placement，并纠正 Pairing。
- Requirements 与两个 node metadata 对齐；Optional Integrations 说明地图 waypoint 的
  client session/read-only 属性和 server validation、动态光边界以及 node 差异。
- Configuration 补 1.21.1 common config；Development 补 API artifact、callback
  failure semantics、一次性 warning 和 pre-1.0 稳定性。
- shader/Pair marker 章节明确 opaque、world depth、原朝向、无 marker entity/动态光源。

### P5 — P2：缺少真实运行环境的发布验证记录

现有 tests 主要覆盖 pure logic、codec、source contract 和部分 geometry。以下路径必须在
实际游戏中验证，结果应记录在第 7 节矩阵中。

重点风险：

- optional dependency 不存在时的 classloading。
- 两个地图 mod 同时安装时的分组、刷新和生命周期。
- shader pack 检测与 Iris material bridge。
- dedicated server 完全没有 client-only mod 时的启动。
- multiplayer 新旧协议处理。
- 旧枪 module settings/NBT 的默认值迁移。

## 6. 发布工作包与优先级

### 6.1 P0：不完成不得制作 release candidate

- [x] 修复 standalone API JAR 缺失类引用。
- [x] 为 API JAR/API sources JAR 加入 LICENSE。
- [x] 增加 API artifact 独立编译/链接/内容测试。
- [x] 决定 REMOTE capability 模型并统一实现、测试、设计和 README。
- [x] 确定 RC 版本号 `0.2.0-rc.1` 并同时更新两个节点。
- [x] 审查 wire schema；两节点 network protocol 均推进到 `2`。
- [ ] 新旧 client/server mismatch 能明确拒绝，而不是进入游戏后错读 snapshot。

### 6.2 P1：release candidate 前完成

- [x] 修正 26.1.2 JEI dependency range。
- [x] 为公共 dimension label/open policy callback 增加异常隔离。
- [x] 决定 Dimensional Traversal 与 Portal Pairing recipe 的权威版本。
- [x] 补 README 的模块、地图、动态光源、common config 和 API 文档。
- [x] 分别完成两个节点 `0.2.0-rc.1` release notes；保留已确认的图片 placeholder。
- [x] 检查主 JAR/API JAR/sources JAR 内容、命名、许可证和无意捆绑依赖。
- [ ] 用最终 JAR 在两个节点完成 Pair I/II marker 的 shaders off、Complementary、
  camera motion、远坐标和遮挡专项验证。
- [ ] 建立并执行第 7 节 smoke matrix。

### 6.3 P2：正式发布前完成或明确登记为已知问题

- [x] 从 public API 移除未接通的 `RiftGunDestinationProviders` draft。
- [x] 记录 26.1.2 相比 1.21.1 的平台差异，例如 Create/Immersive Portals 支持范围。
- [ ] 记录性能基线，尤其是多传送门 + shader + dynamic lights 场景。
- [ ] 清理工作树换行符/stat 噪声，确保 tag 从完全 clean 的 commit 创建。
- [x] 双节点完整测试包含翻译键/资源检查，新 callback failure key 两种语言均存在。
- [ ] 检查 GUI normal/max scale 的 icon bounds 与 `_on`/`_off` 对齐。

### 6.4 发布后技术债

- [x] 抽取跨版本 GUI page/session/layout/rows/settings/presentation state（`76ed68a`）。
- [x] 抽取 preview shared state/tick engine（`76ed68a`）。
- [ ] 为 public API 建立上一版本 binary compatibility baseline。
- [ ] 把 release smoke checklist 固化为仓库文档或 CI 可执行检查。
- [ ] 在升级 Gradle 10 前移除 `archives` artifact declaration；当前 Gradle 9.7 构建会在
  [`build.gradle.kts`](../build.gradle.kts#L184) 对 `apiJar`/`apiSourcesJar` 输出弃用警告，
  Gradle 10 将把它提升为错误。

## 7. 双版本发布验证矩阵

每一项应记录：日期、commit、JAR SHA-256、Minecraft/NeoForge/Java、optional mod 精确版本、
执行者、结果和日志/截图路径。

### 7.1 自动化和 artifact

| ID | 节点 | 场景 | 预期结果 | 状态 |
|---|---|---|---|---|
| A01 | 1.21.1 | Java 21 执行 `build` | 所有 tests 和 JAR verification 通过 | 已通过，558 tests |
| A02 | 26.1.2 | Java 25 执行 `build` | 所有 tests 和 JAR verification 通过 | 已通过，554 tests |
| A03 | 两者 | `git diff --check` | 无 whitespace error | 当前工作树已通过；最终 commit 后重跑 |
| A04 | 两者 | 检查主 JAR | 正确 version、LICENSE、无 accidental nested dependency | 已通过；候选 SHA 见第 9 节 |
| A05 | 两者 | 检查 API JAR | class closure 自洽、包含 LICENSE | 已通过；并确认 removed draft API 缺席 |
| A06 | 两者 | 最小 Addon fixture 只依赖 API JAR | 编译和基础入口 smoke 通过 | 已通过 |
| A07 | 两者 | GUI/preview shared seam architecture tests | 节点 adapter 不重新拥有共享业务状态 | 已通过 |

### 7.2 无 optional mod 基线

| ID | 节点 | 环境 | 预期结果 | 状态 |
|---|---|---|---|---|
| B01 | 1.21.1 | client，仅 NeoForge + RiftGun | 启动、进世界、开 GUI、开门正常 | 待执行 |
| B02 | 26.1.2 | client，仅 NeoForge + RiftGun | 同上 | 待执行 |
| B03 | 1.21.1 | dedicated server，仅 NeoForge + RiftGun | 无 client classloading crash | 已到达 `Done (4.093s)` |
| B04 | 26.1.2 | dedicated server，仅 NeoForge + RiftGun | 无 client classloading crash | 已到达 `Done (0.762s)` |
| B05 | 两者 | client 连接同版本 dedicated server | snapshot、radial、开门和传送正常 | 待执行 |

### 7.3 网络兼容

| ID | 客户端 | 服务端 | 预期结果 | 状态 |
|---|---|---|---|---|
| N01 | 新 1.21.1 | 新 1.21.1 | 正常连接和交互 | 待执行 |
| N02 | 旧 0.1.1-r1 | 新 1.21.1 | 握手阶段明确拒绝，除非有已验证兼容 codec | protocol 1→2 已隔离；实测待执行 |
| N03 | 新 1.21.1 | 旧 0.1.1-r1 | 同上 | protocol 2→1 已隔离；实测待执行 |
| N04 | 新 26.1.2 | 新 26.1.2 | 正常连接和交互 | 待执行 |
| N05 | 旧 0.1.1-r1 | 新 26.1.2 | 明确拒绝或已验证兼容 | protocol 1→2 已隔离；实测待执行 |
| N06 | 新 26.1.2 | 旧 0.1.1-r1 | 明确拒绝或已验证兼容 | protocol 2→1 已隔离；实测待执行 |

### 7.4 LambDynamicLights

| ID | 节点 | Provider | 预期结果 | 状态 |
|---|---|---|---|---|
| L01 | 1.21.1 | 无动态光源 mod | 不崩溃；真实传送门不照亮方块 | 待执行 |
| L02 | 1.21.1 | LambDynamicLights 4.8.10 | 传送门渐亮/渐灭，生命周期正确 | 待执行 |
| L03 | 1.21.1 | 当前验证版本 | 同上，无重复 provider | 待执行 |
| L04 | 26.1.2 | 无动态光源 mod | 不崩溃 | 待执行 |
| L05 | 26.1.2 | LambDynamicLights 4.11.1 | 传送门渐亮/渐灭 | 待执行 |
| L06 | 两者 | I/II preview marker | marker 不发光、不注册 entity luminance | 待执行 |
| L07 | 两者 | 旧 RyoamicLights 仍安装、无 Lamb | RiftGun 不调用旧 API；记录实际共存行为 | 待执行/文档化 |

测试环境一次只保留一个目标 LambDynamicLights JAR；不要同时放置同一 mod 的多个版本，
否则测试结果不能证明声明的下界兼容性。

### 7.5 JEI、JourneyMap、Xaero 和 Create

| ID | 节点 | 组合 | 预期结果 | 状态 |
|---|---|---|---|---|
| I01 | 1.21.1 | 最低支持 JEI | RiftGun recipe/category 加载正常 | 待执行 |
| I02 | 26.1.2 | 修正后的最低 JEI 29.x | 同上 | 待执行 |
| I03 | 两者 | 无 JEI | RiftGun 正常启动 | 待执行 |
| I04 | 两者 | JourneyMap only | waypoint 分组、选择、刷新正常 | 待执行 |
| I05 | 两者 | Xaero only | waypoint 分组、选择、刷新正常 | 待执行 |
| I06 | 两者 | JourneyMap + Xaero | 来源不混淆、稳定 ID 不冲突 | 待执行 |
| I07 | 两者 | 换服务器/登出重进 | session-only 选择被清理，不泄漏旧目标 | 待执行 |
| I08 | 两者 | 修改/删除 waypoint 后刷新 | GUI 不保留不可用选择 | 待执行 |
| I09 | 两者 | 恶意/畸形 waypoint packet | 服务器拒绝超长、NaN/Infinity、未知维度 | unit 已覆盖，运行验证待执行 |
| I10 | 1.21.1 | Create 6.0.7+ | mixer recipes 正常且 common config 生效 | 待执行 |
| I11 | 1.21.1 | 无 Create | 无 classloading/recipe error | 待执行 |

### 7.6 Shader 和 preview

| ID | 节点 | 场景 | 预期结果 | 状态 |
|---|---|---|---|---|
| R01 | 两者 | shaders off | 默认末地环表现正常 | 待执行 |
| R02 | 两者 | Complementary Reimagined r5.x | 中央末地盘面可见，外圈遮挡中央盘面 | 用户已验证功能，最终 JAR 重测 |
| R03 | 两者 | Complementary Unbound r5.x | 同上 | 用户已验证功能，最终 JAR 重测 |
| R04 | 两者 | 未注册 shader pack | 默认中央留空，不强行注入盘面 | 待执行 |
| R05 | 两者 | I/II marker，玩家步行/冲刺/转头及 hurt/nausea camera effects | 保持 world projection；不闪烁、不错误缩放、倾斜或跳回 | 1.21.1 曾验证；最终 JAR 两节点重测 |
| R06 | 两者 | 远离世界原点、改变距离和观察角度 | camera-relative 精度稳定，线宽固定 | 待执行 |
| R07 | 两者 | shaders off，I/II marker 与实体/方块遮挡 | marker 完全不透明；按 world depth 逐像素遮挡，不穿墙 | 26.1.2 修复后待最终 JAR 重测 |
| R08 | 两者 | Complementary Reimagined，I/II marker | shader composition 后仍清晰可见；不透底、不穿墙 | 待最终 JAR 重测 |
| R09 | 26.1.2 | Pair marker 跨 `ExtractLevelRenderStateEvent` → `AfterLevel` | reset 后仍使用同帧 retained state；有无光影均显示 | source regression 已通过；游戏待重测 |
| R10 | 两者 | 大量可见传送门及一个 Pair marker | 无逐帧 log、raycast、投影 allocation 或明显 frame-time 回退 | 待 profile |

### 7.7 Gameplay、GUI 和升级

| ID | 场景 | 预期结果 | 状态 |
|---|---|---|---|
| G01 | 无模块/Pairing only/Remote only/两者同时 | capability、radial、server validation 符合最终决策 | 四组合 pure test 已通过；GUI/server 游戏内待测 |
| G02 | Dimensional Traversal 开关和目标维度 | 配置、GUI、开门、燃料约束一致 | 待执行 |
| G03 | Precision Placement 各 face/orientation | preview 与最终 portal 坐标一致 | 待执行 |
| G04 | 极大 X/Y/Z 输入 | 提交后 clamp；Y 使用目标维度 build height | 待执行 |
| G05 | 模块重复堆叠 | capacity、active count、NBT 持久化正确 | 自动化已覆盖，游戏重测 |
| G06 | 清除传送门按钮 | 文案和实际行为均为清除 portal | 待执行 |
| G07 | normal GUI scale | 16×16 icon 在 26×26 button 内正确居中 | 待执行 |
| G08 | maximum GUI scale | 无像素错位，`_on`/`_off` 坐标一致 | 待执行 |
| G09 | 从 0.1.1-r1 升级旧世界 | destinations、权限、portal state 可加载 | 待执行 |
| G10 | 从 0.1.1-r1 加载旧枪 NBT | 新 module settings 使用安全默认值 | 待执行 |
| G11 | 加载旧 client/server config | 不崩溃；新增字段生成合理默认值 | 待执行 |

## 8. 性能评价与验证建议

### 8.1 当前代码评价

- 没有发现新的发布级逐帧日志。
- `TransitDiagnostics` 的 info 输出受显式 diagnostics 开关控制，不属于遗留 debug log。
- datapack fuel reload 的一次性 info 日志属于正常生命周期日志。
- 普通 placement preview 保留原 batch；Pair I/II marker 合并到一个独立的
  post-composite line batch，没有退回每 segment 独立 flush。
- Pair marker geometry 只在 endpoint 改变时重建；每帧只复用 retained immutable
  render state，不执行中心 raycast、world-to-screen 投影、framebuffer copy 或额外
  post-process。
- 一个完整 Pair marker 仅包含约 11–12 条线；专用 pipeline 静态复用，depth read-only，
  不写入 depth，也不创建逐帧 pipeline/render type。
- preview 世界坐标先以 double 执行 `point - camera`，再转 float，避免远坐标精度损失。
- marker 不注册 dynamic luminance。
- LambDynamicLights 通过 optional initializer/provider 集成，没有把上游实现打包进 RiftGun。

### 8.2 release candidate profile

建议固定一个可重复场景，对上次 release 和 release candidate 做 A/B：

- 同一世界、视距、分辨率、GUI scale 和 shader 设置。
- 0、1、8、32 个可见 RiftGun portal 四档。
- shaders off、Complementary Reimagined、Complementary Unbound 三档。
- LambDynamicLights off/on 两档。
- 每档预热 60 秒、采集至少 180 秒。

记录：

- average FPS 只作参考；重点比较 median、P95、P99 frame time。
- client tick time 和 chunk rebuild 频率。
- GC allocation rate 和明显的短周期 GC spike。
- log 文件增长速度。

建议的 release gate：在相同环境下，常用场景 P95 frame time 不出现可重复的明显回退；
若有回退，必须能归因于用户主动开启的 shader/dynamic-light 功能，并在 release notes 记录。

## 9. 发布 artifact 审计

对两个节点的当前候选构建分别执行并记录：

- [x] 主 JAR 名称包含正确 Minecraft version 和 mod version。
- [x] `neoforge.mods.toml` 中 mod version 与文件名一致。
- [x] 主 JAR 含 `META-INF/LICENSE`。
- [x] API JAR/API sources JAR 含许可证。
- [x] API JAR 公开类依赖闭包完整，并包含所需 API runtime bridge。
- [x] JAR 未嵌入 LambDynamicLights、JEI、JourneyMap、Xaero、Create 或 shader pack。
- [x] 没有 nested JAR、测试类、开发目录或本机绝对路径。
- [x] 资源包、双语语言文件、shader、recipe 和 module tag 均存在于正确节点。
- [x] 26.1.2 主 JAR 包含 Pair marker 专用 `.vsh`/`.fsh`，且不再包含废弃的
  `PairingMarkerOverlay` class。
- [x] 计算并保存当前候选 SHA-256。
- [ ] 用待发布 JAR 而不是开发 classes 目录完成最终 smoke test；任何 marker/render
  代码变化都会使之前的 shaders off/on 验证结果失效。

当前候选哈希（不是最终发布哈希；release notes 保持 `TBD`）：

- `riftgun-1.21.1-v0.2.0-rc.1.jar`：
  `BC69E1BEE7AE10943B5566FDDF11D673D6E5F84C3A5ECF6390D308D40ABFC834`
- `riftgun-26.1.2-v0.2.0-rc.1.jar`：
  `D302DB21D687128F8B829166DD02C4D02E9A46F70853DD406C6C94ABC63A7128`

## 10. Release notes 必须包含的内容

两份 release notes 应至少有以下章节：

1. 版本、日期、previous version、Minecraft、NeoForge、Java、artifact 名称。
2. 主要功能：Pairing、Remote、Precision、Dimensional Traversal、模块堆叠。
3. GUI/radial 与坐标 clamp 行为变化。
4. JourneyMap/Xaero 集成及其只读/服务器校验边界。
5. Addon API `1.2.0`、API JAR 用法和 pre-1.0 stability 声明。
6. LambDynamicLights：
   - RyoamicLights integration 已移除/不再使用。
   - LambDynamicLights 是 optional client dependency。
   - 不安装时不会崩溃，只是没有附近方块动态光。
   - 1.21.1 最低 `4.8.10`，26.1.2 最低 `4.11.1`。
7. Shader compatibility：两个 Complementary 系列支持中央末地盘面，其他光影 fallback
   仍中央留空；Pair I/II marker 在 shaders off/on 下保持 opaque、固定宽度并遵循
   world-depth 遮挡。
8. Recipe/Create integration 变化。
9. 新 config 文件和生效方式。
10. 网络兼容：client/server 必须使用相同新版本。
11. 升级前备份世界；旧枪 NBT/config 的兼容结果。
12. 已知问题和未覆盖的 optional mod 组合。

## 11. 推荐执行顺序

1. [已完成] 决定 REMOTE 与 recipe 两个产品问题。
2. [已完成] 修复 API artifact class closure 和 LICENSE，并加 artifact tests。
3. [已完成] 加固第三方 callback；从 artifact 移除 destination provider draft。
4. [已完成] 修正 JEI metadata、README 和设计文档。
5. [已完成] 确定 `0.2.0-rc.1` 并更新两个节点。
6. [已完成] network protocol 推进到 `2`。
7. [已完成] 生成 release candidate JAR，记录临时候选 SHA-256。
8. 使用最终 JAR 执行第 7 节矩阵，尤其重测 R05–R09 的 Pair marker 生命周期、
   camera projection、光影可见性和遮挡。
9. 修复回归后重新构建；任何代码变化都会使之前 artifact smoke 结果失效。
10. [已完成] 完成两份 RC release notes；日期、最终 SHA 与图片等待发布前补充。
11. 确认 CI green、`git diff --check` 通过、工作树 clean。
12. 从已验证 commit 创建两个 tag 和发布 artifact。

## 12. Go/No-Go 门槛

只有同时满足以下条件才可判定 **Go**：

- [x] S1、S2、S3、P1 的代码/产品决策已关闭；P2 的最终 release identity 待矩阵后确认。
- [x] 已知 P1/P2 代码与文档项已修复；剩余项目为真实环境验证。
- [x] 两版本完整 build/test 在当前 RC 工作树通过；最终 commit 后需重跑。
- [x] API artifact 独立 fixture 通过。
- [x] dedicated server 无 optional client mod 启动通过。
- [ ] network mismatch 行为符合明确策略。
- [ ] Lamb、JEI、地图、shader 的最低声明版本已实际验证。
- [ ] 旧世界、旧枪 NBT、旧 config 升级通过。
- [ ] 最终 JAR 完成 artifact 审计并保存 SHA-256。
- [ ] release notes 与 artifact 内容、版本和已知问题一致。
- [ ] 工作树完全 clean，tag 指向经过验证的精确 commit。

网络协议与 REMOTE 对外说明已经收束。当前维持 **No-Go** 的原因是：最终 JAR 的
Pair marker/shader 游戏内矩阵、optional mod 最低版本、真实 mixed-protocol、升级和
GUI scale 尚未完成；此外工作树尚未提交，发布日期、最终 SHA 与 tag 仍未锁定。
