package nl.vu.cs.cn;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;

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

		/* A new thread for detecting timeouts */

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
			this.tcb.tcb_our_port = (short) port;
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

			this.tcb.tcb_their_ip_addr = dst.getAddress();
			this.tcb.tcb_our_ip_addr = (int) ip.getLocalAddress().getAddress();
			
			
			Random generator = new Random();
			this.tcb.tcb_our_port = (short) generator.nextInt(Short.MAX_VALUE + 1);
			this.tcb.tcb_their_port = (short) port;
			System.out.println("Connect: Ports t o" + this.tcb.tcb_their_port  + "   "  + this.tcb.tcb_our_port);
			System.out.println("Connect: Addr t o" + this.tcb.tcb_their_ip_addr  + "   "  + this.tcb.tcb_our_ip_addr);
			
			int tmp_seq = generator.nextInt(Integer.MAX_VALUE / 2);
			this.tcb.tcb_seq = tmp_seq < 0 ? (-1) * tmp_seq : tmp_seq;
			this.tcb.tcb_ack = 0;

			//send the first SYN packet from the three-way handshake
			//check if we get 10 timeouts
			int count = 0;
			for(int it = 0; it < 10; it++){
				bb = send_tcp_packet(dst.getAddress(),
						new byte[0],
						0,
						(short) tcb.tcb_our_port,
						(short) this.tcb.tcb_their_port,
						this.tcb.tcb_seq,
						this.tcb.tcb_ack,
						TcpPacket.TCP_SYN);
				System.out.println("Connect first packet: seq " + this.tcb.tcb_seq + " ack " + this.tcb.tcb_ack);

				if(bb == null){			// if the send is not successful return false 
					System.out.println("Connect: bb null");
					return false;
				}

				this.tcb.tcb_state = ConnectionState.S_SYN_SENT;



				if(recv_tcp_packet(p, true)){
					System.out.println("Connect: SYN/ACK packet received");
					break;

				} else {
					count ++;
				}
			}

			if(count == 10){
				System.out.println("Connect: Max number of timeouts (SYN/ACK)");
				return false;
			}

			this.tcb.tcb_seq++;
			if(p.checkFlags(TcpPacket.TCP_SYN_ACK) && p.dst_port == this.tcb.tcb_our_port
					&& p.src_port == this.tcb.tcb_their_port && this.tcb.tcb_seq == p.ack){

//				this.tcb.incrSeq(1);
//				this.tcb.tcb_seq++;
				this.tcb.tcb_ack = (int) p.seq + 1;
				bb = send_tcp_packet(dst.getAddress(),
						new byte[0],
						0,
						(short) this.tcb.tcb_our_port,
						(short) this.tcb.tcb_their_port,
						this.tcb.tcb_seq,
						this.tcb.tcb_ack,
						TcpPacket.TCP_ACK);
				System.out.println("Connect third packet: seq" + this.tcb.tcb_seq + " ack " + this.tcb.tcb_ack);


				this.tcb.tcb_state = ConnectionState.S_ESTABLISHED;
				return true;
			}

			return false;
		}
		
