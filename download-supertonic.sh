#!/bin/bash
# ====================================================================
# SupertonicTTS 3 下载脚本
# 使用 gh.ddlc.top 加速
# ====================================================================

set -e

SHERPA_VERSION="v1.13.0"
SHERPA_JAR="sherpa-onnx-v1.13.0.jar"
SHERPA_NATIVE_JAR="sherpa-onnx-native-lib-linux-x64-v1.13.0.jar"
MODEL_NAME="sherpa-onnx-supertonic-3-tts-int8-2026-05-11"
ACCEL="https://ghfast.top/https://github.com"
OFFICIAL="https://github.com"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
LIB_DIR="${PROJECT_ROOT}/sherpa-onnx"
JAR_DIR="${PROJECT_ROOT}/sherpa-onnx"

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

# 1. 更新 sherpa-onnx JAR + Native 库
update_jar() {
    info "======== 更新 sherpa-onnx (${SHERPA_VERSION}) ========"

    # 下载 Java API JAR
    local jar="${JAR_DIR}/sherpa-onnx-java17.jar"
    if [ -f "$jar" ]; then
        read -p "JAR 已存在，是否更新? (y/N): " -n 1 -r; echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            local tmp="/tmp/sherpa-onnx-java17.jar"
            try_download "${ACCEL}/k2-fsa/sherpa-onnx/releases/download/${SHERPA_VERSION}/${SHERPA_JAR}" \
                         "${OFFICIAL}/k2-fsa/sherpa-onnx/releases/download/${SHERPA_VERSION}/${SHERPA_JAR}" \
                         "$tmp" "sherpa-onnx JAR"
            mv "$tmp" "$jar"
        fi
    else
        local tmp="/tmp/sherpa-onnx-java17.jar"
        try_download "${ACCEL}/k2-fsa/sherpa-onnx/releases/download/${SHERPA_VERSION}/${SHERPA_JAR}" \
                     "${OFFICIAL}/k2-fsa/sherpa-onnx/releases/download/${SHERPA_VERSION}/${SHERPA_JAR}" \
                     "$tmp" "sherpa-onnx JAR"
        mv "$tmp" "$jar"
    fi

    # 下载 Native 库
    local native_tmp="/tmp/sherpa-onnx-native.jar"
    try_download "${ACCEL}/k2-fsa/sherpa-onnx/releases/download/${SHERPA_VERSION}/${SHERPA_NATIVE_JAR}" \
                 "${OFFICIAL}/k2-fsa/sherpa-onnx/releases/download/${SHERPA_VERSION}/${SHERPA_NATIVE_JAR}" \
                 "$native_tmp" "sherpa-onnx Native 库"

    info "解压 Native 库..."
    cd /tmp && jar xf "$native_tmp" sherpa-onnx/native/linux-x64/
    cp /tmp/sherpa-onnx/native/linux-x64/libsherpa-onnx-jni.so "${LIB_DIR}/../"
    cp /tmp/sherpa-onnx/native/linux-x64/libonnxruntime.so "${LIB_DIR}/../"
    rm -rf "$native_tmp" /tmp/sherpa-onnx

    info "更新完成"
}

# 2. 下载 SupertonicTTS 模型
download_model() {
    info "======== 下载 SupertonicTTS 3 模型 ========"
    local model_dir="${LIB_DIR}/${MODEL_NAME}"
    
    if [ -d "$model_dir" ] && [ -f "$model_dir/tts.json" ]; then
        read -p "模型已存在，是否重新下载? (y/N): " -n 1 -r; echo
        [[ ! $REPLY =~ ^[Yy]$ ]] && return 0
        rm -rf "$model_dir"
    fi
    
    mkdir -p "$LIB_DIR"
    local tmp="/tmp/${MODEL_NAME}.tar.bz2"
    
    [ -f "$tmp" ] || try_download "${ACCEL}/k2-fsa/sherpa-onnx/releases/download/tts-models/${MODEL_NAME}.tar.bz2" \
                                  "${OFFICIAL}/k2-fsa/sherpa-onnx/releases/download/tts-models/${MODEL_NAME}.tar.bz2" \
                                  "$tmp" "SupertonicTTS 模型"
    
    info "解压中..."
    tar xjf "$tmp" -C "$LIB_DIR"
    rm -f "$tmp"
    
    info "模型下载完成: $model_dir"
    ls -lh "$model_dir/"
}

# 3. 显示使用说明
show_usage() {
    echo ""
    echo "======== 下载完成 ========"
    echo ""
    echo "编译:  mvn clean compile -pl backend"
    echo "测试:  mvn exec:java -pl backend -Dexec.mainClass=com.ctl.chatbot.util.SupertonicTtsUtil"
    echo ""
    echo "API 示例:"
    echo "  SupertonicTtsUtil.speak(\"Hello!\", \"en\");"
    echo "  SupertonicTtsUtil.speak(\"你好！\", \"zh\");"
    echo "  SupertonicTtsUtil.synthesize(\"Hello\", \"en\", \"output.wav\");"
}

# 显示帮助
show_help() {
    echo "用法: $0 [选项]"
    echo ""
    echo "  -j    仅更新 JAR"
    echo "  -m    仅下载模型"
    echo "  -h    显示帮助"
}

# 主函数
main() {
    echo "======== SupertonicTTS 3 下载脚本 ========"
    
    UPDATE_JAR=1
    DOWNLOAD_MODEL=1
    
    while [[ $# -gt 0 ]]; do
        case $1 in
            -j) DOWNLOAD_MODEL=0; shift ;;
            -m) UPDATE_JAR=0; shift ;;
            -h) show_help; exit 0 ;;
            *) error "未知参数: $1" ;;
        esac
    done
    
    [ "$UPDATE_JAR" -eq 1 ] && update_jar
    [ "$DOWNLOAD_MODEL" -eq 1 ] && download_model
    show_usage
}

main "$@"
