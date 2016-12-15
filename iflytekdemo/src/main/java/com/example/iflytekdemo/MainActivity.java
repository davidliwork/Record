package com.example.iflytekdemo;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;

public class MainActivity extends AppCompatActivity {

    private TextView textView;
    private SpeechRecognizer mIat;
    private AudioRecordFunc audioRecordFunc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mIat = SpeechRecognizer.createRecognizer(this, null);
        audioRecordFunc = AudioRecordFunc.getInstance();
        init();
        initView();
    }

    private void init() {
        mIat.stopListening();
        mIat.setParameter(SpeechConstant.PARAMS, null);
        //短信和日常用语：iat (默认)  视频：video  地图：poi  音乐：music
        mIat.setParameter(SpeechConstant.DOMAIN, "iat");
        // 简体中文:"zh_cn", 美式英文:"en_us"
        mIat.setParameter(SpeechConstant.LANGUAGE, "en_us");
        //普通话：mandarin(默认)
        //粤 语：cantonese
        //四川话：lmz
        //河南话：henanese<span style="font-family: Menlo;">     </span>
        mIat.setParameter(SpeechConstant.ACCENT, "mandarin ");
        // 设置听写引擎 "cloud", "local","mixed"  在线  本地  混合
        //本地的需要本地功能集成
        mIat.setParameter(SpeechConstant.ENGINE_TYPE, "cloud");
        // 设置返回结果格式 听写会话支持json和plain
        mIat.setParameter(SpeechConstant.RESULT_TYPE, "json");
        //设置是否带标点符号 0表示不带标点，1则表示带标点。
        mIat.setParameter(SpeechConstant.ASR_PTT, "0");
        //只有设置这个属性为1时,VAD_BOS  VAD_EOS才会生效,且RecognizerListener.onVolumeChanged才有音量返回默认：1
        mIat.setParameter(SpeechConstant.VAD_ENABLE,"1");
        // 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理1000~10000
        mIat.setParameter(SpeechConstant.VAD_BOS, "5000");
        // 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音0~10000
        mIat.setParameter(SpeechConstant.VAD_EOS, "1800");
        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        // 注：AUDIO_FORMAT参数语记需要更新版本才能生效
        mIat.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        //设置识别会话被中断时（如当前会话未结束就开启了新会话等），
        //是否通 过RecognizerListener.onError(com.iflytek.cloud.SpeechError)回调ErrorCode.ERROR_INTERRUPT错误。
        //默认false    [null,true,false]
        mIat.setParameter(SpeechConstant.ASR_INTERRUPT_ERROR,"false");
        //音频采样率  8000~16000  默认:16000
        mIat.setParameter(SpeechConstant.SAMPLE_RATE,"16000");
        //默认:麦克风(1)(MediaRecorder.AudioSource.MIC)
        //在写音频流方式(-1)下，应用层通过writeAudio函数送入音频；
        //在传文件路径方式（-2）下，SDK通过应用层设置的ASR_SOURCE_PATH值， 直接读取音频文件。目前仅在SpeechRecognizer中支持。

//        mIat.setParameter(SpeechConstant.AUDIO_SOURCE, "-1");
        //设置sdcard的路径
        String fileName = AudioFileFunc.getWavFilePath();
        mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH, fileName);
    }

    private void initView() {
        Button startSystem = (Button) findViewById(R.id.start_system);
        startSystem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSystem();
            }
        });

        Button startCustom = (Button) findViewById(R.id.start_custom);
        startCustom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startCustom();
            }
        });

        Button startRecord = (Button) findViewById(R.id.start_record);
        startRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRecord();
            }
        });

        Button stopRrecord = (Button) findViewById(R.id.stop_crecord);
        stopRrecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecord();
            }
        });

        textView = (TextView) findViewById(R.id.text_view);
    }

    public void startSystem() {
        mIat.stopListening();
        //1.创建SpeechRecognizer对象，第二个参数：本地听写时传InitListener

        InitListener initListener = new InitListener() {
            @Override
            public void onInit(int i) {

            }
        };

        RecognizerDialog iatDialog = new RecognizerDialog(this, initListener);
        //2.设置听写参数
        iatDialog.setParameter(SpeechConstant.DOMAIN, "iat");
        iatDialog.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
        iatDialog.setParameter(SpeechConstant.ACCENT, "mandarin ");
        iatDialog.setParameter(SpeechConstant.ASR_PTT, "0");
        //3.设置回调接口
        iatDialog.setListener(new RecognizerDialogListener() {
            @Override
            public void onResult(RecognizerResult recognizerResult, boolean b) {
                if (!b) {
                    String json = recognizerResult.getResultString();
                    String str = JsonParser.parseIatResult(json);
                    textView.setText(str);
                }
            }

            @Override
            public void onError(SpeechError speechError) {
                Log.d("error", speechError.toString());
            }
        });
        //4.开始听写
        iatDialog.show();
    }

    public void startRecord(){
        //开始录音并且保存录音到sd卡
        if(audioRecordFunc != null) {
            audioRecordFunc.startRecordAndFile();
        }
    }

    public void stopRecord(){
        if(audioRecordFunc != null) {
            audioRecordFunc.stopRecordAndFile();
        }
    }

    public void startCustom() {
        //开始进行语音听写
        mIat.startListening(recognizerListener);
    }

    private RecognizerListener recognizerListener = new RecognizerListener() {
        @Override
        public void onVolumeChanged(int i, byte[] bytes) {

        }

        @Override
        public void onBeginOfSpeech() {
            Toast.makeText(getApplicationContext(),"开始识别",Toast.LENGTH_SHORT).show();
            System.out.println("开始识别");
        }

        @Override
        public void onEndOfSpeech() {
            Toast.makeText(getApplicationContext(),"识别结束",Toast.LENGTH_SHORT).show();
            System.out.println("识别结束");
        }

        @Override
        public void onResult(RecognizerResult recognizerResult, boolean b) {
            String str=JsonParser.parseIatResult(recognizerResult.getResultString());
            textView.setText(str);
            System.out.println("识别结果"+str);
        }

        @Override
        public void onError(SpeechError speechError) {
            Toast.makeText(getApplicationContext(),"识别出错",Toast.LENGTH_SHORT).show();
            System.out.println("识别出错");
        }

        @Override
        public void onEvent(int i, int i1, int i2, Bundle bundle) {

        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        stopRecord();
    }

}
