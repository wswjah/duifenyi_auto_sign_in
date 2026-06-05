# 对分易签到助手 - Android 版

自动监控对分易课程签到活动，支持**数字码**、**二维码**、**定位**三种签到类型。基于 Python 版 [duifene_v2](https://github.com) 的核心逻辑，使用 Kotlin + Jetpack Compose 重写为 Android 原生应用。

## 功能

- **微信授权登录** — 分享链接到微信授权，自动提取 code 完成登录，无需账号密码
- **数字码签到** — 自动抓取 4 位签到码并提交
- **二维码签到** — 微信登录后自动完成
- **定位签到** — 可配置多个签到地址，手动选择 + 随机偏移模拟 GPS
- **签到延迟** — 检测到签到后可设置等待 N 秒再执行
- **过期过滤** — 自动跳过 30 分钟前的历史签到
- **地址配置** — 内置定位坐标管理，支持添加/编辑/删除

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
- 检测到待签到后自动执行，结果在日志面板中显示
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
