package nl.vu.cs.cn;

import java.io.IOException;
import java.nio.ByteBuffer;

import nl.vu.cs.cn.IP.IpAddress;
import nl.vu.cs.cn.IP.Packet;
import java.nio.ByteOrder;
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

    	/**
    	 * Construct a client socket.
    	 */
    	private Socket() {

    	}

    	/**
    	 * Construct a server socket bound to the given local port.
		 *
    	 * @param port the local port to use
    	 */
        private Socket(int port) {
			// TODO Auto-generated constructor stub
		}

		/**
         * Connect this socket to the specified destination and port.
         *
         * @param dst the destination to connect to
         * @param port the port to connect to
         * @return true if the connect succeeded.
         */
        public boolean connect(IpAddress dst, int port) {

            // Implement the connection side of the three-way handshake here.
        	ip.getLocalAddress();

            return false;
        }

        /**
         * Accept a connection on this socket.
         * This call blocks until a connection is made.
         */
        public void accept() {

            // Implement the receive side of the three-way handshake here.

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
        
        public void send_tcp_packet(byte[] buf, short src_port, short dst_port, int seq_number, int ack_number, byte flags){
//        	byte[] payload = new byte[buf.length + 5 * 4]; // 5 x 32 bits in the tcp header + the buffer
//        	
//        	payload[0] = (byte) (src_port >> 8);
//        	payload[1] = (byte) (src_port);
//        	
//        	payload[2] = (byte) (dst_port >> 8);
//        	payload[3] = (byte) (dst_port);
//        	
//        	payload[4] = (byte) (seq_number >> 24);
//        	payload[5] = (byte) (seq_number >> 16);
//        	payload[6] = (byte) (seq_number >> 8);
//        	payload[7] = (byte) (seq_number);
//        	
//        	payload[8] = (byte) (ack_number >> 24);
//        	payload[9] = (byte) (ack_number >> 16);
//        	payload[10] = (byte) (ack_number >> 8);
//        	payload[11] = (byte) (ack_number);
//
//        	short hl_fl = 0x5000; //TODO add | for the flags
//        	payload[12] = (byte) (hl_fl >> 8);
//        	payload[13] = (byte) (hl_fl);
//        			
//        	short window_size = 1;
//        	payload[14] = (byte) (window_size >> 8);
//        	payload[15] = (byte) (window_size);
//        	
//        	short tmpchecksum = 0;
//        	payload[14] = (byte) (tmpchecksum >> 8);
//        	payload[15] = (byte) (tmpchecksum);
//        	
//        	short urgpointer = 0;
//        	payload[14] = (byte) (urgpointer >> 8);
//        	payload[15] = (byte) (urgpointer);
        	
        	
//        	System.arraycopy(buf, 0, payload, 16, buf.length);
        	
//        	byte[] pseudo;
//        	if(buf.length % 2 == 0){
//        		pseudo = new byte[payload.length + 12];
//        		
//        	} else {
//        		pseudo = new byte[payload.length + 13];
//        	}
//        	System.out.println("bufer length " + buf.length);		
        	ByteBuffer bb = ByteBuffer.allocate(buf.length + 20);
        	
        	bb.putShort(src_port);
        	bb.putShort(dst_port);
        	bb.putInt(seq_number);
        	bb.putInt(ack_number);
        	
        	bb.put((byte) 0x50);	// The TCP header length = 5 and the 6 empty bits

        	byte mask = (byte) (flags & 0x1B);
        	mask |= (1 << 3);
        	
        	bb.put(mask);
        	bb.putShort((short) 1);
        	 
        	
//        	System.out.println("position " + bb.position());
        	
        	
//        	Packet p = new Packet(3, 6, 10, bb.array(), 100);
        	Packet p = new Packet((int) 0x3000, 6, 10, bb.array(), buf.length + 20); //TODO why the length can be smaller than the data size?
//        	System.out.println(p.toString());
        	print(bb);
        	
        	
        }
        
        void print(ByteBuffer bb){
        	for(int i = 0; i < bb.capacity(); i++){
        		System.out.print(bb.get(i) + " ");
        			System.out.print("\n");
        	}
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
