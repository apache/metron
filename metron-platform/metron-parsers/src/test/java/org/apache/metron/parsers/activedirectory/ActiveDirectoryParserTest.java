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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.simple.JSONArray;
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
        String testString = "<13> KDCRDNLBSTS07 04/18/2016 13:27:45.338\n" +
                "dcName=ABC.google.com\n" +
                "admonEventType=Update\n" +
                "Names:\n" +
                "\tobjectCategory=CN=Group,CN=Schema,CN=Configuration,DC=google,DC=com\n" +
                "\n" +
                "\tname=DL UG COF Technology Associates\n" +
                "\n" +
                "\tdisplayName=#Technology Associates\n" +
                "\n" +
                "\tdistinguishedName=CN=DL UG COF Technology Associates,OU=Mail Enabled Groups,OU=Exchange,DC=google,DC=com\n" +
                "\n" +
                "\tcn=DL UG COF Technology Associates\n" +
                "\n" +
                "Object Details:\n" +
                "\tsAMAccountType=268435457\n" +
                "\n" +
                "\tsAMAccountName=DL UG COF Technology Associates\n" +
                "\n" +
                "\tobjectSid=S-1-5-21-99512129-1830164216-1097030630-1192850\n" +
                "\n" +
                "\tobjectGUID=447354d9-2507-423d-9a4d-8e6aaa3a3ed0\n" +
                "\n" +
                "\twhenChanged=01:26.59 PM, Mon 04/18/2016\n" +
                "\n" +
                "\twhenCreated=12:44.40 PM, Thu 09/11/2014\n" +
                "\n" +
                "\tobjectClass=top|group\n" +
                "\n" +
                "Event Details:\n" +
                "\tuSNChanged=1653936631\n" +
                "\n" +
                "\tuSNCreated=968626542\n" +
                "\n" +
                "\tinstanceType=4\n" +
                "\n" +
                "Additional Details:\n" +
                "\tmsExchVersion=4535486012416\n" +
                "\n" +
                "\tmsExchRecipientDisplayType=1\n" +
                "\n" +
                "\tmsExchPoliciesIncluded={3D722DB5-B274-4C28-B991-46CBB56D9BC7},{26491CFC-9E50-4857-861B-0CB8DF22B5D7}\n" +
                "\n" +
                "\tmsExchALObjectVersion=26\n" +
                "\n" +
                "\tmail=TechnologyAssociates@capitalone.com\n" +
                "\n" +
                "\ttextEncodedORAddress=C=US;A= ;P=COF;O=RIC;S=TechnologyAssociates;\n" +
                "\n" +
                "\tmsExchRequireAuthToSendTo=TRUE\n" +
                "\n" +
                "\tdSCorePropagationData=20160213101716.0Z|20150912043540.0Z|20150723164420.0Z|20150115174111.0Z|16010714223649.0Z\n" +
                "\n" +
                "\tgroupType=8\n" +
                "\n" +
                "\tlegacyExchangeDN=/O=COF/OU=RIC/cn=Recipients/cn=TechnologyAssociates\n" +
                "\n" +
                "\tmanagedBy=CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com\n" +
                "\n" +
                "\tshowInAddressBook=CN=Default Global Address List,CN=All Global Address Lists,CN=Address Lists Container,CN=COF,CN=Microsoft Exchange,CN=Services,CN=Configuration,DC=ds,DC=capitalone,DC=com|CN=All Groups,CN=All Address Lists,CN=Address Lists Container,CN=COF,CN=Microsoft Exchange,CN=Services,CN=Configuration,DC=ds,DC=capitalone,DC=com\n" +
                "\n" +
                "\tinternetEncoding=0\n" +
                "\n" +
                "\tmailNickname=TechnologyAssociates\n" +
                "\n" +
                "\tproxyAddresses=x500:/o=ExchangeLabs/ou=Exchange Administrative Group (FYDIBOHF23SPDLT)/cn=Recipients/cn=6438e74f00604776884c7c3cb8ef174f-DL UG COF T|smtp:TechnologyAssociates@capitalone.mail.onmicrosoft.com|NOTES:TechnologyAssociates@ccmta-domain|X400:C=US;A= ;P=COF;O=RIC;S=TechnologyAssociates;|SMTP:TechnologyAssociates@capitalone.com\n" +
                "\n" +
                "\treportToOriginator=TRUE\n" +
                "\n" +
                "\tauthOrig=CN=RMBX Technology Communications,OU=Resource Mailboxes,OU=Exchange,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 00,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com\n" +
                "\n" +
                "\tinfo=Owner: Elizabeth Dabney (ABC123)\n" +
                "\n" +
                "\tmember=CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 00,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 00,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 10,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 00,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 10,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 10,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 10,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 10,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 10,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 00,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 00,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 00,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 00,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 10,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 00,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 00,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 00,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 00,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 10,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 20,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 00,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 00,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 00,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 10,OU=COAF,OU=Migration,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 00,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 00,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 00,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 00,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 00,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 00,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 00,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 00,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 10,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 00,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 00,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 00,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 00,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 00,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 00,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 00,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 00,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 20,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 00,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 00,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 00,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 00,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 00,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 00,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 00,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 00,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 00,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 00,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 00,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 00,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 00,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 00,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 00,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 10,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 00,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy 00,OU=All Users,DC=google,DC=com|CN=ABC123,OU=Developers,OU=All Users,DC=google,DC=com|CN=ABC123,OU=User Lock Policy";

        List<JSONObject> result = parser.parse(testString.getBytes());
        JSONObject parsedJSON = result.get(0);

        JSONObject json = parsedJSON;

        Iterator<java.util.Map.Entry<Long, String>> it
                = parsedJSON.entrySet().iterator();
        JSONArray arr = new JSONArray();

        assertNotNull(json);

        // ensure json is not null
        assertNotNull(json);
        // ensure json is not empty
        assertTrue(!json.isEmpty());
        json.entrySet().iterator();
        Iterator iter = json.entrySet().iterator();

        // ensure there are no null keys
        testKeysNotNull(json);

        // Test the values in the json against the actual data from the file
        assertEquals(json.get("timestamp").toString(), Long.toString(1461000465338l));
        assertEquals(json.get("simpleMachineName"), "KDCRDNLBSTS07");
        assertEquals(json.get("priority"), Integer.toString(13));
        assertEquals(json.get("dcName"), "ABC.google.com");
        assertEquals(json.get("admonEventType"), "Update");
        assertEquals(((JSONObject) json.get("names")).get("objectCategory"), "CN=Group,CN=Schema,CN=Configuration,DC=google,DC=com");
        assertEquals(((JSONObject) json.get("names")).get("name"), "DL UG COF Technology Associates");
        assertEquals(((JSONObject) json.get("names")).get("displayName"), "#Technology Associates");
        assertEquals(((JSONObject) json.get("names")).get("distinguishedName"), "CN=DL UG COF Technology Associates,OU=Mail Enabled Groups,OU=Exchange,DC=google,DC=com");
        assertEquals(((JSONObject) json.get("names")).get("cn"), "DL UG COF Technology "
                + "Associates");
        assertEquals(((JSONObject) json.get("object")).get("sAMAccountType"), "268435457");
        assertEquals(((JSONObject) json.get("object")).get("sAMAccountName"), "DL UG"
                + " COF Technology Associates");
        assertEquals(((JSONObject) json.get("object")).get("objectSid"), "S-1-5-21-"
                + "99512129-1830164216-1097030630-1192850");
        assertEquals(((JSONObject) json.get("object")).get("objectGUID"), "447354d9-"
                + "2507-423d-9a4d-8e6aaa3a3ed0");
        assertEquals(((JSONObject) json.get("object")).get("whenChanged"), "01:26.59 "
                + "PM, Mon 04/18/2016");
        assertEquals(((JSONObject) json.get("object")).get("whenCreated"), "12:44.40 "
                + "PM, Thu 09/11/2014");
        assertEquals(((JSONObject) json.get("object")).get("objectClass"), "top|group");
        assertEquals(((JSONObject) json.get("event")).get("uSNCreated"), "968626542");
        assertEquals(((JSONObject) json.get("event")).get("uSNChanged"), "1653936631");
        assertEquals(((JSONObject) json.get("event")).get("instanceType"), "4");
        assertEquals(((JSONObject) json.get("additional")).get("msExchVersion"),
                "4535486012416");
        assertEquals(((JSONObject) json.get("additional")).get("msExchRecipientDisplayType"), "1");
        assertEquals(((JSONObject) json.get("additional")).get("msExchPoliciesIncluded"),
                "{3D722DB5-B274-4C28-B991-46CBB56D9BC7},{26491CFC-9E50-"
                        + "4857-861B-0CB8DF22B5D7}");
        assertEquals(((JSONObject) json.get("additional")).get("msExchALObjectVersion"),
                "26");
        assertEquals(((JSONObject) json.get("additional")).get("mail"), "Technology"
                + "Associates@capitalone.com");
        assertEquals(((JSONObject) json.get("additional")).get("textEncodedORAddress"),
                "C=US;A= ;P=COF;O=RIC;S=TechnologyAssociates;");
        assertEquals(((JSONObject) json.get("additional")).get("msExchRequireAuthToSendTo"),
                "TRUE");
        assertEquals(((JSONObject) json.get("additional")).get("dSCorePropagationData"),
                "20160213101716.0Z|20150912043540.0Z|20150723164420.0Z|"
                        + "20150115174111.0Z|16010714223649.0Z");
        assertEquals(((JSONObject) json.get("additional")).get("groupType"), "8");
        assertEquals(((JSONObject) json.get("additional")).get("legacyExchangeDN"),
                "/O=COF/OU=RIC/cn=Recipients/cn=TechnologyAssociates");
        assertEquals(((JSONObject) json.get("additional")).get("managedBy"),
                "CN=ABC123,OU=Developers,OU=All Users,DC=google"
                        + ",DC=com");
        assertEquals(((JSONObject) json.get("additional")).get("showInAddressBook"),
                "CN=Default Global Address List,CN=All Global Address"
                        + " Lists,CN=Address Lists Container,CN=COF,CN="
                        + "Microsoft Exchange,CN=Services,"
                        + "CN=Configuration,DC=ds,DC=capitalone,DC=com|"
                        + "CN=All Groups,CN=All Address Lists,CN=Address"
                        + " Lists Container,CN=COF,CN=Microsoft Exchange"
                        + ",CN=Services,CN=Configuration,DC=ds,DC=capitalone"
                        + ",DC=com");
        assertEquals(((JSONObject) json.get("additional")).get("internetEncoding"), "0");
        assertEquals(((JSONObject) json.get("additional")).get("mailNickname"), "Technology"
                + "Associates");
        assertEquals(((JSONObject) json.get("additional")).get("proxyAddresses"), "x500:"
                + "/o=ExchangeLabs/ou=Exchange Administrative Group "
                + "(FYDIBOHF23SPDLT)/cn=Recipients/"
                + "cn=6438e74f00604776884c7c3cb8ef174f-DL UG COF T|"
                + "smtp:TechnologyAssociates@capitalone.mail.onmicrosoft.com"
                + "|NOTES:TechnologyAssociates@ccmta-domain"
                + "|X400:C=US;A= ;P=COF;O=RIC;S=TechnologyAssociates;"
                + "|SMTP:TechnologyAssociates@capitalone.com");
        assertEquals(((JSONObject) json.get("additional")).get("reportToOriginator"), "TRUE");
        assertEquals(((JSONObject) json.get("additional")).get("authOrig"), "CN=RMBX "
                + "Technology Communications,OU=Resource Mailboxes,"
                + "OU=Exchange,DC=google,DC=com"
                + "|CN=ABC123,OU=User Lock Policy 05,OU=All Users,"
                + "DC=google,DC=com|CN=ABC123,OU=User Lock Policy"
                + " 05,OU=All Users,DC=google,DC=com|"
                + "CN=ABC123,OU=Developers,OU=All Users,"
                + "DC=google,DC=com|CN=ABC123,OU=User Lock Policy"
                + " 05,OU=All Users,DC=google,DC=com"
                + "|CN=ABC123,OU=User Lock Policy 05,OU=All Users,DC=google"
                + ",DC=com|CN=ABC123,OU=User "
                + "Lock Policy 05,OU=All Users,DC=google,"
                + "DC=com|CN=ABC123,OU=User Lock Policy 00,OU=All Users,"
                + "DC=google,DC=com|CN=ABC123,OU=User "
                + "Lock Policy 05,OU=All Users,DC=google"
                + ",DC=com");
        assertEquals(((JSONObject) json.get("additional")).get("info"),
                "Owner: Elizabeth Dabney (ABC123)");

        String[] test = {"ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123", "ABC123"};
        List members = (ArrayList) ((JSONObject) json.get("additional")).get("member");

        for (int i = 0; i < test.length; i++) {
            assertEquals(test[i], members.get(i));
        }
    }

    /**
     * Checks the input JSON object for any null keys. If a particular value in the JSONObject
     * is another JSONObject, then recursively call this method again for the nested JSONObject
     * @param jsonObj: the input JSON object for which to check null keys
     */
    private void testKeysNotNull(JSONObject jsonObj){
        for(Object key: jsonObj.keySet()){
            assertNotNull(key);
            Object jsonValue = jsonObj.get(key);
            if(jsonValue.getClass().equals(JSONObject.class)){
                testKeysNotNull((JSONObject) jsonValue);
            }
        }
    }
}
