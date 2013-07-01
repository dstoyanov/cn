package nl.vu.cs.cn.test;

import java.io.IOException;
import java.util.Random;

import android.test.AndroidTestCase;
import android.test.MoreAsserts;

import nl.vu.cs.cn.BogusTCP;
import nl.vu.cs.cn.BogusTCP.BogusSocket;
import nl.vu.cs.cn.IP.IpAddress;
import nl.vu.cs.cn.TCP;
import nl.vu.cs.cn.TCP.Socket;
import nl.vu.cs.cn.TCPPacket;


public class BasicTests extends AndroidTestCase {

	//	/*
	//	 * Tests if more then one port can be created on a single TCP stack
	//	 */
	//	public void testSingletonPort(){
	//		try {
	//			TCP tcp = new TCP(10);
	//			tcp.socket();
	//			Socket s2 = tcp.socket();
	//
	//			if(s2 == null){
	//				System.out.println("ASSERT: NULL");
	//			}
	//
	//			assertEquals(s2, null);
	//
	//		} catch (IOException e) {
	//			fail("Failed to initialize a TCP stack");
	//		}
	//
	//	}





	//	/*
	//	 * Tests a lost SYN_ACK packet during the 3-way handshake
	//   * It should be retransmit and normal connection should be established
	//	 * */
	//	public void testLostSYNACK(){
	//		class Server extends Thread{
	//			BogusSocket s;
	//
	//			public Server(BogusSocket s){
	//				this.s = s;
	//			}
	//
	//			@Override
	//			public void run() {
	//				this.s.loss_synack_accept();
	//
	//
	//			}
	//		}
	//
	//		class Client extends Thread{
	//			BogusSocket s;
	//			int addr;
	//			int port;
	//
	//			public Client(BogusSocket s, int addr, int port){
	//				this.s = s;
	//				this.addr = addr;
	//				this.port = port;
	//			}
	//
	//			@Override
	//			public void run() {
	//
	//
	//				if( this.s.connect(IpAddress.getAddress("192.168.0." + addr), port) ){ 
	//					//connection established
	//					assertTrue(true);
	//
	//
	//				}
	//			}
	//		}
	//
	//		try {
	//			BogusTCP s_tcp = new BogusTCP(10);
	//			BogusTCP c_tcp = new BogusTCP(11);
	//
	//			int port = 1234;
	//
	//			BogusSocket s_socket = s_tcp.socket(port);
	//			BogusSocket c_socket = c_tcp.socket();
	//
	//			Server s = new Server(s_socket);
	//			Client c = new Client(c_socket, 10, port);
	//
	//			s.start();
	//			c.start();
	//
	//			try {
	//				s.join();
	//				c.join();
	//			} catch (InterruptedException e) {
	//				e.printStackTrace();
	//			}
	//
	//		} catch (IOException e) {
	//			fail("Unable to initialize a socket");
	//		}
	//
	//	}

//	/*
//	 * The MAXLEN parameter of the read method
//	 * is larger than the size of the buffer.
//	 * read should get the buffer and return 
//	 * its size 
//	 *
//	 * */
//	public void testReadLessThenExpected(){
//
//		class Server extends Thread{
//			Socket s;
//			byte[] buffer;
//
//			public Server(Socket s, byte[] buffer){
//				this.s = s;
//				this.buffer = buffer;
//			}
//
//			@Override
//			public void run() {
//				this.s.accept();
//
//				if(this.s.write(this.buffer, 0, this.buffer.length) != this.buffer.length)
//					fail("Unable to write the data");
//
//			}
//		}
//
//		class Client extends Thread{
//			Socket s;
//			int addr;
//			int port;
//			byte[] buffer = new byte[100];
//
//			public Client(Socket s, int addr, int port, byte[] buffer){
//				this.s = s;
//				this.addr = addr;
//				this.port = port;
//				this.buffer = buffer;
//			}
//
//			@Override
//			public void run() {
//				byte[] data = new byte[100];
//				
//				if( this.s.connect(IpAddress.getAddress("192.168.0." + addr), port) ){ 
//					int nbytes = this.s.read(data, 0, 200);
//
//				if(nbytes == 100)
//					MoreAsserts.assertEquals(buffer, data);
//				}
//			}		
//
//		}
//
//		try {
//			TCP s_tcp = new TCP(10);
//			TCP c_tcp = new TCP(11);
//
//			int port = 1234;
//
//			Socket s_socket = s_tcp.socket(port);
//			Socket c_socket = c_tcp.socket();
//
//			Random generator = new Random();
//
//			byte[] buffer = new byte[100];
//			generator.nextBytes(buffer);
//
//			Server s = new Server(s_socket, buffer);
//			Client c = new Client(c_socket, 10, port, buffer);
//
//			s.start();
//			c.start();
//
//			try {
//				s.join();
//				c.join();
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//
//		} catch (IOException e) {
//			fail("Unable to initialize a socket");
//		}
//
//	}





