package nl.vu.cs.cn;

import java.nio.ByteBuffer;
import java.util.Vector;

import nl.vu.cs.cn.IP.IpAddress;

public class TcpControlBlock {
	
	public enum ConnectionState{
		S_CLOSED, S_LISTEN, S_SYN_SENT, S_SYN_RCVD,
		S_ESTABLISHED, S_FIN_WAIT_1, S_FIN_WAIT_2,
		S_TIMED_OUT, S_CLOSING, S_CLOSE_WAIT, S_LAST_ACK;
	}
	
	public int tcb_our_ip_addr;				//Our IP address
	public int tcb_their_ip_addr;				//Their IP address
	public short tcb_our_port;						//Our port number
	public short tcb_their_port;					//Their port number
	public int tcb_seq;				//What we want them to ack
	public int tcb_ack;				//What we think they know we know
	public Vector<ByteBuffer> tcb_p_data;			//The undelivered data
	public int tcb_data_left;						//Undelivered data byetes
	public ConnectionState tcb_state;				//The current connection state
	
	public TcpControlBlock(IpAddress tcb_our_ip_addr){
		this.tcb_our_ip_addr = tcb_our_ip_addr.getAddress();
		this.tcb_seq = (int) Math.random();
		this.tcb_state = ConnectionState.S_CLOSED;
		tcb_p_data = new Vector<ByteBuffer>();
	}
	
	public long incrSeq(int inc) {
		this.tcb_seq= (this.tcb_ack + inc) % Integer.MAX_VALUE*2;
		return this.tcb_ack;
	}
}
