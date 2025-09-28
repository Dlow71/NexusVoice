package com.nexusvoice.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * 文本切分工具：按照句子边界优先，并控制最大分段长度。
 */
public final class TextChunker {

    private TextChunker() {}

    /**
     * 按句子边界将文本切分为不超过 maxChars 的段。
     * 优先以句末标点（。！？!?）/换行切分；必要时退化为按逗号/顿号切分；仍超长则硬切。
     */
    public static List<String> splitBySentence(String text, int maxChars) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return result;
        }
        if (text.length() <= maxChars) {
            result.add(text.trim());
            return result;
        }

        String remaining = text.trim();
        while (!remaining.isEmpty()) {
            if (remaining.length() <= maxChars) {
                result.add(remaining);
                break;
            }

            int cut = bestCutIndex(remaining, maxChars);
            String head = remaining.substring(0, cut).trim();
            if (!head.isEmpty()) {
                result.add(head);
            }
            remaining = remaining.substring(cut).trim();
        }
        return result;
    }

    private static int bestCutIndex(String s, int maxChars) {
        int len = s.length();
        int limit = Math.min(maxChars, len);

        // 1) 句末标点与换行
        int idx = lastIndexOfAny(s, new char[]{'。','！','？','!','?','\n','\r'}, limit);
        if (idx > 0) return idx;

        // 2) 逗号、顿号
        idx = lastIndexOfAny(s, new char[]{'，','、',','}, limit);
        if (idx > 0) return idx;

        // 3) 空格
        idx = s.lastIndexOf(' ', limit - 1);
        if (idx > 0) return idx;

        // 4) 硬切
        return limit;
    }

    private static int lastIndexOfAny(String s, char[] chars, int endExclusive) {
        int bound = Math.min(endExclusive, s.length());
        for (int i = bound - 1; i >= 0; i--) {
            char c = s.charAt(i);
            for (char target : chars) {
                if (c == target) {
                    return i + 1; // 切分点放在标点后
                }
            }
        }
        return -1;
    }
}