//		private int EndianSwap32(int x)
//		{
//		    int y=0;
//		    y += (x & 0x000000FF)<<24;
//		    y += (x & 0xFF000000)>>24;
//		    y += (x & 0x0000FF00)<<8;
//		    y += (x & 0x00FF0000)>>8;
//		    return x;
//		}

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

			while(true){

				
				recv_tcp_packet(p, false);				//does not timeout
			
				
				System.out.println("ACCEPT: a packet accepted");

				if(p.checkFlags(TcpPacket.TCP_SYN) && (!p.checkFlags(TcpPacket.TCP_ACK))
						&& this.tcb.tcb_our_port == p.dst_port) {

					this.tcb.tcb_state = ConnectionState.S_SYN_RCVD;
					this.tcb.tcb_their_ip_addr = (int) p.src_ip ;
					this.tcb.tcb_their_port = (short) p.src_port;
					this.tcb.tcb_ack = (int) p.seq + 1;
					
					Random generator = new Random();
					int tmp_seq = generator.nextInt(Integer.MAX_VALUE / 2);
					this.tcb.tcb_seq = tmp_seq < 0 ? (-1) * tmp_seq : tmp_seq;
					
					//					System.out.println("Accept: dst ip" + this.tcb.tcb_their_ip_addr);
					int count = 0;

//					for(int it = 0; it < 10; it++){
					for(int it = 0; it < 5; it++){
						bb = this.send_tcp_packet(this.tcb.tcb_their_ip_addr,
								new byte[0],
								0,
								this.tcb.tcb_our_port,
								this.tcb.tcb_their_port,
								this.tcb.tcb_seq,
								this.tcb.tcb_ack,
								TcpPacket.TCP_SYN_ACK);
						System.out.println("Accept second packet: seq" + this.tcb.tcb_seq + " ack " + this.tcb.tcb_ack);

						//				System.out.println("Accept: SYN/ACK sent");

						if(bb == null){
							continue;
						}

						//check if we get 10 timeouts
					
						if(recv_tcp_packet(p, true)){
							break;
						} else{
							count ++;
						}
					}

//					if(count == 10){
					if(count == 5){
//				System.out.println("Accept: Max number timeouts");
						this.tcb.tcb_state = ConnectionState.S_LISTEN;
						continue;
					}
//
//					System.out.println("IP " + p.src_ip + " " + tcb.tcb_their_ip_addr + "\n Flags " + p.checkFlags(TcpPacket.TCP_ACK) +
//							"Seq " +  p.ack + " " + (this.tcb.tcb_seq + 1) + "\n" +
//							"Ack "+ p.seq + " "  + this.tcb.tcb_ack + 1);
					int tmp_src_ip = (int) p.src_ip;
					
					if( tmp_src_ip == tcb.tcb_their_ip_addr && p.checkFlags(TcpPacket.TCP_ACK)
							&& p.ack == this.tcb.tcb_seq + 1
							&& p.seq == this.tcb.tcb_ack){
						
						System.out.println("Accept: connected");
						this.tcb.tcb_state = ConnectionState.S_ESTABLISHED;
						this.tcb.tcb_seq++;			//TODO check if that is correct
						return;
					}
				}
			}
		}
		//TODO check in read and write if the ports and IPs are correct	
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
			System.out.println("READ: maxlen " + maxlen);
			// Read from the socket here.
			TcpPacket p = new TcpPacket();
			ByteBuffer bb = ByteBuffer.allocate(maxlen);
			int num_read = 0;
			
			//TODO in case there is no data and the other side closed???
			if(maxlen <= 0)
				return -1;
			
			long oldSeq = -1;
			while(num_read < maxlen){
				
				if(!recv_tcp_packet(p, false))
					return -1;
				
				System.out.println("READ: received packet seq " + p.seq + " length "  + p.length);
			
				//TODO check retransmitting previous packet

				if (p.seq > oldSeq) {
					if(num_read + p.length > maxlen){
						int n = maxlen - num_read;
						bb.put(p.data, 0, n);
					
//						this.tcb.tcb_ack += n;
						this.tcb.tcb_ack = (int) (p.seq + n);
						System.out.println("READ: num_read + p.length > maxlen sending packet seq " + this.tcb.tcb_seq + " ack " + this.tcb.tcb_ack);
						send_tcp_packet(this.tcb.tcb_their_ip_addr,
								new byte[0],
								0,
								this.tcb.tcb_our_port,
								this.tcb.tcb_their_port,
								this.tcb.tcb_seq,
								this.tcb.tcb_ack,
								TcpPacket.TCP_ACK);
						return maxlen;
					}else{
						bb.put(p.data, 0, p.length);
						num_read += p.length;
						
//						this.tcb.tcb_ack += p.length;
						this.tcb.tcb_ack = (int) (p.seq + p.length);
						
						System.out.println("READ: sending packet seq " + this.tcb.tcb_seq + " ack " + this.tcb.tcb_ack);
						send_tcp_packet(this.tcb.tcb_their_ip_addr,
								new byte[0],
								0,
								this.tcb.tcb_our_port,
								this.tcb.tcb_their_port,
								this.tcb.tcb_seq,
								this.tcb.tcb_ack,
								TcpPacket.TCP_ACK);
					}
					oldSeq = p.seq;
				} else {
					System.out.println("READ: duplicate seq received");
				}
			}
			
			bb.rewind();
			/*Check if EOF is in the buffer*/
			for(int j = 0; j < buf.length - offset; j++){
				if(bb.get() == 0xff){
					bb.rewind();
					bb.get(buf, offset, j);
					return j;
				}
			}
			
			bb.rewind();
			bb.get(buf, offset, num_read);
			
			return num_read;
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
			System.out.println("WRITE: len " + len);
			int sent = 0;
			int left;
			int nbytes = -1;
			
			TcpPacket p = new TcpPacket();
			ByteBuffer sentbb;
			System.out.println("WRITE " + offset + "  " + len + "  " + sent);
			//check if we are in the correct state
			
			if(this.tcb.tcb_state == ConnectionState.S_FIN_WAIT_1 ||
					this.tcb.tcb_state == ConnectionState.S_LAST_ACK ||
					this.tcb.tcb_state == ConnectionState.S_CLOSED){
				System.out.println("WRITE not in correct state");
				return -1;
			}

			while(sent < len) {
				System.out.println("WRITE loop " + sent + " " + len);
				left = len - sent;

				if(left > TcpPacket.MAX_PACKET_SIZE)
					nbytes = TcpPacket.MAX_PACKET_SIZE;
				else
					nbytes = left;

				byte[] tmp = new byte[nbytes];
//				System.arraycopy(buf, sent+offset, tmp, sent, nbytes);
				System.arraycopy(buf, sent+offset, tmp, 0, nbytes);

				int count = 0;
				long ackedBytes = 0;
				for(int it = 0; it < 10; it++){
					sentbb = send_tcp_packet(this.tcb.tcb_their_ip_addr,
							tmp,
							nbytes,
							this.tcb.tcb_our_port,
							this.tcb.tcb_their_port,
							this.tcb.tcb_seq,
							this.tcb.tcb_ack,
							TcpPacket.TCP_SYN);
					
					System.out.println("WRITE: sending packet " + "seq " +	this.tcb.tcb_seq + " ack " +
							this.tcb.tcb_ack);

					if(sentbb == null){
						System.out.println("WRITE: failed to send");
						continue;
					}

					
					if(recv_tcp_packet(p, true)){
						System.out.println("WRITE: ACK received");
						ackedBytes = p.ack - this.tcb.tcb_seq;
						break;
					} else {
						System.out.println("WRITE: Timeout ACK");
						count++;
					}
				}
				
				System.out.println("WRITE: count " + count);

				if(count != 10) {
					System.out.println("WRITE ackedBytes " + ackedBytes + " seq " + this.tcb.tcb_seq + " nbytes " + nbytes);
					sent += ackedBytes;
					this.tcb.tcb_seq += ackedBytes;
				} else {
					System.out.println("WRITE: return after timeout - sent " + sent);
					return sent == 0 ? -1 : sent;
				}
				
				
//				if(p.ack != this.tcb.tcb_seq + nbytes){
//					System.out.println("WRITE wrong seq " + p.ack + "  " + this.tcb.tcb_seq + "  " + nbytes);
//					this.tcb.tcb_seq += nbytes;
//					System.out.println("WRITE wrong seq " + p.ack + "  " + this.tcb.tcb_seq + "  " + nbytes);
//					continue;
//				}
				
				
			}
			return nbytes;
		}
