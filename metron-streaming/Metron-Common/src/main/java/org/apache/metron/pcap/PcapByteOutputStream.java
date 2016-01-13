// $codepro.audit.disable explicitThisUsage, lossOfPrecisionInCast
package org.apache.metron.pcap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.krakenapps.pcap.PcapOutputStream;
import org.krakenapps.pcap.file.GlobalHeader;
import org.krakenapps.pcap.packet.PacketHeader;
import org.krakenapps.pcap.packet.PcapPacket;
import org.krakenapps.pcap.util.Buffer;

// TODO: Auto-generated Javadoc
/**
 * The Class PcapByteOutputStream.
 * 
 * @author sheetal
 * @version $Revision: 1.0 $
 */
public class PcapByteOutputStream implements PcapOutputStream {

  /** The Constant LOG. */
  private static final Logger LOG = Logger
      .getLogger(PcapByteOutputStream.class);

  /** The Constant MAX_CACHED_PACKET_NUMBER. */
  private static final int MAX_CACHED_PACKET_NUMBER = 1000;

  /** The cached packet num. */
  private int cachedPacketNum = 0; // NOPMD by sheetal on 1/29/14 2:34 PM

  /** The baos. */
  private ByteArrayOutputStream baos; // NOPMD by sheetal on 1/29/14 2:34 PM

  /** The list. */
  private List<Byte> list; // NOPMD by sheetal on 1/29/14 2:34 PM

  /**
   * Instantiates a new pcap byte output stream.
   * 
   * @param baos
   *          the baos
   */
  public PcapByteOutputStream(ByteArrayOutputStream baos) {
    this.baos = baos;
    list = new ArrayList<Byte>();
    createGlobalHeader();
  }

  /**
   * Instantiates a new pcap byte output stream.
   * 
   * @param baos
   *          the baos
   * @param header
   *          the header
   */
  public PcapByteOutputStream(ByteArrayOutputStream baos, GlobalHeader header) {
    this.baos = baos;
    list = new ArrayList<Byte>();
    copyGlobalHeader(header);
  }

  /**
   * Creates the global header.
   */
  private void createGlobalHeader() {
    /* magic number(swapped) */
    list.add((byte) 0xd4);
    list.add((byte) 0xc3);
    list.add((byte) 0xb2);
    list.add((byte) 0xa1);

    /* major version number */
    list.add((byte) 0x02);
    list.add((byte) 0x00);

    /* minor version number */
    list.add((byte) 0x04);
    list.add((byte) 0x00);

    /* GMT to local correction */
    list.add((byte) 0x00);
    list.add((byte) 0x00);
    list.add((byte) 0x00);
    list.add((byte) 0x00);

    /* accuracy of timestamps */
    list.add((byte) 0x00);
    list.add((byte) 0x00);
    list.add((byte) 0x00);
    list.add((byte) 0x00);

    /* max length of captured packets, in octets */
    list.add((byte) 0xff);
    list.add((byte) 0xff);
    list.add((byte) 0x00);
    list.add((byte) 0x00);

    /* data link type(ethernet) */
    list.add((byte) 0x01);
    list.add((byte) 0x00);
    list.add((byte) 0x00);
    list.add((byte) 0x00);
  }

