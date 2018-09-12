package com.voicetalk.io;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class PcmPlayer implements Runnable {
    private Logger log = LoggerFactory.getLogger(PcmPlayer.class);
    private final Object mutex = new Object();
    private volatile boolean isRecording;
    private List<PcmPlayer.rawData> list;
    private PcmPlayer.rawData rawData;
    private AudioTrack trackPlayer = null;
    private static final int sampleRate = 44100;
    private static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

    public PcmPlayer(){
        super();
        log.debug("PcmPlayer init!");
        list = Collections.synchronizedList(new LinkedList<PcmPlayer.rawData>());
    }

    public void  AudioInit(){
        log.debug("PcmPlayer AudioTrack  init!");
        int bufSize = AudioTrack.getMinBufferSize(sampleRate,
                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                audioEncoding);

        trackPlayer = new AudioTrack(AudioManager.STREAM_MUSIC,
                                                sampleRate,
                                                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                                                audioEncoding,
                                                bufSize,
                                                AudioTrack.MODE_STREAM);

        trackPlayer.play() ;
    }

    @Override
    public void run() {
        log.debug("PcmPlayer thread runing");
        AudioInit();
        while (this.isRecording()) {
            if (list.size() > 0) {
                rawData = list.remove(0);
                trackPlayer.write(rawData.pcm,0,rawData.size);
            } else {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        stop();
    }

    public void stop() {
        log.debug("PcmPlayer thread stop!" +
                  " isRecording:" + isRecording);
        trackPlayer.stop();
        trackPlayer.release();
    }

    public void putData(byte[] buf, int size) {
        PcmPlayer.rawData data = new PcmPlayer.rawData();
        data.size = size;
        System.arraycopy(buf, 0, data.pcm, 0, size);
        list.add(data);
    }

    public void setRecording(boolean isRecording) {
        synchronized (mutex) {
            this.isRecording = isRecording;
            if (this.isRecording) {
                mutex.notify();
            }
        }
    }

    public boolean isRecording() {
        synchronized (mutex) {
            return isRecording;
        }
    }

    class rawData {
        int size;
        byte[] pcm = new byte[2048];
    }
}
