package nl.vu.cs.cn;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
			tcb = new TcpControlBlock();
		}

		/**
		 * Construct a server socket bound to the given local port.
		 *
		 * @param port the local port to use
		 */
		private Socket(int port) {
			tcb = new TcpControlBlock();
			
			if(port < 0){
				System.err.println(port + " is not a valid port number.");
				System.exit(1);
			}
			
			this.tcb.tcb_our_port = port;
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
			TCPPacket p = new TCPPacket();

			if(this.tcb.tcb_state != ConnectionState.S_CLOSED){
				System.err.println("The socket is not in the correct state");
				return false;
			}

			this.tcb.tcb_their_ip_addr = dst.getAddress();
			this.tcb.tcb_our_ip_addr = (int) ip.getLocalAddress().getAddress();
			
			
			Random generator = new Random();
			this.tcb.tcb_our_port = Math.abs(generator.nextInt(2  * Short.MAX_VALUE ));
			this.tcb.tcb_seq = (long) generator.nextInt() + (long)Integer.MAX_VALUE; //unsigned int

			
			this.tcb.tcb_their_port = port;
			
			this.tcb.tcb_ack = 0;

			//send the first SYN packet from the three-way handshake
			//check if we get 10 timeouts
			int count = 0;
			for(int it = 0; it < 10; it++){
				bb = send_tcp_packet(dst.getAddress(),
						new byte[0],
						0,
						this.tcb.tcb_our_port,
						this.tcb.tcb_their_port,
						this.tcb.tcb_seq,
						this.tcb.tcb_ack,
						TCPPacket.TCP_SYN);
				System.out.println("CONNECT first packet: seq " + this.tcb.tcb_seq + " ack " + this.tcb.tcb_ack);

				if(bb == null){			// if the send is not successful return false 
					System.out.println("CONNECT: bb null");
					return false;
				}

				this.tcb.tcb_state = ConnectionState.S_SYN_SENT;



				if(recv_tcp_packet(p, true)){
                    System.out.println("CONNECT: SYN/ACK packet received seq " + p.seq + " ack " + p.ack);
					break;

				} else {
					count ++;
				}
			}

			if(count == 10){
				System.out.println("CONNECT: Max number of timeouts (SYN/ACK)");
				return false;
			}

			this.tcb.tcb_seq = add_uints(this.tcb.tcb_seq, 1);
			if(p.checkFlags(TCPPacket.TCP_SYN_ACK) && p.dst_port == this.tcb.tcb_our_port
					&& p.src_port == this.tcb.tcb_their_port && this.tcb.tcb_seq == p.ack){

//				this.tcb.incrSeq(1);
//				this.tcb.tcb_seq++;
				this.tcb.tcb_ack = add_uints(p.seq , 1);
				bb = send_tcp_packet(dst.getAddress(),
						new byte[0],
						0,
						this.tcb.tcb_our_port,
						this.tcb.tcb_their_port,
						this.tcb.tcb_seq,
						this.tcb.tcb_ack,
						TCPPacket.TCP_ACK);
				System.out.println("CONNECT third packet: seq" + this.tcb.tcb_seq + " ack " + this.tcb.tcb_ack);


				this.tcb.tcb_state = ConnectionState.S_ESTABLISHED;
                System.out.println("CONNECT: returning true");
				return true;
			}

			System.out.println("CONNECT: returning false");
			return false;
		}
		

		/**
		 * Accept a connection on this socket.
		 * This call blocks until a connection is made.
		 */
		public void accept() {

			TCPPacket p = new TCPPacket();
			ByteBuffer bb = null;


			if(this.tcb.tcb_state != ConnectionState.S_CLOSED)
				return;

			this.tcb.tcb_state = ConnectionState.S_LISTEN;
			this.tcb.tcb_our_ip_addr = ip.getLocalAddress().getAddress();

			while(true){

				
				recv_tcp_packet(p, false);				//does not timeout
			
                System.out.println("ACCEPT: a packet accepted seq " + p.seq + " ack " + p.ack);


				if(p.checkFlags(TCPPacket.TCP_SYN) && (!p.checkFlags(TCPPacket.TCP_ACK))
						&& this.tcb.tcb_our_port == p.dst_port) {
					
                    System.out.println("ACCEPT: packet matches flags addresses and ports");

					this.tcb.tcb_state = ConnectionState.S_SYN_RCVD;
					this.tcb.tcb_their_ip_addr = p.src_ip ;
					this.tcb.tcb_their_port =  p.src_port;
					this.tcb.tcb_ack = add_uints(p.seq, 1);
					
					Random generator = new Random();
					this.tcb.tcb_seq = (long) generator.nextInt() + (long)Integer.MAX_VALUE; //unsigned int
//					int tmp_seq = generator.nextInt(Integer.MAX_VALUE / 2);
//					this.tcb.tcb_seq = tmp_seq < 0 ? (-1) * tmp_seq : tmp_seq;
					
					//					System.out.println("Accept: dst ip" + this.tcb.tcb_their_ip_addr);
					int count = 0;

					for(int it = 0; it < 10; it++){
						bb = this.send_tcp_packet(this.tcb.tcb_their_ip_addr,
								new byte[0],
								0,
								this.tcb.tcb_our_port,
								this.tcb.tcb_their_port,
								this.tcb.tcb_seq,
								this.tcb.tcb_ack,
								TCPPacket.TCP_SYN_ACK);
						System.out.println("ACCEPT: sending second packet: seq" + this.tcb.tcb_seq + " ack " + this.tcb.tcb_ack);

						//				System.out.println("Accept: SYN/ACK sent");

						if(bb == null){
                            System.out.println("ACCEPT: sent returned null");
							continue;
						}

						//check if we get 10 timeouts
					
						if(recv_tcp_packet(p, true)){
							if(p.src_ip  == tcb.tcb_their_ip_addr && p.checkFlags(TCPPacket.TCP_ACK)
									&& p.ack == add_uints(this.tcb.tcb_seq, 1)
									&& p.seq == this.tcb.tcb_ack){

								System.out.println("ACCEPT: connected");
								this.tcb.tcb_state = ConnectionState.S_ESTABLISHED;
								this.tcb.tcb_seq = add_uints(this.tcb.tcb_seq, 1);			//TODO check if that is correct
								return;
							}
							System.out.println("ACCEPT: timeout waiting for ack of second packet");
							count ++;
						}
					}

					//no need to check count if you reach here
					if(count == 10){
						System.out.println("ACCEPT: Max number timeouts");
						this.tcb.tcb_state = ConnectionState.S_LISTEN;
						continue;
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
			System.out.println("READ: maxlen " + maxlen + " offset " + offset);
			if (tcb.tcb_state == ConnectionState.S_CLOSE_WAIT) {
				System.out.println("READ: FIN received from other end, read pointless");
				return -1;
			}
			
			

			TCPPacket p = new TCPPacket();
			ByteBuffer bb = ByteBuffer.allocate(maxlen);
			
			int num_read = 0;
			
			//TODO in case there is no data and the other side closed???
			if(maxlen <= 0)
				return -1;
			
			long oldSeq = -1;
			while(num_read < maxlen){

				
				
				if(num_read == 0){ 						//if no data received so far block until receive
					if(!recv_tcp_packet(p, false)) {
						System.err.println("READ: error occured during packet transmission");
						continue;
					}
				} else{									//do not block otherwise
					if(!recv_tcp_packet(p, true)) {
//							String str = new String(buf, "UTF-8");
//							System.out.println("READ: message " + str + " size: " + num_read);
//							System.out.println("READ: message buf " + buf);

							bb.rewind();
							bb.get(buf, offset, num_read);
							
//							try {
//								System.out.println("READ: message " + new String(buf, "UTF-8") );
//							} catch (UnsupportedEncodingException e) {
//								// TODO Auto-generated catch block
//								e.printStackTrace();
//							}
						return num_read;
					}					
				}
				
				System.out.println("READ: received packet seq " + p.seq + " length "  + p.length);
			
				//TODO check retransmitting previous packet

				//TODO p.seq <= oldSeq + maxsize
				
				System.out.println("READ: ips: " + p.src_ip + " " + this.tcb.tcb_their_ip_addr + "\n" +
						"SRCPORTS: " + p.src_port + "  "  + this.tcb.tcb_their_port + "\n" +
						"DSTPORTS: " + p.dst_port + "  " +  this.tcb.tcb_our_port);
				
				
				
				

				
				if (p.seq > oldSeq && 
						p.src_ip == this.tcb.tcb_their_ip_addr &&
						p.src_port == this.tcb.tcb_their_port &&
						p.dst_port == this.tcb.tcb_our_port &&
						p.seq == this.tcb.tcb_ack) {
					
					
					if (p.checkFlags(TCPPacket.TCP_FIN) && (this.tcb.tcb_state == ConnectionState.S_ESTABLISHED || this.tcb.tcb_state == ConnectionState.S_FIN_WAIT_2)){
						int retVal = 0;
						System.out.println("READ: FIN received");
						tcb.tcb_ack = add_uints(p.seq, p.length + 1);
						//make sure read does not get confused
						if (p.length > 0) {
							if (num_read + p.length > maxlen) {
								bb.put(p.data, 0, maxlen - num_read);
								retVal = maxlen;
								
							}  else {
								bb.put(p.data, 0, p.length);
								num_read += p.length;
								retVal = num_read;
							}
						}
						
						send_tcp_packet(this.tcb.tcb_their_ip_addr,
								new byte[0],
								0,
								tcb.tcb_our_port,
								tcb.tcb_their_port,
								tcb.tcb_seq,
								tcb.tcb_ack,
								TCPPacket.TCP_ACK);
						//check if sent successful
						
						if (this.tcb.tcb_state ==ConnectionState.S_ESTABLISHED) {
							this.tcb.tcb_state = ConnectionState.S_CLOSE_WAIT;
							return  retVal;
						}
						
						if (this.tcb.tcb_state == ConnectionState.S_FIN_WAIT_2) {
							try {
								this.tcb.tcb_state  = ConnectionState.S_TIME_WAIT;
								Thread.sleep(1000);
								this.tcb = new TcpControlBlock();
							} catch (InterruptedException e) {
								System.out.println("READ: interrupted while closing");
								this.tcb = new TcpControlBlock();
								e.printStackTrace();
							}
							return retVal;
						}


					}
					
					if(num_read + p.length > maxlen){
						int n = maxlen - num_read;
						bb.put(p.data, 0, n);
					
//						this.tcb.tcb_ack += n;
						this.tcb.tcb_ack = add_uints(p.seq, n);

						send_tcp_packet(this.tcb.tcb_their_ip_addr,
								new byte[0],
								0,
								this.tcb.tcb_our_port,
								this.tcb.tcb_their_port,
								this.tcb.tcb_seq,
								this.tcb.tcb_ack,
								TCPPacket.TCP_ACK);
						
						
						bb.rewind();
						bb.get(buf, offset, maxlen);
						
						return maxlen;
						
					}else{
						bb.put(p.data, 0, p.length);
						num_read += p.length;
						
//						this.tcb.tcb_ack += p.length;
						this.tcb.tcb_ack = add_uints(p.seq, p.length);
						
						System.out.println("READ: sending packet seq " + this.tcb.tcb_seq + " ack " + this.tcb.tcb_ack);
						ByteBuffer b1 = send_tcp_packet(this.tcb.tcb_their_ip_addr,
								new byte[0],
								0,
								this.tcb.tcb_our_port,
								this.tcb.tcb_their_port,
								this.tcb.tcb_seq,
								this.tcb.tcb_ack,
								TCPPacket.TCP_ACK);
						
						if(b1 == null)
							System.out.println("READ: empty bytebuffer");
					}
					oldSeq = p.seq;
				} else {
					System.out.println("READ: duplicate seq received");
				}
			}
			
			bb.rewind();
			
			bb.rewind();
			bb.get(buf, offset, num_read);
			System.out.println("READ returning num_read: " + num_read);
			
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
			
			TCPPacket p = new TCPPacket();
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

				if(left > TCPPacket.MAX_PACKET_SIZE)
					nbytes = TCPPacket.MAX_PACKET_SIZE;
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
							TCPPacket.TCP_SYN);
					
					System.out.println("WRITE: sending packet " + "seq " +	this.tcb.tcb_seq + " ack " +
							this.tcb.tcb_ack);

					if(sentbb == null){
						System.out.println("WRITE: failed to send");
						continue;
					}
					
					if(recv_tcp_packet(p, true) &&
							p.src_ip == this.tcb.tcb_their_ip_addr &&
							p.src_port == this.tcb.tcb_their_port &&
							p.dst_port ==  this.tcb.tcb_our_port ){
						
//						
//						System.out.println("WRITE: ACK received");
//						System.out.println("WRITE: ips: " + p.src_ip + " " + this.tcb.tcb_their_ip_addr + "\n" +
//								"SRCPORTS: " + p.src_port + "  "  + this.tcb.tcb_their_port + "\n" +
//								"DSTPORTS: " + p.dst_port + "  " +  this.tcb.tcb_our_port);
//							
						if (p.checkFlags(TCPPacket.TCP_FIN) && p.seq == this.tcb.tcb_ack && this.tcb.tcb_state == ConnectionState.S_ESTABLISHED) {
							System.out.println("WRITE: FIN received");
							tcb.tcb_ack = add_uints(tcb.tcb_ack, 1) ;
							//make sure read does not get confused
							sentbb = send_tcp_packet(this.tcb.tcb_their_ip_addr,
									new byte[0],
									0,
									tcb.tcb_our_port,
									tcb.tcb_their_port,
									tcb.tcb_seq,
									tcb.tcb_ack,
									TCPPacket.TCP_ACK);
							//check if sent successful
							
							this.tcb.tcb_state = ConnectionState.S_CLOSE_WAIT;
							
						}
						if (p.checkFlags(TCPPacket.TCP_ACK) && p.ack <= add_uints(this.tcb.tcb_seq, nbytes)) {
							ackedBytes = p.ack - this.tcb.tcb_seq;
							break;
						} else {
							System.out.println("WRITE: unexpected packet");
							count ++;
						}
						
					} else {
						System.out.println("WRITE: Timeout ACK");
						count++;
					}
				}
				
				System.out.println("WRITE: count " + count);

				if(count != 10) {
					System.out.println("WRITE ackedBytes " + ackedBytes + " seq " + this.tcb.tcb_seq + " nbytes " + nbytes);
					sent += ackedBytes;
					this.tcb.tcb_seq = add_uints(this.tcb.tcb_seq, ackedBytes);
				} else {
					System.out.println("WRITE: return after timeout - sent " + sent);
					return sent == 0 ? -1 : sent;
				}
				
				
			}
			return sent;
		}

		/**
		 * Closes the connection for this socket.
		 * Blocks until the connection is closed.
		 *
		 * @return true unless no connection was open.
		 */
		
		public boolean close() {
			
			if (this.tcb.tcb_state == ConnectionState.S_CLOSED) {
				System.out.println("CLOSE: already closed, returning false");
				return false;
			}

			// Close the socket cleanly here.
			if (this.tcb.tcb_state == ConnectionState.S_SYN_SENT || this.tcb.tcb_state == ConnectionState.S_LISTEN) {
				System.out.println("CLOSE: connection not established yet - returning true");
				this.tcb = new TcpControlBlock();
				return true;
			}
			
			TCPPacket p = new TCPPacket();
			ByteBuffer bb = null;

			if (this.tcb.tcb_state == ConnectionState.S_CLOSE_WAIT) {
				System.out.println("CLOSE: CLOSE_WAIT");
				for (int it = 0; it < 10; it++) {
					bb = this.send_tcp_packet(this.tcb.tcb_their_ip_addr,
							new byte[0],
							0,
							this.tcb.tcb_our_port,
							this.tcb.tcb_their_port, 
							this.tcb.tcb_seq,
							this.tcb.tcb_ack,
							TCPPacket.TCP_FIN);
					System.out.println("CLOSE: S_CLOSE_WAIT sending fin: seq " + this.tcb.tcb_seq + " ack " + this.tcb.tcb_ack);
					if (bb == null) {
						continue; //TODO should close fail if in case packet cannot be sent
					}
					this.tcb.tcb_state = ConnectionState.S_LAST_ACK;
					if (recv_tcp_packet(p, true)) {
						if (p.checkFlags(TCPPacket.TCP_ACK) &&
								this.tcb.tcb_our_port == p.dst_port &&
								this.tcb.tcb_their_port == p.src_port &&
								this.tcb.tcb_their_ip_addr == p.src_ip &&
								p.ack == (add_uints(this.tcb.tcb_seq , 1))) { //check all the stuff
							//close stuff
							this.tcb.tcb_state = ConnectionState.S_CLOSED;
							System.out.println("CLOSE: CLOSE_WAIT -> LAST_ACK received ack, closing");
							return true;
							
						}
						
					}
					
				}
				return true;
			}
			
			
			if (this.tcb.tcb_state == ConnectionState.S_SYN_RCVD || this.tcb.tcb_state == ConnectionState.S_ESTABLISHED) {
				for (int it = 0; it < 10; it++) {
					bb = this.send_tcp_packet(this.tcb.tcb_their_ip_addr,
							new byte[0],
							0,
							this.tcb.tcb_our_port,
							this.tcb.tcb_their_port, 
							this.tcb.tcb_seq,
							this.tcb.tcb_ack,
							TCPPacket.TCP_FIN);
					System.out.println("CLOSE: S_SYN_RCVD or S_ESTABLISHED sending fin: seq " + this.tcb.tcb_seq + " ack " + this.tcb.tcb_ack);
					if (bb == null) {
						continue; //TODO should close fail if in case packet cannot be sent
					}
					
					this.tcb.tcb_state = ConnectionState.S_FIN_WAIT_1;
					
					if (recv_tcp_packet(p, true)) {
						if (this.tcb.tcb_our_port == p.dst_port &&
								this.tcb.tcb_their_port == p.src_port &&
								this.tcb.tcb_their_ip_addr == p.src_ip) {
							//check all the stuff
							
							if (p.checkFlags(TCPPacket.TCP_FIN) && this.tcb.tcb_ack == p.seq) { //check if your ack matches
								System.out.println("CLOSE: FIN_WAIT_1 received FIN");
								TCPPacket pClosing = new TCPPacket();
								for (int itClosing = 0; itClosing < 10; itClosing++) {
									bb = this.send_tcp_packet(this.tcb.tcb_their_ip_addr,
											new byte[0],
											0,
											this.tcb.tcb_our_port,
											this.tcb.tcb_their_port, 
											this.tcb.tcb_seq,
											add_uints(p.seq, 1),
											TCPPacket.TCP_ACK);
									if (bb == null) {
										continue;//will lead to returning false
									}
									this.tcb.tcb_state = ConnectionState.S_CLOSING;
									System.out.println("CLOSE: FIN_WAIT_1 -> CLOSING sent ack");
									if (recv_tcp_packet(pClosing, true) &&
											this.tcb.tcb_our_port == pClosing.dst_port &&
											this.tcb.tcb_their_ip_addr == pClosing.src_ip &&
											pClosing.checkFlags(TCPPacket.TCP_ACK) &&
											pClosing.ack == add_uints(this.tcb.tcb_seq, 1)) {
										this.tcb.tcb_state = ConnectionState.S_TIME_WAIT;
										System.out.println("CLOSE: FIN_WAIT_1 -> CLOSING -> TIME_WAIT received ack");
										try {
											Thread.sleep(1000);
											this.tcb = new TcpControlBlock();
										} catch (InterruptedException e) {
											System.out.println("CLOSE: interrupted while waiting");
											this.tcb = new TcpControlBlock();
											e.printStackTrace();
										}
										return true;
									}
									
								}

							}
							// else if?
							if (p.checkFlags(TCPPacket.TCP_ACK) && p.ack == add_uints(this.tcb.tcb_seq, 1)) {
								this.tcb.tcb_state = ConnectionState.S_FIN_WAIT_2;
								System.out.println("CLOSE: FIN_WAIT_2 received ack");
								TCPPacket pFinWait2 = new TCPPacket();
								
								//check if a fin follows
								for (int itFinWait2 = 0; itFinWait2 < 10; itFinWait2 ++) {
									//check if packet is for us
									if (recv_tcp_packet(pFinWait2, true) && 
											this.tcb.tcb_our_port == pFinWait2.dst_port &&
											this.tcb.tcb_their_port == pFinWait2.src_port &&
											this.tcb.tcb_their_ip_addr == pFinWait2.src_ip) {
										//check if fin
										if (pFinWait2.checkFlags(TCPPacket.TCP_FIN) && tcb.tcb_ack == pFinWait2.seq) { //check if your ack matches
											//sned ack
											System.out.println("CLOSE: FIN_WAIT_2 received fin");
											bb = this.send_tcp_packet(this.tcb.tcb_their_ip_addr,
													new byte[0],
													0,
													this.tcb.tcb_our_port,
													this.tcb.tcb_their_port, 
													this.tcb.tcb_seq,
													add_uints(pFinWait2.seq, 1),
													TCPPacket.TCP_ACK);
											if (bb == null) {
												continue;//will lead to returning false
											}
											this.tcb.tcb_state = ConnectionState.S_TIME_WAIT;
											System.out.println("CLOSE: FIN_WAIT_2 -> TIME_WAIT closing");
											try {
												Thread.sleep(1000);
												this.tcb = new TcpControlBlock();
												
											} catch (InterruptedException e) {
												System.out.println("CLOSE: Interrupted while waiting");
												this.tcb = new TcpControlBlock();
												e.printStackTrace();
											}
											
										} 
										//fin does not follow, may continue reading, no need to loop furder
										return true;
									}

								}
								//no fin arrives within 10 sec, may continue reading
								return true;
							}

							
							
						}
						
						
					}
					
				}
//				return false; //no acks or fins
			}

			return true;
		}

		public ByteBuffer send_tcp_packet(int dst_address, byte[] buf, int length, int src_port,
				int dst_port, long seq_number, long ack_number, byte flags){

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
			
			pseudo.putChar((char) src_port);
			pseudo.putChar((char) dst_port);
			
			pseudo.put((byte)(seq_number >>> 24));
			pseudo.put((byte)(seq_number >> 16 & 0xff));
			pseudo.put((byte)(seq_number >> 8 & 0xff));
			pseudo.put((byte)(seq_number & 0xff));

			pseudo.put((byte)(ack_number >>> 24));
			pseudo.put((byte)(ack_number >> 16 & 0xff));
			pseudo.put((byte)(ack_number >> 8 & 0xff));
			pseudo.put((byte)(ack_number & 0xff));
			
			pseudo.put((byte) 0x50);	// The TCP header length = 5 and the 4 empty bits

			//			byte mask = (byte) (flags & 0x1b);
			//			mask |= (1 << 3) & 0xff;

			byte mask = (byte)(flags & ~(TCPPacket.TCP_URG | TCPPacket.TCP_RST));
			mask |= TCPPacket.TCP_PUSH;

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

			tcp_packet.putChar((char) src_port);
			tcp_packet.putChar((char) dst_port);
			
			tcp_packet.put((byte)(seq_number >>> 24));
			tcp_packet.put((byte)(seq_number >> 16 & 0xff));
			tcp_packet.put((byte)(seq_number >> 8 & 0xff));
			tcp_packet.put((byte)(seq_number & 0xff));

			tcp_packet.put((byte)(ack_number >>> 24));
			tcp_packet.put((byte)(ack_number >> 16 & 0xff));
			tcp_packet.put((byte)(ack_number >> 8 & 0xff));
			tcp_packet.put((byte)(ack_number & 0xff));
			
			tcp_packet.put((byte) 0x50);

			tcp_packet.put(mask);
			tcp_packet.putShort((short) 8192);	//Window size

			tcp_packet.putShort((short) checksum);
			tcp_packet.putShort((short) 0); //Urgent pointer
			tcp_packet.put(buf);


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

		public boolean recv_tcp_packet (TCPPacket tcpp, boolean timeout) {
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

				pseudo.put(p.data);

				if (p.length % 2 == 1){
					pseudo.put((byte) 0);
				}

				tcpp.ip2tcp(p);

				long checksum = calculateChecksum(pseudo.array());
				if(checksum != 0){
					System.out.println("Recv_tcp_packet: checksum is not 0 " + checksum);
					return false;
				}

				//				System.out.println("Checksum " + Long.toHexString(checksum));

			} catch (IOException e){
//				e.printStackTrace();
				System.out.println("RCV: ioexception");
				return false;
			} catch(InterruptedException e){
//				e.printStackTrace();
				System.out.println("RCV: interrupted");
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
				data = (((buf[i] << 8) & 0xFF00) | ((buf[i + 1]) & 0xFF));
				
				sum += data;

				if ((sum & 0xFFFF0000) > 0) {
					sum = sum & 0xFFFF;
					sum += 1;
				}

				i += 2;
				length -= 2;
			}

			if (length > 0) {

				sum += (buf[i] << 8 & 0xFF00);

				if ((sum & 0xFFFF0000) > 0) {
					sum = sum & 0xFFFF;
					sum += 1;
				}
			}

			sum = ~sum;
			sum = sum & 0xFFFF;
			return sum;

		}
		
		private long add_uints(long x, long y){
			return ((long) ((long)x + (long)y)) % ((long) ((long) 2 *  (long) Integer.MAX_VALUE));
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
