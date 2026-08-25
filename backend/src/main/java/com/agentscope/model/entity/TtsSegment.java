package com.agentscope.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 语音合成记录的分段（内嵌文档）。
 * 每段为一段可独立合成的文本（≤450字），并携带其在原文中的字符偏移，用于播放高亮定位。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TtsSegment {

    /** 该段文本内容 */
    private String text;

    /** 在原文中的起始字符偏移（含） */
    private Integer charStart;

    /** 在原文中的结束字符偏移（不含） */
    private Integer charEnd;

    /** 合成音频时长（秒） */
    private Double duration;
}
