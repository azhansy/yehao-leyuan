# 烨浩乐园（yehao-leyuan）

面向约 **4 岁及以上**儿童的 **Jetpack Compose** Android 应用：在地图式首页进入各小游戏，边玩边熟悉**英文单词与简单句式**，需 **联网** 使用在线英文发音（有道词典接口）。

- **应用 ID**：`com.yehao.leyuan`
- **版本**：以 `app/build.gradle.kts` 中 `versionName` / `versionCode` 为准

## 功能概览

首页为 **乐园地图**，展示下列入口（顺序见 `AppDestinations.games`）。右上角菜单可进入 **关于**（版本信息、官网链接、**英式 / 美式** 朗读音色切换）。

| 入口 | 说明 |
|------|------|
| **字母拼图** | 从动物 + 植物词库随机一词；拖拽字母入槽，完成后鼓励反馈。 |
| **动物王国** | 横向卡片（Emoji + 英文名），点击朗读并带轻微缩放动画。 |
| **植物园** | 与动物页同类交互，数据来自 `plants.json`。 |
| **家庭英语** | 填写家人中文名（可转拼音参与朗读）与英文名，点选句子模版听整句朗读。 |
| **配对形状** | 圆 / 方 / 三角 / 星形拖到轮廓槽位；成功有 Snackbar 与音效。 |
| **数学题** | 简单加减；拖拽数字到问号框，提交后对错反馈与星星气球层。 |
| **赛车** | 三车道躲避障碍，难度随时间升高。 |

## 数据资源

| 文件 | 用途 |
|------|------|
| `app/src/main/assets/animals.json` | 动物列表 |
| `app/src/main/assets/plants.json` | 植物列表（字母拼图与植物园共用词库） |

两文件结构一致，主要字段：`emoji`、`english`、`tint`（`#AARRGGBB`）。动物含 **`chinese`**（界面展示）；植物可带 `chinese`，可自行增删。字母拼图每次开局从 **动物 + 植物** 英文词中随机抽取（仅字母、长度约 2～8）。

## 音频与朗读

- **英文 TTS**：统一走 **有道词典** `dictvoice` 接口；应用内先 **下载到本地缓存** 再播放（`AppAudio`），并对整句失败等情况做 **拆词重试** 等容错。
- **提示音**：`ToneGenerator`、系统按键音等用于点击、对错等轻量反馈。
- **音色**：在 **关于** 页选择英式 / 美式（`TtsVoicePrefs`），全局生效。
- 若需真实动物叫声，可在 `app/src/main/res/raw/` 放置 mp3，再用 `SoundPool` / `MediaPlayer` 加载（当前未捆绑，以控制体积与版权）。

## 应用更新

- 构建时可设置 Gradle 属性 **`UPDATE_BASE_URL`**（默认指向项目发布页），用于检查更新；具体逻辑见 `AppUpdateOverlay` 与 `BuildConfig.UPDATE_BASE_URL`。
- 官网示例：`https://azhansy.github.io/yehao-leyuan/`

## 构建与运行

**环境**：Android Studio（建议最新稳定版）、**JDK 17**、已配置 **Android SDK**（命令行需 `ANDROID_HOME`）。

1. 用 Android Studio **Open** 本仓库根目录，等待 Gradle Sync。
2. 运行 **`app`** 模块到模拟器或真机。

常用命令：

```bash
./gradlew assembleDebug
./gradlew assembleRelease   # 需在项目根目录配置 key.properties 与签名（见 app/build.gradle.kts）
```

**注意**：`defaultConfig` 中 NDK 仅打包 **`arm64-v8a`**，请使用 64 位 ARM 设备或对应模拟器镜像。

## 技术栈

- **语言**：Kotlin  
- **UI**：Jetpack Compose（BOM）、Material 3、Navigation Compose  
- **SDK**：`minSdk` 26，`compileSdk` / `targetSdk` 35  
- **网络**：OkHttp（TTS 音频下载等）

## 仓库结构（简要）

```
app/src/main/java/com/yehao/leyuan/
├── audio/          # AppAudio、TTS 音色偏好等
├── feature/        # 各玩法界面（animal、family、letter、math、race、shape）
├── navigation/     # 路由与首页游戏列表
├── ui/             # 根 Compose、首页地图、关于页、主题
└── update/         # 应用内更新相关
```
