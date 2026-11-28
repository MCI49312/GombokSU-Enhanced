<div align="center">

# GombokSU Enhanced

**A customized fork of [KernelSU-Next](https://github.com/KernelSU-Next/KernelSU-Next) featuring a full Theme & Animation Studio, 57-font picker, 4-icon selector, on-device Custom Spoof, built-in Root Terminal & Diagnostics, App Updater, and automated upstream release syncing.**

[![Latest Release](https://img.shields.io/github/v/release/MCI49312/GombokSU-Enhanced?style=for-the-badge&logo=github)](https://github.com/MCI49312/GombokSU-Enhanced/releases/latest)
[![License](https://img.shields.io/github/license/MCI49312/GombokSU-Enhanced?style=for-the-badge)](LICENSE)
[![Package ID](https://img.shields.io/badge/Package_ID-mci.github.gomboksu-3B82F6?style=for-the-badge&logo=android)](https://github.com/MCI49312/GombokSU-Enhanced/releases/latest)

</div>

---

> **Note:** GombokSU Enhanced is a personal/community customization fork built with AI assistance. **MCI49312 does NOT accept donations.** Please direct any financial support strictly to the upstream [KernelSU-Next](https://github.com/KernelSU-Next/KernelSU-Next) and [KernelSU](https://github.com/tiann/KernelSU) authors who built the underlying kernel root solution.

> **Disclaimer:** The developer is not responsible if you do something wrong and brick your device. Use GombokSU Enhanced at your own risk.

---

## ✨ What's New in GombokSU Enhanced

### 🎨 Theme & Customization Studio
- **Custom Accent Color Engine**: 12 curated presets (*Gombok Blue, Catppuccin, Emerald, Crimson, Amethyst, Sunset, Sakura, Cyber Gold, Ocean Teal, Nord Frost, Dracula, Mint*), dynamic **Material You (Monet)** with **Device Accent** or any custom accent override, plus a full **RGB / Hex Color Picker**.
- **Theme Controls**: System / Light / Dark mode switcher and **Pure Black AMOLED** dark mode.
- **57-Font Dropdown Picker**: Choose from 57 bundled typefaces — including **Comic Sans**, monospace, display, handwriting, serif, and sans-serif families — applied live across the app.
- **Shapes & Profiles**: Custom Card Corner Radius slider (`4dp` – `28dp`) plus **JSON Profile Export & Import** (copy/paste via clipboard or save/load `.json` files covering colors, fonts, nav bar, layout, and animation physics).

### 🎞️ Animations Studio
- **Spring Physics Engine**: Interactive **Live Physics Playground**, **Bounce Multiplier** (`0.0x` – `2.5x`), **Animation Speed** (`0.4x` – `2.5x`), and 6 one-tap presets (*Balanced, Snappy, Bouncy, Jelly, Cinematic, Minimal*).
- **Page Transitions & Splash Intros**: 5 navigation transition styles (*Slide, Bouncy Zoom, Depth Push, Vertical Rise, Elastic Pop*) and 4 startup splash animations (*Classic Spin, Elastic Pop, Pulse Wave, 3D Flip*) with live fullscreen preview.
- **Motion Effects**: Toggleable card press squish & bounce, staggered card entrance cascades, Home status pulse glow, and bouncy navigation bar pills & icons.

### 🧩 4-Icon Launcher & Logo Selector
Switch both the Android launcher icon and in-app logo on the fly, each supporting **Normal** and **Material You (Monochrome)** themed icons:
1. **GombokSU Enhanced** (Comic Sans typography)
2. **KernelSU Next**
3. **KernelSU** (Classic)
4. **Skull & Crossbones (`☠️`)** (Google Emoji style, single-color monochrome in Material You)

### 🖥️ Built-In Root Terminal & Diagnostics
- **Quick Access**: Accessible from the side-by-side **Terminal** & **Updater** icon buttons on the Home screen as well as **Settings**.
- **Root Environment**: Toggle **Global Mount Namespace (`--mount-master`)** and run custom commands via the KernelSU root shell with command history, execution timing, exit codes, and selectable live console output.
- **One-Tap Diagnostics**: Instant chips for *Identity & Kernel*, *KSU Modules* (`libksud.so`), *Kernel Log (`dmesg`)*, *Logcat (`*:W`)*, *Mounts*, *System Props*, *`ksud` & `/data/adb`*, and *Processes*, with copy-to-clipboard and `.txt` export.

### 🥷 On-Device Custom Spoof (Spoofed Build)
- **Custom Package ID & App Name**: Available inside the **Spoofed** build under **Settings → Custom Spoof** and **Customization → Custom Spoof** (with package ID validation, randomizer, and optional old-app cleanup).
- **On-Device Repackaging & `0x3e6` Signing**: Patches binary `AndroidManifest.xml` and shortcuts cleanly on-device and re-signs with APK Signature Scheme v2 using GombokSU Enhanced's permanent `0x3e6` certificate so `kernelsu.ko` continues to recognize the spoofed app as **Working**.
- **Workflow Support**: GitHub Actions `workflow_dispatch` also accepts optional `custom_spoof_package` and `custom_spoof_name` inputs.

### 🧭 Navigation, Home Dashboard & Languages
- **Live Navigation Bar Controls**: Switch between **Floating Pill**, **Classic Docked**, and **Compact Pill** styles, toggle bottom bar visibility live without restarting, configure icon labels, auto-hide on scroll, horizontal swipe tab navigation, and haptic feedback.
- **Home Customization**: Custom top bar title & subtitle, expandable **About Device** card (*Device Model*, *Build Fingerprint*, Kernel, Android, ABI, SELinux, Seccomp), **Show About Device on Home** (enabled by default), **Auto-open About Device on Startup**, and dedicated **Contributors** and **App Updater** screens.
- **Shortcuts & Root Guard**: Launcher shortcuts (*Superuser*, *Modules*) safely redirect to Home when the device is not rooted.
- **Language Support**: Pre-configured in English out of the box, with a full **Hungarian (`hu-HU`)** translation and 20+ supported locales selectable inside the app.

### 🔄 Built-In App Updater
- **Dedicated App Updater Screen** (accessible from the Home icon row and **Settings**).
- **Strict Build Variant Detection**: Automatically distinguishes **Normal** (`mci.github.gomboksu`) vs **Spoofed** builds and never mixes them.
- **Strict Version Comparison & Formatted Changelog**: Compares both semantic version tags/filenames and manager version codes, renders clean formatted GitHub release notes, and provides **Reinstall App** and **Update to New** buttons that hand off cleanly to Android's standard Package Installer.

### ⚡ Boot Image Patching & Automated Upstream Sync
- **Automatic LKM Manager Signature Patching**: Patching a `boot.img` or `init_boot.img` automatically extracts the target KMI `kernelsu.ko`, patches the manager certificate hash to match GombokSU Enhanced's permanent `0x3e6` signing key, and saves `gomboksu_enhanced_patched_YYYYMMDD_HHMMSS.img` to `Downloads`.
- **Automated Upstream Sync**: A GitHub Actions workflow checks upstream `KernelSU-Next` every 5 minutes, syncs the latest `libksud.so`, LKM `.ko` modules (with patched manager hash), and UAPI version, builds and verifies both Normal and Spoofed APKs, and publishes a matching GitHub Release automatically.

---

## 📥 Installation

1. Download the latest APK from [**Releases**](https://github.com/MCI49312/GombokSU-Enhanced/releases/latest):
   - **`GombokSU_Enhanced_vX.Y.Z_<code_>-release.apk`** — Standard build (`mci.github.gomboksu`)
   - **`GombokSU_Enhanced_vX.Y.Z-spoofed_<code_>-release.apk`** — Randomized package ID build (supports on-device **Custom Spoof**)
2. Install the APK on your device.
3. Open **GombokSU Enhanced** → **Install** → **Select a file** → select your stock `init_boot.img` (Android 13+ GKI devices such as Google Pixel) or `boot.img` → tap **Next**.
4. Flash the generated `gomboksu_enhanced_patched_YYYYMMDD_HHMMSS.img` from your `Download` folder via `fastboot`:
   ```bash
   fastboot flash init_boot gomboksu_enhanced_patched_YYYYMMDD_HHMMSS.img
   fastboot reboot
   ```
5. After rebooting, open **GombokSU Enhanced** — it should display **Working [LKM (GKI2)]**.

> **Updating GombokSU Enhanced:** Use the built-in **App Updater** inside the app, or install the newer APK directly over your existing installation (no need to re-flash your boot image unless upgrading to a newer kernel UAPI version).

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
