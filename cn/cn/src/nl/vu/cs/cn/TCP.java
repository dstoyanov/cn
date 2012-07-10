package nl.vu.cs.cn;

import java.io.IOException;
import java.nio.ByteBuffer;

import nl.vu.cs.cn.IP.IpAddress;
import nl.vu.cs.cn.IP.Packet;
import nl.vu.cs.cn.TcpControlBlock.ConnectionState;

/**
 * This class represents a TCP stack. It should be built on top of the IP stack
 * which is bound to a given IP address.
 */
public class TCP {

	/** The underlying IP stack for this TCP stack. */
    private IP ip;

    /**
     * This class represents a TCP socket.
     *
     */
    public class Socket {

    	/* Hint: You probably need some socket specific data. */
    	private TcpControlBlock tcb;

    	/**
    	 * Construct a client socket.
    	 */
    	private Socket() {
    		tcb = new TcpControlBlock(ip.getLocalAddress());
    	}

    	/**
    	 * Construct a server socket bound to the given local port.
		 *
    	 * @param port the local port to use
    	 */
        private Socket(int port) {
        	tcb = new TcpControlBlock(ip.getLocalAddress());
//        	tcb.tcb_our_port = port;
		}

		/**
         * Connect this socket to the specified destination and port.
         *
         * @param dst the destination to connect to
         * @param port the port to connect to
         * @return true if the connect succeeded.
         */
        public boolean connect(IpAddress dst, int port) {
        	ByteBuffer bb;
        	TcpPacket p = new TcpPacket();
            // Implement the connection side of the three-way handshake here.
        	
        	if(this.tcb.tcb_state != ConnectionState.S_CLOSED){
        		System.err.println("The socket is not in the correct state");
        		return false;
        	}
        	
        	this.tcb.tcb_our_port = (short) Math.random();
        	this.tcb.tcb_their_port = (short) port;
        	this.tcb.tcb_our_sequence_number = (int) Math.random();
        	this.tcb.tcb_our_expected_ack = this.tcb.tcb_our_sequence_number + 1;
        	
        	//send the first SYN packet from the three-way handshake
        	bb = send_tcp_packet(dst.getAddress(),
        			new byte[0],
        			0,
        			(short) tcb.tcb_our_port,
        			(short) this.tcb.tcb_their_port,
        			this.tcb.tcb_our_sequence_number,
        			this.tcb.tcb_our_expected_ack,
        			TcpPacket.TCP_SYN);
        	
        	
        	if(bb == null)			// if the send is not successful return false 
        		return false;
        	
        	this.tcb.tcb_state = ConnectionState.S_SYN_SENT;
        	this.tcb.tcb_p_data.add(bb);				//add the byte buffer to a vector until we get an ack
        	
        	//TODO timeouts??
        	if(recv_tcp_packet(p) == false)			//if receiving packet is not successful return false
        		return false;
        	
        	if(p.checkFlags(TcpPacket.TCP_SYN_ACK) && p.dst_port == this.tcb.tcb_our_port
        			&& p.src_port == this.tcb.tcb_their_port && this.tcb.tcb_our_expected_ack == p.ack){
        		tcb.tcb_p_data.remove(bb);
        		
        		bb = send_tcp_packet(dst.getAddress(),
        				new byte[0],
        				0,
            			(short) tcb.tcb_our_port,
            			(short) this.tcb.tcb_their_port,
            			this.tcb.tcb_our_sequence_number,
            			this.tcb.tcb_our_expected_ack,
            			TcpPacket.TCP_ACK);
        		
        		this.tcb.tcb_state = ConnectionState.S_ESTABLISHED;
        	}
        	
        	
            return false;
        }

