package com.voicetalk.io;

import android.os.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class PcmWriter implements Runnable {
	private Logger log = LoggerFactory.getLogger(PcmWriter.class);
	private final Object mutex = new Object();
	private volatile boolean isRecording;
	private rawData rawData;
	private File pcmFile;
	DataOutputStream dataOutputStreamInstance;
	private List<rawData> list;

	public PcmWriter() {
		super();
		pcmFile = new File(Environment.getExternalStorageDirectory()
				.getAbsolutePath() + "/test.pcm");
		log.debug("PcmWriter init!\n " +
		           " pcmFile:" + pcmFile.getAbsolutePath());
		list = Collections.synchronizedList(new LinkedList<rawData>());
	}

	public void StartPcmWriter() {
		BufferedOutputStream bufferedStreamInstance = null;

		if (pcmFile.exists()) {
			pcmFile.delete();
		}

		try {
			pcmFile.createNewFile();
		} catch (IOException e) {
			throw new IllegalStateException("Cannot create file: "
					+ pcmFile.toString());
		}

		try {
			bufferedStreamInstance = new BufferedOutputStream(
					new FileOutputStream(pcmFile));
		} catch (FileNotFoundException e) {
			throw new IllegalStateException("Cannot Open File", e);
		}

		dataOutputStreamInstance = new DataOutputStream(bufferedStreamInstance);

	}

	public void run() {
		log.debug("PcmWriter thread runing");
		StartPcmWriter();
		while (this.isRecording()) {

			if (list.size() > 0) {
				rawData = list.remove(0);
				try {
					for (int i = 0; i < rawData.size; ++i) {
						dataOutputStreamInstance.write(rawData.pcm[i]);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
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

	public void putData(byte[] buf, int size) {
		rawData data = new rawData();
		data.size = size;
		System.arraycopy(buf, 0, data.pcm, 0, size);
		list.add(data);
	}

	public void stop() {
		log.debug("PcmWriter thread stop" +
				" isRecording:" + isRecording);
		try {
			dataOutputStreamInstance.close();
		} catch (IOException e) {
			e.printStackTrace();
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

	class rawData {
		int size;
		byte[] pcm = new byte[2048];
	}
}
