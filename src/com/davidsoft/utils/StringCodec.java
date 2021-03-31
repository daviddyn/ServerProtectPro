package com.davidsoft.utils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;

public final class StringCodec {

    public static final class StandardLineSeparators {
        public static final String CR = "\r";
        public static final String LF = "\n";
        public static final String CRLF = "\r\n";
        public static final String systemDefault = System.lineSeparator();
    }

    public static final class Format {

        private static final Format systemDefault = new Format(
                Charset.forName(System.getProperty("file.encoding")),
                false,
                StandardLineSeparators.systemDefault
        );

        private static final Format jvmDefault = new Format(
                Charset.defaultCharset(),
                false,
                StandardLineSeparators.systemDefault
        );

        public static Format getSystemDefault() {
            return systemDefault;
        }

        public static Format getJvmDefault() {
            return jvmDefault;
        }

        public final Charset charset;
        public final boolean hasBOM;
        public final String lineSeparator;

        public Format(Charset charset, boolean hasBOM, String lineSeparator) {
            this.charset = charset;
            this.hasBOM = hasBOM;
            this.lineSeparator = lineSeparator;
        }

        @Override
        public String toString() {
            return "Format{" +
                    "charset=" + charset +
                    ", hasBOM=" + hasBOM +
                    ", lineSeparator='" + lineSeparator + '\'' +
                    '}';
        }
    }

    private static final Charset[] commonCharsets = new Charset[] {
            Charset.forName(System.getProperty("file.encoding")),
            Charset.defaultCharset(),
            StandardCharsets.UTF_8,
            Charset.forName("GB18030"),
            StandardCharsets.UTF_16LE,
            StandardCharsets.ISO_8859_1,
            StandardCharsets.UTF_16BE,
            Charset.forName("UTF-32LE"),
            Charset.forName("UTF-32BE")
    };

    private static final byte[][] commonCharsetBOMs = new byte[][] {
            null,
            null,
            new byte[] {-17, -69, -65},
            null,
            new byte[] {-1, -2},
            null,
            new byte[] {-2, -1},
            new byte[] {-1, -2, 0, 0},
            new byte[] {0, 0, -2, -1}
    };

    private static byte[] getBomBytes(Charset charset) {
        for (int i = 0; i < commonCharsets.length; i++) {
            if (commonCharsetBOMs[i] != null && commonCharsets[i].equals(charset)) {
                return commonCharsetBOMs[i];
            }
        }
        return null;
    }

    private static boolean isCharacterReadable(char c) {
        if (0x0020 <= c && c < 0x007F) {
            return true;
        }
        if (0x3000 <= c && c < 0x3018) {
            return true;
        }
        if (0x3041 <= c && c < 0x30FF) {
            return true;
        }
        if (0x4E00 <= c && c < 0x9FBB) {
            return true;
        }
        return 0xFF01 <= c && c < 0xFF5F;
    }

    private static float readableRate(String src) {
        if (src.length() == 0) {
            return 1f;
        }
        int sum = 0;
        for (int i = 0; i < src.length(); i++) {
            if (isCharacterReadable(src.charAt(i))) {
                sum++;
            }
        }
        return (float)sum / src.length();
    }

    //out[0] - detected charset: Charset
    //out[1] - produced string (if any): String
    //out[2] - bom length: Integer
    private static void detectCharsetInner(byte[] data, int offset, int len, Object[] out) {
        //检查BOM
        for (int i = 0; i < commonCharsets.length; i++) {
            if (commonCharsetBOMs[i] != null && byteArrayStartsWith(data, 0, commonCharsetBOMs[i])) {
                out[0] = commonCharsets[i];
                out[1] = null;
                out[2] = commonCharsetBOMs[i].length;
                return;
            }
        }
        //字符匹配
        String maxRateEstimate = null;
        Charset maxRateCharset = null;
        float maxRate = 0;
        for (Charset charset : commonCharsets) {
            String estimate = new String(data, offset, len, charset);
            float rate = readableRate(estimate);
            if (rate > maxRate) {
                maxRate = rate;
                maxRateCharset = charset;
                maxRateEstimate = estimate;
                if (rate == 1) {
                    break;
                }
            }
        }
        out[0] = maxRateCharset;
        out[1] = maxRateEstimate;
    }

    public static Charset detectCharset(byte[] data, int offset, int len) {
        if (data == null) {
            return null;
        }
        Object[] out = new Object[3];
        detectCharsetInner(data, offset, len, out);
        return (Charset) out[0];
    }

    public static Charset detectCharset(byte[] data) {
        if (data == null) {
            return null;
        }
        return detectCharset(data, 0, data.length);
    }

