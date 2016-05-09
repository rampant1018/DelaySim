package simulation;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.io.*;
import java.net.*;
import java.util.TimerTask;

import javax.swing.JFrame;
import javax.swing.JLabel;

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
				if(input.equals("exit\n")) {
					break;
				}				
			}
			System.out.println("closed connection");
			
			lgui.close();
			sr.close();
			commandWriter.close();
			commandSocket.close();
			commandSvrSocket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

class Object {
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
	
	/** A minimal constructor */
	public SceneReceiver(String hostname, int rtpLocalPortNumber, int rtpRemotePortNumber, Object object) {
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
		object.setPosX(Integer.parseInt(strPos));
		//System.out.println("RTP received: " + new String(data));
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

class LocalGUI {
	Object object;
	PrintWriter commandSender;
	
	JFrame frame;
	JLabel label;
	java.util.Timer positionUpdatingTimer = null;
	
	public LocalGUI(Object object, PrintWriter commandSender) {
		this.object = object;
		this.commandSender = commandSender;
		
		createAndShowGUI();
		
		TimerTask positionUpdatingTask = new TimerTask() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				label.setText(Integer.toString(object.getPosX()));
			}
		};
		positionUpdatingTimer = new java.util.Timer();
		positionUpdatingTimer.scheduleAtFixedRate(positionUpdatingTask, 0, 5);
		
		frame.addKeyListener(new MyKeyListener());
	}
	
	public void close() {
		positionUpdatingTimer.cancel();
		frame.dispose();
	}
	
	private void createAndShowGUI() {
		frame = new JFrame("LocalApp");
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		
		label = new JLabel();
		label.setText(Integer.toString(object.getPosX()));
		label.setPreferredSize(new Dimension(200, 200));
		frame.add(label);
		
		frame.pack();
		frame.setVisible(true);
	}
	
	class MyKeyListener implements KeyListener {
		@Override
		public void keyTyped(KeyEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void keyPressed(KeyEvent e) {
			// TODO Auto-generated method stub
			switch(e.getKeyCode()) {
			case KeyEvent.VK_W:
				commandSender.println("UpOn");
				break;
			case KeyEvent.VK_S:
				commandSender.println("DownOn");
				break;
			case KeyEvent.VK_A:
				commandSender.println("LeftOn");
				break;
			case KeyEvent.VK_D:
				commandSender.println("RightOn");
				break;
			}
			commandSender.flush();
		}

		@Override
		public void keyReleased(KeyEvent e) {
			// TODO Auto-generated method stub
			switch(e.getKeyCode()) {
			case KeyEvent.VK_W:
				commandSender.println("UpOff");
				break;
			case KeyEvent.VK_S:
				commandSender.println("DownOff");
				break;
			case KeyEvent.VK_A:
				commandSender.println("LeftOff");
				break;
			case KeyEvent.VK_D:
				commandSender.println("RightOff");
				break;
			}
			commandSender.flush();
		}
	}
}