# Mekanism Meka-Tool 模式轮盘调研

> 调研日期：2026-08-26。基线：Mekanism `v1.21.1-10.7.19.85`，固定 commit [`a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80`](https://github.com/mekanism/Mekanism/tree/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80)。本文只使用 Mekanism 官方 GitHub 源码、仓库构建配置和 `LICENSE`。

## 结论

**RiftGun 可以借鉴这套轮盘，也可在遵守 MIT 许可证的前提下复制实现代码；但建议复制交互模型、重写项目内部实现。**

- Mekanism 的核心交互是“按住模式键打开 → 鼠标方向预选 → 松开模式键提交”。不需要左键，但 **hover 的每一帧并不会立即向服务端提交**；提交点是界面关闭、任意鼠标键点击，或滚轮切换。[`GuiRadialSelector#render/removed/mouseClicked`](https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/src/main/java/mekanism/client/gui/GuiRadialSelector.java#L129-L147) [`GuiRadialSelector#removed`](https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/src/main/java/mekanism/client/gui/GuiRadialSelector.java#L200-L241)
- Mekanism **没有“右键切换到另一类轮盘”的专用逻辑**。`mouseClicked` 不检查 `button`，因此左、右、中键都只是对当前 hover 目标执行同一个 `updateSelection`：叶子项提交，nested 项进入下层，中心返回区回上层。RiftGun 的“在模式配置页按右键切到预测模式轮盘”是新产品行为，应显式实现，不能从 Mekanism 原逻辑直接得到。[`GuiRadialSelector#mouseClicked/updateSelection`](https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/src/main/java/mekanism/client/gui/GuiRadialSelector.java#L237-L241) [`GuiRadialSelector#updateSelection`](https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/src/main/java/mekanism/client/gui/GuiRadialSelector.java#L283-L320)
- 网络边界设计值得复用：客户端只上报 equipment slot、nested path 和整数 mode representation；服务端从玩家当前 slot 重新取 item，重放并校验 radial path，把 representation 反解为可用 mode 后才修改 item。[`PacketRadialModeChange`](https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/src/main/java/mekanism/common/network/to_server/PacketRadialModeChange.java#L22-L64)
- 官方许可证是 MIT，允许 use/copy/modify/merge/publish/distribute/sublicense/sell；条件是在所有副本或软件的 substantial portions 中包含原 copyright notice 和 permission notice。[`LICENSE`](https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/LICENSE)

## 按键注册与打开/关闭生命周期

### 按键

Mekanism 把 hand mode switch 注册为 `KeyMapping`，默认是 `N`，conflict context 是 in-game。按下时，只有当主手/副手 item 是普通 mode item，或 radial item 当前没有 radial data 时，才发送普通 `PacketModeChange`；有 radial data 的 Meka-Tool 会被 `allowRadial=false` 过滤掉，避免“按键既打开轮盘又切一次模式”。[`MekanismKeyHandler`](https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/src/main/java/mekanism/client/key/MekanismKeyHandler.java#L31-L44) [`IModeItem#isModeItem`](https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/src/main/java/mekanism/common/item/interfaces/IModeItem.java#L37-L53)

`isRadialPressed()` 会手动检测键盘或鼠标按键的物理状态。当轮盘 Screen 打开导致 in-game conflict context 不再 active 时，它会临时按 GUI context 检测，并在没配 modifier 时容忍 Shift 等额外 modifier。这是“按住后 Screen 仍能持续存在”的关键。[`MekKeyHandler#isRadialPressed`](https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/src/main/java/mekanism/client/key/MekKeyHandler.java#L45-L63)

### 打开、维持与关闭

Client tick 只在当前没有 Screen，或已经是 `GuiRadialSelector` 时尝试维持轮盘。它优先查主手，再查副手；item 必须实现 `IGenericRadialModeItem` 且 `getRadialData(stack)` 非 null。键不再按住、两只手都不再有有效 radial，或 radial 数据不再匹配时，client 通过 `setScreen(null)` 关闭/替换界面。[`ClientTickHandler`](https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/src/main/java/mekanism/client/ClientTickHandler.java#L248-L283)

`GuiRadialSelector#keyPressed` 吞掉所有键事件，它不靠 Esc 或再次 key event 关闭；正常关闭是 Client tick 观察到模式键已松开。Screen 的 `isPauseScreen()` 返回 false。[`GuiRadialSelector`](https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/src/main/java/mekanism/client/gui/GuiRadialSelector.java#L200-L245)

## 鼠标角度与 hover 选区算法

Mekanism 使用等分扇区，设可用项数为 `N`：

1. `angleSize = 360 / N`。
2. 鼠标相对屏幕中心的向量是 `(dx, dy)`，距离用 `length(dx, dy)`。
3. 顶层距中心大于 10 px 才选中；存在 parent 时阈值是 20 px，内圆留给 back button。
4. `angle = degrees(atan2(dy, dx))`。
5. `modeSize = 180 / N`（半个扇区），`selectionAngle = wrapDegrees(angle + modeSize + 90)`。
6. `index = floor(selectionAngle * N / 360)`，最后 `selection = modes[index]`。
7. 回到中心 dead zone 时，`selection = null`，因而松键不会改模式。

该计算和 hover 高亮都在每帧 `render` 中执行。[`GuiRadialSelector#render`](https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/src/main/java/mekanism/client/gui/GuiRadialSelector.java#L80-L147)

视觉上，轮盘主环内半径 40 px、外半径 100 px，icon/text 的中心距离是 70 px；底层环、当前模式、hover 扇区分层渲染，图标与 label 绕圆排布。环是 300 段近似的 `TRIANGLE_STRIP`。[`GuiRadialSelector` 布局常量](https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/src/main/java/mekanism/client/gui/GuiRadialSelector.java#L47-L53) [`drawTorus`](https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/src/main/java/mekanism/client/gui/GuiRadialSelector.java#L248-L269)

## 何时提交

| 交互 | 是否发包 | 行为 |
|---|---:|---|
| 仅移动鼠标/hover | 否 | 只更新 client-side `selection` 与高亮。 |
| 松开模式键 | 可能 | tick 关闭 Screen，`removed()` 对当前 `selection` 执行 `updateSelection`。 |
| 任意鼠标键点击 | 可能 | 立即对当前 selection 提交/进层/返回；不关闭 Screen。 |
| 滚轮 | 是 | 从当前 mode 按环形 index 移动，立即 `updateSelection`。 |
| 鼠标在中心 dead zone 内松键 | 否 | `selection == null`，保持原 mode。 |

只有 selection 非 null、不是 nested 类别，且与当前 mode 不同时才编码并发送 `PacketRadialModeChange`。[`GuiRadialSelector#removed/mouseScrolled/mouseClicked`](https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/src/main/java/mekanism/client/gui/GuiRadialSelector.java#L200-L241) [`GuiRadialSelector#updateSelection`](https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/src/main/java/mekanism/client/gui/GuiRadialSelector.java#L283-L320)

**对 RiftGun 需求的准确映射：**若“自动选择”是指“不需要左键，指向后松开快捷键即选择”，可直接采用 Mekanism 生命周期；若是指“指针一跨入扇区就立刻更改模式”，那不是 Mekanism 行为，也会产生大量 client-to-server packet，不建议这样实现。

## nested 层级与右键

- hover 到 `INestedRadialMode` 后触发 `updateSelection` 会把当前 `radialData` push 到 parent stack，切换到 child data，并清空 selection。它只改 client 层级，不发 mode packet。[`GuiRadialSelector#updateSelection`](https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/src/main/java/mekanism/client/gui/GuiRadialSelector.java#L283-L311)
- child 层的中心 20 px 是 back button；在中心区触发 update 时 poll parent 回上层。[`GuiRadialSelector#render`](https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/src/main/java/mekanism/client/gui/GuiRadialSelector.java#L149-L164) [`GuiRadialSelector#updateSelection`](https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/src/main/java/mekanism/client/gui/GuiRadialSelector.java#L312-L319)
- `mouseClicked` 无 `button == 0/1/...` 分支，所以“右键进 nested”只是“任意键进 nested”的副作用。[`GuiRadialSelector#mouseClicked`](https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/src/main/java/mekanism/client/gui/GuiRadialSelector.java#L237-L241)

## Meka-Tool 如何生成轮盘内容

`ItemMekaTool` 实现 `IRadialModuleContainerItem`，根 radial identifier 是 `mekanism:meka_tool`。[`ItemMekaTool`](https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/src/main/java/mekanism/common/item/gear/ItemMekaTool.java#L86-L96) [`getRadialIdentifier`](https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/src/main/java/mekanism/common/item/gear/ItemMekaTool.java#L445-L448)

item 每次请求 radial data 时遍历已安装 module，只收集 `handlesRadialModeChange()` 的 nested modes：

- 0 个 nested mode：返回 null，轮盘不打开。
- 1 个 nested mode：直接返回该 child `RadialData`，省略只有一个分类的根层。
- 2 个及以上：创建 `NestingRadialData`根层。

这一层的 `getMode/setMode` 也重新遍历 module，由识别该 `RadialData` 的 module 读写模式。[`IRadialModuleContainerItem`](https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/src/main/java/mekanism/common/content/gear/IRadialModuleContainerItem.java#L19-L80)

Screen 本身对当前 `RadialData#getModes()` 为空的处理是先返上层，没上层就关闭。对 child 只有一个叶子选项则仍渲染轮盘；源码 TODO 明确说当前没有自动穿越 single-option nested layer。[`GuiRadialSelector`](https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/src/main/java/mekanism/client/gui/GuiRadialSelector.java#L43-L45) [`0 modes 处理`](https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/src/main/java/mekanism/client/gui/GuiRadialSelector.java#L80-L94)

Meka-Tool 的具体 category 来自 module，例如 excavation speed、blasting power 和 vein mining。每个 module 提供 nested label/icon/color、可用叶子以及 `getMode/setMode`。[`ModuleExcavationEscalationUnit`](https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/src/main/java/mekanism/common/content/gear/mekatool/ModuleExcavationEscalationUnit.java#L43-L92) [`ModuleBlastingUnit`](https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/src/main/java/mekanism/common/content/gear/mekatool/ModuleBlastingUnit.java#L42-L91) [`ModuleVeinMiningUnit`](https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/src/main/java/mekanism/common/content/gear/mekatool/ModuleVeinMiningUnit.java#L64-L105)

## 网络包与 client/server 职责

`PacketRadialModeChange` 的 wire data 是：

```text
EquipmentSlot slot
List<ResourceLocation> path
VarInt networkRepresentation
```

客户端负责视觉、hover 计算、nested path 组装，以及把 mode 转成 integer representation。包作为 client-to-server play payload 注册。[`GuiRadialSelector#updateSelection`](https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/src/main/java/mekanism/client/gui/GuiRadialSelector.java#L283-L310) [`PacketHandler` 注册](https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/src/main/java/mekanism/common/network/PacketHandler.java#L91-L109)

服务端 handler 不信任 client 发来的 item/radial object，而是根据 slot 从 player 重新取 stack，检查 item interface，重新获取 root data，逐段根据 identifier 走 path；任意 path 段不存在就中止。最后 `fromNetworkRepresentation` 返回非 null 才调 item `setMode`。这保护了类型/path/值的有效性，但该包本身没有显式 rate limit 或“client 当前真的打开轮盘”证明。[`PacketRadialModeChange#handle`](https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/src/main/java/mekanism/common/network/to_server/PacketRadialModeChange.java#L38-L64)

## 音效

Meka-Tool hand radial 路径没有专用打开、hover、进层、返回或提交音效：`GuiRadialSelector`、`ClientTickHandler` 的 radial 分支和 `PacketRadialModeChange` 都没有播放 sound。作为对照，MekaSuit armor slot 的普通 mode key 会播放 `HYDRAULIC`，但 hand radial key 分支没有。[`MekanismKeyHandler` hand 与 armor 分支](https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/src/main/java/mekanism/client/key/MekanismKeyHandler.java#L33-L52) [`handlePotentialModeItem` 的 armor 音效](https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/src/main/java/mekanism/client/key/MekanismKeyHandler.java#L77-L97) [`GuiRadialSelector`](https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/src/main/java/mekanism/client/gui/GuiRadialSelector.java)

因此 RiftGun 是否在 hover 变化、打开/关闭、确认时播 UI 音效是自身产品决策，不是复制 Mekanism 所必需。

## Minecraft / NeoForge 版本兼容性

本次核对的官方 tag 明确针对 Minecraft `1.21.1`，编译 NeoForge `21.1.200`，mod metadata 允许 NeoForge `[21.1.194,)`，NeoForge loader `[4,)`。[`gradle.properties`](https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/gradle.properties#L6-L16)

该实现使用 1.21.1/NeoForge 当时的 `RegisterKeyMappingsEvent`、`CustomPacketPayload` + `StreamCodec`、`IPayloadContext`、`ClientTickEvent.Pre`、`GuiGraphics`和 `Screen` API。因此：

- RiftGun 1.21.1 节点可按该实现直接对照移植，但仍应使用 RiftGun 现有 packet registration/config/key abstractions。
- 不能据此声称源码对 RiftGun 26.1.2 是 source-compatible。可复用的是 state machine、角度算法和 server-validation 边界；Screen rendering、input event、payload codec 需在 26.1.2 source set 独立编译验证。

后一条是从已核对 API 对 1.21.1 的明确绑定所作的工程推论，不是 Mekanism 官方对 26.1.2 的兼容承诺。

## 许可证、复制边界与 attribution

Mekanism 仓库在该固定 commit 下的 `LICENSE` 是 MIT，copyright 为 `Copyright (c) 2017-2025 Aidan C. Brady`。它明确允许复制和修改，但要求在所有副本或 substantial portions 中保留 copyright notice 与完整 permission notice。[`Mekanism LICENSE`](https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/LICENSE)

实施建议：

1. **首选独立重写。** 复用“按住/松开状态机”、扇区数学、client preview/server commit 分工这些 idea，用 RiftGun 的类名、布局 helper、packet 和测试重写。这也能避免带入 Mekanism 的 module/nested abstractions。
2. **如直接复制实质性代码，必须归属。** 在 RiftGun 现有 `NOTICE.md` 记录 Mekanism、官方仓库、固定 commit、复用文件/部分；将完整上游 MIT 文本保存为 `THIRD_PARTY_LICENSES/Mekanism-LICENSE.md`。在直接改写的 source 文件头或附近加简短 `Adapted from Mekanism ...` 注释也有助于可追溯性。
3. **不复制图标材质，除非明确把它们作为 MIT 软件的一部分处理并履行同样 notice。** RiftGun 已有 PNG/GUI icon 规范，自制 16×16 透明 PNG 能避免视觉识别和 attribution 混淆。

MIT 允许复制不等于无需保留 notice；“基本复制”一旦达到 substantial portion，上述许可文本保留是必须的。

## 对 RiftGun 的实现建议

```text
mode key down/held
  -> client checks held RiftGun + radial options
  -> open RadialModeScreen with local preview only
  -> mouse direction selects preview (dead zone keeps current mode)
  -> mode key release closes screen
  -> close commits one C2S request if preview differs
  -> server re-resolves held RiftGun and validates requested enum
  -> server writes authoritative gun mode and syncs
```

- 将角度选区抽成不依赖 Minecraft GUI 的 pure function，输入 `dx/dy/optionCount/deadZone`，输出 optional index，可对四象限、正边界、负角、中心 dead zone 和任意 `N` 做单元测试。
- 不要在每次 hover 变化时发包；用 client-only preview，松键只提交一次。
- 服务端 packet 只接受 mode enum/network id，并验证玩家当前主/副手确实是可配置传送枪、该 mode 当前已解锁/允许。
- 给“普通传送枪模式”和“预测模式”建立显式 radial page/type；右键在指定页面切换 page 时要只切 client preview page，不要沿用 Mekanism 的“任意鼠标键都提交”。
- 对 0 个选项，不打开/立即关闭并可选播放失败 UI sound；对 1 个选项，建议仍显示以保留可发现性，或明确不打开，不要无意间自动改模式。
- 将快捷键打开行为与既有“打开传送配置”分开，并定义 key conflict context；模式键必须在自己打开的 Screen 中仍能检测物理按住状态，否则 Screen 会在打开后立即关闭。

## 一手来源索引

- [Mekanism 官方仓库，固定 commit](https://github.com/mekanism/Mekanism/tree/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80)
- [`MekanismKeyHandler`](https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/src/main/java/mekanism/client/key/MekanismKeyHandler.java)
- [`MekKeyHandler`](https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/src/main/java/mekanism/client/key/MekKeyHandler.java)
- [`ClientTickHandler`](https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/src/main/java/mekanism/client/ClientTickHandler.java)
- [`GuiRadialSelector`](https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/src/main/java/mekanism/client/gui/GuiRadialSelector.java)
- [`PacketRadialModeChange`](https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/src/main/java/mekanism/common/network/to_server/PacketRadialModeChange.java)
- [`IRadialModuleContainerItem`](https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/src/main/java/mekanism/common/content/gear/IRadialModuleContainerItem.java)
- [`RadialData` API](https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/src/api/java/mekanism/api/radial/RadialData.java)
- [`gradle.properties`](https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/gradle.properties)
- [Mekanism MIT `LICENSE`](https://github.com/mekanism/Mekanism/blob/a00109e4856fd38b9c5b3dd7f22ce4a59cd65a80/LICENSE)
