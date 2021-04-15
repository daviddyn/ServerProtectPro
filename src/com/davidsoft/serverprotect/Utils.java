package com.davidsoft.serverprotect;

import com.davidsoft.utils.JsonNode;
import com.davidsoft.utils.StringCodec;

import java.io.*;
import java.text.ParseException;
import java.util.Properties;

public final class Utils {

    public static byte[] getFileBytes(File source) {
        FileInputStream in;
        byte[] ret;
        try {
            in = new FileInputStream(source);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        if (source.length() == 0) {
            closeWithoutException(in);
            return new byte[0];
        }
        else if (source.length() > Integer.MAX_VALUE) {
            closeWithoutException(in);
            return null;
        }
        else {
            ret = new byte[(int) source.length()];
            try {
                if (in.read(ret) != source.length()) {
                    ret = null;
                }
            } catch (IOException e) {
                e.printStackTrace();
                ret = null;
            }
        }
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static String getFileString(File source, StringCodec.Format format) {
        return StringCodec.decodeString(getFileBytes(source), format);
    }

    public static JsonNode loadJson(File source, StringCodec.Format format) {
        String jsonSource = getFileString(source, format);
        if (jsonSource == null) {
            return null;
        }
        JsonNode jsonNode = null;
        try {
            jsonNode = JsonNode.parseJson(jsonSource);
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
        return jsonNode;
    }

    public static Properties loadProperties(File source) {
        FileInputStream in;
        try {
            in = new FileInputStream(source);
        } catch (FileNotFoundException e) {
            return null;
        }
        Properties properties = new Properties();
        try {
            properties.load(in);
        } catch (IOException e) {
            properties = null;
        }
        try {
            in.close();
        } catch (IOException ignored) {}
        return properties;
    }

    public static boolean saveFileBytes(File dest, byte[] content) {
        FileOutputStream out;
        try {
            out = new FileOutputStream(dest);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        boolean ret = false;
        try {
            out.write(content);
            ret = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static boolean saveFileString(File dest, String content, StringCodec.Format format) {
        return saveFileBytes(dest, StringCodec.encodeString(content, format == null ? StringCodec.Format.getSystemDefault() : format));
    }

    public static boolean saveJson(File dest, JsonNode content, boolean plain, StringCodec.Format format) {
        return saveFileString(dest, content.toString(plain ? null : "  "), format);
    }

    public static boolean closeWithoutException(Closeable closeable) {
        return closeWithoutException(closeable, false);
    }

    public static boolean closeWithoutException(Closeable closeable, boolean silent) {
        try {
            closeable.close();
        } catch (IOException e) {
            if (!silent) {
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }

    public static boolean isStringEmpty(String src) {
        return src == null || src.isEmpty();
    }

    public static String toNonNullString(String src) {
        return src == null ? "" : src;
    }

    /*

    public static int encodeIp(String ip) {
        int number1, number2, number3, number4;
        int findEnd = ip.indexOf('.');
        number1 = Integer.parseInt(ip.substring(0, findEnd));
        int findPos = findEnd + 1;
        findEnd = ip.indexOf('.', findPos);
        number2 = Integer.parseInt(ip.substring(findPos, findEnd));
        findPos = findEnd + 1;
        findEnd = ip.indexOf('.', findPos);
        number3 = Integer.parseInt(ip.substring(findPos, findEnd));
        findPos = findEnd + 1;
        number4 = Integer.parseInt(ip.substring(findPos));
        return (number1 << 24) | (number2 << 16) | (number3 << 8) | number4;
    }

    public static int encodeIpWithRegex(String ip) {
        int number1, number2, number3, number4;
        int findEnd = ip.indexOf('.');
        String pattern = ip.substring(0, findEnd);
        number1 = "*".equals(pattern) ? (255) : Integer.parseInt(pattern);
        int findPos = findEnd + 1;
        findEnd = ip.indexOf('.', findPos);
        pattern = ip.substring(findPos, findEnd);
        number2 = "*".equals(pattern) ? (255) : Integer.parseInt(pattern);
        findPos = findEnd + 1;
        findEnd = ip.indexOf('.', findPos);
        pattern = ip.substring(findPos, findEnd);
        number3 = "*".equals(pattern) ? (255) : Integer.parseInt(pattern);
        findPos = findEnd + 1;
        pattern = ip.substring(findPos);
        number4 = "*".equals(pattern) ? (255) : Integer.parseInt(pattern);
        return (number1 << 24) | (number2 << 16) | (number3 << 8) | number4;
    }

    public static String decodeIp(int ip) {
        return String.format(
                "%d.%d.%d.%d",
                (ip >> 24) & 0xFF,
                (ip >> 16) & 0xFF,
                (ip >> 8) & 0xFF,
                ip & 0xFF
        );
    }

    public static String decodeIpWithRegex(int ip) {
        int sub = (ip >> 24) & 0xFF;
        String pattern1 = (sub == 255 ? "*" : String.valueOf(sub));
        sub = (ip >> 16) & 0xFF;
        String pattern2 = (sub == 255 ? "*" : String.valueOf(sub));
        sub = (ip >> 8) & 0xFF;
        String pattern3 = (sub == 255 ? "*" : String.valueOf(sub));
        sub = ip & 0xFF;
        String pattern4 = (sub == 255 ? "*" : String.valueOf(sub));
        return pattern1 + "." + pattern2 + "." + pattern3 + "." + pattern4;
    }

    public static boolean checkIp(String ip) {
        int start = 0;
        int patternCount = 0;
        for (int i = 0; i < ip.length() && patternCount < 4; i++) {
            if (ip.charAt(i) == '.') {
                try {
                    int a = Integer.parseInt(ip.substring(start, i));
                    if (a < 0 || a > 255) {
                        return false;
                    }
                }
                catch (NumberFormatException e) {
                    return false;
                }
                start = i + 1;
                patternCount++;
            }
        }
        if (patternCount == 4) {
            return false;
        }
        try {
            int a = Integer.parseInt(ip.substring(start));
            return a >= 0 && a <= 255;
        }
        catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean checkIpWithRegex(String ip) {
        int start = 0;
        int patternCount = 0;
        for (int i = 0; i < ip.length() && patternCount < 4; i++) {
            if (ip.charAt(i) == '.') {
                String pattern = ip.substring(start, i);
                if (!pattern.equals("*")) {
                    try {
                        int a = Integer.parseInt(pattern);
                        if (a < 0 || a > 255) {
                            return false;
                        }
                    }
                    catch (NumberFormatException e) {
                        return false;
                    }
                }
                start = i + 1;
                patternCount++;
            }
        }
        if (patternCount == 4) {
            return false;
        }
        String pattern = ip.substring(start);
        if (!pattern.equals("*")) {
            try {
                int a = Integer.parseInt(ip.substring(start));
                return a >= 0 && a <= 255;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

     */
}