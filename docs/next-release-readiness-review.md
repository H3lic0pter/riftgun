# Rift Gun 下一版本发布准备审查

- 审查日期：2026-09-01
- 审查分支：`dev`
- 审查终点：`e968e61`（`perf(gui): trim preview and row allocations`）及当前工作树
- 上次发布基线：`cfc26e47d95d7f7abf1800a225f05987800d8f07`
- 基线 tag：`mc1.21.1-v0.1.1-r1`、`mc26.1.2-v0.1.1-r1`
- 审查范围：基线之后 66 个 commit、425 个文件
- 变更规模：新增 23,730 行、删除 3,528 行（不含当前工作树）
- 当前结论：**No-Go，不应直接发布**

## 1. 执行摘要

自 `0.1.1-r1` 发布以来，Rift Gun 已经从一次常规修补演变为包含新玩法、
新 GUI、新扩展 API、新第三方集成和新渲染兼容层的大版本更新。主要功能已具备，
两条版本线的自动化测试也全部通过，但发布产物、网络兼容声明和产品规范尚未完全
收束。

当前有两类明确发布阻断：

1. 两节点仍使用已经发布过的 `0.1.1-r1` 版本号，网络 registrar 也仍为协议
   `"1"`，但本轮已经改变网络 snapshot schema。
2. REMOTE 到底由 Portal Pairing Module 提供，还是由独立 Remote Module 提供，
   设计、README 和实现目前互相矛盾。

原 API artifact 阻断已在当前工作树修复：binary artifact 包含
`RiftGunApiBootstrap`，binary/source artifacts 均包含 `META-INF/LICENSE`，并由最终
artifact 内容检查、隔离 classloader 链接 smoke test 和最小 Addon 编译 fixture 覆盖。

解决上述阻断后，仍需修正 26.1.2 JEI metadata、收紧公共 callback 的故障隔离、
更新玩家文档，并完成双 Minecraft 节点的真实运行环境 smoke test。

考虑本轮新增能力和变更规模，建议下一版本进入 **`0.2.0` 系列**，而不是继续把它
描述为 `0.1.1-r2` 级别的小修补。若仍需公开预发布验证，可先发布
`0.2.0-rc.1`，通过矩阵后再发布正式构建。

## 2. 审查方法和证据

本审查同时从两个轴检查变更：

- **Standards**：代码边界、artifact 完整性、运行时安全、性能、维护性、仓库规范。
- **Spec**：实现是否符合设计文档、README、已确认用户需求和对外兼容声明。

本次已确认：

- `git diff --check cfc26e4..HEAD` 通过。
- Minecraft 1.21.1：540 项测试，0 failure、0 error、0 skipped。
- Minecraft 26.1.2：536 项测试，0 failure、0 error、0 skipped。
- 两节点完整 `build` 已通过。
- 跨版本 GUI/preview 重复状态已在 `76ed68a` 抽到 shared 层，并加入 architecture
  source test 防止业务逻辑重新漂回节点 adapter。
- 当前源码没有遗留试错/逐帧 debug log。
- 预览 marker 仍使用共享 batch，不会为任意 I/II 标记添加动态光源。
- LambDynamicLights 保持 client-only、optional、`compileOnly`，未嵌入发布 JAR。
- 当前工作树显示三个 `.M`，但 blob 与 index hash 相同，`git diff --quiet`
  返回成功；它们是换行符/stat 噪声，不是语义修改。打 tag 前仍必须刷新索引并确认
  `git status --short` 真正为空。

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

### S2 — P1：mod 版本和网络协议没有随 wire schema 推进

**证据**