	//	/*
	//	 * The length parameter of the write is larger than 
	//	 * the size of the buffer
	//	 * */
	//	public void testWriteLenBufferSizeMismatch2(){
	//
	//		class Server extends Thread{
	//			Socket s;
	//			byte[] buffer;
	//
	//			public Server(Socket s, byte[] buffer){
	//				this.s = s;
	//				this.buffer = buffer;
	//			}
	//
	//			@Override
	//			public void run() {
	//				this.s.accept();
	//				
	//				if(this.s.write(this.buffer, 0, 120) != this.buffer.length)
	//					fail("Unable to write the data");
	//
	//			}
	//		}
	//
	//		class Client extends Thread{
	//			Socket s;
	//			int addr;
	//			int port;
	//			byte[] buffer = new byte[100];
	//
	//			public Client(Socket s, int addr, int port, byte[] buffer){
	//				this.s = s;
	//				this.addr = addr;
	//				this.port = port;
	//				this.buffer = buffer;
	//			}
	//
	//			@Override
	//			public void run() {
	//				byte[] data = new byte[100];
	//				byte[] tmp = new byte[100];
	//				
	//				int nbytes;
	//				int total_nbytes = 0;
	//				
	//				if( this.s.connect(IpAddress.getAddress("192.168.0." + addr), port) ){ 
	//
	//					while(total_nbytes != 100 && (nbytes = this.s.read(tmp, 0, 100)) != 0){
	//						
	//						System.arraycopy(tmp, 0, data, total_nbytes, nbytes);
	//						
	//						total_nbytes += nbytes;
	//					}
	//					
	//					MoreAsserts.assertEquals(buffer, data);					
	//				}
	//			}
	//		}
	//
	//		try {
	//			TCP s_tcp = new TCP(10);
	//			TCP c_tcp = new TCP(11);
	//
	//			int port = 1234;
	//
	//			Socket s_socket = s_tcp.socket(port);
	//			Socket c_socket = c_tcp.socket();
	//
	//			Random generator = new Random();
	//			
	//			byte[] buffer = new byte[100];
	//			generator.nextBytes(buffer);
	//			
	//			Server s = new Server(s_socket, buffer);
	//			Client c = new Client(c_socket, 10, port, buffer);
	//			
	//			s.start();
	//			c.start();
	//			
	//			try {
	//				s.join();
	//				c.join();
	//			} catch (InterruptedException e) {
	//				e.printStackTrace();
	//			}
	//			
	//		} catch (IOException e) {
	//			fail("Unable to initialize a socket");
	//		}
	//
	//	}




