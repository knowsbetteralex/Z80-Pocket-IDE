#!/usr/bin/env bash
set -euo pipefail

# Emit release notes in both UI languages, without depending on a translation service.
version="${1:?Usage: release-notes.sh VERSION COMMIT RUN REPOSITORY}"
commit="${2:?Missing commit SHA}"
run="${3:?Missing workflow run number}"
repo="${4:?Missing owner/repository}"

# Bilingual concise features are maintained separately from CHANGELOG.
features_file="$(dirname "$0")/../docs/RELEASE_FEATURES.md"
ru_features="$(awk '/^## Русский$/{section="ru";next} /^## English$/{section="en";next} section=="ru" && /^- /{print}' "$features_file")"
en_features="$(awk '/^## Русский$/{section="ru";next} /^## English$/{section="en";next} section=="en" && /^- /{print}' "$features_file")"
test -n "$ru_features" && test -n "$en_features"

cat <<NOTES
## Русский

**Z80 Pocket IDE v${version}** · **Сборка #${run}**

Это автоматически собранная и проверенная версия Android-приложения из ветки **main**.

### Что нового

${ru_features}

- **APK:** скачайте файл **Z80-Pocket-IDE.apk** ниже, в разделе **Assets**.
- **Проверка файла:** **Z80-Pocket-IDE.apk.sha256** содержит контрольную сумму SHA-256.
- **Код сборки:** [коммит ${commit:0:12}](https://github.com/${repo}/commit/${commit}).
- **Изменения:** [история версий / CHANGELOG](https://github.com/${repo}/blob/main/CHANGELOG.md).

**Важно:** APK подписан отладочным ключом Android, а не производственным ключом Google Play. Ключ разных CI-сборок может отличаться. Перед переустановкой сохраните исходники и данные проекта.

---

## English

**Z80 Pocket IDE v${version}** · **Build #${run}**

This is an automatically built and tested Android APK from the **main** branch.

### What's new

${en_features}

- **APK:** download **Z80-Pocket-IDE.apk** from **Assets** below.
- **File verification:** **Z80-Pocket-IDE.apk.sha256** contains the SHA-256 checksum.
- **Source commit:** [${commit:0:12}](https://github.com/${repo}/commit/${commit}).
- **Changes:** [version history / CHANGELOG](https://github.com/${repo}/blob/main/CHANGELOG.md).

**Important:** the APK is Android debug-signed, not Google Play production-signed. The debug signing key may vary between CI builds. Back up source files and project data before reinstalling.
NOTES
