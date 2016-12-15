package com.example.diygooglesearchdemo;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.Charset;

/**
 * ***************************************
 * File Name : SpeechRecognitionActivity
 * Author : Jeco Fang
 * Email : jeco.fang@163.com
 * Create on : 13-7-19
 * All rights reserved 2013 - 2013
 * ****************************************
 */
public class MainActivity extends BaseActivity {
    private static final String TAG = "MainActivity";
    /* Recording params */
    public static final String AUDIO_SOURCE = "AudioSource";
    private static final int DEFAULT_AUDIO_SOURCE = MediaRecorder.AudioSource.VOICE_RECOGNITION;
    public static final String SAMPLE_RATE = "SampleRate";
    private static final int DEFAULT_SAMPLE_RATE = 16000;
    private static final int DEFAULT_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final short DEFAULT_PER_SAMPLE_IN_BYTES = 2;
    private static final short DEFAULT_PER_SAMPLE_IN_BIT = 16;
    public static final String CHANNELS = "Channels";
    private static final short DEFAULT_CHANNELS = 1; //Number of channels (MONO = 1, STEREO = 2)

    /* Web API params */
    public static final String LANGUAGE = "Language";
    private static final String DEFAULT_LANGUAGE = "zh-CN";
    private static final String GOOGLE_VOICE_API_URL =
            "http://www.google.com/speech-api/v1/recognize?xjerr=1&client=chromium&maxresults=1&lang=";
    private static final String USER_AGENT = "Mozilla/5.0";
    private static final int DEFAULT_CONNECT_TIMEOUT = 10 * 1000; //10 sec;
    private static final int DEFAULT_READ_TIMEOUT = 20 * 1000; //20 sec;
    private static final String CONTENT_TYPE_WAV = "audio/L16;rate=16000";

    /* Message Types */
    private static final int MSG_PREPARE_RECORDER = 1;
    private static final int MSG_START_RECORDING = 2;
    private static final int MSG_RECORD_RECORDING = 3;
    private static final int MSG_STOP_RECORDING = 4;
    private static final int MSG_RECORD_STOPPED = 5;
    private static final int MSG_DECODE_DATA = 6;
    private static final int MSG_ERROR = 7;

    /* Errors */
    public static final int ERR_NONE = 0;
    public static final int ERR_UNKNOWN = -1;
    public static final int ERR_UN_SUPPORT_PARAMS = -2;
    public static final int ERR_ILLEGAL_STATE = -3;
    public static final int ERR_RECORDING = -4;
    public static final int ERR_NETWORK = -5;
    public static final int ERR_NO_SPEECH = -6;
    public static final int ERR_NO_MATCH = -7;
    public static final int ERR_DECODING = -8;

    private int mSampleRate;
    private short mChannels;
    private int mAudioSource;

    private AudioRecord mRecorder;
    private int mBufferSize;
    private int mRecordedLength;
    private byte[] mRecordedData;
    private byte[] wavHeader;

    private enum State {
        IDLE,
        BUSY
    }

    private String mLang;

    private Handler mHandler = new InternalHandler();
    private State mState;

