# GKD-XAP 界面层与代码全量体检报告

- 日期：2026-11-28
- 基线：v1.5.9（versionCode 160）/ miuix `0.9.5-rc02` / 包名 `li.songe.gkdxap`
- 体检范围：`gkd-app`、`gkd-priv-ui`、`gkd-selector`、`gkd-db`、`gkd-hidden-api`、`buildSrc`、`benchmark`、`scripts`、`docs`、CI 配置
- 结论级别：全部基于源码静态检查 + 依赖产物比对，未做真机渲染验证

---

## 一、结论（先看这一段）

**界面框架层面没有遗漏。** `androidx.compose.material3` 与 `androidx.compose.material`（除 icons）在全仓 **0 引用**，`ui/` 与 `feature/` 下 `collectAsState` **0 出现**，`scripts/check_miuix_ui.py` 把这三条做成了 CI 硬门禁。535 个 Kotlin 源文件、67,621 行界面代码全部走 miuix。

**真正需要处理的是三件事：**

| # | 问题 | 量级 | 建议 |
|---|---|---|---|
| 1 | 死代码 | **815 行**（零引用，可安全删除） | 立即删 |
| 2 | miuix 0.9.5 新能力未接入 | `miuix-glass` 20+ 组件只用了 2 个；`miuix-preference` 只 import 1 次 | 择机，不必强求 |
| 3 | 具体缺陷 | 2 个 P2 + 5 个 P3 | 按优先级排 |

---

## 二、界面框架覆盖度

### 2.1 禁用项（全部为 0，符合门禁）

| 检查项 | 结果 |
|---|---|
| `androidx.compose.material3` | 0 |
| `androidx.compose.material.*`（除 icons） | 0 |
| `ui/`、`feature/` 下 `collectAsState` | 0（统一 `collectAsStateWithLifecycle`） |
| `androidx.compose.material3.Divider` | 0（统一 `HorizontalDivider`） |

### 2.2 miuix 各包实际使用量

| 包 | import 次数 | 主要符号 |
|---|---|---|
| `miuix.basic` | 297 | Text / Card / Surface / Button / TextField / Switch / Checkbox / TopAppBar / NavigationBar / TabRow / ListPopupColumn / TooltipBox / SnackbarHost |
| `miuix.theme` | 142 | MiuixTheme / LocalContentColor |
| `miuix.icon.extended` | 46 | Add / Back / Blocklist 等 |
| `miuix.nav.core` | 41 | NavKey |
| `miuix.blur` | 15 | rememberLayerBackdrop / layerBackdrop / textureBlur / BlendColorEntry |
| `miuix.material` | 10 | EdgeStroke / BackdropEffects 相关 |
| `miuix.squircle` | 9 | Squircle 形状 |
| `miuix.layout` | 6 | DialogContentLayout / DialogDefaults |
| `miuix.icon.basic` | 6 | ArrowRight / Check / Close / Search / ArrowUpDown |
| `miuix.window` | 3 | WindowBottomSheet / WindowDropdownPreference / WindowListPopup |
| **`miuix.glass`** | **3** | **仅 GlassNavigationBar + GlassFloatingActionButton** |
| **`miuix.preference`** | **1** | **仅 WindowDropdownPreference（TextMenu.kt）** |

---

## 三、可删除的死代码（815 行，零引用）

| 文件 | 行数 | 判定依据 |
|---|---|---|
| `ui/component/effect/`（7 个文件） | **653** | 全仓库（含测试、XML、proguard）grep 零引用；文件头自述 “Mirrored from compose-miuix-ui example / 同步自 miuix@39c40f9 (v0.9.4)”，是从 miuix 示例搬来做 OS3 动态背景实验的残留 |
| `ui/component/AppBarTextField.kt` | 72 | 只有 `ui/home/AppListPage.kt:116` 一处调用，且已被 `GkAppBarTextField` 取代（见 5.1） |
| `ui/component/GkExpandableSection.kt` | 76 | 只有自身声明，无任何调用 |
| `ui/component/GkTextMenu.kt` | 14 | 只有自身声明（纯转调 `TextMenu`） |
| **合计** | **815** | |

`effect/` 明细：`BgEffectBackground.kt` 103 / `BgEffectConfig.kt` 70 / `BgEffectModifier.kt` 185 / `BgEffectPainter.kt` 174 / `ColorBlendToken.kt` 21 / `DeviceType.kt` 9 / `OS3BgFrag.kt` 91。

> 该目录仍参与编译（`gkd-app/src/main`），删除可减少编译单元，不影响 APK（R8 本就会裁掉）。

