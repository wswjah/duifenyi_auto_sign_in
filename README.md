# 对分易签到助手 - Android 版

监控对分易课程签到活动，提供**数字码手动输入**、**汉字九宫格点选**及原有定位签到功能。二维码签到目前存在失效反馈，建议使用微信官方页面。基于 Python 版 [duifene_v2](https://github.com/XiYuNPC/duifene-2.0) 的核心逻辑，使用 Kotlin + Jetpack Compose 重写为 Android 原生应用。

## 功能

- **微信授权登录** — 分享链接到微信授权，自动提取 code 完成登录，无需账号密码
- **数字码签到** — 检测活动后弹窗，手动输入老师提供的 4 位签到码
- **二维码签到** — 已有失效反馈，暂未修复，请使用微信官方签到
- **定位签到** — 可配置多个签到地址，手动选择 + 随机偏移模拟 GPS
- **汉字签到** — 通过学生端 `hanzigrid` 流程获取九个候选字及拼音，按老师提供的顺序点选四字后提交，无需输入生僻字；候选字不是正确答案
- **签到延迟** — 检测到签到后可设置等待 N 秒再执行
- **过期过滤** — 自动跳过 30 分钟前的历史签到
- **地址配置** — 内置定位坐标管理，支持添加/编辑/删除

## 截图

> 请自行添加应用截图

## 使用步骤

### 1. 微信登录

1. 在应用首页点击「**微信登录**」
2. 系统会将授权链接分享到微信（或复制到剪贴板）
3. 在微信中打开链接并完成授权
4. 授权完成后，**复制浏览器地址栏中的完整链接**
5. 返回应用，自动检测链接中的授权码并完成登录
6. 登录成功后会弹出 Toast 提示，自动跳转到课程页面

> 如未自动检测，可点击「手动粘贴授权链接」手动输入

### 2. 选择课程与地址

- 在顶部下拉框中选择要监控的**课程**
- 在「签到地址」卡片中选择**签到坐标**（支持从已配置地址中选择）
- 点击「**配置**」进入地址管理页面，可添加多个位置的经纬度

### 3. 开始监控

- 设置签到延迟（默认 0 秒，即检测到后立即签到）
- 点击「**开始监听**」启动监控
- 应用每 2 秒轮询一次签到活动
- 检测到数字码/汉字活动后弹窗等待人工操作；汉字可撤销、清空或刷新候选字
- 手动签到失败时可在弹窗内重试，提交前核实活动，提交后查询记录确认成功；跳过本次后可用微信完成签到
- 等待手动输入期间暂停轮询，停止监控或活动超时会关闭弹窗；设置的自动签到延迟仅用于原有自动分支
- 定位签到沿用原有流程，结果在日志面板中显示
- 点击「**停止**」结束监控

### 4. 退出登录

- 点击顶栏右侧退出图标
- 清除本地 Cookie 并返回登录界面

## 定位签到配置

定位签到需要配置教室的经纬度坐标。进入「签到地址」→「配置」：

| 字段 | 说明 |
|---|---|
| 地址名称 | 自定义名称（如「教学楼C5」） |
| 经度 | GCJ-02 坐标系经度 |
| 纬度 | GCJ-02 坐标系纬度 |
| 备注 | 教室位置描述（可选） |

未配置地址时，定位签到将使用 **C5 教学楼默认坐标**（深圳技术大学）。

> 建议使用「高德坐标拾取器」或手机地图 App 获取准确坐标。

## 技术栈

| 组件 | 版本 |
|---|---|
| Kotlin | 2.0.0 |
| Android Gradle Plugin | 8.5.2 |
| Gradle | 8.7 |
| Jetpack Compose + Material 3 | BOM 2024.09.00 |
| OkHttp | 4.12.0 |
| Gson | 2.11.0 |
| Jsoup | 1.18.1 |

## 项目结构

```
duifenyi/
├── build.gradle.kts                    # 项目级构建配置
├── settings.gradle.kts                 # 仓库与模块配置
├── gradle.properties
├── gradle/wrapper/
└── app/
    ├── build.gradle.kts                # 模块级构建配置
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── res/
        │   ├── raw/locations.json      # 默认定位配置
        │   └── values/                 # strings, themes
        └── java/com/duifenyi/app/
            ├── DuifenyiApp.kt          # Application
            ├── MainActivity.kt         # 单 Activity
            ├── data/
            │   ├── model/              # Course, CheckInRecord, LocationConfig...
            │   ├── network/            # DuifenyiApi (OkHttp), PersistentCookieJar
            │   ├── local/              # PreferencesManager (SharedPreferences)
            │   └── repository/         # DuifenyiRepository (业务逻辑)
            ├── service/
            │   └── CheckInMonitor.kt   # 2秒轮询协程
            └── ui/
                ├── theme/              # Color, Theme, Type
                ├── navigation/         # NavGraph (路由)
                ├── screen/             # LoginScreen, HomeScreen, LocationConfigScreen
                ├── viewmodel/          # LoginViewModel, HomeViewModel
                └── component/          # CourseSelector, LogView, LocationCard, DelayInput
```

## 更新日志

### 待发布
- 根据反馈，服务端已不再提供正确的数字码/汉字签到码，取消从签到列表自动获取答案的假设
- 新增数字码输入弹窗、汉字九宫格及拼音点选，核对候选字所属活动，防止误用其他课程的候选字
- 手动签到失败保留重试机会，通过签到记录确认结果；修复数字 JSON 状态/类型解析及显式 UA 被覆盖的问题
- 二维码问题本次暂不处理，建议优先使用微信官方签到

本次汉字候选字解析依据学生端 `hanzigrid` 的请求/响应样本实现；提交仍沿用现有 `studentcheckin` / `checkincode` 协议。尚未在真实活动上验证四字提交，若服务端已修改提交协议，还需要脱敏后的实际提交请求及响应才能进一步适配。

### v1.0.1 (2026-10)
- 新增**汉字签到**支持（对分易 2026 新增签到方式，签到类型 4）
- 签到延迟组件改为水平布局，优化界面空间

### v1.0.0
- 首个版本：微信授权登录、数字码/二维码/定位签到、地址配置、2 秒轮询监控

## 构建

1. 用 Android Studio 打开项目目录
2. 等待 Gradle sync 完成
3. `Build → Build APK` 或直接运行到设备

## 注意事项

- 二维码签到必须使用**微信授权登录**（Cookie 中需要微信授权信息）
- 不要频繁启停监控，可能触发服务器限流
- Cookie 过期后需重新登录（约 24-48 小时）
- 如 Gradle 下载失败（国内网络），可在 `gradle-wrapper.properties` 中更换镜像地址

## License

仅供学习交流使用。
