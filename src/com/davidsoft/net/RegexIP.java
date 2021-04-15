package com.davidsoft.net;

import java.text.ParseException;

public class RegexIP {

    public static final int BYTE_REGEX = -1;

    public static long setIpByte(long regexIp, int position, int value) {
        if (position < 0 || position >= 4) {
            throw new IndexOutOfBoundsException();
        }
        int bit = (3 - position) << 3;
        if (value == BYTE_REGEX) {
            return (regexIp & 0xF00000000L | (1L << (3 - position + 32))) | (regexIp & 0xFFFFFFFFL) & ~(0xFFL << bit) | ((value & 0xFFL) << bit);
        }
        else {
            return (regexIp & 0xF00000000L & ~(1L << (3 - position + 32))) | (regexIp & 0xFFFFFFFFL) & ~(0xFFL << bit) | ((value & 0xFFL) << bit);
        }
    }

    public static int getIpByte(long regexIp, int position) {
        if (position < 0 || position >= 4) {
            throw new IndexOutOfBoundsException();
        }
        if ((regexIp & (1L << (3 - position + 32))) == 0) {
            return (((int)regexIp) >> ((3 - position) << 3)) & 0xFF;
        }
        else {
            return BYTE_REGEX;
        }
    }

    public static long buildIp(int byte1, int byte2, int byte3, int byte4) {
        return ((byte1 == BYTE_REGEX ? 8L : 0L) << 32) |
                ((byte2 == BYTE_REGEX ? 4L : 0L) << 32) |
                ((byte3 == BYTE_REGEX ? 2L : 0L) << 32) |
                ((byte4 == BYTE_REGEX ? 1L : 0L) << 32) |
                ((byte1 & 0xFFL) << 24) |
                ((byte2 & 0xFFL) << 16) |
                ((byte3 & 0xFFL) << 8) |
                (byte4 & 0xFFL);
    }

    public static String toString(long regexIp) {
        int byte1 = getIpByte(regexIp, 0);
        int byte2 = getIpByte(regexIp, 1);
        int byte3 = getIpByte(regexIp, 2);
        int byte4 = getIpByte(regexIp, 3);
        return (byte1 == BYTE_REGEX ? "*" : byte1) + "." +
                (byte2 == BYTE_REGEX ? "*" : byte2) + "." +
                (byte3 == BYTE_REGEX ? "*" : byte3) + "." +
                (byte4 == BYTE_REGEX ? "*" : byte4);
    }

    public static long parse(String source) throws ParseException {
        int start = 0, i;
        int patternCount = 0;
        long regex = 0;
        long build = 0;
        int b;
        for (i = 0; i < source.length() && patternCount < 4; i++) {
            if (source.charAt(i) == '.') {
                if (i - start == 1 && source.charAt(start) == '*') {
                    regex = (regex << 1) | 0x100000000L;
                    build = (build << 8) | 0xFFL;
                }
                else {
                    regex = (regex << 8);
                    try {
                        b = Integer.parseInt(source.substring(start, i));
                        if (b < 0 || b > 255) {
                            throw new ParseException("无效IP数值" + b, i);
                        }
                    } catch (NumberFormatException e) {
                        throw new ParseException("无效IP", i);
                    }
                    build = (build << 8) | (long)b;
                }
                start = i + 1;
                patternCount++;
            }
        }
        if (patternCount == 4) {
            throw new ParseException("无效IP", i);
        }
        if (i - start == 1 && source.charAt(start) == '*') {
            regex = (regex << 1) | 0x100000000L;
            build = (build << 8) | 0xFFL;
        }
        else {
            regex = (regex << 8);
            try {
                b = Integer.parseInt(source.substring(start, i));
                if (b < 0 || b > 255) {
                    throw new ParseException("无效IP数值" + b, i);
                }
            } catch (NumberFormatException e) {
                throw new ParseException("无效IP", i);
            }
            build = (build << 8) | (long)b;
        }
        return regex | build;
    }

    public static long fromIp(int ip) {
        return ip;
    }

    public static int toIp(long regexIp) {
        return (int) regexIp;
    }
}
