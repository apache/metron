package org.apache.metron.parsers;

import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.parsers.snort.BasicSnortParser;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class SnortParserTest {
  /**
  01/27-16:01:04.877970 ,129,12,1,"Consecutive TCP small segments exceeding threshold",TCP,10.0.2.2,56642,10.0.2.15,22,52:54:00:12:35:02,08:00:27:7F:93:2D,0x4E,***AP***,0x9AFF3D7,0xC8761D52,,0xFFFF,64,0,59677,64,65536,,,,
   **/
  @Multiline
  public static String goodMessage;

  /**
   { "msg":"\"Consecutive TCP small segments exceeding threshold\""
   , "sig_rev":"1"
   ,"ip_dst_addr":"10.0.2.15"
   ,"ip_dst_port":"22"
   ,"ethsrc":"52:54:00:12:35:02"
   ,"tcpseq":"0x9AFF3D7"
   ,"dgmlen":"64"
   ,"icmpid":""
   ,"tcplen":""
   ,"tcpwindow":"0xFFFF"
   ,"icmpseq":""
   ,"tcpack":"0xC8761D52"
   ,"original_string":"01\/27-16:01:04.877970 ,129,12,1,\"Consecutive TCP small segments exceeding threshold\",TCP,10.0.2.2,56642,10.0.2.15,22,52:54:00:12:35:02,08:00:27:7F:93:2D,0x4E,***AP***,0x9AFF3D7,0xC8761D52,,0xFFFF,64,0,59677,64,65536,,,,"
   ,"icmpcode":""
   ,"tos":"0"
   ,"id":"59677"
   ,"timestamp":1453932941970
   ,"ethdst":"08:00:27:7F:93:2D"
   ,"ip_src_addr":"10.0.2.2"
   ,"ttl":"64"
   ,"source.type":"snort"
   ,"ethlen":"0x4E"
   ,"iplen":"65536"
   ,"icmptype":""
   ,"protocol":"TCP"
   ,"ip_src_port":"56642"
   ,"tcpflags":"***AP***"
   ,"sig_id":"12"
   ,"sig_generator":"129"
   , "is_alert" : "true"
   }
   */

  @Test
  public void testGoodMessage() {
    BasicSnortParser parser = new BasicSnortParser();
    Map out = parser.parse(goodMessage.getBytes()).get(0);
    Assert.assertEquals(out.get("msg"),"\"Consecutive TCP small segments exceeding threshold\"");
    Assert.assertEquals(out.get("sig_rev"), "1");
    Assert.assertEquals(out.get("ip_dst_addr"), "10.0.2.15");
    Assert.assertEquals(out.get("ip_dst_port"), "22");
    Assert.assertEquals(out.get("ethsrc"), "52:54:00:12:35:02");
    Assert.assertEquals(out.get("tcpseq"),"0x9AFF3D7");
    Assert.assertEquals(out.get("dgmlen"), "64");
    Assert.assertEquals(out.get("icmpid"), "");
    Assert.assertEquals(out.get("tcplen"), "");
    Assert.assertEquals(out.get("tcpwindow"), "0xFFFF");
    Assert.assertEquals(out.get("icmpseq").toString().trim(), "");
    Assert.assertEquals(out.get("tcpack"), "0xC8761D52");
    Assert.assertEquals(out.get("icmpcode"), "");
    Assert.assertEquals(out.get("tos"), "0");
    Assert.assertEquals(out.get("id"), "59677");
    Assert.assertEquals(out.get("ethdst"), "08:00:27:7F:93:2D");
    Assert.assertEquals(out.get("ip_src_addr"), "10.0.2.2");
    Assert.assertEquals(out.get("ttl"),"64");
    Assert.assertEquals(out.get("ethlen"),"0x4E");
    Assert.assertEquals(out.get("iplen"),"65536");
    Assert.assertEquals(out.get("icmptype"),"");
    Assert.assertEquals(out.get("protocol"),"TCP");
    Assert.assertEquals(out.get("ip_src_port"),"56642");
    Assert.assertEquals(out.get("tcpflags"),"***AP***");
    Assert.assertEquals(out.get("sig_id"),"12");
    Assert.assertEquals(out.get("sig_generator"), "129");
    Assert.assertEquals(out.get("is_alert"), "true");
  }

  @Test(expected=IllegalStateException.class)
  public void testBadMessage() {
    BasicSnortParser parser = new BasicSnortParser();
    parser.parse("foo bar".getBytes());
  }
}
