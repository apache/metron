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

package org.apache.metron.parsers.activedirectory;

import static org.junit.Assert.assertEquals;
import java.util.List;
import org.json.simple.JSONObject;
import org.junit.Test;

public class ActiveDirectoryParserTest {

    private final String dateFormat = "yyyy MMM dd HH:mm:ss";
    private final String timestampField = "timestamp_string";

    @Test
    public void testParseLoginLine() throws Exception {

        //Set up parser, parse message
        ActiveDirectoryParser parser = new ActiveDirectoryParser();
        //parser.withDateFormat(dateFormat).withTimestampField(timestampField);
        String testString = "<13> KDCRDNLBSTS07 04/18/2016 13:25:19.648\n" +
                "dcName=ABC.google.com\n" +
                "admonEventType=Update\n" +
                "Names:\n" +
                "\tobjectCategory=CN=Group,CN=Schema,CN=Configuration,DC=ds,DC=capitalone,DC=com\n" +
                "\n" +
                "\tname=All Associates - Richmond4\n" +
                "\n" +
                "\tdisplayName=#All Associates - Richmond4\n" +
                "\n" +
                "\tdistinguishedName=CN=All Associates - Richmond4,OU=Distribution Lists,OU=USADCUsers,OU=Exchange,DC=cof,DC=ds,DC=capitalone,DC=com\n" +
                "\n" +
                "\tcn=All Associates - Richmond4\n" +
                "\n" +
                "Object Details:\n" +
                "\tsAMAccountType=268435457\n" +
                "\n" +
                "\tsAMAccountName=All Associates-1\n" +
                "\n" +
                "\tobjectSid=S-1-5-21-99512129-1830164216-1097030630-128786\n" +
                "\n" +
                "\tobjectGUID=c7086f2f-33ab-4f71-9046-a3b1a69603c6\n" +
                "\n" +
                "\twhenChanged=01:25.17 PM, Mon 04/18/2016\n" +
                "\n" +
                "\twhenCreated=01:57.44 PM, Sat 06/26/2004\n" +
                "\n" +
                "\tobjectClass=top|group\n" +
                "\n" +
                "Event Details:\n" +
                "\tuSNChanged=1653933240\n" +
                "\n" +
                "\tuSNCreated=98972\n" +
                "\n" +
                "\tinstanceType=4\n" +
                "\n" +
                "Additional Details:\n" +
                "\tmsExchRecipientDisplayType=1\n" +
                "\n" +
                "\tmsExchPoliciesIncluded={3D722DB5-B274-4C28-B991-46CBB56D9BC7},{26491CFC-9E50-4857-861B-0CB8DF22B5D7}\n" +
                "\n" +
                "\tdLMemDefault=0\n" +
                "\n" +
                "\tmsExchMailboxSecurityDescriptor=\\x01\n" +
                "\n" +
                "\tmsExchHideFromAddressLists=TRUE\n" +
                "\n" +
                "\tmsExchADCGlobalNames=forest:o=COF00000000A97AA9E5C9A4C401|EX5:cn=All Associates - Richmond4,cn=Distribution Lists,ou=RIC,o=COF:groupofnames$person$top00000000A97AA9E5C9A4C401|NT5:2F6F08C7AB33714F9046A3B1A69603C6000000009109DE07A6A4C401|FOREST:211C55D48F611F42A9A2F1AC2FA5536D000000009109DE07A6A4C401\n" +
                "\n" +
                "\tmsExchALObjectVersion=20301\n" +
                "\n" +
                "\treplicationSignature=<binary>\n" +
                "\n" +
                "\tmail=AllAssociates-Richmond4@capitalone.com\n" +
                "\n" +
                "\ttextEncodedORAddress=c=US;a= ;p=COF;o=RIC;s=AllAssociates-Richmond4;\n" +
                "\n" +
                "\tdSCorePropagationData=20160213101819.0Z|20150912043619.0Z|20150723164454.0Z|20150115174139.0Z|16010714223649.0Z\n" +
                "\n" +
                "\tgroupType=8\n" +
                "\n" +
                "\tlegacyExchangeDN=/o=COF/ou=RIC/cn=Distribution Lists/cn=All Associates - Richmond4\n" +
                "\n" +
                "\treplicatedObjectVersion=0\n" +
                "\n" +
                "\tmailNickname=AllAssociates-Richmond4\n" +
                "\n" +
                "\toOFReplyToOriginator=FALSE\n" +
                "\n" +
                "\thideDLMembership=FALSE\n" +
                "\n" +
                "\tproxyAddresses=x500:/o=ExchangeLabs/ou=Exchange Administrative Group (FYDIBOHF23SPDLT)/cn=Recipients/cn=5ec59dcbcbd24d938035bf2a8eb98ab2-All Associa|notes:34bfe0@ccmta-domain|x400:C=US;A= ;P=COF;O=RIC;S=All Associates - Richmond4;|NOTES:AllAssociates-Richmond4@ccmta-domain|X400:C=US;A= ;P=COF;O=RIC;S=AllAssociates-Richmond4;|GWISE:Exchange.RIC.#All Associates - Richmond4|SMTP:AllAssociates-Richmond4@capitalone.com|CCMAIL:All Associates - Richmond4 at RIC\n" +
                "\n" +
                "\treportToOwner=FALSE\n" +
                "\n" +
                "\treportToOriginator=TRUE\n" +
                "\n" +
                "\tauthOrig=CN=tov955,OU=Developers,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=ojg777,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com\n" +
                "\n" +
                "\tmemberOf=CN=All Associates - Richmond,OU=Distribution Lists,OU=USADCUsers,OU=Exchange,DC=cof,DC=ds,DC=capitalone,DC=com\n" +
                "\n" +
                "\tinfo=Add new Richmond Associates to this list until the number reaches 3000.  Then Create new list and continue.\n" +
                "\n" +
                "\tmember=CN=DTN565,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=PMK590,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=KQO983,OU=User Lock Policy 05,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=A-HEW392,OU=IT Support,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=A-NTA865,OU=IT Support,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=TDW209,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=CXC542,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=OBG431,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=KBS882,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=JOZ249,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=HLL365,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=NCL310,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=IPF490,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=TCP607,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=DSZ431,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=PLR674,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=WXO353,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=DLG228,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=DQW568,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=SAC641,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=DVI606,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=FYI958,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=POX499,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=DTM447,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=CMW073,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=FFT087,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=IYX267,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=FEW742,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=MMB609,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=EOK560,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=QXT050,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=AUN540,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=UVD868,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=TVL743,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=ZVC361,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=ZQW302,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=BDW290,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=UXF002,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=MCT121,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=DFQ948,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=LUY727,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=JAM599,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=SUP132,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=LBW495,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=IRW716,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=BYA210,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=HGV654,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=TLK248,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=QVH322,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=RWE421,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=GTT495,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=SLY092,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=UMP842,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=CDL839,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=KOB364,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=RAH646,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=YSZ489,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=KOA082,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=ARA594,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=IDN142,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=JVF222,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=CCS511,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=ZPM234,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=FIP216,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=NVF253,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=AHL201,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=AZD022,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=UDR624,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=ROH306,OU=Citrix,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=FTN767,OU=Citrix,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=NAJ177,OU=Citrix,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=FTL293,OU=Citrix,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=MJZ148,OU=Citrix,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=VEJ904,OU=Citrix,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=MUY292,OU=Citrix,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=MNF706,OU=Citrix,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=YKR915,OU=Citrix,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=UWI385,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=CKW243,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=AHP792,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=TKF803,OU=User Lock Policy 05,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=KJF760,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=XFX896,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=GQQ974,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=URZ592,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=PRB461,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=UDZ649,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=TZW519,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=PYA686,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=ESA703,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=VAF771,OU=Citrix,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=TCV518,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=NRI573,OU=Citrix,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=TDG979,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=WVV903,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=WTN315,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=BBP330,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=KHW185,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=VCK420,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=ZCA908,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=PIL795,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=ZXB930,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=QTF234,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=OPI386,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=MVE217,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=JSV850,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=DEV144,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=QKO962,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=XCS336,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=OKM801,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=PHM073,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=FXH777,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=YTW221,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=LOW633,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=ZOL777,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=DOJ565,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=GUD305,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=NUP113,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=EAI447,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=EHN454,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=VBI662,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=JQZ298,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=PBJ756,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=ODB863,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=OQX004,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=GQG285,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=IZT649,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=ZZH315,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=QXM889,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=RFH609,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=MBY959,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=ATK699,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=ODI471,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=RFU310,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=CUQ272,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=NEQ282,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=TPG775,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=WJK908,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=DOM407,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=ZTJ791,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=XIG725,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=OIT414,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=MXM059,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=AMX761,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=VBR067,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=QIC509,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=VOB993,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=IBZ817,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=CHZ476,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=PTR369,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=PMA852,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=QJR245,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=IIF296,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=IKE789,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=XTC450,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=DJI194,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=OTZ549,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=JHR844,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=GTJ177,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=IXI191,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=YOR906,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=OTT249,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=XTP324,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=GVG846,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=A-AQY844,OU=IT Support,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=A-WNZ167,OU=IT Support,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=IBI947,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=ASX667,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=DKF376,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=VMM148,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=YAX118,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=OQF135,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=KLI032,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=DEW225,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=JVL418,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=YCU149,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=RRY362,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=KUW641,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=RDP188,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=JKC376,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=GXC963,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=HAI337,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=NUO807,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=WNL841,OU=User Lock Policy 05,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=VEB171,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=A-NVJ836,OU=IT Support,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=WJM987,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=EPU146,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=ILY180,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=CPG969,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=SJZ660,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=TEF155,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=UXN780,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=SYP061,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=RKZ682,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=PJE013,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=OWC416,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=OPV326,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=EKE058,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=XDU312,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=QAG878,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=MAA636,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=MIX261,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=AXT022,OU=User Lock Policy 05,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=WSP395,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=PDY934,OU=User Lock Policy 05,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=DFG904,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=PLF889,OU=User Lock Policy 05,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=BED022,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=HZI906,OU=User Lock Policy 05,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=VCZ231,OU=Developers,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=FHI389,OU=User Lock Policy 05,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=SKP424,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=QYB214,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=RYS721,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=OVL930,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=GLQ473,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=ERW219,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=BBQ566,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=JGL563,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=GFW461,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=OWL354,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=MKQ616,OU=User Lock Policy 05,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=KGH627,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=LNX584,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=EGC768,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=TIA534,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=HZX090,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=ZEW398,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=TPI160,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=DXP264,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=IUX690,OU=User Lock Policy 05,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=HWV557,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=XBM172,OU=User Lock Policy 05,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=KJD499,OU=User Lock Policy 05,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=DJY449,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=UHV169,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=MJA609,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=KZL307,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=DFL822,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=VLL481,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=PSD509,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=XUU972,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=FBS431,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=NZY576,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=ZMU127,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=KER139,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=NKF742,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=PLE470,OU=Citrix,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=HLA226,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=ZMT445,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=ORM973,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=BBC474,OU=User Lock Policy 20,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=DLX658,OU=User Lock Policy 20,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=KZT555,OU=User Lock Policy 20,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=IKV522,OU=User Lock Policy 20,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=WXO714,OU=User Lock Policy 20,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=HML153,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=TYQ355,OU=User Lock Policy 20,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=HJI296,OU=User Lock Policy 20,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=NZQ910,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=FFC520,OU=User Lock Policy 20,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=LZX232,OU=User Lock Policy 05,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=ENG229,OU=User Lock Policy 20,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=ALG610,OU=User Lock Policy 20,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=CTE983,OU=User Lock Policy 20,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=UEJ827,OU=User Lock Policy 20,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=BCN222,OU=User Lock Policy 20,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=HEI819,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=MXR862,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=AEE289,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=NPO492,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=SIL959,OU=User Lock Policy 20,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=XOJ132,OU=User Lock Policy 20,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=PYI187,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=UOI366,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=BNO683,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=AWW080,OU=User Lock Policy 05,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=TOK088,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=HIW514,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=WTW027,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=CAR905,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=LFX285,OU=User Lock Policy 20,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=JDM428,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=PYB338,OU=User Lock Policy 05,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=PZS790,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=SIP811,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=FOA674,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=NLE264,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=KGB831,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=FTO264,OU=Citrix,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=XEC485,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=XFT466,OU=Citrix,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=CIA645,OU=Citrix,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=BZT346,OU=Citrix,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=NDF740,OU=Citrix,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=QXU827,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=KZA093,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=OPK130,OU=Citrix,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=SVX028,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=KBB672,OU=Citrix,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=MQZ650,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=OBU624,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=QRM154,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=IER480,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=GZG923,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=OFJ928,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=JJL023,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=DWK618,OU=Citrix,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=PIF004,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=WIJ109,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=ZSX038,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=DXB702,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=UVE688,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=ELV340,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=BXR270,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=MXV056,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=VHG461,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=ZRN359,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=TNX619,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=A-QFM644,OU=IT Support,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=YUN422,OU=User Lock Policy 05,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=WHI691,OU=User Lock Policy 05,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=TWD618,OU=User Lock Policy 05,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=A-PQI853,OU=IT Support,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=OBR782,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=FRU480,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=AVJ160,OU=User Lock Policy 05,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=QWN515,OU=User Lock Policy 05,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=A-PUE856,OU=IT Support,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=A-IPS432,OU=IT Support,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=A-EXO030,OU=IT Support,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=OLN025,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=A-ORX355,OU=IT Support,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=CBC179,OU=User Lock Policy 05,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=BLE968,OU=User Lock Policy 05,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=FMB923,OU=Citrix,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=VLQ056,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=TEF061,OU=User Lock Policy 05,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=GZP705,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=CPK241,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=XHL920,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=ZIB142,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=UER716,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=SPL777,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=LWW203,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=GOX114,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=ROM589,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=BTK865,OU=Citrix,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=XZB391,OU=Citrix,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=QQI220,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=LST485,OU=Citrix,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=YXL872,OU=Citrix,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=KVD101,OU=Citrix,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=JHH590,OU=Citrix,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=RNN798,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=FXI156,OU=Citrix,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=ZTH191,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=A-PUF855,OU=IT Support,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=WIB910,OU=User Lock Policy 05,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=JCW068,OU=User Lock Policy 05,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=A-RLE256,OU=IT Support,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=A-NIH124,OU=IT Support,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=XXK543,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=VRT567,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=VKF434,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=AGM937,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=UVP786,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=INP759,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=ASQ922,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=BCI436,OU=User Lock Policy 05,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=XIJ123,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=BIT472,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=A-UBU422,OU=IT Support,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=A-TUN664,OU=IT Support,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=A-HBW548,OU=IT Support,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=EWR299,OU=User Lock Policy 05,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=VVP041,OU=User Lock Policy 05,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=A-CCM658,OU=IT Support,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=MAW441,OU=User Lock Policy 05,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=AVL812,OU=User Lock Policy 05,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=HLN111,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=VPM033,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=BSW432,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=XCA568,OU=User Lock Policy 05,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=JYE069,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=A-ZUO164,OU=IT Support,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=LMU991,OU=User Lock Policy 05,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=ZON596,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=XES106,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=WUJ403,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=OPY174,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=XSF261,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=KST166,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=IWN848,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=A-QLA071,OU=IT Support,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=A-NBW741,OU=IT Support,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=A-TRW042,OU=IT Support,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=VTV955,OU=Citrix,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=priyanka giri,OU=Resource Mailboxes,OU=Exchange,DC=cof,DC=ds,DC=capitalone,DC=com|CN=ICZ144,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=SMM459,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=XEX428,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=WYL874,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=CNO654,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=ULE931,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=ILT694,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=NQJ712,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=GJJ831,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=EBN012,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=LGO703,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=VXM307,OU=Developers,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=A-MOJ614,OU=IT Support,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=A-MHL640,OU=IT Support,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=A-GES024,OU=IT Support,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=IGS497,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=A-QRY039,OU=IT Support,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=A-JKO033,OU=IT Support,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=A-SBN017,OU=IT Support,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=UID352,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=JWN752,OU=User Lock Policy 20,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=HGM874,OU=User Lock Policy 20,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=KYR404,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=BLI274,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=PAF579,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=DZX704,OU=User Lock Policy 05,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=WOS126,OU=User Lock Policy 20,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=UWS854,OU=User Lock Policy 05,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=PYS493,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=XON020,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=CJH659,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=RBV636,OU=User Lock Policy 05,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=YEZ426,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=HQR281,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=FQV245,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=LBJ188,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=RVB838,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=GPE338,OU=User Lock Policy 05,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=FZN183,OU=User Lock Policy 05,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=CRI517,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=LHU513,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=KRS394,OU=User Lock Policy 05,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=TMG653,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=LGC388,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=HUT899,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=HZW391,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=NVJ836,OU=User Lock Policy 10,OU=All Users,DC=cof,DC=ds,DC=capitalone,DC=com|CN=\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n";
        List<JSONObject> result = parser.parse(testString.getBytes());
        JSONObject parsedJSON = result.get(0);

        //Compare fields
        assertEquals(parsedJSON.get("dcName"), "ABC.google.com");
        //assertEquals(parsedJSON.get("timestamp") + "", "1460742448000");
        assertEquals(parsedJSON.get("admonEventType"), "Update");
        assertEquals(parsedJSON.get("Names"), "objectCategory=CN=ms-DS-Az-Role,CN=Schema,CN=Configuration,DC=google,DC=com");
        assertEquals(parsedJSON.get("name"), "CRA3");
        assertEquals(parsedJSON.get("distinguishedName"), "CN=CRA,CN=AzRoleObjectContainer-f2c06b86-f897-4ca4-ac5e-2762c25c5da4,CN=f2c06b86-f897-4ca4-ac5e-2762c25c5da4,CN=636cb236-cdb1-443b-bfb3-7683dd85b2f4,CN=Authorization,CN=Corporate,OU=Zones,OU=UNIX,DC=google,DC=com");
        assertEquals(parsedJSON.get("cn"), "CRA");
        assertEquals(parsedJSON.get("event_subtype"), "login");
        assertEquals(parsedJSON.get("username"), "rick007");
        assertEquals(parsedJSON.get("ip_src_addr"), "120.43.200.6");
    }

