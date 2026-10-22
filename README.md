# 2048

原生 Android 2048 游戏，完全离线运行。与同级 `Threes` 工程保持相同的 Gradle、Compose、模块和签名布局。

## 功能

- 经典 2048 配色的 4×4 棋盘和凸起 3D 方块；整行滑动、相同数字每步仅合并一次；新方块以 90%/10% 的概率出现 2/4。
- 4096 及以上方块继续合并，不在 2048 达成时结束；仅在无合法移动时结束。
- 手势与方向按钮、撤销、新游戏、分数、最佳分数、音效开关和结束时查看棋盘。
- 顶部左侧显示当前分数与金色最佳分数卡片，右侧显示同底色包围的下一张预览；数字使用与 Threes 相同的 Numbers-Premium 字体。最佳分数保存到本地，重启、撤销和新开局不会清除纪录。
- 下一张提前按 90%/10% 抽定为 2/4，移动后实际生成该数字；无效移动不消耗预览，撤销同步恢复预览。AI 的第一层仅对已知数字的随机落点求期望，后续仍按 2/4 的概率搜索。
- 与 Threes 相同的滑动、合并互动和无效操作 WAV 音效；静音及后台停播行为一致，AI 自动运行默认关闭棋盘音效。
- 普通按钮使用 `click.wav`，AI 自动/暂停按钮使用 `deepClick.wav`；手动声音设置开启时，自动运行中的暂停按钮仍有反馈。
- 拖动时预览方块位移，松手后滑动并弹出新牌和合并牌；无效方向轻微回弹。
- 手动模式与 AI 模式；AI 给出四方向评分、已完成深度、节点数与耗时，并可自动连续运行。
- AI 搜索在后台执行；切换模式、撤销与重开会取消旧搜索。自动运行期间禁用手动操作。

## 模块

- `game-core`：不依赖 Android 的棋盘、规则和对局状态。
- `ai-core`：2048 专用 expectimax 搜索及单元测试。
- `app`：Jetpack Compose UI、音效、ViewModel 和 Android 入口。

Threes 使用的 `nneonneo/threes-ai` 原生引擎依赖 Threes 的单格推进和预告牌规则，不能直接复用于 2048。本工程改用 2048 专用的 expectimax：玩家节点取四方向最大值，随机节点按空格位置及 2/4 的真实概率求期望，迭代加深，并使用空格、连续可合并牌和行列单调性评估。默认每步 200ms、深度上限 8；超时保留上一完整深度的结果。该方案借鉴 `nneonneo/2048-ai` 的搜索与评估思路，没有复制其源码。超大离线残局数据库或训练权重方案对手机内存和 APK 体积要求更高，因此这里采用无需模型文件的本地方案。

## 构建

```powershell
.\gradlew.bat --no-daemon --max-workers=1 :game-core:test :ai-core:test :app:lintDebug :app:assembleDebug :app:assembleRelease
```

Release 包名为 `com.zerocode.game2048`，Debug 为 `com.zerocode.game2048.debug`，可共存。APK 命名为 `2048_v版本号_北京时间版本码_构建类型.apk`。语义版本在 `version.properties` 中维护；版本码按北京时间生成 `yyMMddHH`。Debug 和 Release 使用同一个版本号和版本码。

Release 签名从同级 `../_android_signing/2048.properties` 读取，密钥库路径相对于该配置文件。Debug 在配置存在时也使用同一证书，否则使用 Android 默认调试证书。首次配置可在项目目录运行 `./scripts/create-signing.ps1`；脚本只在文件不存在时创建独立 RSA 4096 位 PKCS12 证书，不会覆盖旧证书。私钥与密码不提交代码仓库，需单独加密备份；丢失证书后无法给同包名的已安装 Release 应用做覆盖更新。

Android Studio 可使用 `.run/` 中的运行、构建、安装配置。`2048-Run` 在运行前会把已连接模拟器的时区调整为北京时间。版本码边界检查：`.\gradlew.bat :app:verifyBuildVersionCode`。
