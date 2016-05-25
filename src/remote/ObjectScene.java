package remote;

import java.util.TimerTask;

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
	boolean spSetX;
	boolean spSetY;
	int spPredictedX;
	int spPredictedY;
	boolean spInRangeX;
	boolean spInRangeY;
	
	java.util.Timer horizontalUpdatingTimer = null;
	java.util.Timer verticalUpdatingTimer = null;
	
	public ObjectScene() {
		initialize();
	}
	
	public void restart() {
		this.close();
		initialize();
	}
	
	public void close() {
		horizontalUpdatingTimer.cancel();
		verticalUpdatingTimer.cancel();
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
		
		// TAP
		taTopFlag = false;
		taBottomFlag = false;
		taLeftFlag = false;
		taRightFlag = false;
		
		taLastHorizontal = STOP;
		taLastVertical = STOP;
		
		// SPP
		spSetX = false;
		spSetY = false;
		spInRangeX = false;
		spInRangeY = false;
		
		enableTAP = false;
		enableSPP = false;
		
		// periodic timer: update object position
		horizontalUpdatingTimer = new java.util.Timer();
		horizontalUpdatingTimer.scheduleAtFixedRate(new HorizontalUpdatingTask(), 0, step_period);
		
		verticalUpdatingTimer = new java.util.Timer();
		verticalUpdatingTimer.scheduleAtFixedRate(new VerticalUpdatingTask(), 0, step_period);
	}
	
	private void updateDirection(int d, boolean enable) {
		if(enable) {
			taUpdateBound(d);
			spUpdatePredicted(d);
			direction |= d;
		}
		else {
			taUpdateLastAction(d);
			direction &= ~d;
		}
	}
	
	private void taUpdateBound(int d) {
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
	
	private void spUpdatePredicted(int d) {
		switch(d) {
		case LEFT:
			if(taLastHorizontal == RIGHT && !spSetX) {
				spSetX = true;
				spPredictedX = taLastStopX + OBJECT_WIDTH / 2 - (500 / step_period) * step_length;
				System.out.println("taLastStopX = " + taLastStopX);
				System.out.println("spPredictedX = " + spPredictedX);
			}
			break;
		case RIGHT:
			if(taLastHorizontal == LEFT && !spSetX) {
				spSetX = true;
				spPredictedX = taLastStopX + OBJECT_WIDTH / 2 + (500 / step_period) * step_length;
				System.out.println("taLastStopX = " + taLastStopX);
				System.out.println("spPredictedX = " + spPredictedX);
			}
			break;
		case UP:
			if(taLastVertical == DOWN && !spSetY) {
				spSetY = true;
				spPredictedY = taLastStopY + OBJECT_HEIGHT / 2 - (500 / step_period) * step_length;
				System.out.println("taLastStopY = " + taLastStopY);
				System.out.println("spPredictedY = " + spPredictedY);
			}
			break;
		case DOWN:
			if(taLastVertical == UP && !spSetY) {
				spSetY = true;
				spPredictedY = taLastStopY + OBJECT_HEIGHT / 2 + (500 / step_period) * step_length;
				System.out.println("taLastStopY = " + taLastStopY);
				System.out.println("spPredictedY = " + spPredictedY);
			}
			break;
		}
	}
	
	private class HorizontalUpdatingTask extends TimerTask {
		public void run() {
			if(!horizontalBoundaryCheck()) {
				return;
			}
			
			if(enableTAP && !taHorizontalBoundaryCheck()) {
				
			}
			
			horizontalMove();
			
			if(enableSPP) {
				if(spInRangeX) {
					if(!spHorizontalPredictedCheck()) {
						System.out.println("exit range x");
						horizontalUpdatingTimer.cancel();
						horizontalUpdatingTimer = new java.util.Timer();
						horizontalUpdatingTimer.scheduleAtFixedRate(new HorizontalUpdatingTask(), 0, step_period);
						spInRangeX = false;
					}
				}
				else {
					if(spHorizontalPredictedCheck()) {
						System.out.println("enter range x");
						horizontalUpdatingTimer.cancel();
						horizontalUpdatingTimer = new java.util.Timer();
						horizontalUpdatingTimer.scheduleAtFixedRate(new HorizontalUpdatingTask(), 0, step_period * 2);
						spInRangeX = true;
					}
				}
			}
		}
		
		private void horizontalMove() {
			if((direction & LEFT) == LEFT) {
				posX -= step_length;
			}
			else if((direction & RIGHT) == RIGHT) {
				posX += step_length;
			}
		}
		
		private boolean horizontalBoundaryCheck() {
			if((direction & LEFT) == LEFT && posX - step_length < LEFT_BOUND) {
				return false;
			}
			else if((direction & RIGHT) == RIGHT && posX + step_length > RIGHT_BOUND - OBJECT_WIDTH) {
				return false;
			}
			
			return true;
		}
		
		private boolean taHorizontalBoundaryCheck() {
			if((direction & LEFT) == LEFT && taLeftFlag && posX - step_length < taLeftBound) {
				return false;
			}
			else if((direction & RIGHT) == RIGHT && taRightFlag && posX + step_length > taRightBound - OBJECT_WIDTH) {
				return false;
			}
			
			return true;
		}
		
		boolean spHorizontalPredictedCheck() {
			final int range = 30;
			
			if((direction & LEFT) == LEFT && spSetX) {
				if(posX + OBJECT_WIDTH / 2 < spPredictedX + range && posX + OBJECT_WIDTH / 2 > spPredictedX - range) {
					return true;
				}
			}
			else if((direction & RIGHT) == RIGHT && spSetX) {
				if(posX - OBJECT_WIDTH / 2 < spPredictedX + range && posX - OBJECT_WIDTH / 2 > spPredictedX - range) {
					return true;
				}
			}
			
			return false;
		}
	}
	
	private class VerticalUpdatingTask extends TimerTask {
		public void run() {
			if(!verticalBoundaryCheck()) {
				return;
			}
			
			if(enableTAP && !taVerticalBoundaryCheck()) {
				return;
			}
			
			verticalMove();
			
			if(enableSPP) {
				if(spInRangeY) {
					if(!spVerticalPredictedCheck()) {
						System.out.println("exit range y");
						verticalUpdatingTimer.cancel();
						verticalUpdatingTimer = new java.util.Timer();
						verticalUpdatingTimer.scheduleAtFixedRate(new VerticalUpdatingTask(), 0, step_period);
						spInRangeY = false;
					}
				}
				else {
					if(spVerticalPredictedCheck()) {
						System.out.println("enter range y");
						verticalUpdatingTimer.cancel();
						verticalUpdatingTimer = new java.util.Timer();
						verticalUpdatingTimer.scheduleAtFixedRate(new VerticalUpdatingTask(), 0, step_period * 2);
						spInRangeY = true;
					}
				}
			}
		}
		
		private void verticalMove() {
			if((direction & UP) == UP) {
				posY -= step_length;
			}
			else if((direction & DOWN) == DOWN) {
				posY += step_length;
			}
		}
		
		private boolean verticalBoundaryCheck() {
			if((direction & UP) == UP && posY - step_length < TOP_BOUND) {
				return false;
			}
			else if((direction & DOWN) == DOWN && posY + step_length > BOTTOM_BOUND - OBJECT_HEIGHT) {
				return false;
			}
			
			return true;
		}
		
		private boolean taVerticalBoundaryCheck() {
			if((direction & UP) == UP && taTopFlag & posY - step_length < taTopBound) {
				return false;
			}
			else if((direction & DOWN) == DOWN && taBottomFlag && posY + step_length > taBottomBound - OBJECT_HEIGHT) {
				return false;
			}
			
			return true;
		}
		
		private boolean spVerticalPredictedCheck() {
			final int range = 30;
			
			if((direction & UP) == UP && spSetY) {
				if(posY + OBJECT_HEIGHT / 2 < spPredictedY + range && posY + OBJECT_HEIGHT / 2 > spPredictedY - range) {
					return true;
				}
			}
			else if((direction & DOWN) == DOWN && spSetY) {
				if(posY - OBJECT_HEIGHT / 2 < spPredictedY + range && posY - OBJECT_HEIGHT / 2 > spPredictedY - range) {
					return true;
				}
			}
			
			return false;
		}
	}
}
