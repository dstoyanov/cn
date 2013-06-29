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
 * The Chat application built on top of the TCP implementation
 * 
 * @author Dimo Stoyanov, Plamen Dimitrov
 */

public class Chat extends Activity{

	/* The two threads in which the two TCP instances run */
	Server server;	
	Client client;

	/* The ports used for the communication between the TCP stacks*/
	int port1 = 1234;
	int port2 = 4321;

	/* The addresses of the two stacks (e.g. 192.168.0.1 and 192.168.0.2)*/
	int addr1 = 1;
	int addr2 = 2;

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
		tv2.setMovementMethod(new ScrollingMovementMethod());
		

		server = new Server(addr1, port1);
		client = new Client(addr2, addr1, port1);

		new Thread(server).start();
		new Thread(client).start();

	}

	/**
	 * Handler for the onClick button event.
	 * Sends the entered text trough the socket of the client.
	 */
	public void sendMessage1(View view){
		EditText et = (EditText) findViewById(R.id.editText1);
		byte[] message = et.getText().toString().getBytes();

		if(client.write_socket.write(message, 0, message.length) == -1)
			tv1.append("Error occured during message transamission1\n");

		et.setText("");
	}


	/**
	 * Handler for the onClick button event.
	 * Sends the entered text trough the socket of the server.
	 */
	public void sendMessage2(View view){
		EditText et = (EditText) findViewById(R.id.editText2);
		byte[] message = et.getText().toString().getBytes();

		//		try {
		//			String m = new String(message, "UTF-8");
		//			System.out.println("Message sent: " + m + " size: " + message.length);
		//			System.out.println("Message sent buf: " + message);
		//		} catch (UnsupportedEncodingException e) {
		//			// TODO Auto-generated catch block
		//			e.printStackTrace();
		//		}


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

			if(dst == 1){
				tv1.append(text + '\n');
			}
			else if(dst == 2){
				tv2.append(text + '\n');
			}

		}
	};

	/**
	 * The "Server" for the chat application.
	 */
	public class Server implements Runnable{

		private TCP tcp;
		private Socket read_socket;
		private Socket write_socket;

		public Server(int addr, int port1){
			try{
				tcp = new TCP(addr);
				tv1.append("New TCP stack created (192.168.0." + addr + ").\n");

				read_socket = tcp.socket(port1 + 1);
				write_socket = tcp.socket(port1);

				tv1.append("A socket listening on port " + port1 + " created.\n");


			} catch(IOException e){

			}
		}

		public void run() {
			Message msg = new Message();
			byte[] buf = new byte[bufsize];
			int nbytes_read;
			byte[] tmp = null;

			write_socket.accept();
			read_socket.accept();
			

			System.out.println("SERVER SOCKETS: " + read_socket.tcb.tcb_our_port + " " +
					read_socket.tcb.tcb_their_port + "\n" +
					write_socket.tcb.tcb_our_port + " " + write_socket.tcb.tcb_their_port);


			msg.obj = "Connection Established\n";
			msg.arg1 = 1;
			handler.sendMessage(msg);

			//			while(true){}

			while((nbytes_read = read_socket.read(buf, 0, bufsize)) !=  -1){

				Message msg1 = new Message();

				try {
					tmp = new byte[nbytes_read];
					System.arraycopy(buf, 0, tmp, 0, nbytes_read);
					msg1.obj = new String(tmp, "UTF-8");

				} catch (UnsupportedEncodingException e) {
					msg1.obj = "Error during message transmission";
				}

				msg1.arg1 = 2;
				handler.sendMessage(msg1);
			}

		}
	}


	/**
	 * The "Client" for the chat application
	 */
	private class Client implements Runnable{

		private TCP tcp;
		private Socket read_socket;
		private Socket write_socket;
		private int dstAddress;

		public Client(int srcAddress, int dstAddress, int port1){

			try{

				tcp = new TCP(srcAddress);
				tv2.append("New TCP stack created (192.168.0." + srcAddress + ").\n");

				read_socket = tcp.socket();
				write_socket = tcp.socket();
				tv2.append("A socket created.\n");

			} catch(IOException e){

			}
			this.dstAddress = dstAddress;
		}

		public void run() {

			Message msg = new Message();
			byte[] buf = new byte[bufsize];
			int nbytes_read;
			byte[] tmp = null;

			if(read_socket.connect(IpAddress.getAddress("192.168.0." + dstAddress), port1) &&
					write_socket.connect(IpAddress.getAddress("192.168.0." + dstAddress), port1 + 1)){

				msg.obj = "Connection Established\n";
				msg.arg1 = 2;
				handler.sendMessage(msg);

//				System.out.println("CLIENT SOCKETS: " + read_socket.tcb.tcb_our_port + " " +
//						read_socket.tcb.tcb_their_port + "\n" +
//						write_socket.tcb.tcb_our_port + " " + write_socket.tcb.tcb_their_port);
				
				while((nbytes_read = read_socket.read(buf, 0, bufsize)) !=  -1){

					Message msg1 = new Message();

					try {
						tmp = new byte[nbytes_read];
						System.arraycopy(buf, 0, tmp, 0, nbytes_read);
						msg1.obj = new String(tmp, "UTF-8");

					} catch (UnsupportedEncodingException e) {
						msg1.obj = "Error during message transmission";
					}

					msg1.arg1 = 1;
					handler.sendMessage(msg1);
				}
			} else{
				msg.obj = "Could not establish a connection";
				msg.arg1 = 2;
				handler.sendMessage(msg);
			}
		}
	}
}
