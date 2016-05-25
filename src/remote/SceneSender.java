package remote;

import java.net.DatagramSocket;

import jlibrtp.DataFrame;
import jlibrtp.Participant;
import jlibrtp.RTPAppIntf;
import jlibrtp.RTPSession;

class SceneSender implements RTPAppIntf {
	/** Holds a RTPSession instance */
	DatagramSocket rtpSocket = null;
	DatagramSocket rtcpSocket = null;
	RTPSession rtpSession = null;
	
	/** A minimal constructor */
	public SceneSender(String hostname, int rtpLocalPortNumber, int rtpRemotePortNumber) {
		System.out.print("Initializing rtp session ... ");
		
		try {
			rtpSocket = new DatagramSocket(rtpRemotePortNumber);
			rtcpSocket = new DatagramSocket(rtpRemotePortNumber + 1);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		rtpSession = new RTPSession(rtpSocket, rtcpSocket);		
		Participant part = new Participant(hostname, rtpLocalPortNumber, rtpLocalPortNumber + 1);
		rtpSession.addParticipant(part);
		
		rtpSession.RTPSessionRegister(this, null, null);
		try{ Thread.sleep(5000); } catch(Exception e) {}
		
		System.out.println("done");
	}
	
	public SceneSender(RTPSession rtpSession) {
		this.rtpSession = rtpSession;
	}
	
	@Override
	public void receiveData(DataFrame frame, Participant participant) {
		// TODO Auto-generated method stub
		byte[] data = frame.getConcatenatedData();
		System.out.println("RTP received: " + new String(data));
	}

	@Override
	public void userEvent(int type, Participant[] participant) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int frameSize(int payloadType) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public void sendData(byte[] data) {
		rtpSession.sendData(data);
	}
	
	public void close() {
		rtpSession.endSession();
	}
}
