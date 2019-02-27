package com.anthonyzero.distributed.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * 文件工具类
 */
public class FileUtil {

    /**
     * 返回lua脚本
     * @param path 路径
     * @return
     */
    public static String getLuaScript(String path) {
        StringBuilder sb = new StringBuilder();

        InputStream stream = FileUtil.class.getClassLoader().getResourceAsStream(path);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream))){

            String str;
            while ((str = br.readLine()) != null) {
                sb.append(str).append(System.lineSeparator());
            }

        } catch (IOException e) {
            System.err.println(e.getStackTrace());
        }
        return sb.toString();
    }
}