    public static String detectLineSeparator(String src) {
        if (src == null) {
            return null;
        }
        for (int i = 0; i < src.length(); i++) {
            switch (src.charAt(i)) {
                case '\n':
                    return StandardLineSeparators.LF;
                case '\r':
                    if (i < src.length() - 1 && src.charAt(i + 1) == '\n') {
                        return StandardLineSeparators.CRLF;
                    }
                    else {
                        return StandardLineSeparators.CR;
                    }
            }
        }
        return null;
    }

    //out[0] - detected charset: Charset
    //out[1] - detected line separator (if any): String
    //out[2] - produced string: String
    //out[3] - has boom: Boolean
    private static void detectFormatInner(byte[] data, int offset, int len, Object[] out) {
        Object[] innerOut = new Object[3];
        detectCharsetInner(data, offset, len, innerOut);
        out[0] = innerOut[0];
        if (out[0] == null) {
            return;
        }
        out[2] = innerOut[1];
        if (out[2] == null) {
            int bomOffset = (int) innerOut[2];
            out[3] = (bomOffset > 0);
            out[2] = new String(data, offset + bomOffset, len - bomOffset, (Charset) out[0]);
        }
        else {
            out[3] = false;
        }
        out[1] = detectLineSeparator((String) out[2]);
    }

    public static Format detectFormat(byte[] data, int offset, int len) {
        if (data == null) {
            return null;
        }
        Object[] out = new Object[4];
        detectFormatInner(data, offset, len, out);
        if (out[0] == null) {
            return null;
        }
        return new Format((Charset)out[0], (boolean)out[3], (String) out[1]);
    }

    public static Format detectFormat(byte[] data) {
        if (data == null) {
            return null;
        }
        return detectFormat(data, 0, data.length);
    }

    public static String decodeString(byte[] data, int offset, int len, Format format) {
        if (data == null) {
            return null;
        }
        String lineSeparator;
        String decoded;
        if (format == null) {
            Object[] out = new Object[4];
            detectFormatInner(data, offset, len, out);
            if (out[0] == null) {
                throw new UnsupportedCharsetException("无法从字节数据中判断字符集");
            }
            lineSeparator = (String)out[1];
            decoded = (String) out[2];
        }
        else {
            int bomOffset = 0;
            if (format.hasBOM) {
                byte[] bomBytes = getBomBytes(format.charset);
                if (bomBytes != null) {
                    bomOffset = bomBytes.length;
                }
            }
            decoded = new String(data, offset + bomOffset, len - bomOffset, format.charset);
            lineSeparator = format.lineSeparator;
        }
        if (lineSeparator != null && !System.lineSeparator().equals(lineSeparator)) {
            decoded = decoded.replace(lineSeparator, System.lineSeparator());
        }
        return decoded;
    }

    public static String decodeString(byte[] data, int offset, int len) {
        if (data == null) {
            return null;
        }
        return decodeString(data, offset, len, null);
    }

    public static String decodeString(byte[] data, Format format) {
        if (data == null) {
            return null;
        }
        return decodeString(data, 0, data.length, format);
    }

    public static String decodeString(byte[] data) {
        if (data == null) {
            return null;
        }
        return decodeString(data, 0, data.length, null);
    }

    public static byte[] encodeString(String src, Format format) {
        String lineSeparator = detectLineSeparator(src);
        if (lineSeparator != null && !lineSeparator.equals(format.lineSeparator)) {
            src = src.replace(lineSeparator, format.lineSeparator);
        }
        byte[] strBytes = src.getBytes(format.charset);
        if (format.hasBOM) {
            byte[] bomBytes = getBomBytes(format.charset);
            if (bomBytes != null) {
                byte[] bomStrByte = new byte[bomBytes.length + strBytes.length];
                System.arraycopy(bomBytes, 0, bomStrByte, 0, bomBytes.length);
                System.arraycopy(strBytes, 0, bomStrByte, bomBytes.length, strBytes.length);
                return bomStrByte;
            }
        }
        return strBytes;
    }

    private static boolean byteArrayStartsWith(byte[] src, int offset, byte[] test) {
        if (src.length - offset < test.length) {
            return false;
        }
        return byteArrayEquals(src, offset, offset + test.length, test, 0, test.length);
    }

    private static boolean byteArrayEquals(byte[] a, int aFromIndex, int aToIndex, byte[] b, int bFromIndex, int bToIndex) {
        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        if (aLength != bLength) {
            return false;
        }
        for (int i = 0; i < aLength; i++) {
            if (a[aFromIndex++] != b[bFromIndex++]) {
                return false;
            }
        }
        return true;
    }
}
