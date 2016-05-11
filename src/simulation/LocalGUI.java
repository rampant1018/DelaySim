package simulation;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.PrintWriter;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

public class LocalGUI extends JFrame implements ActionListener {
	Object object;
	PrintWriter commandSender;
	
	java.util.Timer positionUpdatingTimer = null;
	
	JPanel objPanel;
	ObjectScenePanel osp;
	StatusPanel sp;
	
	// frame per second
	static final int fps = 50;

	
	public LocalGUI(Object object, PrintWriter commandSender) {
		this.object = object;
		this.commandSender = commandSender;
		
		setUIFont (new javax.swing.plaf.FontUIResource("Serif", Font.PLAIN, 20));
		createAndShowGUI();
		
		TimerTask positionUpdatingTask = new TimerTask() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				osp.repaint();
			}
		};
		positionUpdatingTimer = new java.util.Timer();
		positionUpdatingTimer.scheduleAtFixedRate(positionUpdatingTask, 0, 1000 / fps);
	}
	
	public void close() {
		sp.close();
		positionUpdatingTimer.cancel();
		setVisible(false);
		dispose();
	}
	
	private void createAndShowGUI() {
		setTitle("LocalApp");
		setResizable(false);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		
		setFont(new Font("Arial", 0, 20));
		
		// Layout setting
		FlowLayout layout = new FlowLayout(FlowLayout.CENTER, 10, 10);
		setLayout(layout);
		
		// Object drawing panel
		osp = new ObjectScenePanel();
		add(osp);
		
		// Right container(panel)
		JPanel rightPanel = new JPanel();
		rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
		add(rightPanel);
		
		// Status panel
		sp = new StatusPanel();
		rightPanel.add(sp);

		// Destination generate button
		JButton btn = new JButton("Generate Destination");
		btn.setActionCommand("generate destination");
		btn.addActionListener(this);
		rightPanel.add(btn);

		pack();
		setVisible(true);
	}
	
	class ObjectScenePanel extends JPanel implements MouseListener, KeyListener {
		// panel size
		static final int WIDTH = 800;
		static final int HEIGHT = 600;
		
		// destination area
		static final int DA_WIDTH = 50;
		static final int DA_HEIGHT = 50;
		boolean daEnable;
		int daX, daY;
		
		public ObjectScenePanel() {
			daEnable = false; // do not show destination area in the beginning
			
			// enable focus on panel
			setFocusable(true);
			addMouseListener(this);
			
			// control listener
			addKeyListener(this);
			
			setBackground(Color.white);
			setBorder(BorderFactory.createLineBorder(Color.black));
		}
		
		public Dimension getPreferredSize() {
			return new Dimension(WIDTH, HEIGHT);
		}
		
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			
			// paint object
			g.setColor(Color.RED);
			g.fillRect(object.getPosX(), object.getPosY(), Object.WIDTH, Object.HEIGHT);
			g.setColor(Color.BLACK);
			g.drawRect(object.getPosX(), object.getPosY(), Object.WIDTH, Object.HEIGHT);
			
			// paint destination area
			if(daEnable) {
				g.setColor(Color.CYAN);
				g.drawRect(daX, daY, DA_WIDTH, DA_HEIGHT);
			}
		}
		
		public void setDaX(int daX) {
			this.daX = daX;
		}
		
		public void setDaY(int daY) {
			this.daY = daY;
		}
		
		public int getDaX() {
			return daX;
		}
		public int getDaY() {
			return daY;
		}
		
		public void enableDA() {
			daEnable = true;
		}
		
		public void disableDA() {
			daEnable = false;
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			// TODO Auto-generated method stub
			requestFocusInWindow();
		}

		@Override
		public void mousePressed(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mouseExited(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}
		
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
				sp.etl.start();
				break;
			case KeyEvent.VK_P:
				sp.etl.stop();
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
	
	class StatusPanel extends JPanel {
		boolean isRunning = true;
		
		ElapsedTimerLabel etl;
		JLabel daXLabel, daYLabel;
		JLabel objectXLabel, objectYLabel;
		
		public StatusPanel() {
			setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK), "System Status"));
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			
			etl = new ElapsedTimerLabel();
			daXLabel = new JLabel();
			daYLabel = new JLabel();
			objectXLabel = new JLabel();
			objectYLabel = new JLabel();
			
			add(etl);
			add(new JLabel("DA_WIDTH: " + osp.DA_WIDTH));
			add(new JLabel("DA_HEIGHT: " + osp.DA_HEIGHT));
			add(daXLabel);
			add(daYLabel);
			add(objectXLabel);
			add(objectYLabel);
			
			new Thread(new StatusUpdatingTask()).start();
		}
		
		public void close() {
			etl.stop();
			isRunning = false;
		}
		
		class StatusUpdatingTask implements Runnable {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				while(true) {
					if(!isRunning) {
						return;
					}
					daXLabel.setText("daX: " + Integer.toString(osp.getDaX()));
					daYLabel.setText("daY: " + Integer.toString(osp.getDaY()));
					objectXLabel.setText("objectX: " + Integer.toString(object.getPosX()));
					objectYLabel.setText("objectY: " + Integer.toString(object.getPosY()));
				}
			}
		}
		
		class ElapsedTimerLabel extends JLabel {
			long startTime;
			boolean stateRunning;
			
			public ElapsedTimerLabel() {
				setText("0.0 sec");
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
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		if(e.getActionCommand().equals("generate destination")) {
			System.out.println("click button");
		}
	}
	
	private static void setUIFont (javax.swing.plaf.FontUIResource f){
		java.util.Enumeration keys = UIManager.getDefaults().keys();
		while (keys.hasMoreElements()) {
			java.lang.Object key = keys.nextElement();
			java.lang.Object value = UIManager.get (key);
			if (value != null && value instanceof javax.swing.plaf.FontUIResource) {
				UIManager.put (key, f);
			}
		}
	} 
}
