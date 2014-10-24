import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.quickconnectfamily.json.JSONException;
import org.quickconnectfamily.json.JSONInputStream;
import org.quickconnectfamily.json.JSONOutputStream;

public class SandboxServer {

	public static void main(String[] args) {
		UserBean aUser = new UserBean();
		String userInput = "";
		while (true) {
			try {
				// a socket opened on the specified port
				ServerSocket aListeningSocket = new ServerSocket(9291);
				// wait for a connection
				System.out.println("Waiting for client connection request.");
				Socket clientSocket = aListeningSocket.accept();
				// setup the JSON streams for later use.
				JSONInputStream inFromClient = new JSONInputStream(
						clientSocket.getInputStream());
				JSONOutputStream outToClient = new JSONOutputStream(
						clientSocket.getOutputStream());
				// read until the client closes
				// the connection.
				while (true) {
					System.out.println("Waiting for a message from the client.");
					
					BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
					userInput = in.readLine();
					
					HashMap aMap = (HashMap) inFromClient.readObject();
					aUser.setUname((String)aMap.get("uname"));
					aUser.setPword((String)aMap.get("pword"));
					
					switch (userInput){
						case "1":
							login(aUser, outToClient, clientSocket);
							break;
						case "2":
							createNewUser(aUser, clientSocket);
					}
				}
			} catch (Exception e) {
				//e.printStackTrace();
			}
		}

	}
	
	private static void login(UserBean aUser, JSONOutputStream outToClient, Socket clientSocket) throws JSONException, IOException {
		Session session = HibernateUtilSingleton.getSessionFactory().getCurrentSession();
		
		session.beginTransaction();
		
		Query singleUserQuery = session.createQuery("select u from UserBean as u where u.uname='" + aUser.getUname() + "'");
		UserBean queriedUser = (UserBean)singleUserQuery.uniqueResult();
		
		if (queriedUser == null) {
			PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
	        out.println("User does not exist");
	        session.close();
			return;
		}
		else if (!queriedUser.getPword().equals(aUser.getPword())) {
			PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
	        out.println("Incorrect password");
	        session.close();
			return;
		}
		PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
		out.println("Successfully logged in!");
		
		session.close();
	}
	
	private static void createNewUser(UserBean aUser, Socket clientSocket) throws IOException {
		Session session = HibernateUtilSingleton.getSessionFactory().getCurrentSession();
		
		Transaction transaction = session.beginTransaction();
		
		Query singleUserQuery = session.createQuery("select u from UserBean as u where u.uname='" + aUser.getUname() + "'");
		UserBean queriedUser = (UserBean)singleUserQuery.uniqueResult();
		
		if (queriedUser != null) {
			PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
			out.println("User already exists");
			session.close();
			return;
		}
		
		session.save(aUser);
		transaction.commit();
		
		PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
		out.println("User " + aUser.getUname() + " successfully created!");
		
		session.close();
	}
}