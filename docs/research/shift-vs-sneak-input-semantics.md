# Shift 与潜行键的判定及文案惯例

调研时间：2026-08-29
目标版本：Minecraft / NeoForge 1.21.1；同时参考各模组当前 1.21.x 官方源码。

## 结论

`Shift` 与“潜行键”不能视为同一概念：

- `Screen.hasShiftDown()` 检测键盘上的左/右 Shift，是 GUI modifier；玩家把潜行改绑到别的键后，它仍只认 Shift。
- `player.isShiftKeyDown()`、`Input.shiftKeyDown`、`minecraft.options.keyShift.isDown()` 表示 Minecraft 的潜行输入/同步状态，跟随玩家的 Sneak key mapping。方法名保留了历史上的 `Shift`，但语义是 Sneak。
- `player.isCrouching()` 是当前实体姿态，不等同于按住潜行。姿态可能因空间、游泳或其他机制改变；不适合判定“用户正在按潜行键”。

因此，文案应按实际判定命名：

- 使用 `Screen.hasShiftDown()` 的 tooltip 展开、GUI 多选等 modifier：写 `Shift`。
- 使用 `player.isShiftKeyDown()` 或 `options.keyShift.isDown()` 的世界交互与滚轮组合：写“潜行键”/`Sneak`，最好显示动态绑定键名。
- 服务端不能调用 `Screen.hasShiftDown()`；应使用服务端已同步的 `ServerPlayer.isShiftKeyDown()`，或把一次性 modifier 明确放进经过校验的客户端 packet。

## Minecraft / NeoForge

本项目所用 NeoForge 21.1.241 经 NeoForm 生成的 Minecraft 1.21.1 源码显示：

- `Screen.hasShiftDown()` 直接查询 GLFW key `340` 和 `344`，即 Left Shift / Right Shift；它不读取 `options.keyShift`。
- `KeyboardInput.tick()` 把 `options.keyShift.isDown()` 写入 `Input.shiftKeyDown`。
- `LocalPlayer.isShiftKeyDown()` 返回 `input.shiftKeyDown`。
- `Entity.setShiftKeyDown()` 将该状态写入 shared flag；`Entity.isShiftKeyDown()` 从 shared flag 读取。服务端逻辑看到的是同步后的玩家输入状态，不是服务端读取键盘。
- `Entity.isCrouching()` 则检查 `Pose.CROUCHING`，是姿态查询。

公开参考：

