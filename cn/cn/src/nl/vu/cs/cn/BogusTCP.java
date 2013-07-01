package nl.vu.cs.cn;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Random;

import nl.vu.cs.cn.IP;
import nl.vu.cs.cn.IP.IpAddress;
import nl.vu.cs.cn.IP.Packet;
import nl.vu.cs.cn.TCPControlBlock;
import nl.vu.cs.cn.TCPControlBlock.ConnectionState;
import nl.vu.cs.cn.TCPPacket;

/**
 * This class represents a TCP stack. It should be built on top of the IP stack
 * which is bound to a given IP address.
 */
public class BogusTCP {

	/** The underlying IP stack for this TCP stack. */
	private IP ip;

	private BogusSocket instance = null;

	/**
	 * This class represents a TCP socket.
	 *
	 */
	public class BogusSocket {

		/* Hint: You probably need some socket specific data. */
		private TCPControlBlock tcb;

		/* A new thread for detecting timeouts */

		/**
		 * Construct a client socket.
		 */
		private BogusSocket() {
			tcb = new TCPControlBlock();
		}

		/**
		 * Construct a server socket bound to the given local port.
		 *
		 * @param port the local port to use
		 */
		private BogusSocket(int port) {
			tcb = new TCPControlBlock();

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

				if(bb == null){			// if the send is not successful return false 
					System.err.println("CONNECT: sending SYN failed");
					continue;
				}

				this.tcb.tcb_state = ConnectionState.S_SYN_SENT;



				if(recv_tcp_packet(p, true)){
					break;

				} else {
					count ++;
				}
			}

			if(count == 10){
				return false;
			}

			this.tcb.tcb_seq = add_uints(this.tcb.tcb_seq, 1);
			if(p.checkFlags(TCPPacket.TCP_SYN_ACK) && p.dst_port == this.tcb.tcb_our_port
					&& p.src_port == this.tcb.tcb_their_port && this.tcb.tcb_seq == p.ack){

				bb = send_tcp_packet(dst.getAddress(),
						new byte[0],
						0,
						this.tcb.tcb_our_port,
						this.tcb.tcb_their_port,
						this.tcb.tcb_seq,
						add_uints(p.seq, 1),
						TCPPacket.TCP_ACK);
				if (bb == null) {
					return false;
					
				}
				
				this.tcb.tcb_ack = add_uints(p.seq , 1);

				this.tcb.tcb_state = ConnectionState.S_ESTABLISHED;
				return true;
			}

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



				if(p.checkFlags(TCPPacket.TCP_SYN) && (!p.checkFlags(TCPPacket.TCP_ACK))
						&& this.tcb.tcb_our_port == p.dst_port) {


					this.tcb.tcb_state = ConnectionState.S_SYN_RCVD;
					this.tcb.tcb_their_ip_addr = p.src_ip ;
					this.tcb.tcb_their_port =  p.src_port;
					this.tcb.tcb_ack = add_uints(p.seq, 1);

					Random generator = new Random();
					this.tcb.tcb_seq = (long) generator.nextInt() + (long)Integer.MAX_VALUE; //unsigned int

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


						if(bb == null){
							continue;
						}


						if(recv_tcp_packet(p, true)){
							if(p.src_ip  == tcb.tcb_their_ip_addr && p.checkFlags(TCPPacket.TCP_ACK)
									&& p.ack == add_uints(this.tcb.tcb_seq, 1)
									&& p.seq == this.tcb.tcb_ack){

								this.tcb.tcb_state = ConnectionState.S_ESTABLISHED;
								this.tcb.tcb_seq = add_uints(this.tcb.tcb_seq, 1);			
								return;
							}
							count ++;
						}
					}

					//no need to check count if you reach here
					if(count == 10){
						this.tcb.tcb_state = ConnectionState.S_LISTEN;
						continue;
					}
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
			if (this.tcb.tcb_state == ConnectionState.S_CLOSE_WAIT || this.tcb.tcb_state == ConnectionState.S_CLOSING || this.tcb.tcb_state == ConnectionState.S_TIME_WAIT) {
				System.err.println("READ: FIN received from other end, read pointless, will wait forever");
				return -1;
			}
			if (this.tcb.tcb_state != ConnectionState.S_ESTABLISHED && this.tcb.tcb_state != ConnectionState.S_FIN_WAIT_1 && this.tcb.tcb_state != ConnectionState.S_FIN_WAIT_2) {
				System.err.println("READ: cannot read in this state - connection not established ot breaking down");
				return -1;
			}



			TCPPacket p = new TCPPacket();
			ByteBuffer bb = ByteBuffer.allocate(maxlen);
			ByteBuffer readbb = null;

			int num_read = 0;

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

						bb.rewind();
						bb.get(buf, offset, num_read);