    @Test
    public void tetsParseLogoutLine() throws Exception {

        //Set up parser, parse message
        ActiveDirectoryParser parser = new ActiveDirectoryParser();
        //parser.withDateFormat(dateFormat).withTimestampField(timestampField);
        String testString = "<134>Apr 15 18:02:27 PHIXML3RWD [0x81000019][auth][info] [14.122.2.201]: "
                + "User 'hjpotter' logged out from 'default'.";
        List<JSONObject> result = parser.parse(testString.getBytes());
        JSONObject parsedJSON = result.get(0);

        //Compare fields
        assertEquals(parsedJSON.get("priority") + "", "134");
        assertEquals(parsedJSON.get("timestamp") + "", "1460743347000");
        assertEquals(parsedJSON.get("hostname"), "PHIXML3RWD");
        assertEquals(parsedJSON.get("event_code"), "0x81000019");
        assertEquals(parsedJSON.get("event_type"), "auth");
        assertEquals(parsedJSON.get("severity"), "info");
        assertEquals(parsedJSON.get("ip_src_addr"), "14.122.2.201");
        assertEquals(parsedJSON.get("username"), "hjpotter");
        assertEquals(parsedJSON.get("security_domain"), "default");
    }

    @Test
    public void tetsParseRBMLine() throws Exception {

        //Set up parser, parse message
        ActiveDirectoryParser parser = new ActiveDirectoryParser();
        //parser.withDateFormat(dateFormat).withTimestampField(timestampField);
        String testString = "<131>Apr 15 17:36:35 ROBXML3QRS [0x80800018][auth][error] rbm(RBM-Settings): "
                + "trans(3502888135)[request] gtid(3502888135): RBM: Resource access denied.";
        List<JSONObject> result = parser.parse(testString.getBytes());
        JSONObject parsedJSON = result.get(0);

        //Compare fields
        assertEquals(parsedJSON.get("priority") + "", "131");
        assertEquals(parsedJSON.get("timestamp") + "", "1460741795000");
        assertEquals(parsedJSON.get("hostname"), "ROBXML3QRS");
        assertEquals(parsedJSON.get("event_code"), "0x80800018");
        assertEquals(parsedJSON.get("event_type"), "auth");
        assertEquals(parsedJSON.get("severity"), "error");
        assertEquals(parsedJSON.get("process"), "rbm");
        assertEquals(parsedJSON.get("message"), "trans(3502888135)[request] gtid(3502888135): RBM: Resource access denied.");
    }

