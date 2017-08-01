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
package org.apache.metron.rest.service.impl.blockly;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Xml {

  @XmlElement(name="block")
  private List<Block> blocks;

  public List<Block> getBlocks() {
    return blocks;
  }

  public void setBlocks(List<Block> blocks) {
    this.blocks = blocks;
  }

  public Xml addBlock(Block block) {
    if (blocks == null) {
      blocks = new ArrayList<>();
    }
    blocks.add(block);
    return this;
  }

  private static String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><xml>test</xml>";

  public static void main(String[] args) throws JAXBException {
    JAXBContext context = JAXBContext.newInstance(Xml.class);
//    Unmarshaller unmarshaller = context.createUnmarshaller();
//    Xml xmlObject = (Xml) unmarshaller.unmarshal(new File("/Users/rmerriman/Projects/Metron/code/forks/merrimanr/incubator-metron/test.xml"));
//    //Xml xmlObject = (Xml) unmarshaller.unmarshal(new ByteArrayInputStream(xml.getBytes()));
//    System.out.print(xmlObject);


    Xml xmlObject = new Xml().addBlock(new Block().withType("stellar_and").addField(new Field().withName("OP").withValue("AND")));
    Marshaller marshaller = context.createMarshaller();
    //marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
    marshaller.marshal(xmlObject, System.out);
  }
}
