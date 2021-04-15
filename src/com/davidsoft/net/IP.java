package com.davidsoft.net;

import java.text.ParseException;

public final class IP {

    public static int setIpByte(int ip, int position, int value) {
        if (position < 0 || position >= 4) {
            throw new IndexOutOfBoundsException();
        }
        int bit = (3 - position) << 3;
        return ip & ~(0xFF << bit) | ((value & 0xFF) << bit);
    }

    public static int getIpByte(int ip, int position) {
        if (position < 0 || position >= 4) {
            throw new IndexOutOfBoundsException();
        }
        return (ip >> ((3 - position) << 3)) & 0xFF;
    }

    public static int buildIp(int byte1, int byte2, int byte3, int byte4) {
        return ((byte1 & 0xFF) << 24) | ((byte2 & 0xFF) << 16) | ((byte3 & 0xFF) << 8) | (byte4 & 0xFF);
    }

    public static String toString(int ip) {
        return getIpByte(ip, 0) + "." + getIpByte(ip, 1) + "." + getIpByte(ip, 2) + "." + getIpByte(ip, 3);
    }

    public static int parse(String source) throws ParseException {
        int start = 0, i;
        int patternCount = 0;
        int build = 0;
        int b;
        for (i = 0; i < source.length() && patternCount < 4; i++) {
            if (source.charAt(i) == '.') {
                try {
                    b = Integer.parseInt(source.substring(start, i));
                    if (b < 0 || b > 255) {
                        throw new ParseException("无效IP数值" + b, i);
                    }
                }
                catch (NumberFormatException e) {
                    throw new ParseException("无效IP", i);
                }
                start = i + 1;
                patternCount++;
                build = (build << 8) | b;
            }
        }
        if (patternCount == 4) {
            throw new ParseException("无效IP", i);
        }
        try {
            b = Integer.parseInt(source.substring(start, i));
            if (b < 0 || b > 255) {
                throw new ParseException("无效IP数值" + b, i);
            }
        }
        catch (NumberFormatException e) {
            throw new ParseException("无效IP", i);
        }
        return (build << 8) | b;
    }

    public static int fromDomain(Domain domain) {
        if (domain.patternCount() != 4) {
            throw new IllegalArgumentException("此domain不可转为ip");
        }
        int byte1 = Integer.parseInt(domain.patternAt(3));
        if (byte1 < 0 || byte1 > 255) {
            throw new IllegalArgumentException("此domain不可转为ip");
        }
        int byte2 = Integer.parseInt(domain.patternAt(2));
        if (byte2 < 0 || byte2 > 255) {
            throw new IllegalArgumentException("此domain不可转为ip");
        }
        int byte3 = Integer.parseInt(domain.patternAt(1));
        if (byte3 < 0 || byte3 > 255) {
            throw new IllegalArgumentException("此domain不可转为ip");
        }
        int byte4 = Integer.parseInt(domain.patternAt(0));
        if (byte4 < 0 || byte4 > 255) {
            throw new IllegalArgumentException("此domain不可转为ip");
        }
        return buildIp(byte1, byte2, byte3, byte4);
    }

    public static Domain toDomain(int ip) {
        return Domain.absoluteValueOf(
                String.valueOf(getIpByte(ip, 0)),
                String.valueOf(getIpByte(ip, 1)),
                String.valueOf(getIpByte(ip, 2)),
                String.valueOf(getIpByte(ip, 3))
        );
    }
}
