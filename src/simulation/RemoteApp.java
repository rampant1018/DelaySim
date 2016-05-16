package simulation;

import java.io.*;
import java.net.*;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import java.util.TimerTask;

import jlibrtp.*;

public class RemoteApp {
	static final String hostname = "127.0.0.1";
	static final int portNumber = 5678;
	static final int rtpLocalPortNumber = 16384; // local site
	static final int rtpRemotePortNumber = 16386; // remote site(this app)
	
	static final int positionSendingPeriod = 10;
	static final int defaultPositionSendingDelay = 130; // position feedback delay (ms)
	
	static java.util.Timer positionSendingTimer = null;
	static SceneSender ss = null;
	static ObjectScene os = null;
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		ss = new SceneSender(hostname, rtpLocalPortNumber, rtpRemotePortNumber);
		
		// init object and position sending task
		os = new ObjectScene();
		PositionSendingTask pst = new PositionSendingTask(defaultPositionSendingDelay / positionSendingPeriod);
		positionSendingTimer = new java.util.Timer();
		positionSendingTimer.scheduleAtFixedRate(pst, 0, positionSendingPeriod);
		
		// tcp listener: command receiver
		try {
			Socket commandSocket = new Socket(hostname, portNumber);
			BufferedReader commandReader = new BufferedReader(new InputStreamReader(commandSocket.getInputStream()));
						
			while(true) {
				String cmd = commandReader.readLine();
				System.out.println(cmd);
				
				if(!commandProcess(cmd)) {
					break;
				}
			}
			System.out.println("close connection");
			
			positionSendingTimer.cancel();
			ss.close();
			os.close();
			commandReader.close();
			commandSocket.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	static boolean commandProcess(String cmd) {
		Scanner scanner = new Scanner(cmd);
		
		if(scanner.hasNext()) {
			String cmd1 = scanner.next();
			
			if(cmd1.equals("exit")) {
				scanner.close();
				return false;
			}
			
			if(cmd1.equals("cd")) { // change direction
				if(!scanner.hasNext()) {
					System.out.println("Missing arguments: cd args");
				}
				else {
					String arg = scanner.next();
					os.changeDirection(arg);
				}
			}
			else if(cmd1.equals("restart")) {
				os.restart();
			}
			else if(cmd1.equals("setdelay")) {
				if(!scanner.hasNextInt()) {
					System.out.println("Missing arguments: setdelay args");
				}
				else {
					int delay = scanner.nextInt();
					
					positionSendingTimer.cancel();
					PositionSendingTask pst = new PositionSendingTask(delay / positionSendingPeriod);
					positionSendingTimer = new java.util.Timer();
					positionSendingTimer.scheduleAtFixedRate(pst, 0, positionSendingPeriod);
				}
			}
			else if(cmd1.equals("enable")) {
				if(!scanner.hasNext()) {
					System.out.println("Missing arguments: enable args");
				}
				else {
					String arg = scanner.next();
					if(arg.equals("TAP")) {
						os.enableTAP();
					}
					else if(arg.equals("SPP")) {
						os.enableSPP();
					}
					else {
						System.out.println("Error arguments: enable [TAP/SPP]");
					}
				}
			}
			else if(cmd1.equals("disable")) {
				if(!scanner.hasNext()) {
					System.out.println("Missing arguments: disable args");
				}
				else {
					String arg = scanner.next();
					if(arg.equals("TAP")) {
						os.disableTAP();
					}
					else if(arg.equals("SPP")) {
						os.disableSPP();
					}
					else {
						System.out.println("Error arguments: disable [TAP/SPP]");
					}
				}
			}
			else {
				System.out.println("Invalid command!");
			}
		}
		
		scanner.close();
		return true;
	}
	
	static class PositionSendingTask extends TimerTask {
		int delay;
		Queue<String> buffer = null;
		
		public PositionSendingTask(int delay) {
			this.delay = delay;
			
			buffer = new LinkedList<>();
		}
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			String coord = Integer.toString(os.getPosX()) + " " + Integer.toString(os.getPosY());
			buffer.add(coord);
			if(delay > 0) {
				delay--;
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
	
	// Predictor enable flag
	boolean enableTAP;
	boolean enableSPP;
	
	// Target Area(TA) Predictor variables
	boolean taRightFlag;
	boolean taLeftFlag;
	boolean taTopFlag;
	boolean taBottomFlag;
	
	int taLastStopX;
	int taLastStopY;
	int taLastHorizontal;
	int taLastVertical;
	
	int taRightBound;
	int taLeftBound;
	int taTopBound;
	int taBottomBound;
	
	// Stop Point(SP) Predictor variables
	boolean spSet;
	int spPredictedX;
	int spPredictedY;
	
	java.util.Timer sceneUpdatingTimer = null;
	
	public ObjectScene() {
		initialize();
	}
	
	public void restart() {
		this.close();
		initialize();
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
	
	public void enableTAP() {
		enableTAP = true;
	}
	
	public void disableTAP() {
		enableTAP = false;
	}
	
	public void enableSPP() {
		enableSPP = true;
	}
	
	public void disableSPP() {
		enableSPP = false;
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
	
	private void initialize() {
		// object attributes
		posX = 0;
		posY = 0;
		direction = 0x0;
		step_length = 1;
		step_period = 5;
		
		taTopFlag = false;
		taBottomFlag = false;
		taLeftFlag = false;
		taRightFlag = false;
		
		taLastHorizontal = STOP;
		taLastVertical = STOP;
		
		enableTAP = false;
		enableSPP = false;
		
		// periodic timer: update object position
		sceneUpdatingTimer = new java.util.Timer();
		sceneUpdatingTimer.scheduleAtFixedRate(new SceneUpdatingTask(), 0, step_period);
	}
	
	private void updateDirection(int d, boolean enable) {
		if(enable) {
			taUpdateTABound(d);
			direction |= d;
		}
		else {
			taUpdateLastAction(d);
			direction &= ~d;
		}
	}
	
	private void taUpdateTABound(int d) {
		switch(d) {
		case LEFT:
			if(taLastHorizontal == RIGHT) {
				taRightFlag = true;
				taRightBound = taLastStopX + OBJECT_WIDTH;
				System.out.println("taRightBound = " + taRightBound);
			}
			break;
		case RIGHT:
			if(taLastHorizontal == LEFT) {
				taLeftFlag = true;
				taLeftBound = taLastStopX;
				System.out.println("taLeftBound = " + taLeftBound);
			}
			break;
		case UP:
			if(taLastVertical == DOWN) {
				taBottomFlag = true;
				taBottomBound = taLastStopY + OBJECT_HEIGHT;
				System.out.println("taBottomBound = " + taBottomBound);
			}
			break;
		case DOWN:
			if(taLastVertical == UP) {
				taTopFlag = true;
				taTopBound = taLastStopY;
				System.out.println("taTopBound = " + taTopBound);
			}
			break;
		}
	}
	
	private void taUpdateLastAction(int d) {
		switch(d) {
		case LEFT:
			taLastHorizontal = LEFT;
			taLastStopX = posX;
			break;
		case RIGHT:
			taLastHorizontal = RIGHT;
			taLastStopX = posX;
			break;
		case UP:
			taLastVertical = UP;
			taLastStopY = posY;
			break;
		case DOWN:
			taLastVertical = DOWN;
			taLastStopY = posY;
			break;
		}
	}
	
	private class SceneUpdatingTask extends TimerTask {
		@Override
		public void run() {
			// TODO Auto-generated method stub
			if(!boundaryCheck()) {
				return;
			}
			
			// Todo: Target Area Predictor
			if(enableTAP && !taBoundaryCheck()) {
				return;
			}
			
			// Todo: Stop Point Predictor
			
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
			if((direction & LEFT) == LEFT && posX - step_length < LEFT_BOUND) {
				return false;
			}
			else if((direction & RIGHT) == RIGHT && posX + step_length > RIGHT_BOUND - OBJECT_WIDTH) {
				return false;
			}
			
			if((direction & UP) == UP && posY - step_length < TOP_BOUND) {
				return false;
			}
			else if((direction & DOWN) == DOWN && posY + step_length > BOTTOM_BOUND - OBJECT_HEIGHT) {
				return false;
			}
			
			return true;
		}
		
		boolean taBoundaryCheck() {
			if((direction & LEFT) == LEFT && taLeftFlag && posX - step_length < taLeftBound) {
				return false;
			}
			else if((direction & RIGHT) == RIGHT && taRightFlag && posX + step_length > taRightBound - OBJECT_WIDTH) {
				return false;
			}
			
			if((direction & UP) == UP && taTopFlag & posY - step_length < taTopBound) {
				return false;
			}
			else if((direction & DOWN) == DOWN && taBottomFlag && posY + step_length > taBottomBound - OBJECT_HEIGHT) {
				return false;
			}
			
			return true;
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