        /**
         * Accept a connection on this socket.
         * This call blocks until a connection is made.
         */
        public void accept() {

        	TcpPacket p = new TcpPacket();
        	ByteBuffer bb = null;
        
        	
        	if(this.tcb.tcb_state != ConnectionState.S_CLOSED)
        		return;
      
        	this.tcb.tcb_state = ConnectionState.S_LISTEN;
        	
        	recv_tcp_packet(p);
        	

        	if(p.checkFlags(TcpPacket.TCP_SYN) && (!p.checkFlags(TcpPacket.TCP_ACK)) && this.tcb.tcb_our_port == p.src_port) {
        		this.tcb.tcb_state = ConnectionState.S_SYN_RCVD;

            	this.tcb.tcb_their_ip_addr = (int) p.src_ip;
            	this.tcb.tcb_their_port = (short) p.src_port;
            	this.tcb.tcb_our_expected_ack = (int) p.seq_number + 1;
            	this.tcb.tcb_our_sequence_number = (int) Math.random();
            	
            	bb = this.send_tcp_packet(ip.getLocalAddress().getAddress(),
            			new byte[0],
            			0,
            			this.tcb.tcb_our_port,
            			this.tcb.tcb_their_port,
            			this.tcb.tcb_our_sequence_number,
            			this.tcb.tcb_our_expected_ack,
            			TcpPacket.TCP_SYN_ACK);
            	
            	if(bb == null){
            		//TODO break the loop???
            	}
            	
            	recv_tcp_packet(p);
            	if((int)p.src_ip == tcb.tcb_their_ip_addr && p.checkFlags(TcpPacket.TCP_ACK)
            			&& p.ack == this.tcb.tcb_our_sequence_number + 1
            			&& p.seq_number == this.tcb.tcb_our_expected_ack + 1){
            		
            		this.tcb.tcb_state = ConnectionState.S_ESTABLISHED;
            		
            	}
            	
        		
        	}
        }

        /**
         * Reads bytes from the socket into the buffer.
         * This call is not required to return maxlen bytes
         * every time it returns.
         *
         * @param buf the buffer to read into
         * @param offset the offset to begin reading data into
         * @param maxlen the maximum number of bytes to read
         * @return the number of bytes read, or -1 if an error occurs.
         */
        public int read(byte[] buf, int offset, int maxlen) {

            // Read from the socket here.

            return -1;
        }

        /**
         * Writes to the socket from the buffer.
         *
         * @param buf the buffer to
         * @param offset the offset to begin writing data from
         * @param len the number of bytes to write
         * @return the number of bytes written or -1 if an error occurs.
         */
        public int write(byte[] buf, int offset, int len) {

            // Write to the socket here.

            return -1;
        }

        /**
         * Closes the connection for this socket.
         * Blocks until the connection is closed.
         *
         * @return true unless no connection was open.
         */
        public boolean close() {

            // Close the socket cleanly here.

            return false;
        }
        
        private ByteBuffer send_tcp_packet(int dst_address, byte[] buf, int length, short src_port,
        		short dst_port, int seq_number, int ack_number, byte flags){
		
        	ByteBuffer pseudo;
        	ByteBuffer tcp_packet = ByteBuffer.allocate(length + 20);
        	int dst_ip;
        	
        	if(length % 2 == 0){
        		pseudo = ByteBuffer.allocate(length + 32);
        	} else{
        		pseudo = ByteBuffer.allocate(length + 33);
        	}
        	
        	/* The pseudo header for checksum computation	 */
        	int localaddr = ip.getLocalAddress().getAddress();
        	
        	pseudo.put((byte)(localaddr & 0xff));
        	pseudo.put((byte)(localaddr >> 8 & 0xff));
        	pseudo.put((byte)(localaddr >> 16 & 0xff));
        	pseudo.put((byte)(localaddr >>> 24));

        	dst_ip = IpAddress.getAddress("192.168.0." + dst_address).getAddress();
        	pseudo.put((byte)(dst_ip & 0xff));
        	pseudo.put((byte)(dst_ip >> 8 & 0xff));
        	pseudo.put((byte)(dst_ip >> 16 & 0xff));
        	pseudo.put((byte)(dst_ip >>> 24));
        	
        	pseudo.put((byte)0);
        	pseudo.put((byte) 6);
        	pseudo.putShort((short) (length + 20));
        	
        	pseudo.putShort(src_port);
        	pseudo.putShort(dst_port);
        	pseudo.putInt(seq_number);
        	pseudo.putInt(ack_number);
        	
        	pseudo.put((byte) 0x50);	// The TCP header length = 5 and the 4 empty bits

        	byte mask = (byte) (flags & 0x1b);
        	mask |= (1 << 3) & 0xff;
        	
        	System.out.println("mask " + (int)mask);

        	pseudo.put(mask);
        	pseudo.putShort((short) 1);	//Window size
        	
        	pseudo.putInt(0);			//The Checksum and the urgent pointer
        	pseudo.put(buf);
        	
        	if(length % 2 != 0){
        		pseudo.put((byte) 0);
        	}
        	
        	long checksum = calculateChecksum(pseudo.array());
        	System.out.println("Checksum " + Long.toHexString(checksum));

        	tcp_packet.putShort(src_port);
        	tcp_packet.putShort(dst_port);
        	tcp_packet.putInt(seq_number);
        	tcp_packet.putInt(ack_number);
        	tcp_packet.put((byte) 0x50);
        	
        	tcp_packet.put(mask);
        	tcp_packet.putShort((short) 1);	//Window size

        	tcp_packet.putShort((short) checksum);
        	tcp_packet.putShort((short) 0); //Urgent pointer
        	tcp_packet.put(buf);
        	
        	
        	System.out.println(tcp_packet);
        	System.out.println("Local address " + ip.getLocalAddress().toString());
        	
//        	StringBuffer hexString = new StringBuffer();
//        	for(int i = 0; i < tcp_packet.limit(); i++){
//        		String hex = Integer.toHexString(0xff & tcp_packet.get(i));
//        		if(hex.length() ==  1){
//        			hexString.append('0');
//        		}
//        		hexString.append(hex);
//        	}
//        	
//        		System.out.print(hexString);

        	Packet p1 = new Packet(dst_ip, 6, 0, tcp_packet.array(), length + 20); //TODO why the length can be smaller than the data size?
        	p1.source = localaddr;

        	try{
        		ip.ip_send(p1);
        	} catch(IOException e){
        		return null;			//if the send fails
        	}
        	
        	return tcp_packet;
        }
        
