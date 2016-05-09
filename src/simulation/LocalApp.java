package simulation;

import java.io.*;
import java.net.*;

import jlibrtp.*;

public class LocalApp {
	static final String hostname = "127.0.0.1";
	static final int portNumber = 5678;
	static final int rtpLocalPortNumber = 16384; // local site(this app)
	static final int rtpRemotePortNumber = 16386; // remote site
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		SceneReceiver sr = new SceneReceiver(hostname, rtpLocalPortNumber, rtpRemotePortNumber);		
		
		try {
			ServerSocket commandSvrSocket = new ServerSocket(portNumber);
			Socket commandSocket = commandSvrSocket.accept();
			System.out.println("Accepted connection");
			
			BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
			PrintWriter commandWriter = new PrintWriter(commandSocket.getOutputStream(), true);
			
			while(true) {
				String input = stdIn.readLine();
				System.out.println(input);
				
				input += '\n';
				
				commandWriter.print(input);
				commandWriter.flush();
				if(input.equals("exit\n")) {
					break;
				}				
			}
			System.out.println("closed connection");
			
			sr.close();
			commandWriter.close();
			commandSocket.close();
			commandSvrSocket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

class SceneReceiver implements RTPAppIntf {
	/** Holds a RTPSession instance */
	DatagramSocket rtpSocket = null;
	DatagramSocket rtcpSocket = null;
	RTPSession rtpSession = null;
	
	/** A minimal constructor */
	public SceneReceiver(String hostname, int rtpLocalPortNumber, int rtpRemotePortNumber) {
		System.out.print("Initializing rtp session ... ");
		
		try {
			rtpSocket = new DatagramSocket(rtpLocalPortNumber);
			rtcpSocket = new DatagramSocket(rtpLocalPortNumber + 1);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		rtpSession = new RTPSession(rtpSocket, rtcpSocket);		
		Participant part = new Participant(hostname, rtpRemotePortNumber, rtpRemotePortNumber + 1);
		rtpSession.addParticipant(part);
		
		rtpSession.RTPSessionRegister(this, null, null);
		try{ Thread.sleep(5000); } catch(Exception e) {}
		
		System.out.println("done");
	}
	
	public SceneReceiver(RTPSession rtpSession) {
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
	
	public void close() {
		rtpSession.endSession();
	}
}
