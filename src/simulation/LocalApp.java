package simulation;

import java.io.*;
import java.net.*;
import java.util.Scanner;

import jlibrtp.*;

public class LocalApp {
	static final String hostname = "127.0.0.1";
	static final int portNumber = 5678;
	static final int rtpLocalPortNumber = 16384; // local site(this app)
	static final int rtpRemotePortNumber = 16386; // remote site
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		Object object = new Object();
		
		SceneReceiver sr = new SceneReceiver(hostname, rtpLocalPortNumber, rtpRemotePortNumber, object);
		
		try {
			ServerSocket commandSvrSocket = new ServerSocket(portNumber);
			Socket commandSocket = commandSvrSocket.accept();
			System.out.println("Accepted connection");
			
			BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
			PrintWriter commandWriter = new PrintWriter(commandSocket.getOutputStream(), true);
			
			LocalGUI lgui = new LocalGUI(object, commandWriter);
			
			while(true) {
				String input = stdIn.readLine();
				commandWriter.println(input);
				commandWriter.flush();
				if(input.equals("exit")) {
					break;
				}				
			}
			System.out.println("closed connection");
			
			sr.close();
			commandWriter.close();
			commandSocket.close();
			commandSvrSocket.close();
			lgui.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		try{ Thread.sleep(2000); } catch(Exception e) {} // wait to release all resource
		
		System.out.println("Program exit");
	}
}

class Object {
	static final int WIDTH = 20;
	static final int HEIGHT = 20;
	
	int posX, posY;
	
	int getPosX() {
		return posX;
	}
	
	int getPosY() {
		return posY;
	}
	
	void setPosX(int posX) {
		this.posX = posX;
	}
	
	void setPosY(int posY) {
		this.posY = posY;
	}
}

class SceneReceiver implements RTPAppIntf {
	/** Holds a RTPSession instance */
	DatagramSocket rtpSocket = null;
	DatagramSocket rtcpSocket = null;
	RTPSession rtpSession = null;
	
	Object object = null;
	
	PrintWriter writerX = null;
	PrintWriter writerY = null;
	
	long start = 0;
	
	/** A minimal constructor */
	public SceneReceiver(String hostname, int rtpLocalPortNumber, int rtpRemotePortNumber, Object object) {
		try {
			start = System.currentTimeMillis();
			writerX = new PrintWriter("pos_x.txt", "UTF-8");
			writerY = new PrintWriter("pos_y.txt", "UTF-8");
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		System.out.print("Initializing rtp session ... ");
		
		try {
			rtpSocket = new DatagramSocket(rtpLocalPortNumber);
			rtcpSocket = new DatagramSocket(rtpLocalPortNumber + 1);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		rtpSession = new RTPSession(rtpSocket, rtcpSocket);		
		Participant part = new Participant(hostname, rtpRemotePortNumber, rtpRemotePortNumber + 1);
		//Participant part = new Participant("192.168.1.56", rtpRemotePortNumber, rtpRemotePortNumber + 1);
		rtpSession.addParticipant(part);
		
		rtpSession.RTPSessionRegister(this, null, null);
		try{ Thread.sleep(5000); } catch(Exception e) {}
		
		System.out.println("done");
		
		this.object = object;
	}
	
	public SceneReceiver(RTPSession rtpSession, Object object) {
		this.rtpSession = rtpSession;
		this.object = object;
	}
	
	@Override
	public void receiveData(DataFrame frame, Participant participant) {
		// TODO Auto-generated method stub
		byte[] data = frame.getConcatenatedData();
		String strPos = new String(data);
		Scanner scanner = new Scanner(strPos);
		object.setPosX(scanner.nextInt());
		object.setPosY(scanner.nextInt());
		scanner.close();
		
		long current = System.currentTimeMillis();
		writerX.println((current - start) + ", " + object.getPosX());
		writerY.println((current - start) + ", " + object.getPosY());
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
		writerX.close();
		writerY.close();
		rtpSession.endSession();
	}
}