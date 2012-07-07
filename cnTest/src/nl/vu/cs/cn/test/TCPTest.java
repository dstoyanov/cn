package nl.vu.cs.cn.test;

import java.io.IOException;

import nl.vu.cs.cn.TCP;
import nl.vu.cs.cn.TCP.Socket;
import junit.framework.TestCase;

public class TCPTest extends TestCase {
	public void testSend() throws IOException{
		TCP t1 = new TCP(10);
		byte[] buf = {};
		
		
		Socket s = t1.socket();
		s.send_tcp_packet(1677830336, buf, (short)34659, (short)23, 0, 0, (byte) 0x02);
		
	}
}
