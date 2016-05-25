package remote;

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
	static final int defaultPositionSendingDelay = 500; // position feedback delay (ms)
	
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