    @Test
    public void tetsParseOtherLine() throws Exception {

        //Set up parser, parse message
        ActiveDirectoryParser parser = new ActiveDirectoryParser();
        //parser.withDateFormat(dateFormat).withTimestampField(timestampField);
        String testString = "<134>Apr 15 17:17:34 SAGPXMLQA333 [0x8240001c][audit][info] trans(191): (admin:default:system:*): "
                + "ntp-service 'NTP Service' - Operational state down";
        List<JSONObject> result = parser.parse(testString.getBytes());
        JSONObject parsedJSON = result.get(0);

        //Compare fields
        assertEquals(parsedJSON.get("priority") + "", "134");
        assertEquals(parsedJSON.get("timestamp") + "", "1460740654000");
        assertEquals(parsedJSON.get("hostname"), "SAGPXMLQA333");
        assertEquals(parsedJSON.get("event_code"), "0x8240001c");
        assertEquals(parsedJSON.get("event_type"), "audit");
        assertEquals(parsedJSON.get("severity"), "info");
        assertEquals(parsedJSON.get("process"), "trans");
        assertEquals(parsedJSON.get("message"), "(admin:default:system:*): ntp-service 'NTP Service' - Operational state down");
    }

    @Test
    public void testParseMalformedLoginLine() throws Exception {

        //Set up parser, attempt to parse malformed message
        ActiveDirectoryParser parser = new ActiveDirectoryParser();
        //parser.withDateFormat(dateFormat).withTimestampField(timestampField);
        String testString = "<133>Apr 15 17:47:28 ABCXML1413 [rojOut][0x81000033][auth][notice] rick007): "
                + "[120.43.200. User logged into 'cohlOut'.";
        List<JSONObject> result = parser.parse(testString.getBytes());
        JSONObject parsedJSON = result.get(0);

        //Compare fields
        assertEquals(parsedJSON.get("priority") + "", "133");
        assertEquals(parsedJSON.get("timestamp") + "", "1460742448000");
        assertEquals(parsedJSON.get("hostname"), "ABCXML1413");
        assertEquals(parsedJSON.get("security_domain"), "rojOut");
        assertEquals(parsedJSON.get("event_code"), "0x81000033");
        assertEquals(parsedJSON.get("event_type"), "auth");
        assertEquals(parsedJSON.get("severity"), "notice");
        assertEquals(parsedJSON.get("event_subtype"), "login");
        assertEquals(parsedJSON.get("username"), null);
        assertEquals(parsedJSON.get("ip_src_addr"), null);
    }

