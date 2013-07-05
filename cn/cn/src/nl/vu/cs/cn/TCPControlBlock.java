package nl.vu.cs.cn;

/**
 * This class is used for storing the control variables of a TCP stack
 *  (e.g. its state, ip address, port, etc.)
 */
public class TCPControlBlock {
	
	public enum ConnectionState{
		S_CLOSED, S_LISTEN, S_SYN_SENT, S_SYN_RCVD,
		S_ESTABLISHED, S_FIN_WAIT_1, S_FIN_WAIT_2,
		S_TIME_WAIT, S_CLOSING, S_CLOSE_WAIT, S_LAST_ACK;
	}
	
	public int tcb_our_ip_addr;						//Our IP address
	public int tcb_their_ip_addr;					//Their IP address
	public int tcb_our_port;						//Our port number
	public int tcb_their_port;						//Their port number
	public long tcb_seq;							//What we want them to ack
	public long tcb_ack;							//What we think they know we know
	public int tcb_data_left;						//Undelivered data bytes
	public ConnectionState tcb_state;				//The current connection state
	
	public TCPControlBlock(){
		this.tcb_state = ConnectionState.S_CLOSED;
	}
}
