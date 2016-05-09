package simulation;

import java.io.*;
import java.net.Socket;
import java.util.TimerTask;

public class RemoteApp {
	static final String hostName = "127.0.0.1";
	static final int portNumber = 5678;
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		ObjectScene os = new ObjectScene();
		
		// tcp listener: command receiver
		try {
			Socket commandSocket = new Socket(hostName, portNumber);
			BufferedReader commandReader = new BufferedReader(new InputStreamReader(commandSocket.getInputStream()));
			
			while(true) {
				String input = commandReader.readLine();
				System.out.println(input);
				if(input.equals("exit")) {
					break;
				}
				
				os.changeDirection(input);			
				System.out.println(Integer.toHexString(os.getDirection()));
			}
			System.out.println("close connection");
			
			os.close();
			commandReader.close();
			commandSocket.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		
		// periodic timer: update object position
		// periodic timer: position sender
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
	static final int RIGHT_BOUND = 1000;
	static final int UP_BOUND = 0;
	static final int DOWN_BOUND = 1000;
	
	
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
		step_period = 5;
		
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
			if(direction == LEFT && posX - step_length >= LEFT_BOUND) {
				posX -= step_length;
			}
			else if(direction == RIGHT && posX + step_length <= RIGHT_BOUND) {
				posX += step_length;
			}
			
			if(direction == UP && posY - step_length >= LEFT_BOUND) {
				posY -= step_length;
			}
			else if(direction == DOWN && posY + step_length <= RIGHT_BOUND) {
				posY += step_length;
			}
			
			System.out.println(posX + ", " + posY);
		}
	}
}

