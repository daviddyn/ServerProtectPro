package com.davidsoft.net.http;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public final class Utils {

    public static String readHttpLine(InputStream in, StringBuilder bufferReuse) throws IOException {
        //http协议头部内容可以保证一个字节代表一个字符
        bufferReuse.delete(0, bufferReuse.length());
        int b;
        boolean crRead = false;
        while ((b = in.read()) != -1) {
            if (crRead) {
                if (b == '\n') {
                    return bufferReuse.toString();
                }
                else {
                    bufferReuse.append('\n');
                    crRead = false;
                }
            }
            else {
                if (b == '\r') {
                    crRead = true;
                }
                else {
                    bufferReuse.append((char)b);
                }
            }
        }
        throw new EOFException();
    }

    public static String escapeHtml(String src) {
        return src
                .replace("&", "&amp;")
                .replace(" ", "&nbsp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace(System.lineSeparator(), "<br>");
    }

    private static final class QualityValue implements Comparable<QualityValue> {
        private final float quality;
        private final String value;

        private QualityValue(float quality, String value) {
            this.quality = quality;
            this.value = value;
        }

        @Override
        public int compareTo(QualityValue o) {
            return Float.compare(o.quality, quality);
        }
    }

    public static String analyseQualityValues(String qualityValues, Collection<String> supports) {
        if (qualityValues == null) {
            return null;
        }
        ArrayList<QualityValue> qualityList = new ArrayList<>();
        for (String pattern : qualityValues.split(",")) {
            pattern = pattern.trim().toLowerCase();
            int findPos = pattern.indexOf(";");
            if (findPos == -1) {
                qualityList.add(new QualityValue(1, pattern));
            }
            else {
                String value = pattern.substring(0, findPos).trim();
                String rest = pattern.substring(findPos + 1).trim();
                if (rest.startsWith("q=")) {
                    try {
                        qualityList.add(new QualityValue(Float.parseFloat(rest.substring(2)), value));
                    }
                    catch (NumberFormatException e) {
                        qualityList.add(new QualityValue(1, value));
                    }
                }
                else {
                    qualityList.add(new QualityValue(1, value));
                }
            }
        }
        //Collections.sort是稳定的
        Collections.sort(qualityList);
        for (QualityValue value : qualityList) {
            if (supports.contains(value.value)) {
                return value.value;
            }
        }
        return null;
    }

    public static int getDefaultPort(boolean ssl) {
        return ssl ? 443 : 80;
    }

    /*
    public static String getHostFromDomain(String domain) {
        int findPos = domain.indexOf(':');
        if (findPos == -1) {
            return domain;
        }
        else {
            return domain.substring(0, findPos);
        }
    }
    
    public static int getPortFromDomain(String domain, boolean ssl) {
        int findPos = domain.indexOf(':');
        if (findPos == -1) {
            return ssl ? 443: 80;
        }
        else {
            try {
                return Integer.parseInt(domain.substring(findPos + 1));
            }
            catch (NumberFormatException e) {
                return ssl ? 443: 80;
            }
        }
    }
    
    public static String buildDomain(String host, int port, boolean ssl) {
        if (ssl) {
            if (port == 443) {
                return host;
            }
            else {
                return host + ":" + port;
            }
        }
        else {
            if (port == 80) {
                return host;
            }
            else {
                return host + ":" + port;
            }
        }
    }
    
    public static String buildOrigin(String host, int port, boolean ssl) {
        if (ssl) {
            if (port == 443) {
                return "https://" + host;
            }
            else {
                return "https://" + host + ":" + port;
            }
        }
        else {
            if (port == 80) {
                return "http://"+ host;
            }
            else {
                return "http://" + host + ":" + port;
            }
        }
    }

     */
}
