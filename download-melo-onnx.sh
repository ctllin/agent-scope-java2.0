#!/usr/bin/env bash
# 下载 sherpa-onnx Native JNI 库 + MeloTTS ONNX 模型
# 用法: bash scripts/download-melo-onnx.sh [mirror|direct]
#   mirror  — 使用 gh.ddlc.top 加速（默认）
#   direct  — 直连 GitHub

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
DEST="${PROJECT_ROOT}/sherpa-onnx"
VERSION="v1.13.0"
MIRROR="https://ghfast.top/https://github.com"
DIRECT="https://github.com"

USE_MIRROR="${1:-mirror}"
BASE="$MIRROR"
if [ "$USE_MIRROR" = "direct" ]; then
    BASE="$DIRECT"
fi

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

download() {
    local url=$1 output=$2
    rm -f "$output"
    if command -v wget &>/dev/null; then
        wget --timeout=300 --tries=3 -O "$output" "$url" 2>&1 | grep -v "HTTP ERROR" || true
        [ -f "$output" ] && [ -s "$output" ]
    elif command -v curl &>/dev/null; then
        curl -L --retry 3 --connect-timeout 60 --max-time 600 -f -o "$output" "$url"
    else
        return 1
    fi
}

try_download() {
    local accel_url=$1 official_url=$2 output=$3 desc=$4
    info "下载 ${desc}..."
    if download "$accel_url" "$output" 2>/dev/null; then
        return 0
    fi
    warn "加速镜像失败，尝试官方源..."
    download "$official_url" "$output" || error "${desc} 下载失败"
}

mkdir -p "$DEST"

echo "=== 下载 sherpa-onnx Native JNI 库 ==="

JNI_URL="$BASE/k2-fsa/sherpa-onnx/releases/download/$VERSION/sherpa-onnx-$VERSION-linux-x64-jni.tar.bz2"
try_download "$JNI_URL" \
             "https://github.com/k2-fsa/sherpa-onnx/releases/download/$VERSION/sherpa-onnx-$VERSION-linux-x64-jni.tar.bz2" \
             "$DEST/sherpa-onnx-linux-x64-jni.tar.bz2" \
             "sherpa-onnx-linux-x64-jni"

info "解压 native 库..."
cd "$DEST"
tar xjf sherpa-onnx-linux-x64-jni.tar.bz2

# 解压后的目录名: sherpa-onnx-v1.13.0-linux-x64-jni (带v前缀)
EXTRACTED_DIR="sherpa-onnx-${VERSION}-linux-x64-jni"
if [ -d "$EXTRACTED_DIR" ]; then
    cp "$EXTRACTED_DIR/lib/libsherpa-onnx-jni.so" ./ 2>/dev/null || true
    cp "$EXTRACTED_DIR/lib/libonnxruntime.so" ./ 2>/dev/null || true
    rm -rf "$EXTRACTED_DIR"
fi
rm -f sherpa-onnx-linux-x64-jni.tar.bz2

echo ""
echo "=== 下载 MeloTTS zh_en ONNX 模型 ==="

MODEL_URL="$BASE/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-melo-tts-zh_en.tar.bz2"
try_download "$MODEL_URL" \
             "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-melo-tts-zh_en.tar.bz2" \
             "$DEST/vits-melo-tts-zh_en.tar.bz2" \
             "vits-melo-tts-zh_en (约 160MB)"

info "解压模型..."
cd "$DEST"
tar xjf vits-melo-tts-zh_en.tar.bz2
rm -f vits-melo-tts-zh_en.tar.bz2

# 如果模型解压到了嵌套的 sherpa-onnx/ 目录，移到正确位置
if [ -d "$DEST/sherpa-onnx/vits-melo-tts-zh_en" ] && [ ! -d "$DEST/vits-melo-tts-zh_en" ]; then
    mv "$DEST/sherpa-onnx/vits-melo-tts-zh_en" "$DEST/"
    rm -rf "$DEST/sherpa-onnx"
fi

echo ""
echo "=== 完成 ==="
echo ""
ls -lh "$DEST/"
echo ""
if [ -d "$DEST/vits-melo-tts-zh_en" ]; then
    echo "模型文件:"
    ls -lh "$DEST/vits-melo-tts-zh_en/"
fi
