package nl.vu.cs.cn;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import nl.vu.cs.cn.IP.IpAddress;
import nl.vu.cs.cn.TCP.Socket;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

/**
 * The Chat application built on top of the TCP implementation.
 * It creates a "Server" and a "Client" threads. The "Server" thread
 * waits for (e.g. accepts) the connection from the "Client". Each
 * thread uses 2 TCP stacks because of the lack of multiplexing
 * capabilities. One stack is used for reading and one for writing. 
 * 
 * @author Dimo Stoyanov, Plamen Dimitrov
 */

public class Chat extends Activity{

	/* The two threads in which the two TCP instances run */
	Server server;	
	Client client;

	/* Maximum buffer size for reading/writing */
	int bufsize = 128;

	/* GUI elements for text display */
	TextView tv1;
	TextView tv2;


	/* Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		tv1 = (TextView) findViewById(R.id.textView1);
		tv2 = (TextView) findViewById(R.id.textView2);

		tv1.setMovementMethod(new ScrollingMovementMethod());
		tv1.setMovementMethod(new ScrollingMovementMethod());

		/* The ports used for the communication between the TCP stacks*/
		int port = 1234;

		/* The addresses of the four TCP stacks */
		int server_send_addr = 1;
		int server_rcv_addr = 2;
		
		int client_send_addr = 3;
		int client_rcv_addr = 4;

		server = new Server(server_send_addr, server_rcv_addr, port);
		
		client = new Client(server_send_addr, server_rcv_addr, 
				client_send_addr, client_rcv_addr, port);

		new Thread(server).start();
		new Thread(client).start();
		


	}

	/**
	 * Handler for the onClick button event.
	 * Sends the entered text trough the writing socket of the client.
	 */
	public void sendMessage1(View view){
		EditText et = (EditText) findViewById(R.id.editText1);
		byte[] message = et.getText().toString().getBytes();

		if(client.write_socket.write(message, 0, message.length) == -1)
			tv1.append("Error occured during message transamission\n");

		et.setText("");
	}


	/**
	 * Handler for the onClick button event.
	 * Sends the entered text trough the writing socket of the server.
	 */
	public void sendMessage2(View view){
		EditText et = (EditText) findViewById(R.id.editText2);
		byte[] message = et.getText().toString().getBytes();

		if(server.write_socket.write(message, 0, message.length) == -1)
			tv2.append("Error occured during message transmission\n");

		et.setText("");
	}


	/**
	 * Handler for sending messages between the server and client threads and the 
	 * GUI thread.
	 */
	Handler handler = new Handler(){
		public void handleMessage(Message msg){
			String text = (String) msg.obj;
			int dst = msg.arg1;

			if(dst == 1)
				tv1.append(text + '\n');
			else if(dst == 2)
				tv2.append(text + '\n');

		}
	};

	/**
	 * The "Server" for the chat application.
	 */
	public class Server implements Runnable{

		private TCP write_tcp;		// a TCP instance used for writing by the server
		private TCP read_tcp;		// a TCP instance used for reading by the server

		private Socket read_socket;	// a socked used for reading by the server
		private Socket write_socket;// a socket used for writing by the server

		public Server(int s_send_addr, int s_rcv_addr, int port){
			try{
				read_tcp = new TCP(s_rcv_addr);
				write_tcp = new TCP(s_send_addr);

				tv1.append("New TCP stack initialized\n");

				write_socket = write_tcp.socket(port);
				read_socket = read_tcp.socket(port);

				tv1.append("Sockets created\n");


			} catch(IOException e){
				tv1.append("Not able to initialize TCP stack\n exiting...\n");
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e1) {
					/* not needed since there is no other thread 
					 * that can interrupt the sleep */
				}
			}
		}

		public void run() {
			Message msg = new Message();
			byte[] buf = new byte[bufsize];
			int nbytes_read;
			byte[] tmp = null;

			write_socket.accept();
			read_socket.accept();

			msg.obj = "Connection Established\n";
			msg.arg1 = 1;
			handler.sendMessage(msg);

			/* wait to receive a message from the client */
			while((nbytes_read = read_socket.read(buf, 0, bufsize)) !=  -1){

				Message msg1 = new Message();

				try {
					/* Get only the part of the buffer which is
					 * filled by the read */
					tmp = new byte[nbytes_read];
					System.arraycopy(buf, 0, tmp, 0, nbytes_read);
					
					msg1.obj = new String(tmp, "UTF-8");

				} catch (UnsupportedEncodingException e) {
					msg1.obj = "Error during message transmission";
				}
				
				/* Send the message to the GUI thread*/
				msg1.arg1 = 2;
				handler.sendMessage(msg1);
			}
		}
	}


	/**
	 * The "Client" for the chat application
	 */
	private class Client implements Runnable{

		private TCP read_tcp;
		private TCP write_tcp;
		
		private int port;
		
		private int s_send_addr;
		private int s_rcv_addr;

		private Socket read_socket;
		private Socket write_socket;

		public Client(int s_send_addr, int s_rcv_addr, 
				int c_send_addr, int c_rcv_addr, int port){

			try{
				this.port = port;
				
				this.s_send_addr = s_send_addr;
				this.s_rcv_addr = s_rcv_addr;

				read_tcp = new TCP(c_rcv_addr);
				write_tcp = new TCP(c_send_addr);
				tv2.append("New TCP stack initialized\n");

				read_socket = read_tcp.socket();
				write_socket = write_tcp.socket();
				tv2.append("A socket created.\n");

			} catch(IOException e){
				tv1.append("Not able to initialize TCP stack\n exiting...\n");
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e1) {
					/* not needed since there is no other thread 
					 * that can interrupt the sleep */
				}
			}
		}

		public void run() {

			Message msg = new Message();
			byte[] buf = new byte[bufsize];
			int nbytes_read;
			byte[] tmp = null;

			if(read_socket.connect(IpAddress.getAddress("192.168.0." + this.s_send_addr), port) &&
					write_socket.connect(IpAddress.getAddress("192.168.0." + this.s_rcv_addr), port)){


				msg.obj = "Connection Established\n";
				msg.arg1 = 2;
				handler.sendMessage(msg);

				while((nbytes_read = read_socket.read(buf, 0, bufsize)) !=  -1){

					Message msg1 = new Message();

					try {
						/* Get only the part of the buffer which is
						 * filled by the read */
						tmp = new byte[nbytes_read];
						System.arraycopy(buf, 0, tmp, 0, nbytes_read);
						
						msg1.obj = new String(tmp, "UTF-8");

					} catch (UnsupportedEncodingException e) {
						msg1.obj = "Error during message transmission";
					}

					/* Send the message to the GUI thread */
					msg1.arg1 = 1;
					handler.sendMessage(msg1);
				}
			}
		}
	}
}
