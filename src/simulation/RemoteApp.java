package simulation;

import java.io.*;
import java.net.*;
import java.util.LinkedList;
import java.util.Queue;
import java.util.TimerTask;

import jlibrtp.*;

public class RemoteApp {
	static final String hostname = "127.0.0.1";
	static final int portNumber = 5678;
	static final int rtpLocalPortNumber = 16384; // local site
	static final int rtpRemotePortNumber = 16386; // remote site(this app)
	
	static final int positionSendingPeriod = 5;
	static final int positionSendingDelay = 500 / positionSendingPeriod; // position feedback delay (ms)
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		SceneSender ss = new SceneSender(hostname, rtpLocalPortNumber, rtpRemotePortNumber);
		ObjectScene os = new ObjectScene();
		
		// periodic timer: position sender
		PositionSendingTask pst = new PositionSendingTask(ss, os);
		java.util.Timer timer = new java.util.Timer();
		timer.scheduleAtFixedRate(pst, 0, positionSendingPeriod);
		
		// tcp listener: command receiver
		try {
			Socket commandSocket = new Socket(hostname, portNumber);
			BufferedReader commandReader = new BufferedReader(new InputStreamReader(commandSocket.getInputStream()));
						
			while(true) {
				String input = commandReader.readLine();
				System.out.println(input);
				
				if(input.equals("exit")) {
					break;
				}
				os.changeDirection(input);
			}
			System.out.println("close connection");
			
			timer.cancel();
			ss.close();
			os.close();
			commandReader.close();
			commandSocket.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	static class PositionSendingTask extends TimerTask {
		SceneSender ss;
		ObjectScene os;
		
		int delayStep = positionSendingDelay;
		Queue<String> buffer = null;
		
		public PositionSendingTask(SceneSender ss, ObjectScene os) {
			this.ss = ss;
			this.os = os;
			
			buffer = new LinkedList<>();
		}
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			String coord = Integer.toString(os.getPosX()) + " " + Integer.toString(os.getPosY());
			buffer.add(coord);
			if(delayStep > 0) {
				delayStep--;
			}
			else {
				ss.sendData(buffer.poll().getBytes());
			}
		}
	}
}

class ObjectScene {
	// object attributes
	private int posX, posY;
	private int direction; // first byte indicates Left/Right, second byte indicates Up/Down
	private int step_length; // object moving step length
	private int step_period; // object moving period (ms)
	
	// scene boundary (inclusive)
	static final int LEFT_BOUND = 0;
	static final int RIGHT_BOUND = 800;
	static final int TOP_BOUND = 0;
	static final int BOTTOM_BOUND = 600;
	
	// object parameters
	static final int OBJECT_WIDTH = 20;
	static final int OBJECT_HEIGHT = 20;
	
	// Direction constants
	static final int STOP = 0x0;
	static final int LEFT = 0x01;
	static final int RIGHT = 0x02;
	static final int UP = 0x10;
	static final int DOWN = 0x20;
	
	java.util.Timer sceneUpdatingTimer = null;
	
	public ObjectScene() {
		// object attributes
		posX = 0;
		posY = 0;
		direction = 0x0;
		step_length = 1;
		step_period = 20;
		
		// periodic timer: update object position
		sceneUpdatingTimer = new java.util.Timer();
		sceneUpdatingTimer.scheduleAtFixedRate(new SceneUpdatingTask(), 0, step_period);
	}
	
	public void close() {
		sceneUpdatingTimer.cancel();
	}
	
	public int getDirection() {
		return direction;
	}
	
	public int getPosX() {
		return posX;
	}
	
	public int getPosY() {
		return posY;
	}
	
	public void changeDirection(String command) {
		if(command.equals("LeftOn")) {
			updateDirection(LEFT, true);
		}
		else if(command.equals("LeftOff")) {
			updateDirection(LEFT, false);
		}
		else if(command.equals("RightOn")) {
			updateDirection(RIGHT, true);
		}
		else if(command.equals("RightOff")) {
			updateDirection(RIGHT, false);
		}
		else if(command.equals("UpOn")) {
			updateDirection(UP, true);
		}
		else if(command.equals("UpOff")) {
			updateDirection(UP, false);
		}
		else if(command.equals("DownOn")) {
			updateDirection(DOWN, true);
		}
		else if(command.equals("DownOff")) {
			updateDirection(DOWN, false);
		}
		else {
			System.err.println("ChangeDirection: Invalid command");
		}
	}
	
	private void updateDirection(int d, boolean enable) {
		if(enable) {
			direction |= d;
		}
		else {
			direction &= ~d;
		}
	}
	
	private class SceneUpdatingTask extends TimerTask {
		@Override
		public void run() {
			// TODO Auto-generated method stub
			if(!boundaryCheck()) {
				return;
			}
			
			move();
		}
		
		void move() {
			if((direction & LEFT) == LEFT) {
				posX -= step_length;
			}
			else if((direction & RIGHT) == RIGHT) {
				posX += step_length;
			}
			
			if((direction & UP) == UP) {
				posY -= step_length;
			}
			else if((direction & DOWN) == DOWN) {
				posY += step_length;
			}
		}
		
		boolean boundaryCheck() {
			if((direction & LEFT) == LEFT && posX - step_length >= LEFT_BOUND) {
				return true;
			}
			else if((direction & RIGHT) == RIGHT && posX + step_length <= RIGHT_BOUND - OBJECT_WIDTH) {
				return true;
			}
			
			if((direction & UP) == UP && posY - step_length >= TOP_BOUND) {
				return true;
			}
			else if((direction & DOWN) == DOWN && posY + step_length <= BOTTOM_BOUND - OBJECT_HEIGHT) {
				return true;
			}
			
			return false;
		}
	}
}

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