    @Test
    public void tetsParseMalformedLogoutLine() throws Exception {

        //Set up parser, attempt to parse malformed message
        ActiveDirectoryParser parser = new ActiveDirectoryParser();
        //parser.withDateFormat(dateFormat).withTimestampField(timestampField);
        String testString = "<134>Apr 15 18:02:27 PHIXML3RWD [0x81000019][auth][info] [14.122.2.201: "
                + "User 'hjpotter' logged out from 'default.";
        List<JSONObject> result = parser.parse(testString.getBytes());
        JSONObject parsedJSON = result.get(0);

        //Compare fields
        assertEquals(parsedJSON.get("priority") + "", "134");
        assertEquals(parsedJSON.get("timestamp") + "", "1460743347000");
        assertEquals(parsedJSON.get("hostname"), "PHIXML3RWD");
        assertEquals(parsedJSON.get("event_code"), "0x81000019");
        assertEquals(parsedJSON.get("event_type"), "auth");
        assertEquals(parsedJSON.get("severity"), "info");
        assertEquals(parsedJSON.get("ip_src_addr"), null);
        assertEquals(parsedJSON.get("username"), null);
        assertEquals(parsedJSON.get("security_domain"), null);
    }

    @Test
    public void tetsParseMalformedRBMLine() throws Exception {

        //Set up parser, parse message
        ActiveDirectoryParser parser = new ActiveDirectoryParser();
        //parser.withDateFormat(dateFormat).withTimestampField(timestampField);
        String testString = "<131>Apr 15 17:36:35 ROBXML3QRS [0x80800018][auth][error] rbmRBM-Settings): "
                + "trans3502888135)[request] gtid3502888135) RBM: Resource access denied.";
        List<JSONObject> result = parser.parse(testString.getBytes());
        JSONObject parsedJSON = result.get(0);

        //Compare fields
        assertEquals(parsedJSON.get("priority") + "", "131");
        assertEquals(parsedJSON.get("timestamp") + "", "1460741795000");
        assertEquals(parsedJSON.get("hostname"), "ROBXML3QRS");
        assertEquals(parsedJSON.get("event_code"), "0x80800018");
        assertEquals(parsedJSON.get("event_type"), "auth");
        assertEquals(parsedJSON.get("severity"), "error");
        assertEquals(parsedJSON.get("process"), null);
        assertEquals(parsedJSON.get("message"), "rbmRBM-Settings): trans3502888135)[request] gtid3502888135) RBM: Resource access denied.");
    }

