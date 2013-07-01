package nl.vu.cs.cn.test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import nl.vu.cs.cn.IP.IpAddress;
import nl.vu.cs.cn.TCP;
import nl.vu.cs.cn.TCP.Socket;
import junit.framework.TestCase;
/*
 * Tests the case when the length of the sent packet is less than the MAXLEN
 * waited by the read
 * */
public class BasicTests extends TestCase {

	String client_resutl = "";

	private class Client implements Runnable{

		private TCP tcp;

		private int port;

		private int dst_address;

		private Socket socket;

		int bufsize = 128;

		public Client(int our_address, int their_address, int port){

			try{
				this.port = port;
				this.dst_address = their_address;


				tcp = new TCP(our_address);
				socket = tcp.socket();

			} catch(IOException e){

			}
		}

		public void run() {
			int nbytes_read = 0;
			byte[] buf = new byte[bufsize];
			byte[] tmp = null;

			if(socket.connect(IpAddress.getAddress("192.168.0." + this.dst_address), this.port)){
				
				if((nbytes_read = socket.read(buf, 0, bufsize)) !=  0){

					tmp = new byte[nbytes_read];
					System.arraycopy(buf, 0, tmp, 0, nbytes_read);

					try {
						client_resutl += new String(tmp, "UTF-8");
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	public class Server implements Runnable{
		private Socket socket;

		byte[] message;

		public Server(int our_address, int port, String m){
			try{
				TCP tcp = new TCP(our_address);

				this.socket = tcp.socket(port);

				this.message = m.getBytes();

			} catch(IOException e){
				e.printStackTrace();
			}
		}

		public void run() {
			this.socket.accept();
			System.out.println("TEST: connection established server");


			this.socket.write(message, 0, message.length);

		}
	}

	public void testReadShort(){
		String message_to_send = "some not very long message";

		Server s = new Server(1, 6543, message_to_send);
		Client c = new Client(2, 1, 6543);

		Thread t1 = new Thread(s);
		Thread t2 = new Thread(c);

		t1.start();
		t2.start();

		try {
			t1.join();
			t2.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		assertEquals(message_to_send, client_resutl);


	}



}
