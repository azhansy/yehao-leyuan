# 烨浩乐园（yehao-leyuan）

面向约 4 岁儿童的 Jetpack Compose Android 应用：边玩边熟悉**英文单词**，包含四个主功能页与底部导航。

- 动物列表：**`assets/animals.json`**
- 植物列表（用于字母拼图词库）：**`assets/plants.json`**（格式与动物相同）

字母拼图每次开局从动物 + 植物英文词中**随机**抽取一词（仅字母、长度约 2～8）。动物 JSON 另含 **`chinese`**（中文名，界面展示）。两文件均为 `emoji`、`english`、`tint`（`#AARRGGBB`）；植物可带 `chinese`，可自行增删。

## 功能概览

| 页面 | 说明 |
|------|------|
| **字母拼图** | 大标题「拼出单词!」，示例单词 `CAT`，上方图示与英文，拖拽彩色字母到槽位吸附，完成后弹窗「做得好！」。 |
| **动物王国** | 横向滚动动物卡片（Emoji + 英文名称），点击播放 **TTS 英文朗读** 与轻微缩放动画。 |
| **配对形状!** | 圆形 / 方形 / 三角形 / 星星，拖到对应轮廓槽位；成功有 Snackbar 与音效。 |
| **解答数学题!** | 简单加减，拖拽数字到问号框，**提交**后「太棒了！」/「再试一次」与星星气球奖励层。 |

## 音频说明

- 使用 `TextToSpeech`（美式英语）朗读动物名与部分鼓励语；`ToneGenerator` 提供轻量点击与对错提示音。
- 若需真实动物叫声，可在 `app/src/main/res/raw/` 加入 mp3，并用 `SoundPool` / `MediaPlayer` 按文件名加载（当前未捆绑音频文件，避免仓库体积与版权风险）。

## 构建与运行

1. 安装 **Android Studio**（建议最新稳定版）与 **JDK 17**。
2. **Open** 本目录，等待 Gradle Sync。
3. 运行 `app` 模块到模拟器或真机。

命令行（需已配置 `ANDROID_HOME`）：

```bash
./gradlew assembleDebug
```

## 技术栈

- Kotlin 1.9.25、Jetpack Compose（BOM）、Material 3、Navigation Compose  
- `minSdk` 26，`compileSdk` / `targetSdk` 35  

应用 ID：`com.yehao.leyuan`。