---

## 四、miuix 0.9.5 有、我们没有接入的能力

### 4.1 `miuix-glass`（20+ 组件，用了 2 个）

未接但可能有用：`GlassTopAppBar`、`GlassBottomSheet` / `GlassBottomSheetSurface`、`GlassDialog` / `GlassDialogSurface` / `GlassDialogButton`、`GlassCard`、`GlassTextField`、`GlassSearchOverlay`、`GlassTabRow`、`GlassIconButton`、`GlassPanel`、`GlassPress`、`GlassFloatingToolbar`、`GlassNavigationRail`。

对应我们目前的自研实现：

| 我们的实现 | 行数 | 可对应的官方组件 |
|---|---|---|
| `GkDialog.kt`（含 `GkModalBottomSheet`，走 `WindowBottomSheet`） | 370 | `GlassDialog` / `GlassBottomSheet` |
| `GkTopAppBar.kt` | 81 | `GlassTopAppBar` |
| `AppBarTextField.kt` + `GkAppBarTextField.kt`（均基于 `BasicTextField`） | 164 | `GlassTextField` |
| `GkRuleGroupCard` / `SubsItemCard` / `GkAppRuleRestrictionCard`（基于 `Card`） | — | `GlassCard` |

### 4.2 `miuix-preference`（14 个组件，import 1 次）

`SwitchPreference`、`CheckboxPreference`、`RadioButtonPreference`、`SliderPreference`、`ArrowPreference`、`WindowDropdownPreference`、`WindowSpinnerPreference` 全部未用；我们自建了 `SettingItem` / `PreferenceGroup` / `GkSwitch` / `GkCheckbox`（这些已由门禁强制共用同一渲染器，属于有意为之，不建议改）。

### 4.3 `miuix-ui` 里未使用的控件

`Slider`（0 次）、`ColorPicker` / `ColorPalette`、`NavigationRail`、`Badge`、`BreadcrumbBar`、`ScrollBar`、`SearchBar`、`NumberPicker`、`PagerNonTouchScroll`、`MorphHeaderRow`。

> **说明**：上一轮已确定“保持 miuix 默认界面，方便后续迭代”，因此本节只作能力清单，不构成必须改动的建议。若要动，成本最低的两处是：
> 1. `feature/settings/ai/AiProviderDetailPage.kt:109` 的 `TabRow` → `GlassScrollableTabRow`（液态玻璃开启时风格统一）；
> 2. `GkModalBottomSheet`（`GkDialog.kt:140-149`，现用 `WindowBottomSheet`）→ 玻璃模式走 `GlassBottomSheet`。

---

## 五、具体缺陷

### 5.1 P2 — 搜索框有两份实现，已分叉

`AppBarTextField.kt`（旧）与 `GkAppBarTextField.kt`（新）是同一个组件的两代实现，diff 显示新版多做了三件事：

- 外部 `value` 变化时**保留选区**：`selection.start/end` 各自 `coerceIn(0, value.length)`，并清空 `composition`；
- 文字颜色取 `LocalContentColor.current`（旧版硬编码 `onSurface`）；
- 显式 `MutableInteractionSource`。

旧版目前仍被 `ui/home/AppListPage.kt:116` 使用（应用列表搜索框）。症状：搜索时若输入法处于预输入态、同时外部状态回写 `value`，光标与组合态会被重置。

**建议**：把 `AppListPage` 切到 `GkAppBarTextField`，删除 `AppBarTextField.kt`。

### 5.2 P2 — `WebViewPage` 在 `shouldInterceptRequest` 里 `runBlocking`

`ui/WebViewPage.kt:214`：

```kotlin
runBlocking(Dispatchers.IO) {
    val docConfig = client.get(DOC_CONFIG_URL).body<DocConfig>()
    val textUrl = docConfig.htmlUrlMap[path]?.let { docConfig.mirrorBaseUrl + it }
    ...
}
```

`shouldInterceptRequest` 由 WebView 的网络线程同步调用，这里串行发起**两次** HTTP（先配置、再正文），每次打开 `gkd.li` 文档主框架都会阻塞该线程。网络差时表现为文档页长时间白屏。

**建议**：`DocConfig` 做内存缓存（一次会话只取一次），并考虑改名/预取；或改为本地静态文案 + 异步刷新。

### 5.3 P2 — 架构文档版本号过期

`docs/miuix-ui.md:3` 仍写「固定使用 Miuix `0.9.4`」，实际已是 `0.9.5-rc02`。这份文档是给人看的架构说明，且 `check_miuix_ui.py` 的注释明确要求「Bump this together with the pin」，文档没跟上。