- [`versions/1.21.1/gradle.properties`](../versions/1.21.1/gradle.properties#L10)
  仍为 `mod_version=0.1.1-r1`。
- [`versions/26.1.2/gradle.properties`](../versions/26.1.2/gradle.properties#L9)
  仍为 `mod_version=0.1.1-r1`。
- 1.21.1 和 26.1.2 的 `NeoForgeNetworkAdapter` 仍使用 registrar `"1"`：
  [1.21.1](../versions/1.21.1/src/main/java/dev/riftgun/network/NeoForgeNetworkAdapter.java#L18)、
  [26.1.2](../versions/26.1.2/src/main/java/dev/riftgun/network/NeoForgeNetworkAdapter.java#L16)。
- 本轮新增 `PortalGunViewState` envelope，并改变 `Gun` snapshot 的字段集合。
  例如旧 schema 的 `SurfaceRange` 已被新的 REMOTE/Precision/Pairing 状态取代或扩展。

**影响**

- 新 artifact 会复用已发布 artifact identity，用户和发布平台难以区分文件。
- 旧客户端和新服务器可能以“相同 mod 版本、相同 network protocol”建立连接，
  之后再以默认值静默解析不存在或含义已变化的 NBT 字段。
- 静默错读比明确断开更危险，因为它可能表现为错误 GUI、错误 module state 或错误请求。

**发布前要求**

- 两节点同步推进 mod version。
- 明确审查旧 `0.1.1-r1` 与新 schema 是否双向兼容。
- 如果不能证明双向兼容，推进 registrar protocol，使 NeoForge 在握手阶段拒绝新旧混连。
- 添加新旧 protocol mismatch 的集成测试或最小手工记录。

**验收条件**

- 发布 JAR 文件名、manifest version 和 release notes 使用同一新版本。
- 新客户端/旧服务器、旧客户端/新服务器均被明确拒绝，或存在经过测试的兼容 codec。
- 相同新版本的客户端和服务器可以正常完成 snapshot、radial、外部地图目标和开门请求。

### S3 — 已修复：API artifacts 缺少 MIT LICENSE

**解决结果**

- `apiJar` 和 `apiSourcesJar` 都从仓库根目录复制 `LICENSE` 到
  `META-INF/LICENSE`。
- artifact-content test 同时检查 binary 和 sources JAR，防止 task 重构再次遗漏。

### S4 — P2：公共 provider callback 缺少故障隔离

**证据**

- [`RiftGunDimensionLabels.java`](../src/main/java/dev/riftgun/api/RiftGunDimensionLabels.java#L27)
  在生成 snapshot label 时依次调用第三方 provider。
- [`RiftGunPortalOpenPolicies.java`](../src/main/java/dev/riftgun/api/RiftGunPortalOpenPolicies.java#L25)
  在开门请求热路径调用第三方 policy。
- provider 抛出的 `RuntimeException` 当前会向 RiftGun 主流程传播。

**影响**

- 一个坏 Addon 可以阻断所有 dimension label，甚至中断 GUI snapshot。
- 一个坏 portal-open policy 可以阻断正常开门流程。
- 公共扩展点发布后，这类失败会被玩家归因于 RiftGun。

**发布前要求**

- 每个 provider 独立 `try/catch RuntimeException`。
- Dimension label provider 出错时跳过该 provider，继续尝试后续 provider 或 fallback。
- Portal-open policy 建议 fail-closed，避免异常时绕过权限限制。
- 使用 provider ID 输出一次性或 rate-limited warning，禁止每 tick/每帧刷 log。
- 添加“第一个 provider 抛异常、第二个仍可工作”的测试。

**验收条件**

- 坏 provider 不会让整个 snapshot 或 server tick 失败。
- policy 异常不会放宽访问权限。
- 日志足够定位 provider ID，但不会在重复请求中刷屏。

### S5 — P2：README 遗漏 1.21.1 common config

**证据**

- [`versions/1.21.1/.../RiftGun.java`](../versions/1.21.1/src/main/java/dev/riftgun/RiftGun.java#L143)
  注册 `IntegrationConfig.SPEC` 为 COMMON config。
- [`README.md`](../README.md#L176) 目前只说明 client/server config。

**影响**

- 玩家不知道 `riftgun-common.toml` 的存在。
- Create mixer integration 开关及其重启/加载语义不清楚。

**发布前要求**

- README 增加 `riftgun-common.toml`，并标注它目前只适用于 1.21.1 的 Create integration。
- 说明开关需要重启、reload 还是重新进入世界才能生效。
- release notes 单独列出新 config，避免旧玩家漏看。

**验收条件**

- README 中的配置文件列表与两个节点实际注册结果一致。
- 每个配置文件注明 side、主要用途和生效时机。

### S6 — P3：`RiftGunDestinationProviders` 是未接通的公开 API

**证据**

- [`RiftGunDestinationProviders.java`](../src/main/java/dev/riftgun/api/RiftGunDestinationProviders.java#L9)
  提供注册和查询 API。
- 生产代码没有消费 `RiftGunDestinationProviders`；只有测试调用它。

**影响**

- Addon 可以成功注册 provider，但玩家不会在 RiftGun 中看到任何结果。
- API version 已到 `1.2.0`，继续公开该接口会固化一个没有行为契约的 seam。

**发布前建议**

- 若本轮承诺支持通用 destination provider：接入 GUI/source aggregation，并定义
  server trust boundary、排序、刷新和生命周期。
- 若本轮只支持内置 JourneyMap/Xaero bridge：暂时从 public API artifact 移除该接口，
  待生产消费点完成后再发布。

**验收条件**

- 注册 provider 后有明确、可测试的生产行为；或 API JAR 不再承诺该能力。

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

### S8 — P2：26.1.2 JEI dependency range 与编译版本不一致

**证据**

- [`versions/26.1.2/gradle.properties`](../versions/26.1.2/gradle.properties#L7)
  编译 `jei_version=29.29.0.76`。
- [`versions/26.1.2/.../neoforge.mods.toml`](../versions/26.1.2/src/main/resources/META-INF/neoforge.mods.toml#L38)
  声明 `versionRange="[19.21,)"`。

**影响**

- metadata 会把没有验证过、可能不兼容当前 bridge API 的 JEI 版本视为可接受。
- 1.21.1 的 version range 被复制到 26.1.2，平台兼容声明不准确。

**发布前要求**

- 以真实验证的最低 26.1.2 JEI 版本为下界，建议从当前编译版本
  `29.29.0.76` 开始，除非兼容测试证明可以放宽。
- 在 release smoke matrix 中测试 metadata 下界，而不只是开发环境当前版本。

**验收条件**

- manifest 范围与已验证版本一致。
- 无 JEI 时 RiftGun 正常启动；安装范围下界 JEI 时配方类别能正常加载。

## 5. Spec 审查发现

### P1 — P1：REMOTE capability 的产品定义冲突

**互相冲突的来源**

- [`portal-pairing-design.md`](portal-pairing-design.md#L38) 规定 REMOTE 仅在枪具有
  Portal Pairing capability 时可见/可选。
- [`README.md`](../README.md#L117) 说 Portal Pairing Module 添加 REMOTE placement。
- [`PortalGunCapabilities.java`](../src/main/java/dev/riftgun/module/PortalGunCapabilities.java#L59)
  以独立 `PortalModuleKind.REMOTE` 判断 REMOTE 是否安装。
- [`PortalModules.java`](../src/main/java/dev/riftgun/module/PortalModules.java#L66)
  注册了独立 Remote Module。

**当前实际行为**

- 只安装 Portal Pairing Module 时，玩家不能使用 REMOTE。
- 只安装 Remote Module 时，可获得 REMOTE 相关能力；Pairing 的 SMART fallback 还同时
  取决于 Pairing 与 Remote 是否安装。

**建议**

丢掉过时的文档。以目前为准

**验收条件**

- 无模块、只装 Pairing、只装 Remote、同时安装两者，四种组合都有 capability test。
- GUI 可见性、radial 可选项和 server request validation 使用同一个能力模型。


### P2 — P1：release notes 草稿已创建，release identity 尚未推进

**现状**

- 两节点仍为 `0.1.1-r1`。
- 已按 `0.2.0-r1` 创建两节点草稿：
  - [`1.21.1-v0.2.0-r1.md`](release-notes/1.21.1-v0.2.0-r1.md)
  - [`26.1.2-v0.2.0-r1.md`](release-notes/26.1.2-v0.2.0-r1.md)
- 草稿仍保留发布日期、最终 artifact、SHA-256、network protocol、REMOTE capability、
  recipe 基线和图片的发布前 TODO；创建草稿不代表该 finding 已关闭。
- 本轮包含新 gameplay、API、network schema、optional dependency 和 shader 行为，不属于
  旧发布的 rebuild。

**发布前要求**

- 确定下一版本号，建议使用 `0.2.0` 系列。
- 使用`docs/release-note-prompt.md`分别新增：
  - `docs/release-notes/1.21.1-v<version>.md`
  - `docs/release-notes/26.1.2-v<version>.md`
- 两份 release notes 共享核心内容，但平台差异不能省略。
- release notes 至少写明：
  - Portal Pairing、Remote、Precision Placement、Dimensional Traversal。
  - 模块堆叠和 recipe 变化。
  - JourneyMap/Xaero 只读 waypoint integration。
  - Public Addon API `1.2.0` 及其稳定性声明。
  - RyoamicLights → LambDynamicLights 迁移。
  - Complementary Reimagined/Unbound 的中央末地盘面支持。
  - network compatibility 与 client/server 同版本要求。
  - 新 config 文件和升级/备份说明。

**验收条件**

- Gradle version、JAR 文件名、manifest、tag、release title、release notes 完全一致。
- 两节点 release notes 明确各自 Java、NeoForge 和 optional dependency 版本。

### P3 — P2：两处 recipe 与当前设计文档不一致

#### Dimensional Traversal

- [`dimensional-traversal-design.md`](dimensional-traversal-design.md#L19) 明确写
  “No crafting recipe in this iteration”。
- 当前两个节点都提供 `dimensional_traversal_module.json`。

#### Portal Pairing

- [`portal-pairing-design.md`](portal-pairing-design.md#L64) 规定 Ender Pearl、Compass、
  Redstone、Quartz 主题。
- 当前 recipe 使用 poisonous potato、chain、oxidized copper、copper block 等材料。

**建议**

丢掉过时的文档。

### P4 — P2：README 未覆盖完整玩家能力

**缺失或不完整内容**

- 模块表缺少 Dimensional Traversal、Remote、Precision Placement。
- Portal Pairing 对 REMOTE 的描述与实现冲突。
- 没有充分解释 JourneyMap/Xaero 的只读数据流、server validation 和会话生命周期。
- 没有面向 Addon 作者说明 API artifact、API version 和 pre-1.0 稳定性。
- 没有说明 `riftgun-common.toml`。
- 旧 release notes 仍提到 RyoamicLights，新版本升级文档尚未解释迁移。

**发布前要求**

- 更新 Requirements、Getting Started、Modules、Optional Integrations、Configuration、
  Compatibility 和 Development/API 章节。
- 明确“没有 LambDynamicLights 时不崩溃，只是不产生附近方块动态光”。
- 明确 I/II preview marker 永远不参与动态光源注册。
- 对地图 integration 说明客户端读取 waypoint、服务器重新校验维度和坐标，不能把客户端
  waypoint 当成可信 server state。

**验收条件**

- 一名未阅读设计文档的玩家只看 README 即可正确选择模块和 optional mods。
- README 版本表与两个 manifest/Gradle 文件一致。

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
- [ ] 决定 REMOTE capability 模型并统一实现、测试、设计和 README。
- [ ] 确定下一版本号并同时更新两个节点。
- [ ] 审查 wire schema；不兼容时推进 network protocol。
- [ ] 新旧 client/server mismatch 能明确拒绝，而不是进入游戏后错读 snapshot。

### 6.2 P1：release candidate 前完成

- [ ] 修正 26.1.2 JEI dependency range。
- [ ] 为公共 dimension label/open policy callback 增加异常隔离。
- [ ] 决定 Dimensional Traversal 与 Portal Pairing recipe 的权威版本。
- [ ] 补 README 的模块、地图、动态光源、common config 和 API 文档。
- [x] 分别起草两个节点 `0.2.0-r1` release notes；最终发布前清除草稿 TODO。
- [ ] 检查主 JAR/API JAR/sources JAR 内容、命名、许可证和无意捆绑依赖。
- [ ] 建立并执行第 7 节 smoke matrix。

### 6.3 P2：正式发布前完成或明确登记为已知问题

- [ ] 接通或暂缓发布 `RiftGunDestinationProviders`。
- [ ] 记录 26.1.2 相比 1.21.1 的平台差异，例如 Create/Immersive Portals 支持范围。
- [ ] 记录性能基线，尤其是多传送门 + shader + dynamic lights 场景。
- [ ] 清理工作树换行符/stat 噪声，确保 tag 从完全 clean 的 commit 创建。
- [ ] 检查所有玩家可见中英文翻译键。
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
| A01 | 1.21.1 | Java 21 执行 `build` | 所有 tests 和 JAR verification 通过 | 已通过，548 tests；版本 bump 后重跑 |
| A02 | 26.1.2 | Java 25 执行 `build` | 所有 tests 和 JAR verification 通过 | 已通过，544 tests；版本 bump 后重跑 |
| A03 | 两者 | `git diff --check` | 无 whitespace error | 已通过，最终 commit 后重跑 |
| A04 | 两者 | 检查主 JAR | 正确 version、LICENSE、无 accidental nested dependency | 待执行 |
| A05 | 两者 | 检查 API JAR | class closure 自洽、包含 LICENSE | 自动化已通过；最终版本 bump 后重跑 |
| A06 | 两者 | 最小 Addon fixture 只依赖 API JAR | 编译和基础入口 smoke 通过 | 自动化已通过；最终版本 bump 后重跑 |
| A07 | 两者 | GUI/preview shared seam architecture tests | 节点 adapter 不重新拥有共享业务状态 | 已通过 |

### 7.2 无 optional mod 基线

| ID | 节点 | 环境 | 预期结果 | 状态 |
|---|---|---|---|---|
| B01 | 1.21.1 | client，仅 NeoForge + RiftGun | 启动、进世界、开 GUI、开门正常 | 待执行 |
| B02 | 26.1.2 | client，仅 NeoForge + RiftGun | 同上 | 待执行 |
| B03 | 1.21.1 | dedicated server，仅 NeoForge + RiftGun | 无 client classloading crash | 待执行 |
| B04 | 26.1.2 | dedicated server，仅 NeoForge + RiftGun | 无 client classloading crash | 待执行 |
| B05 | 两者 | client 连接同版本 dedicated server | snapshot、radial、开门和传送正常 | 待执行 |

### 7.3 网络兼容

| ID | 客户端 | 服务端 | 预期结果 | 状态 |
|---|---|---|---|---|
| N01 | 新 1.21.1 | 新 1.21.1 | 正常连接和交互 | 待执行 |
| N02 | 旧 0.1.1-r1 | 新 1.21.1 | 握手阶段明确拒绝，除非有已验证兼容 codec | 当前风险 |
| N03 | 新 1.21.1 | 旧 0.1.1-r1 | 同上 | 当前风险 |
| N04 | 新 26.1.2 | 新 26.1.2 | 正常连接和交互 | 待执行 |
| N05 | 旧 0.1.1-r1 | 新 26.1.2 | 明确拒绝或已验证兼容 | 当前风险 |
| N06 | 新 26.1.2 | 旧 0.1.1-r1 | 明确拒绝或已验证兼容 | 当前风险 |

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
| R05 | 1.21.1 | I/II preview，玩家步行/冲刺/转头 | 不遮挡闪烁、不周期位移/跳回 | 用户已验证修复，最终 JAR 重测 |
| R06 | 1.21.1 | 远离世界原点的 I/II preview | camera-relative 精度稳定 | 待执行 |
| R07 | 两者 | preview 与实体/方块遮挡 | depth 行为正确 | 待执行 |
| R08 | 两者 | 大量可见传送门 | 无逐帧 log、无明显 allocation/frame-time 回退 | 待 profile |

### 7.7 Gameplay、GUI 和升级

| ID | 场景 | 预期结果 | 状态 |
|---|---|---|---|
| G01 | 无模块/Pairing only/Remote only/两者同时 | capability、radial、server validation 符合最终决策 | 当前 spec 冲突 |
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
- 1.21.1 preview 修复保留一个共享 render batch，没有退回每 marker 独立 flush。
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

对两个节点的最终构建分别执行并记录：

- [ ] 主 JAR 名称包含正确 Minecraft version 和 mod version。
- [ ] manifest 中 mod version 与文件名一致。
- [ ] 主 JAR 含 `META-INF/LICENSE`。
- [x] API JAR/API sources JAR 含许可证。
- [x] API JAR 公开类依赖闭包完整，并包含所需 API runtime bridge。
- [ ] JAR 未嵌入 LambDynamicLights、JEI、JourneyMap、Xaero、Create 或 shader pack。
- [ ] 没有 debug fixture、测试类、开发配置或本机绝对路径。
- [ ] 资源包、语言文件、shader、recipe 和 tags 均存在于正确节点。
- [ ] 计算并保存 SHA-256。
- [ ] 用待发布 JAR 而不是开发 classes 目录完成最终 smoke test。

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
   仍中央留空。
8. Recipe/Create integration 变化。
9. 新 config 文件和生效方式。
10. 网络兼容：client/server 必须使用相同新版本。
11. 升级前备份世界；旧枪 NBT/config 的兼容结果。
12. 已知问题和未覆盖的 optional mod 组合。

## 11. 推荐执行顺序

1. 先决定 REMOTE 与 recipe 两个产品问题，避免代码修复后再次返工。
2. [已完成] 修复 API artifact class closure 和 LICENSE，并加 artifact tests。
3. 加固第三方 callback；处理 destination provider API 是否发布。
4. 修正 JEI metadata、README 和设计文档。
5. 确定 `0.2.0` 系列版本并更新两个节点。
6. 审查/推进 network protocol。
7. 生成 release candidate JAR，记录 SHA-256。
8. 使用最终 JAR 执行第 7 节矩阵。
9. 修复回归后重新构建；任何代码变化都会使之前 artifact smoke 结果失效。
10. 完成两份 release notes。
11. 确认 CI green、`git diff --check` 通过、工作树 clean。
12. 从已验证 commit 创建两个 tag 和发布 artifact。

## 12. Go/No-Go 门槛

只有同时满足以下条件才可判定 **Go**：

- [ ] S1、S2、S3、P1、P2 全部关闭。
- [ ] 所有 P1/P2 项已修复，或经明确产品决策登记为可接受风险。
- [ ] 两版本完整 build/test 在最终 commit 上通过。
- [ ] API artifact 独立 fixture 通过。
- [ ] dedicated server 无 optional client mod 启动通过。
- [ ] network mismatch 行为符合明确策略。
- [ ] Lamb、JEI、地图、shader 的最低声明版本已实际验证。
- [ ] 旧世界、旧枪 NBT、旧 config 升级通过。
- [ ] 最终 JAR 完成 artifact 审计并保存 SHA-256。
- [ ] release notes 与 artifact 内容、版本和已知问题一致。
- [ ] 工作树完全 clean，tag 指向经过验证的精确 commit。

只要新旧网络端仍可能以协议 `"1"` 混连，或 REMOTE capability 仍存在互相冲突的
对外说明，就应维持 **No-Go**。
