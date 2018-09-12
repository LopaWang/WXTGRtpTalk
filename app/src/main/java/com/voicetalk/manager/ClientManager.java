package com.voicetalk.manager;

import android.util.Log;

import com.voicetalk.io.*;
import com.voicetalk.rtp.InitSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ClientManager implements Runnable, Consumer {
	private Logger log = LoggerFactory.getLogger(ClientManager.class);
	private final Object mutex = new Object();
	public static final int CLIENT = 1;
	public static final int SERVER = 2;
	private int mode = CLIENT;
	private int seq = 0;
	private volatile boolean isRecording;
	private volatile boolean isRunning;
	private processedData pData;
	private List<processedData> list;
	private PcmRecorder recorder = null;
	private int localRtpPort = 8082;
	private int localRtcpPort = 8083;
	private int destRtpPort = 8084;
	private int destRtcpPort = 8085;
	private String destNetworkAddress = "192.168.1.106";
    private InitSession session = null;
    private PcmWriter pcmWriter = null;
	private PcmPlayer pcmPlayer = null;

	public ClientManager(int localRtpPort, int localRtcpPort, int destRtpPort, int destRtcpPort, String destNetworkAddress) {
		super();
		this.localRtpPort = localRtpPort;
		this.localRtcpPort = localRtcpPort;
		this.destRtpPort = destRtpPort;
		this.destRtcpPort = destRtcpPort;
		this.destNetworkAddress = destNetworkAddress;
		Log.i("ClientManager", "ClientManager init !\n " +
				" localRtpPort:" + localRtpPort +
				" localRtcpPort:" + localRtcpPort +
				" destRtpPort:" + destRtpPort +
				" destRtcpPort:" + destRtcpPort +
				" destNetworkAddress:" + destNetworkAddress );
		list = Collections.synchronizedList(new LinkedList<processedData>());
	}

	private  void StartPcmWriter(){
        pcmWriter = new PcmWriter();
        pcmWriter.setRecording(true);
        Thread th = new Thread(pcmWriter);
        th.start();
    }

    private  void StartPcmPlayer(){
        pcmPlayer = new PcmPlayer();
        pcmPlayer.setRecording(true);
        Thread th = new Thread(pcmPlayer);
        th.start();
    }

    private void StartRtpSession(){
        session = new InitSession(this,
                                    localRtpPort,
                                    localRtcpPort,
                                    destRtpPort,
                                    destRtcpPort,
                                    destNetworkAddress);
    }

	public void setMode(int mode) {
		this.mode = mode;
	}

	public void run() {
		log.debug("ClientManager thread runing");
		
		while (this.isRunning()) {
			synchronized (mutex) {
				while (!this.isRecording) {
					try {
						mutex.wait();
					} catch (InterruptedException e) {
						throw new IllegalStateException("Wait() interrupted!",
								e);
					}
				}
			}

			if(this.mode == SERVER) {
				StartPcmPlayer();
				//StartPcmWriter();
			}

			StartRtpSession();
			startPcmRecorder();

			while (this.isRecording()) {
				if (list.size() > 0) {
					writeTag();
					log.debug("list size = {}", list.size());
				} else {
					try {
						Thread.sleep(20);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			
			recorder.stop();
			while(list.size() > 0){
				writeTag();
				log.debug("list size = {}", list.size());
			}
			stop();
		}
	}

	private void startPcmRecorder(){
		recorder = new PcmRecorder(this);
		recorder.setRecording(true);
		Thread th = new Thread(recorder);
		th.start();
	}
	
	private void writeTag() {
		pData = list.remove(0);
		if(this.mode == CLIENT){
			if (session != null){
				log.debug("发送的数据:"+ Arrays.toString(pData.processed) + " size:" + pData.size);
				session.sendData(pData.processed);
			}
		}
		if(this.mode == SERVER){
            pcmPlayer.putData(pData.processed, pData.size);
            //pcmWriter.putData(pData.processed, pData.size);

		}
	}

	public void putData(long ts, byte[] buf, int size) {
		processedData data = new processedData();
		data.ts = ts;
		data.size = size;
		System.arraycopy(buf, 0, data.processed, 0, size);
		list.add(data);
	}

	private void stop() {
		log.debug("ClientManager thread stop" +
				" isRecording:" + isRecording +
				" isRunning:" + isRunning);
		session.stop();
		if(this.mode == SERVER) {
			//pcmWriter.setRecording(false);
			pcmPlayer.setRecording(false);
		}

	}

	public boolean isRunning() {
		synchronized (mutex) {
			return isRunning;
		}
	}

	public void setRunning(boolean isRunning) {
		synchronized (mutex) {
			this.isRunning = isRunning;
			if (this.isRunning) {
				mutex.notify();
			}
		}
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

	class processedData {
		private long ts;
		private int size;
		private byte[] processed = new byte[2048];
	}
}
