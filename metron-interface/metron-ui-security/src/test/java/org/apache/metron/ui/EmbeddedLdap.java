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
package org.apache.metron.ui;

import java.io.File;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.ldap.LdapService;
import org.apache.directory.server.protocol.shared.SocketAcceptor;
import org.apache.directory.server.protocol.shared.store.LdifFileLoader;
import org.apache.directory.server.xdbm.Index;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * A Bean based wrapper for an Embedded Apache Directory Server used to back
 * LDAP Authentication.
 */
@Component
public class EmbeddedLdap implements InitializingBean, DisposableBean {

    public static final String EMBEDDED_LDAP_PROFILE = "embedded-ldap";
    private Logger LOG = LoggerFactory.getLogger(this.getClass());

    @Value("${ldap.provider.url}")
    private String providerUrl;

    @Value("${ldap.provider.userdn}")
    private String providerUserDn;

    @Value("${ldap.provider.password}")
    private String providerPassword;

    @Value("${ldap.user.dn.patterns}")
    private String userDnPatterns;

    @Value("${ldap.user.passwordAttribute}")
    private String passwordAttribute;

    @Value("${ldap.user.searchBase}")
    private String userSearchBase;

    @Value("${ldap.user.searchFilter}")
    private String userSearchFilter;

    @Value("${ldap.group.searchBase}")
    private String groupSearchBase;

    @Value("${ldap.group.roleAttribute}")
    private String groupRoleAttribute;

    @Value("${ldap.group.searchFilter}")
    private String groupSearchFilter;

    @Rule
    public TemporaryFolder workdir = new TemporaryFolder();

    private LdapService ldapService;

    private DefaultDirectoryService directoryService;

    private Partition partition;

    @Override
    public void destroy() throws Exception {
        LOG.info("Stopping embedded LDAP");

        ldapService.stop();
        directoryService.shutdown();

        workdir.delete();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        workdir.create();

        LOG.info("Starting embedded LDAP");
        LOG.debug("Using temporary directory %s", workdir.toString());

        directoryService = new DefaultDirectoryService();
        directoryService.setWorkingDirectory(workdir.getRoot());
        directoryService.getChangeLog().setEnabled(false);

        partition = addPartition("testPartition", "dc=org");
        addIndex("objectClass", "ou", "uid", "cn");

        SocketAcceptor socketAcceptor = new SocketAcceptor(null);

        Pattern p = Pattern.compile("ldaps?://([^:]*):(\\d*).*");
        Matcher m = p.matcher(providerUrl);
        int port;
        if (m.matches()) {
            port = Integer.parseInt(m.group(2));
        } else {
            port = 33389;
        }

        ldapService = new LdapService();
        ldapService.setIpPort(port);
        ldapService.setSearchBaseDn(userSearchBase);
        ldapService.setDirectoryService(directoryService);
        ldapService.setSocketAcceptor(socketAcceptor);

        directoryService.startup();
        ldapService.start();

        // load default schema
        applyLdif(new File("schema.ldif"));
        LOG.debug("LDAP server started");
    }

    private Partition addPartition(String partitionId, String partitionDn) throws Exception {
        Partition partition = new JdbmPartition();
        partition.setId(partitionId);
        partition.setSuffix(partitionDn);
        directoryService.addPartition(partition);

        return partition;
    }

    public void addIndex(String... attrs) {
        HashSet<Index<?, ServerEntry>> indexedAttributes = new HashSet<Index<?, ServerEntry>>();

        for (String attribute : attrs) {
            indexedAttributes.add(new JdbmIndex<String, ServerEntry>(attribute));
        }

        ((JdbmPartition) partition).setIndexedAttributes(indexedAttributes);
    }

    public void applyLdif(File ldifFile) throws Exception {
        new LdifFileLoader(directoryService.getAdminSession(), ldifFile, null, this.getClass().getClassLoader())
                .execute();
    }
}
