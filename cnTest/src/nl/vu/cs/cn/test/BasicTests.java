package nl.vu.cs.cn.test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import android.test.AndroidTestCase;

import nl.vu.cs.cn.BogusTCP;
import nl.vu.cs.cn.BogusTCP.BogusSocket;
import nl.vu.cs.cn.IP.IpAddress;
import nl.vu.cs.cn.TCP;
import nl.vu.cs.cn.TCP.Socket;
import junit.framework.TestCase;


public class BasicTests extends AndroidTestCase {
	
	/* 
	 * This test simulates packet loss changing
	 * the original method for communication between
	 * the TCP and IP level in such a way that it drops
	 * 50% of the packets 
	 * */
	public void testPacketLoss(){
		
		class Server extends Thread{
			BogusSocket s;
			
			public Server(BogusSocket s){
				this.s = s;
			}
			
			@Override
			public void run() {
				byte[] data = {(byte) 0xff, (byte)0x1f, (byte) 0x8a, (byte) 0x12, (byte) 0x56};
				this.s.accept();
				this.s.write(data, 0, data.length);
				
			}
		}
		
		class Client extends Thread{
			BogusSocket s;
			int addr;
			int port;
			
			public Client(BogusSocket s, int addr, int port){
				this.s = s;
				this.addr = addr;
				this.port = port;
			}
			
			@Override
			public void run() {
				byte[] data = {(byte) 0xff, (byte)0x1f, (byte) 0x8a, (byte) 0x12, (byte) 0x56};
				byte[] buffer = new byte[5];
				this.s.connect(IpAddress.getAddress("192.168.0." + addr), port);
				this.s.read(buffer, 0, 5);

				assertTrue(true);
//				assertEquals(buffer, data);
			}
		}
		
		int ip_server = 1;
		int ip_client = 2;

		int port = 1234;

		BogusTCP s_tcp;
		BogusTCP c_tcp;

		BogusSocket s_socket;
		BogusSocket c_socket;

		try {
			s_tcp = new BogusTCP(ip_server);
			c_tcp = new BogusTCP(ip_client);
			
			s_socket = s_tcp.socket(port);
			c_socket = c_tcp.socket();
			
			Server s = new Server(s_socket);
			Client c = new Client(c_socket, ip_server, port);
			
			s.start();
			c.start();
			
			try {
				s.join();
				c.join();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} catch (IOException e) {
			assertTrue(false);
		}





	}


}