        private boolean recv_tcp_packet(TcpPacket tcpp){
        	Packet p = new Packet();
        	ByteBuffer pseudo;
        	
        	try{
        		ip.ip_receive(p);

        		int pseudoLength = p.length + 12;
        		
        		if (p.length % 2 == 1)			//in case the number of bytes is odd
        			pseudoLength++;
        		
        		pseudo = ByteBuffer.allocate(pseudoLength);
        		
            	pseudo.put((byte)(p.source & 0xff));
            	pseudo.put((byte)(p.source >> 8 & 0xff));
            	pseudo.put((byte)(p.source >> 16 & 0xff));
            	pseudo.put((byte)(p.source >>> 24));

            	pseudo.put((byte)(p.destination & 0xff));
            	pseudo.put((byte)(p.destination >> 8 & 0xff));
            	pseudo.put((byte)(p.destination >> 16 & 0xff));
            	pseudo.put((byte)(p.destination >>> 24));
        		
        		pseudo.put((byte) 0);
        		pseudo.put((byte) p.protocol);
        		pseudo.putShort((short) p.length);
        		
        		pseudo.put(p.data);
        		
        		if (p.length % 2 == 1){
        			pseudo.put((byte) 0);
        		}

        		tcpp = new TcpPacket(p);
        		
        		long checksum = calculateChecksum(pseudo.array());
        		if(checksum != 0){
        			return false;
        		}
        		
            	System.out.println("Checksum " + Long.toHexString(checksum));
            	
        	} catch (IOException e){
        		e.printStackTrace();
        		return false;
        	}
        	return true;
        }
        
        private long calculateChecksum(byte[] buf) {
            int length = buf.length;
            int i = 0;

            long sum = 0;
            long data;

            // Handle all pairs
            while (length > 1) {
              // Corrected to include @Andy's edits and various comments on Stack Overflow
              data = (((buf[i] << 8) & 0xFF00) | ((buf[i + 1]) & 0xFF));
              sum += data;
              // 1's complement carry bit correction in 16-bits (detecting sign extension)
              if ((sum & 0xFFFF0000) > 0) {
                sum = sum & 0xFFFF;
                sum += 1;
              }

              i += 2;
              length -= 2;
            }

            // Handle remaining byte in odd length buffers
            if (length > 0) {
              // Corrected to include @Andy's edits and various comments on Stack Overflow
              sum += (buf[i] << 8 & 0xFF00);
              // 1's complement carry bit correction in 16-bits (detecting sign extension)
              if ((sum & 0xFFFF0000) > 0) {
                sum = sum & 0xFFFF;
                sum += 1;
              }
            }

            // Final 1's complement value correction to 16-bits
            sum = ~sum;
            sum = sum & 0xFFFF;
            return sum;

          }
    }

    /**
     * Constructs a TCP stack for the given virtual address.
     * The virtual address for this TCP stack is then
     * 192.168.1.address.
     *
     * @param address The last octet of the virtual IP address 1-254.
     * @throws IOException if the IP stack fails to initialize.
     */
    public TCP(int address) throws IOException {
        ip = new IP(address);
    }

    /**
     * @return a new socket for this stack
     */
    public Socket socket() {
        return new Socket();
    }

    /**
     * @return a new server socket for this stack bound to the given port
     * @param port the port to bind the socket to.
     */
    public Socket socket(int port) {
        return new Socket(port);
    }

}
