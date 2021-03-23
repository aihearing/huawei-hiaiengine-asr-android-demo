
package com.huawei.asrdemo.util;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by cwx435411 on 2017/8/9.
 */
public class Utils {
    private static final String TAG = "Utils";

    public static List<String> readLineFormFile(String filePath) {
        List<String> name = new ArrayList<>();
        File resultFile = new File(filePath);
        FileInputStream fis = null;
        BufferedReader br = null;
        if (!resultFile.exists()) {
            return null;
        }
        try {
            fis = new FileInputStream(resultFile);
            br = new BufferedReader(new InputStreamReader(fis));
            String line;
            while ((line = br.readLine()) != null) {
                name.add(line);
            }
        } catch (Exception e) {
            Log.e(TAG, "readLineFormFile : " + e.getMessage());
        } finally {
            if (null != fis) {
                try {
                    fis.close();
                    br.close();
                } catch (IOException e) {
                    Log.e(TAG, "writeNewName IOException : " + e.getMessage());
                }
            }
        }
        return name;
    }

    /**
     * 判断文件是否存在
     *
     * @param fstPath 文件路径
     * @return 是否存在
     */
    public static boolean isExists(String fstPath) {
        File file = new File(fstPath);
        return file.exists();
    }
}