### 5.4 P3 — 硬编码中文文案与 `UiStrings` 两套并存

- `strings.xml`：1065 条，`UiStrings` 由 `buildSrc/.../GenerateUiStringsTask.kt` 生成，全仓调用 **1236 次**（覆盖 926 条）；
- 但源码里另有 **约 609 处**含中文的字符串字面量，其中约 **520 处是真实 UI 文案**（标题/提示/按钮/无障碍描述），例：`ui/home/SettingsPage.kt` 111 处、`feature/settings/ai/AiProviderDetailPage.kt` 67 处、`AiProviderModelsTab.kt` 51 处。

后果：改一句文案要么改代码、要么改资源，无从统一；后续若要做多语言基本不可能。

**建议**：不做大迁移。新写的界面统一走 `UiStrings`，老界面随功能改动顺手迁移即可。

### 5.5 P3 — 139 条 `strings.xml` 条目无任何引用

例如 `a11y_background_running_warning`、`a11y_other_services_warning`、`ai_max_tokens_retry`、`about_title`、`action_delete_records` 等。开启 `isShrinkResources = true` 后不占 APK 体积，但会干扰“哪些文案还在用”的判断。

### 5.6 P3 — Lazy 列表 18 处 `items()` 只有 6 处带 key

未带 key 的 12 处里，部分是静态短列表（无影响）；滚动列表（如 `SnapshotNodesPage.kt:333`、`CrashReportPage.kt:136`、`GkLazyCopyableText.kt:57`）建议补 `key =`，避免增删时重组错位。

### 5.7 P3 — 依赖无 checksum 锁定

`gradle/` 下只有 `libs.versions.toml`、`wrapper`、`gradle-daemon-jvm.properties`，**没有 `verification-metadata.xml`**，即没有依赖校验和、也没有 dependency locking。Maven Central 原则上禁止覆盖已发布版本，但真出现重传时本地与 CI 可能拿到不同字节。

**建议**：`./gradlew --write-verification-metadata sha256 help` 生成一次固定下来。

---

## 六、健康项（无需处理）

| 项 | 结果 |
|---|---|
| 编译 | `:gkd-app:compileGkdDebugKotlin --rerun-tasks` 0 错误 |
| 代码卫生 | `TODO` / `FIXME` / `HACK` 0；`println` / `System.out` 0；`GlobalScope` 0 |
| 测试 | 105 个测试文件（gkd-app 83 / gkd-priv-ui 6 / gkd-selector 13 / gkd-db 3） |
| 混淆与压缩 | perf / release 均 `isMinifyEnabled = true` + `isShrinkResources = true` |
| 备份 | `allowBackup="false"`，且同时配了 `backup_rules` 与 `data_extraction_rules` |
| 网络安全 | 已配 `networkSecurityConfig` |
| `ExposeService`（`exported=true` 且无系统权限） | 应用层 token 校验，`MessageDigest.isEqual` 常量时间比较；token 存 `noBackupFilesDir`，不随备份外泄 |
| 其他导出组件 | FileProvider `exported=false` + `grantUriPermissions`；ShizukuProvider 要求 `INTERACT_ACROSS_USERS_FULL`；5 个 QS Tile 均要求 `BIND_QUICK_SETTINGS_TILE`；无障碍服务 `BIND_ACCESSIBILITY_SERVICE` |
| `HorizontalDivider` | 11 处，全部走 miuix |
| 资源语言目录 | 仅 `values` + `values-night`（面向中文用户，无多语言需求） |

---

## 七、建议执行顺序

| 顺序 | 动作 | 风险 | 收益 |
|---|---|---|---|
| 1 | 删 `ui/component/effect/`（653 行）+ `GkExpandableSection.kt`（76）+ `GkTextMenu.kt`（14） | 零 | 少 743 行死代码，编译单元减少 |
| 2 | `AppListPage` 切 `GkAppBarTextField`，删 `AppBarTextField.kt`（72） | 低 | 修掉 P2 光标跳变 |
| 3 | `docs/miuix-ui.md` 版本号改 `0.9.5-rc02` | 零 | 文档与代码一致 |
| 4 | `WebViewPage` 缓存 `DocConfig` | 中 | 修掉 P2 文档页卡顿 |
| 5 | 加 `verification-metadata.xml` | 低 | 依赖可复现 |
| 6 | 抽样补 key、清理 139 条未用字符串 | 低 | 可维护性 |

前 3 项可以合并成一次提交（改动小、可一次性验证），第 4 项建议单独提交便于回滚。
