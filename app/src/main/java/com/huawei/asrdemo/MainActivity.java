/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2019-2020. All rights reserved.
 */

package com.huawei.asrdemo;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.huawei.asrdemo.util.StoragePermission;
import com.huawei.asrdemo.util.Utils;
import com.huawei.hiai.asr.AsrConstants;
import com.huawei.hiai.asr.AsrError;
import com.huawei.hiai.asr.AsrListener;
import com.huawei.hiai.asr.AsrRecognizer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "MainActivity";

    private static final int END_AUTO_TEST = 0;

    private static final int DELAYED_START_RECORD = 3;

    private static final int DELAYED_WRITE_PCM = 4;

    private AsrRecognizer mAsrRecognizer;

    private TextView showResult;

    private Button recognizeFileBtn;

    private Button stopListeningBtn;

    private Button writePcmBtn;

    private Button updateLexiconBtn;

    private Button cancelListeningBtn;

    private Button startRecordBtn;

    private boolean isRecognizeFile = false;

    private boolean isAutoTestEnd = false;

    private boolean isWritePcm = false;

    private int count = 0;

    private MyAsrListener mMyAsrListener = new MyAsrListener();

    private List<String> pathList = new ArrayList<>();

    private List<String> writePcmList = new ArrayList<>();

    private int pcmFileLength = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StoragePermission.getAllPermission(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        mAsrRecognizer = AsrRecognizer.createAsrRecognizer(this);
        makeResDir();
        initView();
        if (isSupportAsr()) {
            initEngine(AsrConstants.ASR_SRC_TYPE_RECORD);
        } else {
            Log.e(TAG, "not support asr!");
        }
    }

    private boolean isSupportAsr() {
        PackageManager packageManager = getPackageManager();
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo("com.huawei.hiai", 0);
            Log.d(TAG, "Engine versionName: " + packageInfo.versionName + " ,versionCode: " + packageInfo.versionCode);
            if (packageInfo.versionCode <= 801000300) {
                return false;
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, "not support asr");
            return false;
        }
        return true;
    }

    private void makeResDir() {
        createDirectory(Constant.TEST_HIAI_PATH);
        createDirectory(Constant.TEST_FILES_PATH);
        createDirectory(Constant.TEST_FILE_PATH);
        createDirectory(Constant.TEST_DEMO_PATH);
        createDirectory(Constant.TEST_DEMO_FILES_PATH);
        createDirectory(Constant.TEST_DEMO_FILE_PATH);
    }

    private void createDirectory(String path) {
        File file = new File(path);
        if (!file.exists()) {
            file.mkdir();
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause() ");
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop() ");
        super.onStop();
        reset();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyEngine();
        mAsrRecognizer = null;
    }

    private void initView() {
        Log.d(TAG, "initView() ");

        writePcmBtn = (Button) findViewById(R.id.button_writePcm);
        writePcmBtn.setOnClickListener(this);

        updateLexiconBtn = (Button) findViewById(R.id.button_updateLexicon);
        updateLexiconBtn.setOnClickListener(this);

        stopListeningBtn = (Button) findViewById(R.id.button_stopListening);
        stopListeningBtn.setOnClickListener(this);

        cancelListeningBtn = (Button) findViewById(R.id.button_cacelListening);
        cancelListeningBtn.setOnClickListener(this);

        startRecordBtn = (Button) findViewById(R.id.start_record);
        startRecordBtn.setOnClickListener(this);

        recognizeFileBtn = (Button) findViewById(R.id.recognize_file);
        recognizeFileBtn.setOnClickListener(this);

        showResult = (TextView) findViewById(R.id.start_record_show);
    }

    private void destroyEngine() {
        Log.d(TAG, "destroyEngine() ");
        if (mAsrRecognizer != null) {
            mAsrRecognizer.destroy();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_stopListening:
                stopListening();
                break;
            case R.id.button_writePcm:
                writePcm();
                break;
            case R.id.button_updateLexicon:
                updateLexicon();
                break;
            case R.id.button_cacelListening:
                cancelListening();
                break;
            case R.id.start_record:
                startRecord();
                break;
            case R.id.recognize_file:
                recognizeFile();
                break;
            default:
                break;
        }
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.d(TAG, "handle message: " + msg.what);
            switch (msg.what) {
                case END_AUTO_TEST:
                    initEngine(AsrConstants.ASR_SRC_TYPE_RECORD);
                    startListening(AsrConstants.ASR_SRC_TYPE_RECORD, null);
                    isAutoTestEnd = false;
                    isWritePcm = false;
                    break;
                case DELAYED_START_RECORD:
                    if (isAutoTestEnd || isWritePcm) {
                        if (mAsrRecognizer != null) {
                            mAsrRecognizer.destroy();
                        }
                        mHandler.sendEmptyMessageDelayed(END_AUTO_TEST, 300);
                    } else {
                        startListening(AsrConstants.ASR_SRC_TYPE_RECORD, null);
                    }
                    break;
                case DELAYED_WRITE_PCM:
                    handleWritePcm();
                    break;
                default:
                    break;
            }
        }
    };

    private void recognizeFile() {
        Log.d(TAG, "recognizeFile() ");
        pathList.clear();
        getFilePath(Constant.TEST_DEMO_FILE_PATH);
        Log.d(TAG, "fileTotalCount: " + pathList.toString());
        if (pathList.size() <= 0) {
            Toast.makeText(MainActivity.this, "请放入需要测试语音文件！", Toast.LENGTH_SHORT).show();
            return;
        }
        isRecognizeFile = true;
        if (mAsrRecognizer != null) {
            mAsrRecognizer.destroy();
        }
        initEngine(AsrConstants.ASR_SRC_TYPE_FILE);
    }

    public void setBtEnabled(boolean isEnabled) {
        recognizeFileBtn.setEnabled(isEnabled);
        stopListeningBtn.setEnabled(isEnabled);
        cancelListeningBtn.setEnabled(isEnabled);
        startRecordBtn.setEnabled(isEnabled);
        writePcmBtn.setEnabled(isEnabled);
        updateLexiconBtn.setEnabled(isEnabled);
    }

    private void startRecord() {
        Log.d(TAG, "startRecord() ");
        isRecognizeFile = false;
        startRecordBtn.setEnabled(false);
        showResult.setText("识别中：");
        mHandler.sendEmptyMessage(DELAYED_START_RECORD);
    }

    /**
     * 初始化引擎
     *
     * @param srcType 数据源类型
     */
    private void initEngine(int srcType) {
        Log.d(TAG, "initEngine() srcType" + srcType);
        Intent initIntent = new Intent();
        initIntent.putExtra(AsrConstants.ASR_AUDIO_SRC_TYPE, srcType);
        initIntent.putExtra(AsrConstants.ASR_VAD_END_WAIT_MS, 2000); // 设置前置vad时间
        initIntent.putExtra(AsrConstants.ASR_VAD_FRONT_WAIT_MS, 4000); // 设置后置vad时间
        if (mAsrRecognizer != null) {
            mAsrRecognizer.init(initIntent, mMyAsrListener);
        }
    }

    /**
     * 调用asr的startListening接口
     *
     * @param srcType 数据源类型
     * @param filePath 如果识别的是音频文件，传入音频文件路径
     */
    private void startListening(int srcType, String filePath) {
        Log.d(TAG, "startListening() " + "src_type:" + srcType);

        Intent intent = new Intent();
        intent.putExtra(AsrConstants.ASR_VAD_END_WAIT_MS, 2000);
        intent.putExtra(AsrConstants.ASR_VAD_FRONT_WAIT_MS, 4000);
        intent.putExtra(AsrConstants.ASR_TIMEOUT_THRESHOLD_MS, 20000);
        if (srcType == AsrConstants.ASR_SRC_TYPE_FILE) {
            intent.putExtra(AsrConstants.ASR_SRC_FILE, filePath);
        }

        if (mAsrRecognizer != null) {
            mAsrRecognizer.startListening(intent);
        }
    }

    private void stopListening() {
        Log.d(TAG, "stopListening() ");
        if (mAsrRecognizer != null) {
            mAsrRecognizer.stopListening();
        }
    }

    private void cancelListening() {
        Log.d(TAG, "cancelListening() ");
        startRecordBtn.setEnabled(true);
        if (mAsrRecognizer != null) {
            mAsrRecognizer.cancel();
        }
    }

    private void writePcm() {
        Log.d(TAG, "writePcm() ");
        writePcmList.clear();
        pcmFileLength = getFilePath(Constant.TEST_DEMO_PCM_PATH);
        if (writePcmList.size() == 0) {
            Toast.makeText(this, "请放入PCM文件", Toast.LENGTH_SHORT).show();
            return;
        }
        if (mAsrRecognizer != null) {
            mAsrRecognizer.destroy();
        }
        initEngine(AsrConstants.ASR_SRC_TYPE_PCM);
        isWritePcm = true;
    }

    private void handleWritePcm() {
        Log.d(TAG, "handleWritePcm() ");
        startListening(AsrConstants.ASR_SRC_TYPE_PCM, null);
        ByteArrayOutputStream bos = null;
        BufferedInputStream in = null;
        Random random = new Random();

        if (pcmFileLength <= 0) {
            return;
        }
        try {
            File file = new File(writePcmList.get(random.nextInt(pcmFileLength)));
            if (!file.exists()) {
                throw new FileNotFoundException("file not exists");
            }
            bos = new ByteArrayOutputStream((int) file.length());
            in = new BufferedInputStream(new FileInputStream(file));
            int bufSize = 1280;
            byte[] buffer = new byte[bufSize];
            int len;
            while (-1 != (len = in.read(buffer, 0, bufSize))) {
                bos.reset();
                bos.write(buffer, 0, len);
                mAsrRecognizer.writePcm(bos.toByteArray(), bos.toByteArray().length);
            }
        } catch (Exception e) {
            Log.d(TAG, "handleWritePcm :" + e.getMessage());
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (bos != null) {
                    bos.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "handleWritePcm :" + e.getMessage());
            }
        }
    }

    /**
     * 导热词
     */
    private void updateLexicon() {
        Log.d(TAG, "updateLexicon() ");

        Intent intent = new Intent();
        ArrayList<String> nameList = new ArrayList<>();
        JSONObject data = new JSONObject();
        try {
            if (Utils.isExists(Constant.TEST_CONTACT_PATH + "/contact.txt")) {
                nameList.add(AsrConstants.ASR_LEXICON_NAME_CONTACT);
                data.put(AsrConstants.ASR_LEXICON_NAME_CONTACT,
                    putJsonArray(Utils.readLineFormFile(Constant.TEST_CONTACT_PATH + "/contact.txt")));
            }
            if (Utils.isExists(Constant.TEST_CONTACT_PATH + "/address.txt")) {
                nameList.add(AsrConstants.ASR_LEXICON_NAME_ADDRESS);
                data.put(AsrConstants.ASR_LEXICON_NAME_ADDRESS,
                    putJsonArray(Utils.readLineFormFile(Constant.TEST_CONTACT_PATH + "/address.txt")));
            }
            if (Utils.isExists(Constant.TEST_CONTACT_PATH + "/song.txt")) {
                nameList.add(AsrConstants.ASR_LEXICON_NAME_SONG);
                data.put(AsrConstants.ASR_LEXICON_NAME_SONG,
                    putJsonArray(Utils.readLineFormFile(Constant.TEST_CONTACT_PATH + "/song.txt")));
            }
            if (Utils.isExists(Constant.TEST_CONTACT_PATH + "/others.txt")) {
                nameList.add(AsrConstants.ASR_LEXICON_NAME_OTHERS);
                data.put(AsrConstants.ASR_LEXICON_NAME_OTHERS,
                    putJsonArray(Utils.readLineFormFile(Constant.TEST_CONTACT_PATH + "/others.txt")));
            }
            if (Utils.isExists(Constant.TEST_CONTACT_PATH + "/app.txt")) {
                nameList.add(AsrConstants.ASR_LEXICON_NAME_APP);
                data.put(AsrConstants.ASR_LEXICON_NAME_APP,
                    putJsonArray(Utils.readLineFormFile(Constant.TEST_CONTACT_PATH + "/app.txt")));
            }

            intent.putExtra(AsrConstants.ASR_LEXICON_NAME, nameList);
            intent.putExtra(AsrConstants.ASR_LEXICON_ITEMS, data.toString());
            if (mAsrRecognizer != null) {
                mAsrRecognizer.updateLexicon(intent);
            }
        } catch (JSONException e) {
            Log.e(TAG, "updateLexicon exception");
        }
    }

    public JSONArray putJsonArray(List<String> data) {
        JSONArray jsonArray = new JSONArray();
        if (data.size() > 0) {
            int size = data.size();
            for (int i = 0; i < size; i++) {
                jsonArray.put(data.get(i));
            }
        }
        return jsonArray;
    }

    private class MyAsrListener implements AsrListener {
        @Override
        public void onInit(Bundle params) {
            Log.d(TAG, "onInit()");
            int result = params.getInt(AsrConstants.ASR_ERROR_CODE, -1);
            if (result != 0) {
                // 初始化失败
                return;
            }
            if (isRecognizeFile) {
                setBtEnabled(false);
                Log.d(TAG, "handleMessage: " + count + " path :" + pathList.get(count));
                startListening(AsrConstants.ASR_SRC_TYPE_FILE, pathList.get(count));
            } else if (isWritePcm) {
                setBtEnabled(false);
                mHandler.sendEmptyMessageDelayed(DELAYED_WRITE_PCM, 1000);
            }
        }

        @Override
        public void onBeginningOfSpeech() {
            Log.d(TAG, "onBeginningOfSpeech() called");
        }

        @Override
        public void onRmsChanged(float rmsdB) {
            Log.d(TAG, "onRmsChanged() called with: rmsdB = [" + rmsdB + "]");
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
            Log.d(TAG, "onBufferReceived() called with: buffer = [" + buffer + "]");
        }

        @Override
        public void onEndOfSpeech() {
            Log.d(TAG, "onEndOfSpeech: ");
        }

        @Override
        public void onError(int error) {
            Log.d(TAG, "onError() called with: error = [" + error + "]");
            if (error == AsrError.SUCCESS) {
                return;
            }
            if (error == AsrError.ERROR_CLIENT_INSUFFICIENT_PERMISSIONS) {
                Toast.makeText(getApplicationContext(), "请在设置中打开麦克风权限!", Toast.LENGTH_LONG).show();
            }

            setBtEnabled(true);
        }

        @Override
        public void onRecordStart() {
            Log.d(TAG, "onRecordStart");
        }

        @Override
        public void onRecordEnd() {
            Log.d(TAG, "onRecordEnd");
        }

        @Override
        public void onResults(Bundle results) {
            Log.d(TAG, "onResults() called with: results = [" + results + "]");
            stopListening();
            getOnResult(results, AsrConstants.RESULTS_RECOGNITION);
            setBtEnabled(true);
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
            Log.d(TAG, "onPartialResults() called with: partialResults = [" + partialResults + "]");
            getOnResult(partialResults, AsrConstants.RESULTS_PARTIAL);
        }

        @Override
        public void onEnd() {
            Log.d(TAG, "onEnd()");
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
            Log.d(TAG, "onEvent() called with: eventType = [" + eventType + "], params = [" + params + "]");
        }

        @Override
        public void onLexiconUpdated(String s, int i) {
            Log.d(TAG, "onLexiconUpdated() called with: lexiconName = [" + s + "], error = [" + i + "]");
            if (i == 0) {
                Toast.makeText(MainActivity.this, s + "导词成功", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(MainActivity.this, s + "导词失败", Toast.LENGTH_LONG).show();
            }
        }
    }

    private String getOnResult(Bundle partialResults, String key) {
        String json = partialResults.getString(key);
        final StringBuilder sb = new StringBuilder();
        try {
            JSONObject result = new JSONObject(json);
            JSONArray items = result.getJSONArray("result");
            for (int i = 0; i < items.length(); i++) {
                String word = items.getJSONObject(i).getString("word");
                double confidences = items.getJSONObject(i).getDouble("confidence");
                sb.append(word);
            }
            Log.d(TAG, "getOnResult: " + sb.toString());
            showResult.setText(sb.toString());
        } catch (JSONException exp) {
            Log.e(TAG, "JSONException: " + exp.toString());
        }
        return sb.toString();
    }

    /**
     * 通过递归得到当前文件夹里所有的文件数量和路径
     *
     * @param path 文件路径
     * @return count
     */
    public int getFilePath(String path) {
        int sum = 0;
        Log.i(TAG, "getFilePath()" + path);
        try {
            File file = new File(path);
            File[] list = file.listFiles();
            if (list == null) {
                Log.d(TAG, "getFilePath: fileList is null!");
                return 0;
            }
            for (int i = 0; i < list.length; i++) {
                if (list[i].isFile()) {
                    String[] splitPath = list[i].toString().split("\\.");
                    if (splitPath[splitPath.length - 1].equals("pcm")
                        || splitPath[splitPath.length - 1].equals("wav")) {
                        sum++;
                        writePcmList.add(list[i].toString());
                        String filePath = list[i].toString().replaceFirst(Constant.TEST_PACKAGE, Constant.HIAI_PACKAGE);
                        pathList.add(filePath);
                    }
                } else {
                    sum += getFilePath(list[i].getPath());
                }
            }
        } catch (NullPointerException ne) {
            Log.e(TAG, "NullPointerException: " + ne.getMessage());
        }
        return sum;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Log.i(TAG, "onBackPressed() : finish()");
        finish();
    }

    private void reset() {
        cancelListening();
        setBtEnabled(true);
        showResult.setText("");
    }
}
