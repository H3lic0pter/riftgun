# 异步开门与 GUI 修复清单

- [x] 所有目标统一走 non-blocking destination chunk preparation；最长 5 秒，超过 1.2 秒才显示加载提示。
- [x] 每位玩家只有一个 latest-wins 请求；同目标去重，换目标释放旧 ticket，过期 callback 由 request ID 拦截。
- [x] 开门成功前不扣 fuel、不关闭旧门；死亡、logout、换维度、枪被移动时取消。
- [x] GUI 选点与 Safety 均使用 300 ms debounce；选点即时高亮，关闭 GUI 或开门前 flush。
- [x] 浏览 Safety 不加载 chunk；位置指纹缓存 5 秒，未加载显示“开门时检查”。
- [x] unsafe GUI 的 prepared ticket 最多保留 15 秒；取消、换点或关闭 GUI 时释放。
- [x] 分组主按钮支持鼠标左键 next、右键 previous 并循环；`▼` 独立展开 opaque、modal-input-capturing popup。
- [x] fuel gauge 与 bucket mode 分离为独立 42×19 状态条；overfill 使用实际数值、满条和 gold outline。
- [x] Portal Gun tooltip 的 fluid 行使用 profile 精确 RGB；溢出时追加 gold `Overfilled/已溢出` 行。
- [x] 三种 profile 色值：unstable `#A855D4`、portal `#58BFFF`、dimensional `#4FCB72`。
- [x] Portal pair 使用共享 absolute server clock；跨维度 vehicle/passenger tree 使用新实体引用重建。
- [ ] 真机手动验证未强加载的跨维度目标、5 秒 timeout、15 秒 unsafe confirmation expiry。

> Splash effect 已冻结。本次只改变 profile RGB，不改粒子数量、采样、时序、速度、重力、寿命或方向行为。
