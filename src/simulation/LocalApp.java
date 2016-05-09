package simulation;

import java.io.*;
import java.net.*;

public class LocalApp {

	static final int portNumber = 5678;
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		try {
			ServerSocket commandSvrSocket = new ServerSocket(portNumber);
			Socket commandSocket = commandSvrSocket.accept();
			System.out.println("Accepted connection");
			
			BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
			PrintWriter commandWriter = new PrintWriter(commandSocket.getOutputStream(), true);
			
			while(true) {
				String input = stdIn.readLine();
				System.out.println(input);
				
				input += '\n';
				commandWriter.print(input);
				commandWriter.flush();
				if(input.equals("exit\n")) {
					break;
				}				
			}
			commandWriter.close();
			commandSocket.close();
			commandSvrSocket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
