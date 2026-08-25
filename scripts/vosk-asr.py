import os
import json
import queue
import sounddevice as sd
from vosk import Model, KaldiRecognizer

# 1. 配置模型路径（确保模型文件夹在当前目录下）
MODEL_PATH = "/home/software/AI/vosk-model-small-cn-0.22"
SAMPLE_RATE = 16000  # 16kHz 采样率，Vosk 的最佳识别频率

def check_model():
    """检查模型是否存在，防止新手找不到模型报错"""
    if not os.path.exists(MODEL_PATH):
        print(f"❌ 错误：未找到模型文件夹 '{MODEL_PATH}'")
        print("💡 提示：请确保已下载并解压模型，且与代码在同一目录下。")
        return False
    return True

def start_realtime_asr():
    """启动实时语音识别"""
    if not check_model():
        return

    print("⏳ 正在加载语音模型，请稍候...")
    model = Model(MODEL_PATH)
    recognizer = KaldiRecognizer(model, SAMPLE_RATE)

    # 创建一个队列，用于在麦克风回调和主线程之间安全传递音频数据
    audio_queue = queue.Queue()

    def callback(indata, frames, time, status):
        """麦克风数据回调函数，将捕获的音频放入队列"""
        if status:
            print(f"⚠️ 麦克风警告: {status}")
        audio_queue.put(bytes(indata))

    print("\n🎙️ 麦克风已就绪！请开始说话（按 Ctrl+C 停止）")
    print("-" * 40)

    try:
        # 打开麦克风输入流
        with sd.RawInputStream(samplerate=SAMPLE_RATE, blocksize=8000, dtype='int16',
                               channels=1, callback=callback):
            while True:
                # 从队列获取音频数据
                data = audio_queue.get()

                # 喂给识别器
                if recognizer.AcceptWaveform(data):
                    # 当识别出一句完整的话时，输出结果
                    result = json.loads(recognizer.Result())
                    if result.get("text"):
                        print(f"🗣️ 识别结果: {result['text']}")

    except KeyboardInterrupt:
        # 优雅地处理 Ctrl+C 退出
        print("\n🛑 识别已停止。")
        # 输出最后可能未识别完的半句话
        final_result = json.loads(recognizer.FinalResult())
        if final_result.get("text"):
            print(f"🗣️ 最后的结果: {final_result['text']}")
    except Exception as e:
        print(f"❌ 发生错误: {e}")

if __name__ == "__main__":
    start_realtime_asr()