  /**
   * Copy global header.
   * 
   * @param header
   *          the header
   */
  private void copyGlobalHeader(GlobalHeader header) {
    final byte[] magicNumber = intToByteArray(header.getMagicNumber());
    final byte[] majorVersion = shortToByteArray(header.getMajorVersion());
    final byte[] minorVersion = shortToByteArray(header.getMinorVersion());
    final byte[] zone = intToByteArray(header.getThiszone());
    final byte[] sigFigs = intToByteArray(header.getSigfigs());
    final byte[] snapLen = intToByteArray(header.getSnaplen());
    final byte[] network = intToByteArray(header.getNetwork());

    list.add(magicNumber[0]);
    list.add(magicNumber[1]);
    list.add(magicNumber[2]);
    list.add(magicNumber[3]);

    list.add(majorVersion[1]);
    list.add(majorVersion[0]);

    list.add(minorVersion[1]);
    list.add(minorVersion[0]);

    list.add(zone[3]);
    list.add(zone[2]);
    list.add(zone[1]);
    list.add(zone[0]);

    list.add(sigFigs[3]);
    list.add(sigFigs[2]);
    list.add(sigFigs[1]);
    list.add(sigFigs[0]);

    list.add(snapLen[3]);
    list.add(snapLen[2]);
    list.add(snapLen[1]);
    list.add(snapLen[0]);

    list.add(network[3]);
    list.add(network[2]);
    list.add(network[1]);
    list.add(network[0]);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.krakenapps.pcap.PcapOutputStream#write(org.krakenapps.pcap.packet
   * .PcapPacket)
   */
  /**
   * Method write.
   * 
   * @param packet
   *          PcapPacket
   * 
   * 
   * @throws IOException
   *           * @see org.krakenapps.pcap.PcapOutputStream#write(PcapPacket) * @see
   *           org.krakenapps.pcap.PcapOutputStream#write(PcapPacket)
   */
 
  public void write(PcapPacket packet) throws IOException {
    PacketHeader packetHeader = packet.getPacketHeader();

    int tsSec = packetHeader.getTsSec();
    int tsUsec = packetHeader.getTsUsec();
    int inclLen = packetHeader.getInclLen();
    int origLen = packetHeader.getOrigLen();

    addInt(tsSec);
    addInt(tsUsec);
    addInt(inclLen);
    addInt(origLen);

    Buffer payload = packet.getPacketData();

    try {
      payload.mark();
      while (true) {
        list.add(payload.get());
      }
    } catch (BufferUnderflowException e) {
      //LOG.debug("Ignorable exception while writing packet", e);
      payload.reset();
    }

    cachedPacketNum++;
    if (cachedPacketNum == MAX_CACHED_PACKET_NUMBER) {
      flush();
    }
  }

  /**
   * Adds the int.
   * 
   * @param number
   *          the number
   */
  private void addInt(int number) {
    list.add((byte) (number & 0xff));
    list.add((byte) ((number & 0xff00) >> 8));
    list.add((byte) ((number & 0xff0000) >> 16));
    list.add((byte) ((number & 0xff000000) >> 24));
  }

  /**
   * Int to byte array.
   * 
   * @param number
   *          the number
   * 
   * @return the byte[]
   */
  private byte[] intToByteArray(int number) {
    return new byte[] { (byte) (number >>> 24), (byte) (number >>> 16),
        (byte) (number >>> 8), (byte) number };
  }

  /**
   * Short to byte array.
   * 
   * @param number
   *          the number
   * 
   * @return the byte[]
   */
  private byte[] shortToByteArray(short number) {
    return new byte[] { (byte) (number >>> 8), (byte) number };
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.krakenapps.pcap.PcapOutputStream#flush()
   */
  /**
   * Method flush.
   * 
   * 
   * @throws IOException
   *           * @see org.krakenapps.pcap.PcapOutputStream#flush() * @see
   *           org.krakenapps.pcap.PcapOutputStream#flush()
   */
 
  public void flush() throws IOException {
    byte[] fileBinary = new byte[list.size()];
    for (int i = 0; i < fileBinary.length; i++) {
      fileBinary[i] = list.get(i);
    }

    list.clear();
    baos.write(fileBinary);
    cachedPacketNum = 0;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.krakenapps.pcap.PcapOutputStream#close()
   */
  /**
   * Method close.
   * 
   * 
   * @throws IOException
   *           * @see org.krakenapps.pcap.PcapOutputStream#close() * @see
   *           org.krakenapps.pcap.PcapOutputStream#close()
   */
 
  public void close() throws IOException {
    flush();
    baos.close(); // $codepro.audit.disable closeInFinally
  }
}
