/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.pcap;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import org.krakenapps.pcap.PcapInputStream;
import org.krakenapps.pcap.file.GlobalHeader;
import org.krakenapps.pcap.packet.PacketHeader;
import org.krakenapps.pcap.packet.PcapPacket;
import org.krakenapps.pcap.util.Buffer;
import org.krakenapps.pcap.util.ByteOrderConverter;
import org.krakenapps.pcap.util.ChainBuffer;

/**
 * The Class PcapByteInputStream.
 * 
 * @author sheetal
 * @version $Revision: 1.0 $
 */
public class PcapByteInputStream implements PcapInputStream {

  /** The is. */
  private DataInputStream is;

  /** The global header. */
  private GlobalHeader globalHeader;

  /**
   * Opens pcap file input stream.
   * 
   * @param pcap
   *          the byte array to be read
   * 
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  public PcapByteInputStream(byte[] pcap) throws IOException {
    is = new DataInputStream(new ByteArrayInputStream(pcap)); // $codepro.audit.disable
                                                              // closeWhereCreated
    readGlobalHeader();
  }

  /**
   * Reads a packet from pcap byte array.
   * 
   * @return the packet throws IOException the stream has been closed and the
   *         contained input stream does not support reading after close, or
   *         another I/O error occurs. * @throws IOException Signals that an I/O
   *         exception has occurred. * @see
   *         org.krakenapps.pcap.PcapInputStream#getPacket()
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */

  public PcapPacket getPacket() throws IOException {
    return readPacket(globalHeader.getMagicNumber());
  }

  /**
   * Gets the global header.
   * 
   * 
   * @return the global header
   */
  public GlobalHeader getGlobalHeader() {
    return globalHeader;
  }

  /**
   * Read global header.
   * 
   * 
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  private void readGlobalHeader() throws IOException {
    int magic = is.readInt();
    short major = is.readShort();
    short minor = is.readShort();
    int tz = is.readInt();
    int sigfigs = is.readInt();
    int snaplen = is.readInt();
    int network = is.readInt();

    globalHeader = new GlobalHeader(magic, major, minor, tz, sigfigs, snaplen,
        network);

    if (globalHeader.getMagicNumber() == 0xD4C3B2A1) {
      globalHeader.swapByteOrder();
    }
  }

  /**
   * Read packet.
   * 
   * @param magicNumber
   *          the magic number
   * @return the pcap packet * @throws IOException Signals that an I/O exception
   *         has occurred. * @throws EOFException the EOF exception
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  private PcapPacket readPacket(int magicNumber) throws IOException {
    PacketHeader packetHeader = readPacketHeader(magicNumber);
    Buffer packetData = readPacketData(packetHeader.getInclLen());
    return new PcapPacket(packetHeader, packetData);
  }

  /**
   * Read packet header.
   * 
   * @param magicNumber
   *          the magic number
   * @return the packet header * @throws IOException Signals that an I/O
   *         exception has occurred. * @throws EOFException the EOF exception
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  private PacketHeader readPacketHeader(int magicNumber) throws IOException {
    int tsSec = is.readInt();
    int tsUsec = is.readInt();
    int inclLen = is.readInt();
    int origLen = is.readInt();

    if (magicNumber == 0xD4C3B2A1) {
      tsSec = ByteOrderConverter.swap(tsSec);
      tsUsec = ByteOrderConverter.swap(tsUsec);
      inclLen = ByteOrderConverter.swap(inclLen);
      origLen = ByteOrderConverter.swap(origLen);
    }

    return new PacketHeader(tsSec, tsUsec, inclLen, origLen);
  }

  /**
   * Read packet data.
   * 
   * @param packetLength
   *          the packet length
   * @return the buffer * @throws IOException Signals that an I/O exception has
   *         occurred.
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  private Buffer readPacketData(int packetLength) throws IOException {
    byte[] packets = new byte[packetLength];
    is.read(packets);

    Buffer payload = new ChainBuffer();
    payload.addLast(packets);
    return payload;
    // return new PacketPayload(packets);
  }

  /**
   * Closes pcap stream handle.
   * 
   * 
   * @throws IOException
   *           Signals that an I/O exception has occurred. * @see
   *           org.krakenapps.pcap.PcapInputStream#close()
   */

  public void close() throws IOException {
    is.close(); // $codepro.audit.disable closeInFinally
  }
}
