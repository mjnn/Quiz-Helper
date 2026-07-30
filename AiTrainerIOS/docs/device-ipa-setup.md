# 真机 IPA 构建指南

CI 工作流 **[iOS Export IPA](../../.github/workflows/ios-ipa.yml)** 可在 GitHub Actions 云端签名并导出 `.ipa`，无需本地 Mac。

Bundle ID：`com.aitrainer.practice`

---

## 前置条件

1. **Apple Developer 账号**（个人或公司，[developer.apple.com](https://developer.apple.com)）
2. 在 [Certificates, Identifiers & Profiles](https://developer.apple.com/account/resources) 中注册 App ID：`com.aitrainer.practice`
3. （`development` / `ad-hoc`）在开发者后台登记测试 iPhone 的 **UDID**
4. 在 [App Store Connect → 用户与访问 → 集成 → API 密钥](https://appstoreconnect.apple.com/access/integrations/api) 创建 **App Store Connect API Key**（角色：Developer 或 Admin）

记下：

| 项 | 示例 |
|----|------|
| Team ID | `AB12CD34EF`（Membership 页面） |
| Key ID | `XXXXXXXXXX` |
| Issuer ID | UUID 格式 |

下载 `.p8` 私钥文件（仅可下载一次，请妥善保存）。

---

## 配置 GitHub Secrets

仓库 → **Settings** → **Secrets and variables** → **Actions** → **New repository secret**

| Secret 名称 | 内容 |
|-------------|------|
| `APPLE_DEVELOPER_TEAM_ID` | 10 位 Team ID |
| `APPLE_API_KEY_ID` | API Key 的 Key ID |
| `APPLE_API_ISSUER_ID` | Issuer ID |
| `APPLE_API_PRIVATE_KEY` | `.p8` 文件内容的 **Base64**（见下方命令） |

PowerShell 生成 Base64：

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("AuthKey_XXXXXXXXXX.p8"))
```

macOS / Linux：

```bash
base64 -i AuthKey_XXXXXXXXXX.p8 | pbcopy
```

---

## 触发构建

1. 打开 https://github.com/mjnn/Quiz-Helper/actions
2. 选择 **iOS Export IPA** → **Run workflow**
3. 选项：
   - **export_method**
     - `development` — 开发版，仅已登记 UDID 的设备可装（适合自测）
     - `ad-hoc` — 内测分发，需 Ad Hoc 描述文件与设备 UDID
     - `app-store` — 用于 TestFlight / App Store（勾选 upload_testflight 可自动上传）
   - **upload_testflight** — 仅 `app-store` 时有效

4. 成功后 → 该 Run → **Artifacts** → 下载 `AiTrainer-<method>-ipa`

---

## 安装到 iPhone

### development / ad-hoc IPA

- 用 **Apple Configurator**、**Xcode → Window → Devices**，或 **爱思助手** 等工具安装
- 设备 UDID 必须已在开发者后台登记
- 首次打开：设置 → 通用 → VPN与设备管理 → 信任开发者

### app-store（TestFlight）

- 勾选 **upload_testflight** 后，在 [App Store Connect](https://appstoreconnect.apple.com) → TestFlight 添加内部测试员
- 测试员通过 TestFlight App 安装（无需 UDID）

---

## 与 Simulator 构建的区别

| 工作流 | 产物 | 用途 |
|--------|------|------|
| **iOS Build & Test** | `AiTrainer-Simulator.app` | 模拟器验证、每日 CI |
| **iOS Export IPA** | `AiTrainer-*.ipa` | 真机安装 / TestFlight |

Simulator 包**不能**装到真机；真机 IPA **不能**在模拟器运行。

---

## 常见问题

**Archive 失败：No profiles for com.aitrainer.practice**  
→ 确认 App ID 已注册，API Key 权限足够，Team ID 正确。

**Export 失败：设备未包含在 profile**  
→ `development`/`ad-hoc` 需在开发者后台添加设备 UDID 后重新 Run workflow。

**不想用 API Key，用手动证书？**  
→ 可在本地 Mac 用 Xcode 打开工程，配置 Signing & Capabilities 后 Product → Archive；云端 CI 推荐使用 API Key + 自动签名（本仓库默认方案）。
