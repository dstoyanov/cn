package nl.vu.cs.cn;

import java.io.IOException;

import nl.vu.cs.cn.IP.IpAddress;
import nl.vu.cs.cn.TCP.Socket;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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

	int bufsize = 100;

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
//		byte[] message = et.getText().toString().getBytes();

		//		client.write_socket.write(message, 0, message.length);
		et.setText("text");
	}


	/**
	 * Handler for the onClick button event.
	 * Sends the entered text trough the socket of the server.
	 */
	public void sendMessage2(View view){
		EditText et = (EditText) findViewById(R.id.editText2);
//		byte[] message = et.getText().toString().getBytes();

		//		server.write_socket.write(message, 0, message.length);
		et.setText("text");
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
				tv1.append(text);
			else if(dst == 2)
				tv2.append(text);

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

			write_socket.accept();
			read_socket.accept();

			
			msg.obj = "Connection Established\n";
			msg.arg1 = 1;
			handler.sendMessage(msg);
			
			while(true){}
			//			
			//			byte[] buf = new byte[bufsize];
			//			
			//			while(true){
			//				read_socket.read(buf, 0, bufsize);
			//				this.tv.append(buf.toString());
			//			}
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
				tv2.append("New TCP stack created (192.168.0." + dstAddress + ").\n");

				read_socket = tcp.socket();
				write_socket = tcp.socket();
				tv2.append("A socket created.\n");

			} catch(IOException e){

			}
			this.dstAddress = dstAddress;
		}

		public void run() {

			Message msg = new Message();

			read_socket.connect(IpAddress.getAddress("192.168.0." + dstAddress), port1);
			write_socket.connect(IpAddress.getAddress("192.168.0." + dstAddress), port1 + 1);

			msg.obj = "Connection Established\n";
			msg.arg1 = 2;
			handler.sendMessage(msg);

			while(true){}
			//			tv.append("Connection established.\n");
			//			
			//			byte[] buf = new byte[bufsize];
			//			
			//			
			//			//TODO socket.isConnected?
			//			while(true){
			//				read_socket.read(buf, 0, bufsize);
			//
			//				this.tv.append(buf.toString());
			//			}
		}
	}
}