	//	/*
	//	 * The length parameter of the write is smaller than 
	//	 * the size of the buffer
	//	 * */
	//	public void testWriteLenBufferSizeMismatch1(){
	//
	//		class Server extends Thread{
	//			Socket s;
	//			byte[] buffer;
	//
	//			public Server(Socket s, byte[] buffer){
	//				this.s = s;
	//				this.buffer = buffer;
	//			}
	//
	//			@Override
	//			public void run() {
	//				this.s.accept();
	//				
	//				if(this.s.write(this.buffer, 0, 50) != 50)
	//					fail("Unable to write the data");
	//
	//			}
	//		}
	//
	//		class Client extends Thread{
	//			Socket s;
	//			int addr;
	//			int port;
	//			byte[] buffer = new byte[50];
	//
	//			public Client(Socket s, int addr, int port, byte[] buffer){
	//				this.s = s;
	//				this.addr = addr;
	//				this.port = port;
	//				System.arraycopy(buffer, 0, this.buffer, 0, 50);
	//			}
	//
	//			@Override
	//			public void run() {
	//				byte[] data = new byte[50];
	//				byte[] tmp = new byte[100];
	//				
	//				int nbytes;
	//				int total_nbytes = 0;
	//				
	//				if( this.s.connect(IpAddress.getAddress("192.168.0." + addr), port) ){ 
	//
	//					while(total_nbytes != 50 && (nbytes = this.s.read(tmp, 0, 50)) != 0){
	//						
	//						System.arraycopy(tmp, 0, data, total_nbytes, nbytes);
	//						
	//						total_nbytes += nbytes;
	//					}
	//					
	//					MoreAsserts.assertEquals(buffer, data);					
	//				}
	//			}
	//		}
	//
	//		try {
	//			TCP s_tcp = new TCP(10);
	//			TCP c_tcp = new TCP(11);
	//
	//			int port = 1234;
	//
	//			Socket s_socket = s_tcp.socket(port);
	//			Socket c_socket = c_tcp.socket();
	//
	//			Random generator = new Random();
	//			
	//			byte[] buffer = new byte[100];
	//			generator.nextBytes(buffer);
	//			
	//			Server s = new Server(s_socket, buffer);
	//			Client c = new Client(c_socket, 10, port, buffer);
	//			
	//			s.start();
	//			c.start();
	//			
	//			try {
	//				s.join();
	//				c.join();
	//			} catch (InterruptedException e) {
	//				e.printStackTrace();
	//			}
	//			
	//		} catch (IOException e) {
	//			fail("Unable to initialize a socket");
	//		}
	//
	//	}






	//	/*
	//	 * Writes and data for 10 full-sized packets 
	//	 *
	//	 * */
	//	public void testWriteLargeAmount(){
	//
	//		class Server extends Thread{
	//			Socket s;
	//			byte[] buffer;
	//
	//			public Server(Socket s, byte[] buffer){
	//				this.s = s;
	//				this.buffer = buffer;
	//			}
	//
	//			@Override
	//			public void run() {
	//				this.s.accept();
	//				
	//				if(this.s.write(this.buffer, 0, this.buffer.length) != this.buffer.length)
	//					fail("Unable to write the data");
	//
	//			}
	//		}
	//
	//		class Client extends Thread{
	//			Socket s;
	//			int addr;
	//			int port;
	//			byte[] buffer;
	//
	//			public Client(Socket s, int addr, int port, byte[] buffer){
	//				this.s = s;
	//				this.addr = addr;
	//				this.port = port;
	//				this.buffer = buffer;
	//			}
	//
	//			@Override
	//			public void run() {
	//				byte[] data = new byte[TCPPacket.MAX_PACKET_SIZE * 10];
	//				byte[] tmp = new byte[TCPPacket.MAX_PACKET_SIZE * 10];
	//				
	//				int nbytes;
	//				int total_nbytes = 0;
	//				
	//				if( this.s.connect(IpAddress.getAddress("192.168.0." + addr), port) ){ 
	//
	//					while(total_nbytes != TCPPacket.MAX_PACKET_SIZE * 10 && 
	//							(nbytes = this.s.read(tmp, 0, TCPPacket.MAX_PACKET_SIZE * 10)) != 0){
	//						
	//						System.arraycopy(tmp, 0, data, total_nbytes, nbytes);
	//						
	//						total_nbytes += nbytes;
	//					}
	//					
	//					MoreAsserts.assertEquals(buffer, data);					
	//				}
	//			}
	//		}
	//
	//		try {
	//			TCP s_tcp = new TCP(10);
	//			TCP c_tcp = new TCP(11);
	//
	//			int port = 1234;
	//
	//			Socket s_socket = s_tcp.socket(port);
	//			Socket c_socket = c_tcp.socket();
	//
	//			Random generator = new Random();
	//			
	//			byte[] buffer = new byte[TCPPacket.MAX_PACKET_SIZE * 10];
	//			generator.nextBytes(buffer);
	//			
	//			Server s = new Server(s_socket, buffer);
	//			Client c = new Client(c_socket, 10, port, buffer);
	//			
	//			s.start();
	//			c.start();
	//			
	//			try {
	//				s.join();
	//				c.join();
	//			} catch (InterruptedException e) {
	//				e.printStackTrace();
	//			}
	//			
	//		} catch (IOException e) {
	//			fail("Unable to initialize a socket");
	//		}
	//
	//	}
	//	

