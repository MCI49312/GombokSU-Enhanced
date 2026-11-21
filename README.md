<div align="center">

# GombokSU Enhanced

**A customized fork of [KernelSU-Next](https://github.com/KernelSU-Next/KernelSU-Next) with an advanced theme studio, 57-font picker, 4-icon selector, built-in auto-updater, and automated upstream release syncing.**

[![Latest Release](https://img.shields.io/github/v/release/MCI49312/GombokSU-Enhanced?style=for-the-badge&logo=github)](https://github.com/MCI49312/GombokSU-Enhanced/releases/latest)
[![License](https://img.shields.io/github/license/MCI49312/GombokSU-Enhanced?style=for-the-badge)](LICENSE)
[![Package ID](https://img.shields.io/badge/Package_ID-mci.github.gomboksu-3B82F6?style=for-the-badge&logo=android)](https://github.com/MCI49312/GombokSU-Enhanced/releases/latest)

</div>

---

> **Note:** GombokSU Enhanced is a personal/community customization fork built with AI assistance. **MCI49312 does NOT accept donations.** Please direct any financial support strictly to the upstream [KernelSU-Next](https://github.com/KernelSU-Next/KernelSU-Next) and [KernelSU](https://github.com/tiann/KernelSU) authors who built the underlying kernel root solution.


> **Disclaimer:** The developer is not responsible if you do something wrong and brick your device. Use GombokSU Enhanced at your own risk.

## ✨ What's New in GombokSU Enhanced

### 🎨 Theme & Customization Studio
- **Custom Accent Color Engine**: 12 curated presets (*Gombok Blue, Catppuccin, Emerald, Crimson, Amethyst, Sunset, Sakura, Cyber Gold, Ocean Teal, Nord Frost, Dracula, Mint*) plus a full **RGB / Hex Color Picker**.
- **Theme Controls**: System / Light / Dark mode switcher, Material You (Monet) dynamic colors toggle, and **Pure Black AMOLED** dark mode.
- **57-Font Dropdown Picker**: Choose from 57 bundled typefaces — including **Comic Sans**, monospace, display, handwriting, serif, and sans-serif families — applied live across the app.
- **Shapes & Layout**: Custom Card Corner Radius slider (`4dp` – `28dp`).

### 🧩 4-Icon Launcher & Logo Selector
Switch both the Android launcher icon and in-app logo on the fly, each supporting **Normal** and **Material You (Monochrome)** themed icons:
1. **GombokSU Enhanced** (Comic Sans typography)
2. **KernelSU Next**
3. **KernelSU** (Classic)
4. **Skull & Crossbones (`☠️`)** (Google Emoji style, single-color monochrome in Material You)

### 🧭 Navigation & Home Dashboard
- **Live Navigation Bar Controls**: Switch between **Floating Pill**, **Classic Docked**, and **Compact Pill** styles, toggle bottom bar visibility live without restarting, configure icon labels, auto-hide on scroll, horizontal swipe tab navigation, and haptic feedback.
- **About Device Card**: Expandable card on Home showing **Device Model** (marketing name + model code), **Build Fingerprint**, Kernel, Android version, ABI, SELinux, and Seccomp status.
- **Home Customization**: Custom top bar title & subtitle, **Show About Device on Home** toggle (enabled by default), **Auto-open About Device on Startup** toggle, and dedicated **Contributors** and **App Updater** pages.

### 🔄 Built-In App Updater
- **Dedicated App Updater Screen** (accessible from **Home** and **Settings**).
- **Strict Build Variant Detection**: Automatically detects whether you are running the **Normal** (`mci.github.gomboksu`) or **Spoofed** build and only downloads/installs matching APKs — never confusing Normal with Spoofed or vice versa.
- **Strict Version Comparison**: Compares both semantic version tags/filenames and manager version codes so an already-installed version (e.g., `v3.4.2`) will not trigger an update for itself.
- **Automatic App Update**: Optionally downloads and installs new releases automatically (via root `pm install` with automatic relaunch, or standard Android Package Installer fallback).
- **Two Quick Actions**: **Reinstall App** and **Update to New** pinned at the bottom of the Updater page.

### ⚡ Boot Image Patching & Automated Upstream Sync
- **Automatic LKM Manager Signature Patching**: Patching a `boot.img` or `init_boot.img` inside GombokSU Enhanced automatically extracts the target KMI `kernelsu.ko`, patches the manager certificate hash to match GombokSU Enhanced's permanent signing key (`mci.github.gomboksu`), and saves the output as `gomboksu_enhanced_patched_YYYYMMDD_HHMMSS.img` in `Downloads`.
- **Automated Releases**: A GitHub Actions workflow checks upstream `KernelSU-Next` every 5 minutes, syncs the latest `libksud.so`, LKM `.ko` modules, and UAPI version, builds both Normal and Spoofed APKs, verifies the APK v2 signature, and publishes a matching GitHub Release within ~5–10 minutes.

---

## 📥 Installation

1. Download the latest APK from [**Releases**](https://github.com/MCI49312/GombokSU-Enhanced/releases/latest):
   - **`GombokSU_Enhanced_vX.Y.Z_<code_>-release.apk`** — Standard build (`mci.github.gomboksu`)
   - **`GombokSU_Enhanced_vX.Y.Z-spoofed_<code_>-release.apk`** — Randomized package ID build
2. Install the APK on your device.
3. Open **GombokSU Enhanced** → **Install** → **Select a file** → select your stock `init_boot.img` (Android 13+ GKI devices such as Google Pixel) or `boot.img` → tap **Next**.
4. Flash the generated `gomboksu_enhanced_patched_YYYYMMDD_HHMMSS.img` from your `Download` folder via `fastboot`:
   ```bash
   fastboot flash init_boot gomboksu_enhanced_patched_YYYYMMDD_HHMMSS.img
   fastboot reboot
   ```
5. After rebooting, open **GombokSU Enhanced** — it should display **Working [LKM (GKI2)]**.

> **Updating GombokSU Enhanced:** Use the built-in **App Updater** screen inside the app, or install the newer APK directly over your existing installation (no need to re-flash your boot image unless upgrading to a newer kernel UAPI version).

---

## 🐛 Bug Reports

Found an issue specific to **GombokSU Enhanced**? Please open an issue on the [GitHub Issues page](https://github.com/MCI49312/GombokSU-Enhanced/issues).

---

## 👏 Credits

- **[MCI49312](https://github.com/MCI49312)** — GombokSU Enhanced customization & automation *(I do NOT accept donations)*
- **[rifsxd (Rifat Azad)](https://github.com/rifsxd)** — [KernelSU-Next](https://github.com/KernelSU-Next/KernelSU-Next) Author
- **[tiann (Weishu)](https://github.com/tiann)** — [KernelSU](https://github.com/tiann/KernelSU) Author
- **[fatalcoder524](https://github.com/fatalcoder524)**, **[pershoot](https://github.com/pershoot)**, **[maxsteeel](https://github.com/maxsteeel)** — KernelSU-Next Maintainers

---

## 📄 License

- Files under the `kernel` directory are licensed under **GPL-2.0-only**.
- All other parts of the repository (including the Android Manager app and `ksud`) are licensed under **GPL-3.0-or-later**. See [LICENSE](LICENSE) for full details.
