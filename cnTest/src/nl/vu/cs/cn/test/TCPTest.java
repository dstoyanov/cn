package nl.vu.cs.cn.test;

import java.io.IOException;

import nl.vu.cs.cn.IP.IpAddress;
import nl.vu.cs.cn.TCP;
import nl.vu.cs.cn.TCP.Socket;
import junit.framework.TestCase;

public class TCPTest extends TestCase {
	public void testSend() throws IOException, InterruptedException{
		TCP t1 = new TCP(10);
		

		byte[] rcv_buf = new byte[3];
		
		Socket s1 = t1.socket();
		
		System.out.println("Sockets created");
		
		
		
		Test1 test1 = new Test1();
		new Thread(test1).start();
		
		s1.connect(IpAddress.getAddress(11),1234);
		
		Thread.sleep(4000);
		s1.read(rcv_buf, 0, 0);
		
		for(int i = 0; i < 3; i++){
			System.out.println(rcv_buf[i]);
		}

		

	}
	
	public class Test1 implements Runnable{

		@Override
		public void run() {
			// TODO Auto-generated method stub
			
			try {
				TCP t2 = new TCP(11);
				Socket s2 = t2.socket();
				s2.accept();
				System.out.println("connected");
				byte[] buf = {(byte) 1, (byte) 2, (byte) 3};
				s2.write(buf, 0, 3);
				
					} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			};
			
		}
		
	}
}
	