//		/**
//		 * Reads bytes from the socket into the buffer.
//		 * This call is not required to return maxlen bytes
//		 * every time it returns.
//		 *
//		 * @param buf the buffer to read into
//		 * @param offset the offset to begin reading data into
//		 * @param maxlen the maximum number of bytes to read
//		 * @return the number of bytes read, or -1 if an error occurs.
//		 */
//		public int read(byte[] buf, int offset, int maxlen) {
//			// Read from the socket here.
//			TcpPacket p = new TcpPacket();
//			ByteBuffer bb = ByteBuffer.allocate(maxlen);
//			int num_read = 0;
//			
//			//TODO in case there is no data and the other side closed???
//			if(maxlen <= 0)
//				return -1;
//			
//			while(num_read < maxlen){
//				try{
//					if(!recv_tcp_packet(p, false))
//						return -1;
//				} catch(InterruptedException e){
//					e.printStackTrace();
//				}
//				//TODO check retransmitting previous packet
//				
////				System.out.println("READ: SEQ " + this.tcb.tcb_seq + "  " + p.ack);
////				if(p.ack != this.tcb.tcb_seq + 1){	//if not the correct ack then continue
////					System.out.println("READ: SEQ != ACK");
////					continue;
////				}else{
////					System.out.println("READ: SEQ == ACK");
////				}
//
//				if(p.ack != this.tcb.tcb_seq)
//					continue;
//				
//				this.tcb.tcb_seq = (int) p.ack + 1;
//				this.tcb.tcb_ack = (int) p.seq + 1;
//				
////				System.out.println("READ: buf " + p.data[0]);
//				if(num_read + p.length > maxlen){
//					num_read += p.length;
//					int n = maxlen - num_read;
//					bb.put(p.data, 0, n);
//					
//					this.tcb.tcb_ack += p.length;
//					send_tcp_packet(this.tcb.tcb_their_ip_addr,
//							new byte[0],
//							0,
//							this.tcb.tcb_our_port,
//							this.tcb.tcb_their_port,
//							this.tcb.tcb_seq,
//							this.tcb.tcb_ack,
//							TcpPacket.TCP_ACK);
//					
//					return maxlen;
//				}else{
//					bb.put(p.data, 0, p.length);
//					num_read += p.length;
//					
//					this.tcb.tcb_ack += p.length;
//	
//					send_tcp_packet(this.tcb.tcb_their_ip_addr,
//							new byte[0],
//							0,
//							this.tcb.tcb_our_port,
//							this.tcb.tcb_their_port,
//							this.tcb.tcb_seq,
//							this.tcb.tcb_ack,
//							TcpPacket.TCP_ACK);
//				}
//			}
//			
//			bb.rewind();
//			/*Check if EOF is in the buffer*/
//			for(int j = 0; j < buf.length - offset; j++){
//				if(bb.get() == 0xff){
//					bb.rewind();
//					bb.get(buf, offset, j);
//					return j;
//				}
//			}
//			
//			bb.rewind();
//			bb.get(buf, offset, num_read);
//			
//			return num_read;
//		}
//
//		/**
//		 * Writes to the socket from the buffer.
//		 *
//		 * @param buf the buffer to
//		 * @param offset the offset to begin writing data from
//		 * @param len the number of bytes to write
//		 * @return the number of bytes written or -1 if an error occurs.
//		 */
//		public int write(byte[] buf, int offset, int len) {
//			int sent = 0;
//			int left;
//			int nbytes = -1;
//			
//			TcpPacket p = new TcpPacket();
//			ByteBuffer sentbb;
////			System.out.println("WRITE " + offset + "  " + len + "  " + sent);
//			//check if we are in the correct state
//			
//			if(this.tcb.tcb_state == ConnectionState.S_FIN_WAIT_1 ||
//					this.tcb.tcb_state == ConnectionState.S_LAST_ACK ||
//					this.tcb.tcb_state == ConnectionState.S_CLOSED){
//				System.out.println("WRITE not in correct state");
//				return -1;
//			}
//
//			while(sent < len) {
////				System.out.println("WRITE loop " + sent + " " + len);
//				left = len - sent;
//
//				if(left > TcpPacket.MAX_PACKET_SIZE)
//					nbytes = TcpPacket.MAX_PACKET_SIZE;
//				else
//					nbytes = left;
//
//				byte[] tmp = new byte[nbytes];
//				System.arraycopy(buf, sent+offset, tmp, 0, nbytes);
//				
//				System.out.println("WRITE: tmp: " + tmp[0]);
//
//				int count = 0;
//				for(int it = 0; it < 10; it++){
//					sentbb = send_tcp_packet(this.tcb.tcb_their_ip_addr,
//							tmp,
//							nbytes,
//							this.tcb.tcb_our_port,
//							this.tcb.tcb_their_port,
//							this.tcb.tcb_seq,
//							this.tcb.tcb_ack,
//							TcpPacket.TCP_SYN);
//					
////					System.out.println("Write sending packet " + "seq " +	this.tcb.tcb_seq + " ack" +
////							this.tcb.tcb_ack + " nbytes " + nbytes);
//
//					
//					System.out.println("WRITE: send ACK" + this.tcb.tcb_ack + " SEQ: " + this.tcb.tcb_seq);
//					
//					if(sentbb == null)
//						continue;
//					
//					this.tcb.tcb_seq += nbytes;
//
//					try{
//						if(recv_tcp_packet(p, true)){
//							//TODO Check if the correct ack
//							System.out.println("WRITE: received ACK: " + p.ack + " SEQ:" + p.seq);
//							break;
//						}
//					} catch(InterruptedException e1){
//						System.out.println("WRITE: Timeout ACK");
//						count ++;
//					}
//				}
//
//				if(count != 10)
//					sent += nbytes;
//				else
//					return sent;
//				
//				
////				if(p.ack != this.tcb.tcb_seq + nbytes){
////					System.out.println("WRITE wrong seq " + p.ack + "  " + this.tcb.tcb_seq + "  " + nbytes);
////					this.tcb.tcb_seq += nbytes;
////					System.out.println("WRITE wrong seq " + p.ack + "  " + this.tcb.tcb_seq + "  " + nbytes);
////					continue;
////				}
//			}
//			return nbytes;
//		}

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

		//		private int count = 0;

		public ByteBuffer send_tcp_packet(int dst_address, byte[] buf, int length, short src_port,
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

			dst_ip = dst_address;
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

			//			byte mask = (byte) (flags & 0x1b);
			//			mask |= (1 << 3) & 0xff;

			byte mask = (byte)(flags & ~(TcpPacket.TCP_URG | TcpPacket.TCP_RST));
			mask |= TcpPacket.TCP_PUSH;

			//			System.out.println("Packet number " + count + " mask " + Byte.toString(mask));
			//			count++;
			//			System.out.println("mask " + (int)mask);

			pseudo.put(mask);
			pseudo.putShort((short) 8192);	//Window size equal to the maximal size of a packet	

			pseudo.putInt(0);			//The Checksum and the urgent pointer
			pseudo.put(buf);

			if(length % 2 != 0){
				pseudo.put((byte) 0);
			}

			long checksum = calculateChecksum(pseudo.array());
			//			System.out.println("Checksum " + Long.toHexString(checksum));

			tcp_packet.putShort(src_port);
			tcp_packet.putShort(dst_port);
			tcp_packet.putInt(seq_number);
			tcp_packet.putInt(ack_number);
			tcp_packet.put((byte) 0x50);

			tcp_packet.put(mask);
			tcp_packet.putShort((short) 8192);	//Window size

			tcp_packet.putShort((short) checksum);
			tcp_packet.putShort((short) 0); //Urgent pointer
			tcp_packet.put(buf);


			//			System.out.println(tcp_packet);
			//			System.out.println("Local address " + ip.getLocalAddress().toString());

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
			//			System.out.println("Send: " + length);
			Packet p1 = new Packet(dst_ip, 6, 0, tcp_packet.array(), length + 20); //TODO why the length can be smaller than the data size?
			p1.source = localaddr;
			//			System.out.println(p1.toString());

			try{
				ip.ip_send(p1);
			} catch(IOException e){
				return null;			//if the send fails
			}

			return tcp_packet;
		}

//		public boolean recv_tcp_packet (TcpPacket tcpp, boolean timeout) throws InterruptedException{
		public boolean recv_tcp_packet (TcpPacket tcpp, boolean timeout) {
			Packet p = new Packet();
			ByteBuffer pseudo;

			try{
				if(timeout == true)
					ip.ip_receive_timeout(p, 1);
				else
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

				//				System.out.println(p.source + "  " + p.destination + "  " + p.protocol + " " + p.length + "  " + p.data);


				pseudo.put(p.data);

				if (p.length % 2 == 1){
					pseudo.put((byte) 0);
				}

				tcpp.set_all(p);

				long checksum = calculateChecksum(pseudo.array());
				if(checksum != 0){
					System.out.println("Recv_tcp_packet: checksum is not 0 " + checksum);
					return false;
				}

				//				System.out.println("Checksum " + Long.toHexString(checksum));

			} catch (IOException e){
				e.printStackTrace();
				return false;
			} catch(InterruptedException e){
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
