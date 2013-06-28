package nl.vu.cs.cn.test;

import java.io.IOException;

import nl.vu.cs.cn.IP.IpAddress;
import nl.vu.cs.cn.TCP;
import nl.vu.cs.cn.TCP.Socket;
import nl.vu.cs.cn.TcpPacket;
import junit.framework.TestCase;

public class TCPTest extends TestCase {
	public void testSend() throws IOException, InterruptedException{
		TCP t1 = new TCP(10);
		boolean result;
		byte[] rcv_buf = new byte[3];

		Socket s1 = t1.socket();

		System.out.println("Sockets created");

		Test1 test1 = new Test1();
		new Thread(test1).start();


		result = s1.connect(IpAddress.getAddress("192.168.0." + 11), 1234);
		
		System.out.println("Connect: Result " + result);
		Thread.sleep(4000);
		int n = s1.read(rcv_buf, 0, 3);
		
		System.out.print("Reading Buff ");
		for(int i = 0; i < 3; i++){
			System.out.print(rcv_buf[i] + "  ");
		}
		
		System.out.println("Number bytes read " + n);
	}

	public class Test1 implements Runnable{

		@Override
		public void run() {
			// TODO Auto-generated method stub

			try {
				TCP t2 = new TCP(11);
				//create a server socket
				Socket s2 = t2.socket(1234);
				s2.accept();
				System.out.println("connected");

				byte[] buf = {(byte) 0x01, (byte) 0x02, (byte) 0x03};

				System.out.print("Writing Buff ");
				for(int i = 0; i < 3; i++){

					System.out.print(buf[i] + "  ");
				}
				
				int n =	s2.write(buf, 0, 3);
				System.out.println("number bytes written " + n);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			};

		}

	}
	
//	public void testSend1() throws IOException, InterruptedException{
//		TCP t1 = new TCP(10);
//		TCP t2 = new TCP(11);
//		TcpPacket p = new TcpPacket();
//		
//		Socket s1 = t1.socket();
//		Socket s2 = t2.socket();
//		byte[] send_buf = {(byte) 1, (byte) 2, (byte) 3};
//		
//		byte[] rcv_buf = new byte[5];
//		
//		s1.send_tcp_packet(11, send_buf, 3, (short)1234, (short)5678, 0, 0, (byte)0x20);
//		
//		s2.recv_tcp_packet(p, false);
//		
//		System.out.println("length " + p.data.length + " " + p.src_port);
//	}
}
