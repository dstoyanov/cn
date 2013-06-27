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

public class Chat extends Activity{

	Server server;
	Client client;

	int port1 = 1234;
	int port2 = 4321;

	int addr1 = 1;
	int addr2 = 2;

	int bufsize = 100;

	TextView tv1;
	TextView tv2;


	/** Called when the activity is first created. */
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

	public void sendMessage1(View view){
		EditText et = (EditText) findViewById(R.id.editText1);
//		byte[] message = et.getText().toString().getBytes();

		//		client.write_socket.write(message, 0, message.length);
		et.setText("text");
	}

	public void sendMessage2(View view){
		EditText et = (EditText) findViewById(R.id.editText2);
//		byte[] message = et.getText().toString().getBytes();

		//		server.write_socket.write(message, 0, message.length);
		et.setText("text");
	}

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

			System.out.println("CONN3");
			read_socket.accept();
			write_socket.accept();
			
			System.out.println("CONN2");

//			msg.obj = "Connection Established\n";
//			msg.arg1 = 1;
//			handler.sendMessage(msg);
			
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
			System.out.println("CONN1");

//			msg.obj = "Connection Established\n";
//			msg.arg1 = 2;
//			handler.sendMessage(msg);

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
