package simulation;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.PrintWriter;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

public class LocalGUI {
	Object object;
	PrintWriter commandSender;
	
	JFrame frame;
	JLabel label;
	java.util.Timer positionUpdatingTimer = null;
	
	JPanel objPanel;
	ObjectScenePanel osp;
	
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
		frame.setVisible(false);
		frame.dispose();
	}
	
	private void createAndShowGUI() {
		frame = new JFrame("LocalApp");
		frame.setResizable(false);
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		
		// Layout setting
		FlowLayout layout = new FlowLayout(FlowLayout.CENTER, 10, 10);
		frame.setLayout(layout);
		
		// Object drawing panel
		osp = new ObjectScenePanel();
		frame.add(osp);
		
		label = new JLabel();
		label.setText(Integer.toString(object.getPosX()));
		label.setPreferredSize(new Dimension(200, 200));
		frame.add(label);
		
		frame.pack();
		frame.setVisible(true);
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