	//	/*
	//	 * The server writes more then MAXLEN. 
	//	 * The client loops until reads all the data.
	//	 * */
	//	public void testWriteMoreThanMAXLEN(){
	//
	//		class Server extends Thread{
	//			Socket s;
	//			byte[] buffer;
	//
	//			public Server(Socket s, byte[] buffer){
	//				this.s = s;
	//				this.buffer = buffer;
	//			}
	//
	//			@Override
	//			public void run() {
	//				this.s.accept();
	//				
	//				if(this.s.write(this.buffer, 0, this.buffer.length) != this.buffer.length)
	//					fail("Unable to write the data");
	//
	//			}
	//		}
	//
	//		class Client extends Thread{
	//			Socket s;
	//			int addr;
	//			int port;
	//			byte[] buffer;
	//
	//			public Client(Socket s, int addr, int port, byte[] buffer){
	//				this.s = s;
	//				this.addr = addr;
	//				this.port = port;
	//				this.buffer = buffer;
	//			}
	//
	//			@Override
	//			public void run() {
	//				byte[] data = new byte[1000];
	//				byte[] tmp = new byte[200];
	//				
	//				int nbytes;
	//				int total_nbytes = 0;
	//				
	//				if( this.s.connect(IpAddress.getAddress("192.168.0." + addr), port) ){ 
	//
	//					while(total_nbytes != 1000 && (nbytes = this.s.read(tmp, 0, 200)) != 0){
	//						
	//						System.arraycopy(tmp, 0, data, total_nbytes, nbytes);
	//						
	//						total_nbytes += nbytes;
	//					}
	//					
	//					MoreAsserts.assertEquals(buffer, data);					
	//				}
	//			}
	//		}
	//
	//		try {
	//			TCP s_tcp = new TCP(10);
	//			TCP c_tcp = new TCP(11);
	//
	//			int port = 1234;
	//
	//			Socket s_socket = s_tcp.socket(port);
	//			Socket c_socket = c_tcp.socket();
	//
	//			Random generator = new Random();
	//			
	//			byte[] buffer = new byte[1000];
	//			generator.nextBytes(buffer);
	//			
	//			Server s = new Server(s_socket, buffer);
	//			Client c = new Client(c_socket, 10, port, buffer);
	//			
	//			s.start();
	//			c.start();
	//			
	//			try {
	//				s.join();
	//				c.join();
	//			} catch (InterruptedException e) {
	//				e.printStackTrace();
	//			}
	//			
	//		} catch (IOException e) {
	//			fail("Unable to initialize a socket");
	//		}
	//
	//	}




