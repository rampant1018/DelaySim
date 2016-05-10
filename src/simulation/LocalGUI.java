package simulation;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.PrintWriter;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class LocalGUI extends JFrame implements ActionListener {
	Object object;
	PrintWriter commandSender;
	
	JLabel label;
	java.util.Timer positionUpdatingTimer = null;
	
	JPanel objPanel;
	ObjectScenePanel osp;
	ElapsedTimerLabel etl;
	
	// frame per second
	static final int fps = 50;
	
	public LocalGUI(Object object, PrintWriter commandSender) {
		this.object = object;
		this.commandSender = commandSender;
		
		createAndShowGUI();
		
		TimerTask positionUpdatingTask = new TimerTask() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				label.setText(Integer.toString(object.getPosX()) + ", " + Integer.toString(object.getPosY()));
				osp.repaint();
			}
		};
		positionUpdatingTimer = new java.util.Timer();
		positionUpdatingTimer.scheduleAtFixedRate(positionUpdatingTask, 0, 1000 / fps);
		
		frame.addKeyListener(new ControlListener());
	}
	
	public void close() {
		positionUpdatingTimer.cancel();
		setVisible(false);
		dispose();
	}
	
	private void createAndShowGUI() {
		setTitle("LocalApp");
		setResizable(false);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		
		// Layout setting
		FlowLayout layout = new FlowLayout(FlowLayout.CENTER, 10, 10);
		setLayout(layout);
		
		// Object drawing panel
		osp = new ObjectScenePanel();
		add(osp);
		
		// Elapsed timer label
		etl = new ElapsedTimerLabel();
		frame.add(etl);
		
		// Destination generate button
		JButton btn = new JButton("Generate Destination");
		btn.setActionCommand("generate destination");
		btn.addActionListener(this);
		add(btn);
		
		pack();
		setVisible(true);
	}
	
	class ObjectScenePanel extends JPanel {
		// panel size
		static final int WIDTH = 800;
		static final int HEIGHT = 600;
		
		public ObjectScenePanel() {
			setBackground(Color.white);
			setBorder(BorderFactory.createLineBorder(Color.black));
		}
		
		public Dimension getPreferredSize() {
			return new Dimension(WIDTH, HEIGHT);
		}
		
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			
			g.setColor(Color.RED);
			g.fillRect(object.getPosX(), object.getPosY(), Object.WIDTH, Object.HEIGHT);
			g.setColor(Color.BLACK);
			g.drawRect(object.getPosX(), object.getPosY(), Object.WIDTH, Object.HEIGHT);
		}
	}
	
	class ElapsedTimerLabel extends JLabel {
		long startTime;
		boolean stateRunning;
		
		public ElapsedTimerLabel() {
			setBorder(BorderFactory.createLineBorder(Color.BLACK));
			setPreferredSize(new Dimension(200, 100));
			
			stateRunning = false;
		}
		
		public void start() {
			if(!stateRunning) {
				startTime = System.currentTimeMillis();
				stateRunning = true;
				new Thread(new ElapsedTimerUpdatingTask()).start();
			}
		}
		
		public void stop() {
			if(stateRunning) {
				stateRunning = false;
			}
		}
		
		class ElapsedTimerUpdatingTask implements Runnable {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				while(true) {
					if(!stateRunning) {
						return;
					}
					
					long currentTime = System.currentTimeMillis();
					long diffTime = currentTime - startTime;
					long second = diffTime / 1000;
					long millsecond = diffTime % 1000;
					setText(second + "." + millsecond + " sec");
				}
			}
		}
	}
	
	class ControlListener implements KeyListener {
		@Override
		public void keyTyped(KeyEvent e) {
			// TODO Auto-generated method stub
		}

		@Override
		public void keyPressed(KeyEvent e) {
			// TODO Auto-generated method stub
			switch(e.getKeyCode()) {
			case KeyEvent.VK_W:
				commandSender.println("cd UpOn");
				break;
			case KeyEvent.VK_S:
				commandSender.println("cd DownOn");
				break;
			case KeyEvent.VK_A:
				commandSender.println("cd LeftOn");
				break;
			case KeyEvent.VK_D:
				commandSender.println("cd RightOn");
				break;
			case KeyEvent.VK_O:
				etl.start();
				break;
			case KeyEvent.VK_P:
				etl.stop();
				break;
			}
			commandSender.flush();
		}

		@Override
		public void keyReleased(KeyEvent e) {
			// TODO Auto-generated method stub
			switch(e.getKeyCode()) {
			case KeyEvent.VK_W:
				commandSender.println("cd UpOff");
				break;
			case KeyEvent.VK_S:
				commandSender.println("cd DownOff");
				break;
			case KeyEvent.VK_A:
				commandSender.println("cd LeftOff");
				break;
			case KeyEvent.VK_D:
				commandSender.println("cd RightOff");
				break;
			}
			commandSender.flush();
		}
	}
}
