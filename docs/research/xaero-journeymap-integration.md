# Xaero's Minimap / JourneyMap 坐标联动调研

> 调研日期：2026-08-26。范围：RiftGun 的 NeoForge 1.21.1 与 Minecraft 26.1.2 节点。本文只使用官方项目页、官方仓库/API 源码和官方指定的 Maven artifact。

## 结论

**能做，但两个集成的稳定性不同。**

- **JourneyMap：建议实现。** JourneyMap API v2 直接提供 `IClientAPI#getAllWaypoints()` 与按维度读取的重载；返回值是 copy，修改 copy 不会修改 JourneyMap。这与 RiftGun 的“只读虚拟分组”模型正好匹配。[官方 `IClientAPI`](https://github.com/TeamJM/journeymap-api/blob/1.21.1_2.0.0/common/src/main/java/journeymap/api/v2/client/IClientAPI.java#L251-L277)
- **Xaero's Minimap：技术上可行，但应标记为 experimental compatibility。** 官方提供了给开发者的 Maven dependency，但没有像 JourneyMap 那样独立、有读取合约的 waypoint API；只能编译期引用 Minimap 的 public implementation classes。[官方开发者依赖说明](https://modrinth.com/mod/xaeros-minimap#for-developers)
- **联动必须在客户端采集。** Xaero's Minimap 是客户端地图；JourneyMap 也明确说地图展示需要客户端，服务端安装只用于管理/限制功能。[官方 Xaero 项目页](https://modrinth.com/mod/xaeros-minimap) [官方 JourneyMap 项目页](https://modrinth.com/plugin/journeymap)

## 建议的 RiftGun 产品行为

1. 在 GUI 左栏动态追加两个虚拟 section：`Xaero's Minimap` 与 `JourneyMap`。没有安装对应 mod 时不显示。
2. 条目可查看、选择和传送，但不提供 rename、改坐标、改分组、drag/reorder、delete 或写回地图 mod。“不可编辑”由来源类型而不是特殊 UUID 判断。
3. 不将外部 waypoint 复制进 `PortalPlayerData.destinations()`；GUI 打开时建立 client-only snapshot，关闭/换服务器时丢弃。这能保持单一事实来源，地图端删除/改名不会在 RiftGun 留下孤儿副本。
4. 发射时使用新的 `EXTERNAL_DESTINATION` request，发送 source/id/name/dimension/x/y/z/yaw snapshot；服务端必须复用现有 dimension allow-list、world-border、数值有限性、安全落点与燃料检查。不能把“GUI 不可编辑”当作 packet 可信边界。
5. 现有 `RiftGunDestinationProvider` 接收 `ServerPlayer`，是 server-side provider，不适合直接读 client-only 地图。建议新建 client seam，例如 `ClientExternalDestinationProvider -> List<ExternalDestinationSnapshot>`，不要让 common/server classes 引用 Xaero/JourneyMap types。

## JourneyMap

### 读取与字段映射

- `IClientAPI#getAllWaypoints()` 返回当前 game/server 的全部 waypoint copy；也可传 `ResourceKey<Level>` 按维度读取。这不需要读 JourneyMap 的私有存储文件。[官方源码](https://github.com/TeamJM/journeymap-api/blob/1.21.1_2.0.0/common/src/main/java/journeymap/api/v2/client/IClientAPI.java#L251-L277)
- `Waypoint` 有稳定 `guid`、name、XYZ，以及 `getPrimaryDimension()` / `getDimensions()`。RiftGun 应用 `guid` 作 provider-local identity，用 primary dimension 作传送维度；若 primary dimension 缺失或无法解析为 Minecraft resource identifier，该条目禁用并显示原因。[官方 `Waypoint`](https://github.com/TeamJM/journeymap-api/blob/1.21.1_2.0.0/common/src/main/java/journeymap/api/v2/common/waypoint/Waypoint.java#L21-L126)
- JourneyMap v2 waypoint 坐标为 integer；JourneyMap 没有与 RiftGun 一致的必填 yaw。建议 yaw 使用 RiftGun 的无方向默认值（0°），或在传送时保持玩家当前 yaw；这是产品决策，不是 JourneyMap 数据。

### 初始化、刷新和依赖

- 官方推荐用 `@JourneyMapPlugin` + `IClientPlugin` 获得 `IClientAPI`，且要把所有 JourneyMap type 限制在 plugin/compat classes 内，避免 JourneyMap 缺席时 classload 失败。NeoForge 可自动发现 plugin。[官方 how-to](https://github.com/TeamJM/journeymap-api/blob/1.21.1_2.0.0/docs/howto.md)
- Waypoint CRUD/READ 事件可用于实时 invalidate snapshot。旧 client event 已标记弃用，官方指向 `CommonEventRegistry.WAYPOINT_EVENT`；事件 context 包含 CREATE、UPDATE、DELETED、READ，READ 还会在 join、dimension change 与 cache refresh 时批量触发。[官方 `WaypointEvent`](https://github.com/TeamJM/journeymap-api/blob/1.21.1_2.0.0/common/src/main/java/journeymap/api/v2/client/event/WaypointEvent.java#L28-L70)
- 依赖应为 `compileOnly info.journeymap:journeymap-api-neoforge:<version>`，不 shading，不把 `journeymap.*` classes 放进 RiftGun jar；mod metadata 用 optional/client dependency。这是 JourneyMap 官方明确要求。[官方 how-to](https://github.com/TeamJM/journeymap-api/blob/1.21.1_2.0.0/docs/howto.md)

### 目标版本

| RiftGun 节点 | 官方 API artifact | 判定 |
|---|---|---|
| 1.21.1 NeoForge | `journeymap-api-neoforge:2.0.0-1.21.1` | 有 stable artifact 与对应源码 branch，可实现。 |
| 26.1.2 NeoForge | `journeymap-api-neoforge:2.0.0-26.1` | Maven 已发布 stable 26.1 artifact；JourneyMap 官方项目页列出 26.1.x NeoForge 支持。实现前仍应用项目的 26.1.2 toolchain 做 compile/runtime smoke test。 |

版本索引：[官方指定的 JourneyMap Maven 镜像](https://maven.blamejared.com/info/journeymap/journeymap-api-neoforge/) [JourneyMap 官方项目页](https://modrinth.com/plugin/journeymap)

### 许可证

API 许可条款允许自己的代码把 API 作为 dependency，但禁止分发 `journeymap.*` source/classes。所以 compileOnly 是必须的，不能 shading API。[官方 API 源码中的许可声明](https://github.com/TeamJM/journeymap-api/blob/1.21.1_2.0.0/common/src/main/java/journeymap/api/v2/client/IClientPlugin.java#L1-L19)

## Xaero's Minimap

### 已验证的读取路径

以官方 Maven 的 deobfuscated NeoForge artifact `26.4.2` 做 `javap -public`。Artifact 是官方项目页为“hook into this mod”指定的构建依赖，不是反编译后重发的第三方 jar。[官方开发者说明](https://modrinth.com/mod/xaeros-minimap#for-developers) [官方 Maven 根索引](https://chocolateminecraft.com/maven/xaero/minimap/)

| 节点 | public 访问路径 | 官方 artifact |
|---|---|---|
| 1.21.1 | `XaeroMinimapSession.getCurrentSession()` → `getWaypointsManager()` → inherited `getWorldManager()` → current root/world → each `WaypointSet#getWaypoints()` | [`xaerominimap-neoforge-1.21.1:26.4.2`](https://chocolateminecraft.com/maven/xaero/minimap/xaerominimap-neoforge-1.21.1/26.4.2/xaerominimap-neoforge-1.21.1-26.4.2.jar) |
| 26.1.2 | `XaeroMinimapSession.getCurrentSession()` → `getHudMod()` → HUD module/session → `MinimapSession#getWorldManager()` → current root/world → each `WaypointSet#getWaypoints()` | [`xaerominimap-neoforge-26.1.2:26.4.2`](https://chocolateminecraft.com/maven/xaero/minimap/xaerominimap-neoforge-26.1.2/26.4.2/xaerominimap-neoforge-26.1.2-26.4.2.jar) |

实际需要的 public 数据是 `Waypoint#getName/getX/getY/getZ/getYaw/isDisabled/isTemporary`，维度来自所属 `MinimapWorld#getDimId()`，set 名来自 `WaypointSet#getName()`。为防止读到其他服务器的本地记录，只应遍历 **current root container** 下的 worlds，不遍历全局 root map。

### 风险与刷新

- 1.21.1 与 26.1.2 的 public class graph 已明显不同：26.1.2 的 `XaeroMinimapSession` 不再提供 `getWaypointsManager()`，旧 `WaypointsManager/WaypointWorld/WaypointSet` 类也已移除/收敛到 `xaero.hud.minimap.*`。因此必须在两个 version source set 各写 adapter，不能假设一套 common binary API。上表两个官方 artifact 可直接验证该差异。
- 未找到 Xaero 官方文档化的 native waypoint CREATE/UPDATE/DELETE event 或兼容性保证。不使用 mixin 或反射私有字段；最终产品决策是仅在 RiftGun GUI 首次打开和玩家点击刷新按钮时重建 snapshot，不做定时轮询，disconnect/server switch 时清空。
- Xaero waypoint 没有已验证的持久 GUID。建议 snapshot ID 由 `source + current-root + dimension + set + name + x/y/z` 确定性派生；rename/move 会被视为新条目，这是可接受的只读 snapshot 语义。
- 不建议直接解析 Xaero waypoint 文件：world/server/sub-world/dimension 路径和维度缩放是它的内部模型，从活会话 public objects 读取能避免重复这套选择逻辑。

### 依赖与许可证

- 按官方说明加 `https://chocolateminecraft.com/maven`，NeoForge 用 `compileOnly`/`implementation` 开发 artifact，运行时依赖必须为 optional client。[官方开发者说明](https://modrinth.com/mod/xaeros-minimap#for-developers)
- 项目页标记许可证为 ARR，不要 shading、复制 classes 或重发 Xaero jar。“官方教程允许 hook”足以支持普通的 optional compile linkage，但不等于稳定 API/ABI 承诺。[官方项目页的 License 与 For Developers](https://modrinth.com/mod/xaeros-minimap)
- 与 direct linkage 相比，reflection 不会降低许可证风险，反而会把缺失成员从 compile error 变成 runtime error。建议直接 compileOnly + 严格的 compat classloading boundary + 每个 RiftGun 版本锁定已测 Xaero 版本范围。

## 数据模型与测试建议

```text
client map adapter
  -> ExternalDestinationSnapshot(source, stableId, name, dimension, x, y, z, yaw?)
  -> read-only virtual GUI section
  -> selection request
  -> server validates the supplied target with existing RiftGun policies
  -> open portal; never write back to map mod
```

建议最少测试：

- 对应 mod 不存在时 RiftGun 可正常启动，dedicated server 不 classload compat types。
- 同时安装两个地图 mod；同名 waypoint 不冲突。
- Overworld/Nether/End 与 modded dimension ID 的映射；不存在或服务端禁用的维度不能发射。
- rename/move/delete/disable waypoint 后 GUI 刷新；换服务器不泄露上一服务器条目。
- 虚拟条目无 edit/delete/drag/group/share 动作，但可 select/fire。
- 手工构造 packet 的 NaN/infinity、越界坐标、未知维度与越权目标均被服务端拒绝。

## 已确认的实施决策

- 外部 waypoint 严格只读：可查看、选择、发射，不可编辑、删除、重排、改组或再次分享。
- 显示所有已启用且持久的 waypoint，排除 temporary/deathpoint，并保留来源 set/group 作副标题。
- 发射时使用玩家当前 yaw；来源 yaw 不进入协议。
- JourneyMap 使用官方 event 自动刷新；Xaero 锁定已测版本，只在 GUI 打开或手动刷新时读取。兼容失败时隐藏分组，在设置页显示来源版本，并且每次会话只记录一次 warning。
- 客户端每来源开关默认开启；服务端总开关也默认开启。管理员必须注意：服务端能验证 dimension、有限坐标、world bounds、placement、fuel 与既有 policy，但无法证明 client-supplied waypoint 真正来自未修改的地图模组客户端。因此启用此功能在信任模型上等价于允许客户端提交任意坐标。
- 服务端只在当前登录会话内缓存所选外部目标，不写入玩家 destinations；disconnect、server switch、来源关闭或刷新后 stable ID 消失都会清除。