						return num_read;
					}					
				}


				if (p.seq > oldSeq && 
						p.src_ip == this.tcb.tcb_their_ip_addr &&
						p.src_port == this.tcb.tcb_their_port &&
						p.dst_port == this.tcb.tcb_our_port &&
						p.seq == this.tcb.tcb_ack) {


					if (p.checkFlags(TCPPacket.TCP_FIN) && (this.tcb.tcb_state == ConnectionState.S_ESTABLISHED || this.tcb.tcb_state == ConnectionState.S_FIN_WAIT_2)){
						System.out.println("READ: FIN received");
						readbb = send_tcp_packet(this.tcb.tcb_their_ip_addr,
								new byte[0],
								0,
								this.tcb.tcb_our_port,
								this.tcb.tcb_their_port,
								this.tcb.tcb_seq,
								add_uints(p.seq, p.length + 1),
								TCPPacket.TCP_ACK);
						
						//check if sent successfully
						if (readbb == null) {
							System.out.println("READ: ack of in failed to send");
							continue;
						}
						
						int retVal = num_read;
						this.tcb.tcb_ack = add_uints(p.seq, p.length + 1);
						
						//if packet contains data
						if (p.length > 0) {
							//if data does not fit into buffer, truncate
							if (num_read + p.length > maxlen) {
								bb.put(p.data, 0, maxlen - num_read);
								retVal = maxlen;

							}  else {
								bb.put(p.data, 0, p.length);
								num_read += p.length;
								retVal = num_read;
							}
						}


						if (this.tcb.tcb_state ==ConnectionState.S_ESTABLISHED) {
							//not going to receive packets anymore, returning
							this.tcb.tcb_state = ConnectionState.S_CLOSE_WAIT;
							return  retVal;
						}

						if (this.tcb.tcb_state == ConnectionState.S_FIN_WAIT_2) {
							//delete TCB and close
							try {
								this.tcb.tcb_state  = ConnectionState.S_TIME_WAIT;
								Thread.sleep(1000);
								this.tcb = new TCPControlBlock();
							} catch (InterruptedException e) {
								System.out.println("READ: interrupted while closing");
								this.tcb = new TCPControlBlock();
								e.printStackTrace();
							}
							return retVal;
						}
					}
					//if data does not fit into buffer - truncate and ack received
					if(num_read + p.length > maxlen){
						int n = maxlen - num_read;
						readbb = send_tcp_packet(this.tcb.tcb_their_ip_addr,
								new byte[0],
								0,
								this.tcb.tcb_our_port,
								this.tcb.tcb_their_port,
								this.tcb.tcb_seq,
								add_uints(p.seq, n),
								TCPPacket.TCP_ACK);
						if (readbb == null) {
							System.out.println("READ: ack to write failed");
							continue;
						}
						
						bb.put(p.data, 0, n);

						this.tcb.tcb_ack = add_uints(p.seq, n);

						bb.rewind();
						bb.get(buf, offset, maxlen);

						return maxlen;

					}else{
						System.out.println("READ: sending packet seq " + this.tcb.tcb_seq + " ack " + this.tcb.tcb_ack);
						readbb =  send_tcp_packet(this.tcb.tcb_their_ip_addr,
								new byte[0],
								0,
								this.tcb.tcb_our_port,
								this.tcb.tcb_their_port,
								this.tcb.tcb_seq,
								add_uints(p.seq, p.length),
								TCPPacket.TCP_ACK);

						if(readbb == null) {
							System.out.println("READ: ack to write failed");
							continue;
						}
						
						bb.put(p.data, 0, p.length);
						num_read += p.length;

						this.tcb.tcb_ack = add_uints(p.seq, p.length);

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
			System.out.println("WRITE: len " + len + " offset " + offset);
			if(this.tcb.tcb_state == ConnectionState.S_FIN_WAIT_1 ||
					this.tcb.tcb_state == ConnectionState.S_FIN_WAIT_2 ||
					this.tcb.tcb_state == ConnectionState.S_TIME_WAIT ||
					this.tcb.tcb_state == ConnectionState.S_CLOSING ||
					this.tcb.tcb_state == ConnectionState.S_LAST_ACK ||
					this.tcb.tcb_state == ConnectionState.S_CLOSED){
//				System.out.println("WRITE: not in correct state, promised not to write anymore (called close)");
				return -1;
			}
			
			if (this.tcb.tcb_state != ConnectionState.S_ESTABLISHED && this.tcb.tcb_state != ConnectionState.S_CLOSE_WAIT) {
				System.out.println("WRITE: not in a state where it can write - connection not set up or disrupted");
			}
			
			//bytes sent
			int sent = 0;
			//bytes left to write
			int left;
			
			int nbytes = -1;

			TCPPacket p = new TCPPacket();
			ByteBuffer sentbb;
			//check if we are in the correct state


			while(sent < len) {
				System.out.println("WRITE loop " + sent + " " + len);
				left = len - sent;

				if(left > TCPPacket.MAX_PACKET_SIZE)
					nbytes = TCPPacket.MAX_PACKET_SIZE;
				else
					nbytes = left;

				byte[] tmp = new byte[nbytes];
				
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
							(byte) 0x00);


					if(sentbb == null){
//						System.out.println("WRITE: failed to send data packet");
						continue;
					}

					if(recv_tcp_packet(p, true) &&
							p.src_ip == this.tcb.tcb_their_ip_addr &&
							p.src_port == this.tcb.tcb_their_port &&
							p.dst_port ==  this.tcb.tcb_our_port ){

						
						if (p.checkFlags(TCPPacket.TCP_FIN) && p.seq == this.tcb.tcb_ack && this.tcb.tcb_state == ConnectionState.S_ESTABLISHED) {
							System.out.println("WRITE: FIN received");
							//make sure read does not get confused
							sentbb = send_tcp_packet(this.tcb.tcb_their_ip_addr,
									new byte[0],
									0,
									this.tcb.tcb_our_port,
									this.tcb.tcb_their_port,
									this.tcb.tcb_seq,
									add_uints(this.tcb.tcb_ack, 1),
									TCPPacket.TCP_ACK);
							
							//check if sent successful
							if (sentbb == null) {
								continue;
							}

							this.tcb.tcb_ack = add_uints(this.tcb.tcb_ack, 1) ;
							this.tcb.tcb_state = ConnectionState.S_CLOSE_WAIT;

						}
						if (p.checkFlags(TCPPacket.TCP_ACK) && p.ack <= add_uints(this.tcb.tcb_seq, nbytes)) {
							ackedBytes = p.ack < this.tcb.tcb_seq ? p.ack + Integer.MAX_VALUE - this.tcb.tcb_seq : p.ack - this.tcb.tcb_seq;
//							ackedBytes = p.ack - this.tcb.tcb_seq;
							break;
						} else {
							count ++;
						}

					} else {
						count++;
					}
				}


				if(count != 10) {
					sent += ackedBytes;
					this.tcb.tcb_seq = add_uints(this.tcb.tcb_seq, ackedBytes);
				} else {
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
				return false;
			}

	
			if (this.tcb.tcb_state == ConnectionState.S_SYN_SENT || this.tcb.tcb_state == ConnectionState.S_LISTEN) {
				this.tcb = new TCPControlBlock();
				return true;
			}

			TCPPacket p = new TCPPacket();
			ByteBuffer bb = null;

			if (this.tcb.tcb_state == ConnectionState.S_CLOSE_WAIT) {
				for (int it = 0; it < 10; it++) {
					bb = this.send_tcp_packet(this.tcb.tcb_their_ip_addr,
							new byte[0],
							0,
							this.tcb.tcb_our_port,
							this.tcb.tcb_their_port, 
							this.tcb.tcb_seq,
							this.tcb.tcb_ack,
							TCPPacket.TCP_FIN);
					
					if (bb == null) {
						continue; //failed to send FIN
					}
					
					this.tcb.tcb_state = ConnectionState.S_LAST_ACK;
					
					if (recv_tcp_packet(p, true)) {
						if (p.checkFlags(TCPPacket.TCP_ACK) &&
								this.tcb.tcb_our_port == p.dst_port &&
								this.tcb.tcb_their_port == p.src_port &&
								this.tcb.tcb_their_ip_addr == p.src_ip &&
								p.ack == (add_uints(this.tcb.tcb_seq , 1))) { 
							
							//close
							this.tcb.tcb_state = ConnectionState.S_CLOSED;
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
					
					if (bb == null) {
						continue; //sending fin failed
					}

					this.tcb.tcb_state = ConnectionState.S_FIN_WAIT_1;

					if (recv_tcp_packet(p, true)) {
						if (this.tcb.tcb_our_port == p.dst_port &&
								this.tcb.tcb_their_port == p.src_port &&
								this.tcb.tcb_their_ip_addr == p.src_ip) {
						

							if (p.checkFlags(TCPPacket.TCP_FIN) && this.tcb.tcb_ack == p.seq) {
								
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
										continue; //sending ack failed
									}
									
									this.tcb.tcb_state = ConnectionState.S_CLOSING;
									
									
									if (recv_tcp_packet(pClosing, true) &&
											this.tcb.tcb_our_port == pClosing.dst_port &&
											this.tcb.tcb_their_ip_addr == pClosing.src_ip &&
											pClosing.checkFlags(TCPPacket.TCP_ACK) &&
											pClosing.ack == add_uints(this.tcb.tcb_seq, 1)) {
										
										this.tcb.tcb_state = ConnectionState.S_TIME_WAIT;
										//wait and close
										
										try {
											Thread.sleep(1000);
											this.tcb = new TCPControlBlock();
											
										} catch (InterruptedException e) {
											
											System.out.println("CLOSE: interrupted while waiting");
											this.tcb = new TCPControlBlock();
											e.printStackTrace();
										}
										return true;
									}
								}
							}

							if (p.checkFlags(TCPPacket.TCP_ACK) && p.ack == add_uints(this.tcb.tcb_seq, 1)) {

								this.tcb.tcb_state = ConnectionState.S_FIN_WAIT_2;
								TCPPacket pFinWait2 = new TCPPacket();

								//check if a fin follows
								for (int itFinWait2 = 0; itFinWait2 < 10; itFinWait2 ++) {
									
									//check if packet is for us
									if (recv_tcp_packet(pFinWait2, true) && 
											this.tcb.tcb_our_port == pFinWait2.dst_port &&
											this.tcb.tcb_their_port == pFinWait2.src_port &&
											this.tcb.tcb_their_ip_addr == pFinWait2.src_ip) {
										
										//check if fin
										if (pFinWait2.checkFlags(TCPPacket.TCP_FIN) && tcb.tcb_ack == pFinWait2.seq) {
											//send ack
											
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
												continue; //sending ack failed
											}
											
											this.tcb.tcb_state = ConnectionState.S_TIME_WAIT;
											
											System.out.println("CLOSE: FIN_WAIT_2 -> TIME_WAIT closing");
											
											//wait and close
											try {
												Thread.sleep(1000);
												this.tcb = new TCPControlBlock();

											} catch (InterruptedException e) {
												
												this.tcb = new TCPControlBlock();
												e.printStackTrace();
											}

										} 
										//fin does not follow, may continue reading, no need to loop further
										return true;
									}

								}
								//no fin arrives within 10 sec, may continue reading
								return true;
							}
						}
					}
				}
			}

			return true;
		}

		/**
		 * A method used for sending packets trough the IP layer. It encodes the
		 * parameters and creates an IP packet.
		 * 
		 * @param dst_address the destination address
		 * @param buf the data to send
		 * @param length the length of the data
		 * @param src_port the source port of the packet
		 * @param dst_port the destination port
		 * @param ack_number the ack number
		 * @param flags the flags of the TCP packet
		 * */
		private ByteBuffer send_tcp_packet(int dst_address, byte[] buf, int length, int src_port,
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


			byte mask = (byte)(flags & ~(TCPPacket.TCP_URG | TCPPacket.TCP_RST));
			mask |= TCPPacket.TCP_PUSH;

			pseudo.put(mask);
			pseudo.putShort((short) 8192);	//Window size equal to the maximal size of a packet	

			pseudo.putInt(0);			//The Checksum and the urgent pointer
			pseudo.put(buf);

			if(length % 2 != 0){
				pseudo.put((byte) 0);
			}

			long checksum = calculateChecksum(pseudo.array());

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


			Packet p1 = new Packet(dst_ip, 6, 0, tcp_packet.array(), length + 20);
			p1.source = localaddr;

			try{
				ip.ip_send(p1);
			} catch(IOException e){
				return null;			//if the send fails
			}

			return tcp_packet;
		}
		
		
		/**
		 * A method used for communication with the IP layer. It receives an IP packet,
		 * decodes it and computes the checksum.
		 * 
		 * @param tcpp the packet to be read
		 * @param blocking if use blocking or non-blocking version of the method
		 *  */
		private boolean recv_tcp_packet (TCPPacket tcpp, boolean blocking) {
			Packet p = new Packet();
			ByteBuffer pseudo;

			try{
				if(blocking == true)
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


			} catch (IOException e){
				return false;
			} catch(InterruptedException e){
				System.out.println("RCV: interrupted");
				return false;
			}
			return true;
		}

		/**
		 * A method used for computing the checksum of an byte array
		 * */
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
	public BogusTCP(int address) throws IOException {
		ip = new IP(address);
	}

	/**
	 * @return a new socket for this stack
	 */
	public BogusSocket socket() {
		if(instance == null || instance.tcb.tcb_state == ConnectionState.S_CLOSED){
			instance = new BogusSocket();
			return instance;
		}
		else 
			return null;
	}

	/**
	 * @return a new server socket for this stack bound to the given port
	 * @param port the port to bind the socket to.
	 */
	public BogusSocket socket(int port) {
		if(instance == null || instance.tcb.tcb_state == ConnectionState.S_CLOSED){
			instance = new BogusSocket(port);
			return instance;
		}
		else 
			return null;
	}

}