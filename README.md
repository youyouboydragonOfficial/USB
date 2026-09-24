# USB Speed Tester & Capacity Benchmark

スマートフォンに接続したUSBメモリ・SDカード・外部ストレージの**実効転送速度（シーケンシャル／ランダム読み書き）**および**偽装容量（Fake Drive）判定**を行うAndroidアプリです。

---

## 🚀 GitHub ReleaseでAPKを自動配布する方法

このリポジトリには **GitHub Actions 自動ビルド・リリースワークフロー**（`.github/workflows/release.yml`）が含まれています。

### 方法 1: タグ（バージョン）をプッシュして自動リリース
Gitでバージョンタグを付けてプッシュすると、GitHub上で自動的にビルドが走り、**Releases** ページに `app-debug.apk` が添付されて公開されます。

```bash
git tag v1.0.0
git push origin v1.0.0
```

### 方法 2: GitHubの画面から手動でリリースを実行 (Workflow Dispatch)
1. GitHubリポジトリの **Actions** タブを開きます。
2. 左メニューの **「Build and Release APK」** を選択します。
3. **「Run workflow」** ボタンをクリックします。
4. ビルド完了後、**Releases** または **Artifacts** に `app-debug.apk` が生成されます。

### 方法 3: 手動でGitHub Releasesにアップロードする場合
手元やAI StudioからダウンロードしたAPK（`app-debug.apk`）を直接アップロードする場合：
1. GitHubリポジトリの右側にある **「Releases」** -> **「Draft a new release」** を開く。
2. タグ名（例: `v1.0.0`）を入力。
3. 下部の **「Attach binaries by dropping them here or selecting them」** エリアに `app-debug.apk` をドラッグ＆ドロップ。
4. **「Publish release」** をクリック。

---

## 🛠 ローカルビルド方法

```bash
./gradlew assembleDebug
```
生成先: `app/build/outputs/apk/debug/app-debug.apk`
