package nl.vu.cs.cn;

import java.nio.ByteBuffer;

import nl.vu.cs.cn.IP.Packet;

/**
 * A class representing a TCP packet.
 */
public class TCPPacket {

	/* The TCP header flags */
	public static final byte TCP_FIN = 0x01;
	public static final byte TCP_SYN = 0x02;
	public static final byte TCP_RST = 0x04;
	public static final byte TCP_PUSH = 0x08;
	public static final byte TCP_ACK = 0x10;
	public static final byte TCP_URG = 0x20;
	public static final byte TCP_SYN_ACK = 0x12;
	
	public static final int MAX_PACKET_SIZE = 7000; 
//	private static final int MAX_BUF_SIZE = 1000 * MAX_PACKET_SIZE;
	
	public int src_port;
	public int dst_port;
	public long seq;
	public long ack;
	public short header_length;
	public byte flag;
	public int checksum;
	public byte[] data;
	public int src_ip;
	public int dst_ip;
	public int length;
	
	
	public TCPPacket(){}
	
	/**
	 * Reads the payload and the header of an IP packet and
	 * sets the corresponding values in a TCP packet
	 * 
	 *  @param p the IP packet to read
	 */
	public void ip2tcp(Packet p){
		ByteBuffer bb = ByteBuffer.wrap(p.data);

		this.src_ip = p.source;
		this.dst_ip = p.destination;
		
		this.src_port = bb.getChar();
		this.dst_port = bb.getChar();
		

		this.seq = bb.getInt() & 0xFFFFFFFFL;
		this.ack = bb.getInt() & 0xFFFFFFFFL;
		
		bb.get();
		
		this.flag = bb.get();
		bb.getShort();										//window size
		this.length = p.length - 20;						//the length of the data, 20 is the length of the TCP header

		this.checksum = bb.getShort();						//checksum	
		bb.getShort();										//urgent pointer
		
		data = new byte[p.length - 20];
		
		bb.get(data);
	}
	
//	public boolean checkFlags(byte mask) {
//		if ((mask & this.flag) == mask) {
//			return true;
//		}
//		return false;
//	}
	
	public boolean checkFlags(byte mask){
		if((mask & this.flag & ~TCP_PUSH) == mask){
			return true;
		}
		return false;
	}
}
