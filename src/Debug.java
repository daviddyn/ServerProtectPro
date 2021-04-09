package com.davidsoft.serverprotect;

import com.davidsoft.serverprotect.libs.HttpPath;

import java.io.IOException;
import java.util.Arrays;

public final class Debug {

    public static void main(String[] args) throws IOException {
        HttpPath httpPath = HttpPath.parse("/build/abc/");
        System.out.println(httpPath);
        System.out.println(Arrays.toString(httpPath.getPatterns()));
    }
}
