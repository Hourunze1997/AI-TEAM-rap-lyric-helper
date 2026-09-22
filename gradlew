#!/usr/bin/env sh
# 自包含 Gradle Wrapper 脚本 — 自动下载 Gradle 并执行
# 解决 Docker 环境无 gradle 命令的问题

APP_NAME="Gradle"
APP_VERSION="8.2"

# 项目根目录
DIR="$(cd "$(dirname "$0")" && pwd)"

# Gradle 分发缓存目录
GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
GRADLE_DIST_DIR="$GRADLE_USER_HOME/wrapper/dists"
GRADLE_DIST_SUBDIR="gradle-$APP_VERSION-bin"
GRADLE_DIST_PATH="$GRADLE_DIST_DIR/$GRADLE_DIST_SUBDIR/gradle-$APP_VERSION"

# 如果系统已安装 gradle 且版本匹配则直接使用
if command -v gradle > /dev/null 2>&1; then
    SYSTEM_VER=$(gradle --version 2>/dev/null | grep -i "^Gradle " | head -1 | awk '{print $2}')
    if [ "$SYSTEM_VER" = "$APP_VERSION" ]; then
        exec gradle "$@"
    fi
fi

# 下载并解压 Gradle
if [ ! -d "$GRADLE_DIST_PATH" ]; then
    mkdir -p "$GRADLE_DIST_DIR"
    ZIP_FILE="$GRADLE_DIST_DIR/$GRADLE_DIST_SUBDIR.zip"
    if [ ! -f "$ZIP_FILE" ]; then
        echo "Downloading Gradle $APP_VERSION..."
        URL="https://services.gradle.org/distributions/gradle-$APP_VERSION-bin.zip"
        if command -v curl > /dev/null 2>&1; then
            curl -L -o "$ZIP_FILE" "$URL" || { echo "curl failed"; exit 1; }
        elif command -v wget > /dev/null 2>&1; then
            wget -O "$ZIP_FILE" "$URL" || { echo "wget failed"; exit 1; }
        else
            echo "Error: curl or wget is required to download Gradle"
            exit 1
        fi
    fi
    echo "Extracting Gradle $APP_VERSION..."
    if command -v unzip > /dev/null 2>&1; then
        unzip -q -o "$ZIP_FILE" -d "$GRADLE_DIST_DIR/$GRADLE_DIST_SUBDIR/"
    else
        # 尝试用 jar 解压（JDK 自带）
        if command -v jar > /dev/null 2>&1; then
            mkdir -p "$GRADLE_DIST_DIR/$GRADLE_DIST_SUBDIR/"
            cd "$GRADLE_DIST_DIR/$GRADLE_DIST_SUBDIR/" && jar xf "$ZIP_FILE" && cd "$DIR"
        else
            echo "Error: unzip or jar is required to extract Gradle"
            exit 1
        fi
    fi
fi

# 执行 Gradle
exec "$GRADLE_DIST_PATH/bin/gradle" -p "$DIR" "$@"
