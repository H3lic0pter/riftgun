# RiftGun 动态光源依赖与替代方案调研

> 调研日期：2026-09-01。范围：RiftGun 的 NeoForge `1.21.1` 与 `26.1.2` 两个构建节点。本文只使用本仓库源码/构建配置，以及候选项目自己的 Modrinth、GitHub、官方文档和源码。以下“事实”均附一手来源；“判断/推断”单独标明。

> 实施状态：推荐迁移已于 2026-09-01 完成。下文“当前依赖”章节记录的是迁移前基线。

## 结论

**可以替换，而且建议从 RyoamicLights 迁移到官方 LambDynamicLights v4。** 推荐版本是：

| RiftGun 节点 | 推荐 provider | 已核实的正式版本 | 状态 |
|---|---|---|---|
| Minecraft 1.21.1 / NeoForge | LambDynamicLights | `4.8.10+1.21.1` | 可用，但上游明确标记 1.21 系列 EOL |
| Minecraft 26.1.2 / NeoForge | LambDynamicLights | `4.11.1+26.1.2` | 可用，2026-08 仍有维护更新 |

两者都由同一上游、同一套 v4 multi-loader API 提供，都是 client-side provider；服务器不需要安装。官方发布页把两个版本都列为 Fabric / NeoForge / Quilt 构建。[1.21.1 `4.8.10` 发布页](https://modrinth.com/mod/lambdynamiclights/version/4.8.10%2B1.21.1) [26.1.2 `4.11.1` 发布页](https://modrinth.com/mod/lambdynamiclights/version/pnv86kjJ) [官方 v4 开发文档](https://lambdaurora.dev/projects/lambdynamiclights/docs/v4/)

**推荐的集成方式不是静态 JSON，而是 LambDynamicLights v4 Java API。** RiftGun 的亮度会随传送门 opening/closing lifecycle 变化，当前 `PortalDynamicLightLevel` 返回运行时计算值；静态 entity JSON 只能保留固定亮度。v4 Java API 可以注册自定义 `EntityLuminance`，并由同一个 initializer 覆盖两种 portal entity，因而可以完整保留渐亮/渐暗和 client config 上限。[当前亮度算法](../../versions/1.21.1/src/main/java/dev/riftgun/client/light/PortalDynamicLightLevel.java) [官方 Java API：initializer、entity luminance 与 registration event](https://lambdaurora.dev/projects/lambdynamiclights/docs/v4/java.html)

## 当前依赖到底是什么

### 事实

- RiftGun **没有必需的动态光源 runtime dependency**。当前构建把 RyoamicLights 声明为 optional client compile-only，最终 JAR 不会捆绑它；README 也明确说除 NeoForge 外没有必需 runtime dependency。[`build.gradle.kts`](../../build.gradle.kts) [`README.md`](../../README.md)
- `1.21.1` 节点固定到 RyoamicLights `0.2.11+mc1.21.1`，Modrinth project/version ID 为 `reCfnRvJ` / `tApwsw9C`。[节点配置](../../versions/1.21.1/gradle.properties) [对应官方 NeoForge 发布](https://modrinth.com/mod/ryoamiclights/version/0.2.11%2Bmc1.21.1-neoforge)
- 客户端启动后先检查 mod ID `ryoamiclights`，存在时通过 `org.thinkingstudio.ryoamiclights.api.DynamicLightHandlers` 为普通 portal 和 relocation portal 注册 handler；缺失或链接失败时只禁用环境动态光，不影响传送门本身。[provider 边界](../../versions/1.21.1/src/main/java/dev/riftgun/client/light/PortalDynamicLights.java) [Ryoamic compat](../../versions/1.21.1/src/main/java/dev/riftgun/client/light/RyoamicLightsPortalCompat.java)
- `26.1.2` 节点没有 provider dependency，也没有实际注册逻辑；`PortalDynamicLights` 只是一次性记录“disabled”的 no-op seam。[26.1.2 provider seam](../../versions/26.1.2/src/main/java/dev/riftgun/client/light/PortalDynamicLights.java)
- RyoamicLights `0.2.11` 官方页面标注为 **Alpha**、client-side、支持 Minecraft 1.21–1.21.1 和 NeoForge，并要求 ObsidianUI；该发布距本次调研约两年。项目页描述它是 LambDynamicLights 的非官方 Architectury port，许可证为 MIT。[RyoamicLights 0.2.11 NeoForge 发布](https://modrinth.com/mod/ryoamiclights/version/0.2.11%2Bmc1.21.1-neoforge) [官方项目页](https://modrinth.com/mod/ryoamiclights/versions)

### 判断

- 当前设计的 optional boundary 是合理的：动态光源是 client visual enhancement，不应该变成 dedicated server 的硬依赖。
- 问题不在 RiftGun 的亮度计算，而在 provider 已停留在旧的非官方 fork：它只覆盖 1.21.1，导致 26.1.2 永久 no-op；旧 API 也把 RiftGun 绑定到 fork 私有包名。
- 当前 shared `neoforge.mods.toml` 对 `ryoamiclights` 的 optional dependency 会进入两个节点；但 26.1.2 根本没有 compat。这不会强制安装 RyoamicLights，却会产生不准确的兼容性元数据。迁移时应让 metadata 与每个节点的实际 provider 一致。

## 候选对比

| 候选 | 1.21.1 NeoForge | 26.1.2 NeoForge | Client/server | API 与适配性 | 维护/许可证 | 结论 |
|---|---:|---:|---|---|---|---|
| **LambDynamicLights（官方）** | 是，`4.8.10` | 是，`4.11.1` | client-only | v4 multi-loader initializer；entity JSON；自定义 `EntityLuminance`；registration event；官方 Maven API/runtime artifacts | 26.1.2 于 2026-08 仍更新；Lambda License | **推荐** |
| **RyoamicLights（当前）** | 是，`0.2.11` Alpha | 否 | client-only | 旧 `DynamicLightHandlers`，现有代码适配成本最低 | 发布约两年前；MIT | 仅适合维持旧实现，不解决双版本 |
| **Sodium Dynamic Lights** | 是 | 否（官方兼容范围止于 1.21.5） | client-only | 基于旧 LambDynamicLights 的 multiloader port；还增加 Sodium Options API 依赖面 | 官方项目页约一年未更新；MIT | 不推荐，版本更窄且与现代 Sodium 有已知兼容问题 |
| **Lucent** | 否 | 否 | client-only | 定位为 Forge/NeoForge dynamic-light API | 官方版本止于 1.20.4、约两年未更新；Lucent API License | 排除 |
| **AtomicStryker Dynamic Lights** | 有 NeoForge 版本 | 是 | 联机时 client + server | 快速放置/移除 fake air/water blocks；上游仅泛称可从源码添加 light | 活跃；custom license | 不符合 RiftGun 当前纯客户端 optional visual 模型 |
| **Tschipcraft Dynamic Lights** | 是 | 是 | server-side | 以 vanilla light blocks/marker entities 实现；不是 client render API | 活跃；custom license | 版本广，但属于另一种产品/部署模型，不是平替 |

来源：

- LambDynamicLights 的官方项目页列出 client-side、NeoForge 和覆盖 1.21.x、26.1.x 的版本范围，并链接官方源码/API。[项目页](https://modrinth.com/mod/lambdynamiclights) [GitHub](https://github.com/LambdAurora/LambDynamicLights)
- 官方 changelog 记录 v4.5.0 开始 full NeoForge support，v4.8.1 加入动态光源 culling 与 adaptive ticking；`4.8.10` 又明确是 Minecraft 1.21 的 EOL 更新。[changelog](https://github.com/LambdAurora/LambDynamicLights/blob/26.2/CHANGELOG.md) [1.21.1 发布](https://modrinth.com/mod/lambdynamiclights/version/4.8.10%2B1.21.1)
- Sodium Dynamic Lights 官方页面列出的最高兼容游戏版本为 1.21.5，没有 26.1.x；其 GitHub 许可证是 MIT。项目 issue 也记录其 Sodium Options API 与 Sodium 0.8 的兼容问题。[官方版本页](https://modrinth.com/mod/sodium-dynamic-lights/versions) [MIT license](https://github.com/txnimc/SodiumDynamicLights/blob/main/LICENSE.md) [Sodium 0.8 issue](https://github.com/txnimc/SodiumDynamicLights/issues/80)
- Lucent 官方页面列出的最高版本为 Minecraft 1.20.4，故不能覆盖任一目标节点。[官方版本页](https://modrinth.com/mod/lucent/versions)
- AtomicStryker 官方页虽提供 26.1.2 NeoForge 构建，但明确说明用 fake air/water blocks、联机需要 client/server 都安装，而且光源数量会带来较高 FPS 成本。[官方 CurseForge 页](https://www.curseforge.com/minecraft/mc-mods/dynamic-lights)
- Tschipcraft 官方页覆盖 1.21.1 到 26.1.2 NeoForge，却明确是 server-side、基于 light blocks/marker entities，并建议 heavily modded setup 使用 LambDynamicLights 这类 client mod。[官方 CurseForge 页](https://www.curseforge.com/minecraft/mc-mods/tschipcrafts-dynamic-lights-mod)
- LambDynamicLights 的发布元数据明确把 RyoamicLights 与 Sodium Dynamic Lights 列作 known incompatibilities，迁移后不应让用户同时安装它们。[1.21.1 发布](https://modrinth.com/mod/lambdynamiclights/version/4.8.10%2B1.21.1) [26.1.2 发布](https://modrinth.com/mod/lambdynamiclights/version/pnv86kjJ)

### 关于“性能更好”的准确说法

**事实：** LambDynamicLights v4.8.1 官方 changelog 记录了 light-source culling（减少 chunk-section rebuild）与 adaptive ticking（远处、镜头后的光源降低 tick 频率），并允许在 performance 页面配置；v4.8.9 还修复了 recurring null scheduler crashes。这些优化均已包含在推荐的 `4.8.10` 和 `4.11.1` 中。[官方 changelog](https://github.com/LambdAurora/LambDynamicLights/blob/26.2/CHANGELOG.md) [GitHub releases](https://github.com/LambdAurora/LambDynamicLights/releases)

**推断：** 相比停在旧 LambDynamicLights 架构上的 RyoamicLights `0.2.11`，官方 v4 在大量/远距离动态光源下更可能稳定、重建成本更低。但没有在 RiftGun 的目标 modpack、相同场景、相同 JVM/显卡上做 A/B profile，因此不能把“必然提升 FPS”当作已验证事实。RiftGun 通常只有少量 portal light sources，迁移的首要收益是双版本覆盖和活跃维护，性能收益是次要但有架构依据的预期。

## 推荐迁移设计

### 1. 保持 optional、client-only

- 用户只在客户端安装 LambDynamicLights；vanilla server 和 dedicated NeoForge server 都不需要它。官方文档明确说 provider 是 client-side，initializer 在 server 不执行。[官方 Java API 文档](https://lambdaurora.dev/projects/lambdynamiclights/docs/v4/java.html)
- RiftGun 继续 `compileOnly` API，不把 provider 或 API JAR shade/JarJar 进 RiftGun。
- metadata 改为可选 `lambdynlights`，删除 `ryoamiclights`。由于两个 Minecraft 节点使用不同 provider 版本，建议用 node property 生成各自的 version range，而不是在 shared TOML 写死一个范围。

### 2. 使用官方 Maven artifact，不再使用 Modrinth opaque ID 编译 API

官方文档给出的坐标是：

```kotlin
compileOnly("dev.lambdaurora.lambdynamiclights:lambdynamiclights-api:<version>")
```

- 1.21.1 使用 `4.8.10+1.21.1`。该节点属于“before 26.1”，NeoForge/Mojmap build 需按官方示例增加 `net.minecraft.mappings=mojmap` artifact attribute。
- 26.1.2 使用 `4.11.1+26.1.2`，按“26.1 and onwards”示例直接使用 API artifact。
- 如需 dev run 验证，再以 local runtime 加入 `lambdynamiclights-runtime`；release JAR 不捆绑它。

来源：[官方开发环境依赖说明](https://lambdaurora.dev/projects/lambdynamiclights/docs/v4/) [1.21.1 release/version ID](https://modrinth.com/mod/lambdynamiclights/version/4.8.10%2B1.21.1) [26.1.2 release/version ID](https://modrinth.com/mod/lambdynamiclights/version/pnv86kjJ)

### 3. 用 initializer + custom `EntityLuminance` 保留动态亮度

官方 API 在 NeoForge 上也采用 loader-independent initializer，并通过 `neoforge.mods.toml` 的 `modproperties -> yumi:entrypoints -> lambdynlights:initializer` 声明；这样由 provider 决定初始化时序，不需要 RiftGun 在 `ClientModEvents` 中主动探测并调用 provider。[官方 initializer 文档](https://lambdaurora.dev/projects/lambdynamiclights/docs/v4/java.html)

建议实现一个 RiftGun luminance provider：

1. `PortalEntity` 调用现有 `PortalDynamicLightLevel.forPortal`。
2. `EntityRelocationPortalEntity` 调用现有 `forRelocationPortal`。
3. 通过 `EntityLightSourceManager` registration event 分别注册两个 `EntityType`。
4. 保持亮度结果 clamp 在 `0..15`；官方 API 的 luminance contract 也是 `0..15`。

静态 `assets/riftgun/dynamiclights/entity/*.json` 适合固定亮度 entity，但不能表达 RiftGun 当前随 lifecycle/config 变化的值；若强行只用 JSON，opening/closing fade 会退化为常亮/常灭。[官方 entity JSON 格式](https://lambdaurora.dev/projects/lambdynamiclights/docs/v4/entity.html) [当前 lifecycle 算法](../../versions/1.21.1/src/main/java/dev/riftgun/client/light/PortalDynamicLightLevel.java)

### 4. 清理旧 provider 边界

预计涉及（不含测试）4–7 个小文件：

- build logic 与两个 node properties；
- `neoforge.mods.toml` initializer/optional dependency metadata；
- 新 LambDynamicLights initializer / luminance provider；
- 删除 `RyoamicLightsPortalCompat`；
- 删除或收缩两个节点的 `PortalDynamicLights.initialize()` 与 `ClientModEvents` 调用。

**迁移成本判断：低到中。** 亮度业务算法已经隔离在 `PortalDynamicLightLevel`，无需改 portal lifecycle、renderer 或网络同步；主要风险在 Gradle mapping attributes、NeoForge manifest entrypoint 语法，以及分别启动两个节点验证 initializer 被 provider 调用。

## 许可证与发布注意事项

- RyoamicLights 与 Sodium Dynamic Lights 标为 MIT。[RyoamicLights 项目页](https://modrinth.com/mod/ryoamiclights/versions) [Sodium Dynamic Lights license](https://github.com/txnimc/SodiumDynamicLights/blob/main/LICENSE.md)
- LambDynamicLights 使用自定义 **Lambda License**，其文本明确说明它不符合通常的 FOSS 定义；修改/分发其源码或 binaries 有额外条款，分发 binary 还涉及取得作者书面批准。[官方 LICENSE](https://github.com/LambdAurora/LambDynamicLights/blob/26.2/LICENSE)
- **工程建议（不是法律意见）：** RiftGun 只通过官方 API `compileOnly`，不复制上游实现、不修改、不重分发、不 shade/JarJar provider/API；最终用户从上游渠道单独安装 LambDynamicLights。这样能把 RiftGun 的发布物与上游 binary 分发清楚分离。若未来想把 LambDynamicLights 或其 API JAR 捆绑进 RiftGun/modpack installer，应先单独审查 Lambda License 并向上游确认授权。

## 验证清单

若执行迁移，至少验证：

1. 两节点在 **未安装 LambDynamicLights** 时均能启动，且没有 optional API classloading/linkage error。
2. `1.21.1 + NeoForge + LambDynamicLights 4.8.10`：两类 portal 都发光，opening/closing 亮度渐变与 config 上限生效。
3. `26.1.2 + NeoForge + LambDynamicLights 4.11.1`：同上，不再出现当前 no-op 日志。
4. dedicated server 不安装 provider 也能启动。
5. Iris/Complementary 开关不改变动态光源注册结果；动态光源是 provider 的 client lighting path，不应依赖 shader-pack profile。
6. 不同时安装 RyoamicLights 或 Sodium Dynamic Lights；官方 metadata 将它们列为 incompatible。
7. 用相同传送门数量做一次 Spark/JFR 或 frame-time A/B，记录 chunk rebuild 与 client tick；在此之前只声称“架构更新、版本覆盖更广”，不要对实际 FPS 作定量承诺。

## 最终建议

采用 **单一 provider、分版本 pin**：

- `1.21.1` pin `LambDynamicLights 4.8.10+1.21.1`，接受该 Minecraft 分支已 EOL，只做兼容性维护。
- `26.1.2` pin `LambDynamicLights 4.11.1+26.1.2`。
- RiftGun 内部使用同一套 v4 initializer / custom luminance 设计，必要时仅保留很薄的 node-specific build/manifest 差异。
- 不再并行支持 RyoamicLights：官方 LambDynamicLights 本身声明与它不兼容，双 provider compatibility layer 会增加测试矩阵，也没有版本覆盖收益。

这比“1.21.1 用 RyoamicLights、26.1.2 再找另一个 provider”更稳：用户面对的是同一品牌和配置体系，RiftGun 只维护一个公开 API seam，未来升级到 26.2 也已有同一上游的正式版本可接续。[LambDynamicLights releases](https://github.com/LambdAurora/LambDynamicLights/releases)