    @Test
    public void tetsParseMalformedOtherLine() throws Exception {

        //Set up parser, parse message
        ActiveDirectoryParser parser = new ActiveDirectoryParser();
        //parser.withDateFormat(dateFormat).withTimestampField(timestampField);
        String testString = "<134>Apr 15 17:17:34 SAGPXMLQA333 [0x8240001c][audit][info] trans 191)  admindefaultsystem*): "
                + "ntp-service 'NTP Service' - Operational state down:";
        List<JSONObject> result = parser.parse(testString.getBytes());
        JSONObject parsedJSON = result.get(0);

        //Compare fields
        assertEquals(parsedJSON.get("priority") + "", "134");
        assertEquals(parsedJSON.get("timestamp") + "", "1460740654000");
        assertEquals(parsedJSON.get("hostname"), "SAGPXMLQA333");
        assertEquals(parsedJSON.get("event_code"), "0x8240001c");
        assertEquals(parsedJSON.get("event_type"), "audit");
        assertEquals(parsedJSON.get("severity"), "info");
        assertEquals(parsedJSON.get("process"), null);
        assertEquals(parsedJSON.get("message"), "trans 191)  admindefaultsystem*): "
                + "ntp-service 'NTP Service' - Operational state down:");
    }


    @Test
    public void testParseEmptyLine() throws Exception {

        //Set up parser, attempt to parse malformed message
        ActiveDirectoryParser parser = new ActiveDirectoryParser();
        String testString = "";
        List<JSONObject> result = parser.parse(testString.getBytes());
        assertEquals(result, "[{\"names\":{},\"additional\":{},\"device_generated_timestamp\":1463433765189,\"ingest_timestamp\":1463433765189,\"source_type\":\"ActiveDirectory\",\"event\":{},\"object\":{}");
    }

}