    private ImageView imageView;
    private TextView textView;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = (ImageView) findViewById(R.id.iv_speaking);
        textView = (TextView) findViewById(R.id.tv_result);
        mState = State.IDLE;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mState == State.IDLE) {
            Intent intent = getIntent();
            mAudioSource = intent.getIntExtra(AUDIO_SOURCE, DEFAULT_AUDIO_SOURCE);
            mSampleRate = intent.getIntExtra(SAMPLE_RATE, DEFAULT_SAMPLE_RATE);
            mChannels = intent.getShortExtra(CHANNELS, DEFAULT_CHANNELS);
            mLang = intent.getStringExtra(LANGUAGE);
            if (mLang == null || mLang.trim().length() == 0) {
                mLang = DEFAULT_LANGUAGE;
            }
            if (!isNetworkAvailable()) {
                Message message = mHandler.obtainMessage(MSG_ERROR, ERR_NETWORK);
                mHandler.sendMessage(message);
            } else {
                mHandler.sendEmptyMessageDelayed(MSG_PREPARE_RECORDER, 500);
            }
        }
    }

    private boolean isNetworkAvailable() {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) getApplicationContext()
                    .getSystemService(
                            Context.CONNECTIVITY_SERVICE);
            final NetworkInfo info = connectivityManager.getActiveNetworkInfo();
            return info != null && info.isConnected();
        } catch (SecurityException e) {
            // The IME does not have the permission to check the networking
            // status. We hope for the best.
            return true;
        }
    }

    private class InternalHandler extends Handler {
        private long lastTalkTime;
        private long startTime;
        AnimationDrawable animationDrawable;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_PREPARE_RECORDER:
                    mState = State.BUSY;
                    prepareRecorder();
                    break;
                case MSG_START_RECORDING:
                    startTime = System.currentTimeMillis();
                    lastTalkTime = 0;
                    startRecording();
                    textView.setText(R.string.speech);
                    break;
                case MSG_RECORD_RECORDING:
                    //After 5 seconds started recording, if there is no speech, send stop message.
                    //In recording if no speech time exclude 3 seconds, send stop message
                    long currentTime = System.currentTimeMillis();
                    int volume = msg.arg1;
                    if (lastTalkTime == 0) {
                        if (volume >= 30) {
                            lastTalkTime = currentTime;
                            startAnimationIfNeed(animationDrawable);
                        } else {
                            stopAnimation(animationDrawable);
                            if (currentTime - startTime >= 5 * 1000) {
                                mHandler.sendEmptyMessage(MSG_STOP_RECORDING);
                            }
                        }
                    } else {
                        if (volume >= 30) {
                            lastTalkTime = currentTime;
                            startAnimationIfNeed(animationDrawable);
                        } else {
                            stopAnimation(animationDrawable);
                            if (currentTime - lastTalkTime >= 3 * 1000) {
                                mHandler.sendEmptyMessage(MSG_STOP_RECORDING);
                            }
                        }
                    }
                    break;
                case MSG_STOP_RECORDING:
                    stopAnimation(animationDrawable);
                    stopRecording();
                    break;
                case MSG_RECORD_STOPPED:
                    byte[] wavData = getWavData();
                    startWebRecognizer(wavData);

                    if (mRecorder != null) {
                        mRecorder.release();
                        mRecorder = null;
                    }
                    break;
                case MSG_DECODE_DATA:
                    String data = "";
                    if (msg.obj != null) {
                        data = msg.obj.toString();
                    }
                    if (data.trim().length() > 0) {
                        startParseJson(data.trim());
                    } else {
                        Message message = mHandler.obtainMessage(MSG_ERROR, ERR_UNKNOWN, 0);
                        mHandler.sendMessage(message);
                    }
                    break;
                case MSG_ERROR:
                    mState = State.IDLE;
                    if (mRecorder != null) {
                        mRecorder.release();
                        mRecorder = null;
                    }
                    Intent intent = new Intent();
                    intent.putExtra(SPEECH_RESULT_STATUS, msg.arg1);
                    if (msg.obj != null) {
                        intent.putExtra(SPEECH_RESULT_VALUE, msg.obj.toString());
                    }
                    setResult(RESULT_OK, intent);
                    finish();
                    break;
                default:
                    break;
            }
        }
    }

    private void prepareRecorder() {
        int minBufferSize = AudioRecord.getMinBufferSize(mSampleRate,
                AudioFormat.CHANNEL_IN_MONO, DEFAULT_AUDIO_ENCODING);
        if (minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Message msg = mHandler.obtainMessage(MSG_ERROR, ERR_UN_SUPPORT_PARAMS, 0);
            mHandler.sendMessage(msg);
            return;
        } else if (minBufferSize == AudioRecord.ERROR) {
            Log.w(TAG, "Unable to query hardware for output property");
            minBufferSize = mSampleRate * (120 / 1000) * DEFAULT_PER_SAMPLE_IN_BYTES * mChannels;
        }
        mBufferSize = minBufferSize * 2;

        mRecorder = new AudioRecord(mAudioSource, mSampleRate,
                AudioFormat.CHANNEL_IN_MONO, DEFAULT_AUDIO_ENCODING, mBufferSize);
        if (mRecorder.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord initialize failed");
            Message msg = mHandler.obtainMessage(MSG_ERROR, ERR_ILLEGAL_STATE, 0);
            mHandler.sendMessage(msg);
            return;
        }

        mRecordedLength = 0;
        int maxRecordLength = mSampleRate * mChannels * DEFAULT_PER_SAMPLE_IN_BYTES * 35;
        mRecordedData = new byte[maxRecordLength];
        Message msg = mHandler.obtainMessage(MSG_START_RECORDING);
        mHandler.sendMessage(msg);
    }

    private void startRecording() {
        if (mRecorder == null
                || mRecorder.getState() != AudioRecord.STATE_INITIALIZED) {
            Message msg = mHandler.obtainMessage(MSG_ERROR, ERR_ILLEGAL_STATE, 0);
            mHandler.sendMessage(msg);
            return;
        }

        mRecorder.startRecording();
        if (mRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
            textView.setText(R.string.recording);
            new Thread() {
                @Override
                public void run() {
                    byte[] tmpBuffer = new byte[mBufferSize / 2];
                    while (mRecorder != null
                            && mRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                        int numOfRead = mRecorder.read(tmpBuffer, 0, tmpBuffer.length);
                        if (numOfRead < 0) {
                            Message msg = mHandler.obtainMessage(MSG_ERROR, ERR_RECORDING, 0);
                            mHandler.sendMessage(msg);
                            break;
                        }

                        float sum = 0;
                        for (int i = 0; i < tmpBuffer.length; i += 2) {
                            short t = (short) (tmpBuffer[i] | (tmpBuffer[i + 1] << 8));
                            sum += Math.abs(t);
                        }
                        float rms = sum / (tmpBuffer.length * 2);
                        Message msg = mHandler.obtainMessage(MSG_RECORD_RECORDING, (int) rms, 0);
                        mHandler.sendMessage(msg);
                        if (mRecordedData.length > mRecordedLength + numOfRead) {
                            System.arraycopy(tmpBuffer, 0, mRecordedData, mRecordedLength, numOfRead);
                            mRecordedLength += numOfRead;
                        } else {
                            break;
                        }
                    }
                    mHandler.sendEmptyMessage(MSG_RECORD_STOPPED);
                }
            }.start();

        } else {
            Message msg = mHandler.obtainMessage(MSG_ERROR, ERR_ILLEGAL_STATE, 0);
            mHandler.sendMessage(msg);
        }
    }

    private void stopRecording() {
        if (mRecorder != null
                && mRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
            mRecorder.stop();
        }
    }

    private void createWavHeaderIfNeed(boolean forceCreate) {
        if (!forceCreate && wavHeader != null) {
            return;
        }
        // sample rate * number of channel * bit per sample / bit per bytes
        int avgBytesPerSec = mSampleRate * mChannels * DEFAULT_PER_SAMPLE_IN_BIT / 8;
        wavHeader = new byte[]{
                'R', 'I', 'F', 'F',           //id = RIFF , fixed chars
                0, 0, 0, 0,                // RIFF WAVE chunk size = 36 + data length
                'W', 'A', 'V', 'E',           //  Type
                /* Format chunk */
                'f', 'm', 't', ' ',          // id = 'fmt '
                16, 0, 0, 0,              // format chunk size = 16, if 18, means existing extension message
                1, 0,                     // format tag, 0x0001 = 16 pcm
                (byte) mChannels, 0, // number of channels (MONO = 1, STEREO =2)
                /* 4 bytes , sample rate */
                (byte) (mSampleRate & 0xff),
                (byte) ((mSampleRate >> 8) & 0xff),
                (byte) ((mSampleRate >> 16) & 0xff),
                (byte) ((mSampleRate >> 24) & 0xff),
                /* 4 bytes average bytes per seconds */
                (byte) (avgBytesPerSec & 0xff),
                (byte) ((avgBytesPerSec >> 8) & 0xff),
                (byte) ((avgBytesPerSec >> 16) & 0xff),
                (byte) ((avgBytesPerSec >> 24) & 0xff),
                /* 2 bytes, block align */
                /******************************
                 *              sample 1
                 ******************************
                 * channel 0 least| channel 0 most|
                 * ******************************/
                (byte) (DEFAULT_PER_SAMPLE_IN_BIT * mChannels / 8), // per sample in bytes
                0,
                /* 2 bytes, Bits per sample */
                16, 0,
                /* data chunk */
                'd', 'a', 't', 'a', /// Id = 'data'
                0, 0, 0, 0   // data size, set 0 due to unknown yet
        };
    }

    private void setWavHeaderInt(int offset, int value) {
        if (offset < 0 || offset > 40) {
            //total length = 44, int length = 4,
            //44 - 4 = 40
            throw new IllegalArgumentException("offset out of range");
        }
        createWavHeaderIfNeed(false);

        wavHeader[offset++] = (byte) (value & 0xff);
        wavHeader[offset++] = (byte) ((value >> 8) & 0xff);
        wavHeader[offset++] = (byte) ((value >> 16) & 0xff);
        wavHeader[offset] = (byte) ((value >> 24) & 0xff);
    }

    private byte[] getWavData() {
        setWavHeaderInt(4, 36 + mRecordedLength);
        setWavHeaderInt(40, mRecordedLength);
        byte[] wavData = new byte[44 + mRecordedLength];
        System.arraycopy(wavHeader, 0, wavData, 0, wavHeader.length);
        System.arraycopy(mRecordedData, 0, wavData, wavHeader.length, mRecordedLength);
        return wavData;
    }

    private HttpURLConnection getConnection() {
        HttpURLConnection connection = null;
        try {
            URL httpUrl = new URL(GOOGLE_VOICE_API_URL + mLang);
            connection = (HttpURLConnection) httpUrl.openConnection();
            connection.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT);
            connection.setReadTimeout(DEFAULT_READ_TIMEOUT);
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setRequestProperty("Content-Type", CONTENT_TYPE_WAV);
        } catch (MalformedURLException ex) {
            Log.e(TAG, "getConnection();Invalid url format");
        } catch (ProtocolException ex) {
            Log.e(TAG, "getConnection();Un support protocol", ex);
        } catch (IOException ex) {
            Log.e(TAG, "getConnection();IO error while open connection", ex);
        }
        return connection;
    }

    private void startWebRecognizer(final byte[] wavData) {
        textView.setText(R.string.analyzing);
        final HttpURLConnection connection = getConnection();
        if (connection == null) {
            Message msg = mHandler.obtainMessage(MSG_ERROR, ERR_NETWORK, 0);
            mHandler.sendMessage(msg);
        } else {
            new Thread() {
                @Override
                public void run() {
                    try {
                        DataOutputStream dos = new DataOutputStream(connection.getOutputStream());
                        dos.write(wavData);
                        dos.flush();
                        dos.close();

                        InputStreamReader inputStreamReader = new InputStreamReader(connection.getInputStream(),
                                Charset.forName("utf-8"));
                        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                        StringBuilder sb = new StringBuilder();
                        String tmpStr = null;
                        while ((tmpStr = bufferedReader.readLine()) != null) {
                            sb.append(tmpStr);
                        }
                        Message msg = mHandler.obtainMessage(MSG_DECODE_DATA, sb.toString());
                        mHandler.sendMessage(msg);
                    } catch (IOException ex) {
                        Message msg = mHandler.obtainMessage(MSG_ERROR, ERR_NETWORK, 0);
                        mHandler.sendMessage(msg);
                    }
                }
            }.start();
        }
    }

    private void startParseJson(String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            int status = jsonObject.getInt("status");
            if (status == 0) {
                JSONArray hypotheses = jsonObject.getJSONArray("hypotheses");
                if (hypotheses != null && hypotheses.length() > 0) {
                    JSONObject hypot = hypotheses.optJSONObject(0);
                    String speechText = hypot.getString("utterance");
                    Message msg = mHandler.obtainMessage(MSG_ERROR, ERR_NONE, 0, speechText);
                    mHandler.sendMessage(msg);
                }
            } else if (status == 4) {
                Message msg = mHandler.obtainMessage(MSG_ERROR, ERR_NO_SPEECH, 0);
                mHandler.sendMessage(msg);
            } else if (status == 5) {
                Message msg = mHandler.obtainMessage(MSG_ERROR, ERR_NO_MATCH, 0);
                mHandler.sendMessage(msg);
            }
        } catch (JSONException ex) {
            Log.e(TAG, "Decode JSON error");
            Message msg = mHandler.obtainMessage(MSG_ERROR, ERR_DECODING, 0);
            mHandler.sendMessage(msg);
        }
    }

    private void startAnimationIfNeed(AnimationDrawable animationDrawable) {
        imageView.setVisibility(View.VISIBLE);
        if (animationDrawable == null) {
//            imageView.setBackgroundResource(R.anim.speak_view);
            animationDrawable = (AnimationDrawable) imageView.getBackground();
        }

        if (animationDrawable != null && !animationDrawable.isRunning()) {
            animationDrawable.start();
        }
    }

    private void stopAnimation(AnimationDrawable animationDrawable) {
        imageView.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == GET_SPEECH_RESULT) {
            if (resultCode == RESULT_CANCELED) {
                //do nothing for now
            } else if (resultCode == RESULT_OK) {
                switch (data.getIntExtra(SPEECH_RESULT_STATUS, 0)) {
                    case MainActivity.ERR_NONE:
                        String text = data.getStringExtra(SPEECH_RESULT_VALUE);
                        if (text != null && text.trim().length() > 0) {
//                            submitText(text);
                        }
                        break;
                    default:
                        Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }
    }

}
