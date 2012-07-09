package nl.vu.cs.cn;

import java.nio.ByteBuffer;

import nl.vu.cs.cn.IP.Packet;

public class TcpPacket {
	public static final byte TCP_FIN = 0x01;
	public static final byte TCP_SYN = 0x02;
	public static final byte TCP_RST = 0x04;
	public static final byte TCP_PUSH = 0x08;
	public static final byte TCP_ACK = 0x10;
	public static final byte TCP_URG = 0x20;
	public static final byte TCP_SYN_ACK = 0x22;
	
	public int src_port;
	public int dst_port;
	public long seq_number;
	public long ack;
	public short header_length;
	public byte flag;
	public int checksum;
	public byte[] data;
	public long src_ip;
	public long dst_ip;
	
	public TcpPacket(Packet p){
		ByteBuffer bb = ByteBuffer.wrap(p.data);

		this.src_ip = p.source & 0xffffffff;
		this.dst_ip = p.destination & 0xffffffff;
		
		this.src_port = bb.getShort() & 0xffff;
		this.dst_port = bb.getShort() & 0xffff;
		
		this.seq_number = bb.getInt() & 0xffffffff;
		this.ack = bb.getInt() & 0xffffffff;
		
		bb.get();
		
		this.flag = bb.get();

		this.checksum = bb.getShort() & 0xffff;
		bb.get(data);
	}
	public boolean checkFlags(byte mask) {
		if ((mask & this.flag) == mask) {
			return true;
		}
		return false;
	}
	
	
	public TcpPacket(){}
}
