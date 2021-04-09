package com.davidsoft.http;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 使用标准：HTML4.01+RFC1738
 */
public final class UrlCodec {

    /**
     * RFC1738
     */
    private static boolean needEncode(byte b) {
        if (39 <= b && b < 47) {
            return false;
        }
        if (48 <= b && b < 58) {
            return false;
        }
        if (65 <= b && b < 91) {
            return false;
        }
        if (97 <= b && b < 123) {
            return false;
        }
        switch (b) {
            case 33:
            case 36:
            case 95:
                return false;
        }
        return true;
    }

    public static String urlEncode(byte[] content) {
        StringBuilder builder = new StringBuilder(content.length * 3);
        for (byte b : content) {
            if (needEncode(b)) {
                builder.append('%');
                builder.append(String.format("%02X", b));
            } else {
                builder.append((char) b);
            }
        }
        return builder.toString();
    }

    //容错规则：%后面至少有1个16进制位时就解码、允许小写的16进制位、不满足解码规则的原序输出
    private static byte[] urlDecode(String encoded, Charset charset) {
        byte[] out = new byte[encoded.length() << 2];
        int stateNumber = 0;
        int hexBuild = 0;
        int inP = 0, outP = 0;
        byte[] strBytes;
        while (inP < encoded.length()) {
            char c = encoded.charAt(inP);
            switch (stateNumber) {
                case 0:
                    switch (c) {
                        case '%':
                            stateNumber = 1;
                            hexBuild = 0;
                            break;
                        case '+':
                            strBytes = " ".getBytes(charset);
                            System.arraycopy(strBytes, 0, out, outP, strBytes.length);
                            outP += strBytes.length;
                            break;
                        default:
                            strBytes = String.valueOf(c).getBytes(charset);
                            System.arraycopy(strBytes, 0, out, outP, strBytes.length);
                            outP += strBytes.length;
                            break;
                    }
                    inP++;
                    break;
                case 1:
                    if ('0' <= c && c <= '9') {
                        hexBuild = c - '0';
                        stateNumber = 2;
                    }
                    else if ('A' <= c && c <= 'F') {
                        hexBuild = c - 'A' + 10;
                        stateNumber = 2;
                    }
                    else if ('a' <= c && c <= 'f') {
                        hexBuild = c - 'a' + 10;
                        stateNumber = 2;
                    }
                    else {
                        strBytes = ("%" + c).getBytes(charset);
                        System.arraycopy(strBytes, 0, out, outP, strBytes.length);
                        outP += strBytes.length;
                        stateNumber = 0;
                    }
                    inP++;
                    break;
                case 2:
                    if ('0' <= c && c <= '9') {
                        hexBuild = (hexBuild << 4) | (c - '0');
                        inP++;
                    }
                    else if ('A' <= c && c <= 'F') {
                        hexBuild = (hexBuild << 4) | (c - 'A' + 10);
                        inP++;
                    }
                    else if ('a' <= c && c <= 'f') {
                        hexBuild = (hexBuild << 4) | (c - 'a' + 10);
                        inP++;
                    }
                    out[outP++] = (byte)hexBuild;
                    stateNumber = 0;
                    break;
            }
        }
        strBytes = new byte[outP];
        System.arraycopy(out, 0, strBytes, 0, outP);
        return strBytes;
    }

    public static byte[] urlDecode(String encoded) {
        return urlDecode(encoded, StandardCharsets.UTF_8);
    }

    //charset参数的作用：%后接16进制仅能表示1个字节的信息，charset参数决定以何种字符集解析由%表示的字节数据。此参数不会影响非%的内容。
    public static String urlDecodeString(String encoded, Charset charset) {
        return new String(urlDecode(encoded, charset), charset);
    }
}