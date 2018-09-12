package com.voicetalk.rtp;

import android.util.Log;

import java.net.DatagramSocket;
import com.voicetalk.io.Consumer;
import com.voicetalk.manager.ClientManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jlibrtp.DataFrame;
import jlibrtp.Participant;
import jlibrtp.RTPAppIntf;
import jlibrtp.RTPSession;

public class InitSession implements RTPAppIntf {
    private Logger log = LoggerFactory.getLogger(ClientManager.class);
	public RTPSession rtpSession = null;
	int RTP_BUFFSIZE_MAX = 1480;
    private Consumer consumer;

	public InitSession(Consumer consumer,int localRtpPort, int localRtcpPort, int destRtpPort, int destRtcpPort, String destNetworkAddress) {
		super();
		this.consumer = consumer;
        log.debug("InitSession thread runing");
		DatagramSocket rtpSocket = null;
		DatagramSocket rtcpSocket = null;

		try {
			rtpSocket = new DatagramSocket(localRtpPort);
			rtcpSocket = new DatagramSocket(localRtcpPort);
		} catch (Exception e) {
            log.error("发送创建会话异常抛出:"+e);
		}

		//建立会话
		rtpSession = new RTPSession(rtpSocket, rtcpSocket);
        rtpSession.naivePktReception(true);
		rtpSession.RTPSessionRegister(this,null,null);
		//设置参与者（目标IP地址，RTP端口，RTCP端口）
		Participant p = new Participant(destNetworkAddress, destRtpPort, destRtcpPort);
		rtpSession.addParticipant(p);
	}

    byte [] buf;
	public void receiveData(DataFrame frame, Participant p){
        log.info( "接收到数据: "+ frame.getConcatenatedData()
                  + " , 参与者CNAME： " + p.getCNAME()
                  + "同步源标识符(" + p.getSSRC() + ")" );

        if (buf == null){
            buf = frame.getConcatenatedData();
        } else {
            buf = byteMerger(buf, frame.getConcatenatedData());
        }
        if (frame.marked()){
			Log.i("InitSession", "receiveData:InitSession ");
			consumer.putData(System.currentTimeMillis(), buf,buf.length);
            buf = null;
        }
	}

    //System.arraycopy()方法
    public static byte[] byteMerger(byte[] bt1, byte[] bt2){
        byte[] bt3 = new byte[bt1.length+bt2.length];
        System.arraycopy(bt1, 0, bt3, 0, bt1.length);
        System.arraycopy(bt2, 0, bt3, bt1.length, bt2.length);
        return bt3;
    }

	public void userEvent(int type, Participant[] participant) {
		// TODO Auto-generated method stub
	}

	public int frameSize(int payloadType) {
		return 1;
	}

	public void sendData(byte[] bytes) {
		int dataLength = (bytes.length - 1) / RTP_BUFFSIZE_MAX + 1;
		final byte[][] data = new byte[dataLength][];
		final boolean[] marks = new boolean[dataLength];
		marks[marks.length - 1] = true;
		int x = 0;
		int y = 0;
		int length = bytes.length;
		for (int i = 0; i < length; i++){
			if (y == 0){
				data[x] = new byte[length - i > RTP_BUFFSIZE_MAX ? RTP_BUFFSIZE_MAX : length - i];
			}
			data[x][y] = bytes[i];
			y++;
			if (y == data[x].length){
				y = 0;
				x++;
			}
		}
		rtpSession.sendData(data, null, marks, -1, null);
	}

	public void stop() {
		rtpSession.endSession();
	}
}