- NeoForge 官方 Key Mappings 文档要求游戏内可重绑定动作通过 `KeyMapping#consumeClick`/mapping 状态检测，GUI 内匹配 mapping 则用 `IKeyMappingExtension#isActiveAndMatches`：[Key Mappings](https://docs.neoforged.net/docs/1.21.1/misc/keymappings/)
- Minecraft 1.21.1 映射源码索引可核对 [`Screen.hasShiftDown()`](https://mappings.dev/1.21.1/net/minecraft/client/gui/screens/Screen.html#hasShiftDown())、[`KeyboardInput`](https://mappings.dev/1.21.1/net/minecraft/client/player/KeyboardInput.html) 与 [`LocalPlayer.isShiftKeyDown()`](https://mappings.dev/1.21.1/net/minecraft/client/player/LocalPlayer.html#isShiftKeyDown())。该站不是 Mojang 官方仓库，仅作为公开定位入口；上面的实现结论以本项目解析的官方 Minecraft artifact 为准。

## Create / Ponder

Create 明确混用了两套概念，但用途分得很清楚：

1. 物品详情 tooltip 使用 `Screen.hasShiftDown()` 展开，因此这是物理 Shift modifier：[ItemDescription.java L103-L110](https://github.com/Creators-of-Create/Create/blob/0924e93639ad5f61cfc39a221d909e16f2893df1/src/main/java/com/simibubi/create/foundation/item/ItemDescription.java#L103-L110)。
2. Create 还注册了独立、可重绑定的 `Shift Modifier`，默认 Left Shift；`AllKeys.shiftDown()`读取该 mapping 的实际绑定键，而不是 Minecraft Sneak mapping：[AllKeys.java L22-L29](https://github.com/Creators-of-Create/Create/blob/0924e93639ad5f61cfc39a221d909e16f2893df1/src/main/java/com/simibubi/create/AllKeys.java#L22-L29)、[L118-L124](https://github.com/Creators-of-Create/Create/blob/0924e93639ad5f61cfc39a221d909e16f2893df1/src/main/java/com/simibubi/create/AllKeys.java#L118-L124)。
3. 世界交互使用 `player.isShiftKeyDown()`；例如 Hand Crank 反向旋转：[HandCrankBlock.java L63-L70](https://github.com/Creators-of-Create/Create/blob/0924e93639ad5f61cfc39a221d909e16f2893df1/src/main/java/com/simibubi/create/content/kinetics/crank/HandCrankBlock.java#L63-L70)。对应 Ponder 文案写的是 `Sneak and Hold Right-Click`，不是 `Shift`：[KineticsScenes.java L910-L920](https://github.com/Creators-of-Create/Create/blob/0924e93639ad5f61cfc39a221d909e16f2893df1/src/main/java/com/simibubi/create/infrastructure/ponder/scenes/KineticsScenes.java#L910-L920)。
4. 常规物品说明同样把 `player.isShiftKeyDown()` 类世界操作描述为 `Sneak`，例如 Wrench：[tooltips.json L80-L84](https://github.com/Creators-of-Create/Create/blob/0924e93639ad5f61cfc39a221d909e16f2893df1/src/main/resources/assets/create/lang/default/tooltips.json#L80-L84)。

Create 的惯例最适合本项目：modifier 展开写 `Shift`，游戏动作写 `Sneak`。

## Ender IO

Ender IO 也依据场景选用不同 API：

1. 高级 item tooltip 用 `Screen.hasShiftDown()`，对应英文 `<Hold Shift>`、中文 `<按住Shift>`：[TooltipHandler.java L38-L50](https://github.com/Team-EnderIO/EnderIO/blob/90618ded289ae1cdeebaec0553833d303839a259/enderio/src/main/java/com/enderio/enderio/client/foundation/tooltip/TooltipHandler.java#L38-L50)、[zh_cn.json L605](https://github.com/Team-EnderIO/EnderIO/blob/90618ded289ae1cdeebaec0553833d303839a259/enderio/src/main/resources/assets/enderio/lang/zh_cn.json#L605)。这是物理 Shift tooltip modifier，文案一致。
2. Travel Anchor 的向下触发读取 `Input.shiftKeyDown`，并在代码中命名为 `isNewCrouch`：[TravelClientEventHandler.java L21-L48](https://github.com/Team-EnderIO/EnderIO/blob/90618ded289ae1cdeebaec0553833d303839a259/enderio/src/main/java/com/enderio/enderio/client/content/travel/TravelClientEventHandler.java#L21-L48)。这属于 Sneak action。
3. Conduit Probe 的组合滚轮使用 `player.isShiftKeyDown()`：[ConduitProbeScrollListener.java L17-L30](https://github.com/Team-EnderIO/EnderIO/blob/90618ded289ae1cdeebaec0553833d303839a259/enderio/src/main/java/com/enderio/enderio/client/content/conduits/ConduitProbeScrollListener.java#L17-L30)。
4. 服务端的 Capacitor Bank 交互同样用 `event.getEntity().isShiftKeyDown()`：[CapacitorBankBlock.java L81-L88](https://github.com/Team-EnderIO/EnderIO/blob/90618ded289ae1cdeebaec0553833d303839a259/enderio/src/main/java/com/enderio/enderio/content/machines/capacitor_bank/CapacitorBankBlock.java#L81-L88)。
5. 需要直接遵循潜行绑定的客户端移动逻辑会明确读取 `options.keyShift.isDown()`：[DarkSteelLadderHandler.java L19-L30](https://github.com/Team-EnderIO/EnderIO/blob/90618ded289ae1cdeebaec0553833d303839a259/enderio/src/main/java/com/enderio/enderio/content/decor/DarkSteelLadderHandler.java#L19-L30)。

## Mekanism

Mekanism 展示了 GUI modifier 与世界 Sneak action 的典型差别：

1. Side Configuration 页面按住 Shift 清除全部物质类型，代码用 `Screen.hasShiftDown()`：[GuiSideConfiguration.java L83-L104](https://github.com/mekanism/Mekanism/blob/11162452affe7b17b25cde251308c9d047c42e87/src/main/java/mekanism/client/gui/element/window/GuiSideConfiguration.java#L83-L104)。datagen 文案明确写 `Hold Shift`：[MekanismLangProvider.java L1219-L1224](https://github.com/mekanism/Mekanism/blob/11162452affe7b17b25cde251308c9d047c42e87/src/datagen/main/java/mekanism/client/lang/MekanismLangProvider.java#L1219-L1224)。
2. Configurator 的世界交互使用 `player.isShiftKeyDown()`，并把分支方法命名为 `onSneakRightClick`：[ItemConfigurator.java L152-L160](https://github.com/mekanism/Mekanism/blob/11162452affe7b17b25cde251308c9d047c42e87/src/main/java/mekanism/common/item/ItemConfigurator.java#L152-L160)。
3. 模式切换快捷键把 `player.isShiftKeyDown()` 的结果发给服务端，用于反向切换：[MekanismKeyHandler.java L31-L44](https://github.com/mekanism/Mekanism/blob/11162452affe7b17b25cde251308c9d047c42e87/src/main/java/mekanism/client/key/MekanismKeyHandler.java#L31-L44)。
4. Shift+滚轮切换模式也读取 `minecraft.player.isShiftKeyDown()`：[ClientTickHandler.java L285-L297](https://github.com/mekanism/Mekanism/blob/11162452affe7b17b25cde251308c9d047c42e87/src/main/java/mekanism/client/ClientTickHandler.java#L285-L297)。

Mekanism 的 GUI 文案经常直接写 `Shift`，且对应实现确实是 `Screen.hasShiftDown()`；世界交互分支则按 Sneak 语义命名。

## 对 RiftGun 的具体建议

当前源码的判定可分为两组：

| 场景 | 当前判定 | 正确语义 | 建议文案 |
| --- | --- | --- | --- |
| 模块详情展开 | `Screen.hasShiftDown()` / `Minecraft.hasShiftDown()` | 物理 Shift modifier | 保留“按住 Shift 查看模块详情” / `Hold Shift ...` |
| 配对模式选择 A/B（左键触发） | 客户端及服务端 `player.isShiftKeyDown()` | 潜行 binding | 动态显示当前潜行键，并组合“潜行键+左键” |
| Pairing module 快速放置 A 门 | `player.isShiftKeyDown()` | 潜行 binding | “潜行键 + 互动键” / `Sneak + Use`；优先动态显示绑定键 |
| Remote distance 滚轮 | `player.isShiftKeyDown()`、`options.keyShift.isDown()` | 潜行 binding | “潜行键 + 滚轮” / `Sneak + Scroll`；优先动态显示绑定键 |
| GUI 搜索框反向 Tab 等 | `Screen.hasShiftDown()` | 物理 Shift modifier | 若需要提示，应写 Shift |

### 推荐实现层级

1. 最佳：客户端提示传入 `minecraft.options.keyShift.getTranslatedKeyMessage()`，显示用户当前绑定（例如“左 Alt + 滚轮”）。语义标签仍用“潜行”。
2. 若当前 UI/翻译结构不便传动态组件：中文统一写“潜行键”，英文统一写 `Sneak`，不要写死 `Shift`。
3. 保留模块详情 tooltip 的 `Shift` 文案，因为其实现就是 `Screen.hasShiftDown()`；若希望模块详情也跟随潜行改绑，则必须同时把判定改成 `options.keyShift.isDown()`，不能只改文案。
4. 不建议用 `isCrouching()` 代替 `isShiftKeyDown()`；前者是 pose，不能可靠表达按键意图。

## 实施结果

RiftGun 已采用动态键名方案：配对 A/B、配对快速使用和 Remote 滚轮提示通过
`options.keyShift.getTranslatedKeyMessage()` 显示当前潜行绑定；模块详情展开仍明确显示
`Shift`，与其 `Screen.hasShiftDown()` 判定保持一致。