	//	/*
	//	 * Tests the case when the initial seq numbers are large
	//	 * */
	//	public void testLargeSeq(){
	//
	//		class Server extends Thread{
	//			BogusSocket s;
	//			byte[] buffer;
	//
	//			public Server(BogusSocket s, byte[] buffer){
	//				this.s = s;
	//				this.buffer = buffer;
	//			}
	//
	//			@Override
	//			public void run() {
	//				this.s.accept();
	//				
	//				if(this.s.write(this.buffer, 0, this.buffer.length) != this.buffer.length)
	//					fail("Unable to write the data");
	//
	//			}
	//		}
	//
	//		class Client extends Thread{
	//			BogusSocket s;
	//			int addr;
	//			int port;
	//			byte[] buffer;
	//
	//			public Client(BogusSocket s, int addr, int port, byte[] buffer){
	//				this.s = s;
	//				this.addr = addr;
	//				this.port = port;
	//				this.buffer = buffer;
	//			}
	//
	//			@Override
	//			public void run() {
	//				byte[] data = new byte[1000];
	//				if( this.s.connect(IpAddress.getAddress("192.168.0." + addr), port) ){ 
	//
	//					this.s.read(data, 0, 1000);
	//
	//					MoreAsserts.assertEquals(buffer, data);
	//				}
	//			}
	//		}
	//
	//		try {
	//			BogusTCP s_tcp = new BogusTCP(10);
	//			BogusTCP c_tcp = new BogusTCP(11);
	//
	//			int port = 1234;
	//
	//			BogusSocket s_socket = s_tcp.socket(port);
	//			BogusSocket c_socket = c_tcp.socket();
	//
	//			Random generator = new Random();
	//			
	//			byte[] buffer = new byte[1000];
	//			generator.nextBytes(buffer);
	//			
	//			Server s = new Server(s_socket, buffer);
	//			Client c = new Client(c_socket, 10, port, buffer);
	//			
	//			s.start();
	//			c.start();
	//			
	//			try {
	//				s.join();
	//				c.join();
	//			} catch (InterruptedException e) {
	//				e.printStackTrace();
	//			}
	//			
	//		} catch (IOException e) {
	//			fail("Unable to initialize a socket");
	//		}
	//
	//	}



	//	/* 
	//	 * This test simulates packet loss changing
	//	 * the original method for communication between
	//	 * the TCP and IP level in such a way that it drops
	//	 * 50% of the packets 
	//	 * */
	//	public void testPacketLoss(){
	//		
	//		class Server extends Thread{
	//			BogusSocket s;
	//			
	//			public Server(BogusSocket s){
	//				this.s = s;
	//			}
	//			
	//			@Override
	//			public void run() {
	//				byte[] data = {(byte) 0xff, (byte)0x1f, (byte) 0x8a, (byte) 0x12, (byte) 0x56};
	//				this.s.bogus_accept();
	//				this.s.bogus_write(data, 0, data.length);
	//				
	//			}
	//		}
	//		
	//		class Client extends Thread{
	//			BogusSocket s;
	//			int addr;
	//			int port;
	//			
	//			public Client(BogusSocket s, int addr, int port){
	//				this.s = s;
	//				this.addr = addr;
	//				this.port = port;
	//			}
	//			
	//			@Override
	//			public void run() {
	//				byte[] data = {(byte) 0xff, (byte)0x1f, (byte) 0x8a, (byte) 0x12, (byte) 0x56};
	//				byte[] buffer = new byte[5];
	//				if( this.s.bogus_connect(IpAddress.getAddress("192.168.0." + addr), port) ){ 
	//				
	//					this.s.bogus_read(buffer, 0, 5);
	//					MoreAsserts.assertEquals(buffer, data);
	//				}
	//			}
	//		}
	//		
	//		int ip_server = 1;
	//		int ip_client = 2;
	//
	//		int port = 1234;
	//
	//		BogusTCP s_tcp;
	//		BogusTCP c_tcp;
	//
	//		BogusSocket s_socket;
	//		BogusSocket c_socket;
	//
	//		try {
	//			s_tcp = new BogusTCP(ip_server);
	//			c_tcp = new BogusTCP(ip_client);
	//			
	//			s_socket = s_tcp.socket(port);
	//			c_socket = c_tcp.socket();
	//			
	//			Server s = new Server(s_socket);
	//			Client c = new Client(c_socket, ip_server, port);
	//			
	//			s.start();
	//			c.start();
	//			
	//			try {
	//				s.join();
	//				c.join();
	//			} catch (Exception e) {
	//				// TODO Auto-generated catch block
	//				e.printStackTrace();
	//			}
	//
	//		} catch (IOException e) {
	//			fail("Failed to create TCP stacks");
	//		}
	//	}


}
