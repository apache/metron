#
#  Licensed to the Apache Software Foundation (ASF) under one or more
#  contributor license agreements.  See the NOTICE file distributed with
#  this work for additional information regarding copyright ownership.
#  The ASF licenses this file to You under the Apache License, Version 2.0
#  (the "License"); you may not use this file except in compliance with
#  the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#
%define timestamp           %(date +%Y%m%d%H%M)
%define version             %{?_version}%{!?_version:UNKNOWN}
%define full_version        %{version}%{?_prerelease}
%define prerelease_fmt      %{?_prerelease:.%{_prerelease}}          
%define vendor_version      %{?_vendor_version}%{!?_vendor_version: UNKNOWN}
%define url                 http://metron.apache.org/
%define base_name           metron
%define name                %{base_name}-%{vendor_version}
%define versioned_app_name  %{base_name}-%{version}
%define buildroot           %{_topdir}/BUILDROOT/%{versioned_app_name}-root
%define installpriority     %{_priority} # Used by alternatives for concurrent version installs
%define __jar_repack        %{nil}

%define metron_root         %{_prefix}/%{base_name}
%define metron_home         %{metron_root}/%{full_version}
%define metron_extensions_lib %{metron_home}/extension_lib
%define metron_extensions_alt_lib %{metron_home}/extension_alt_lib
%define metron_extensions_etc %{metron_home}/extension_etc
%define metron_extensions_etc_parsers %{metron_extensions_etc}/parsers
%define metron_extensions_alt_etc %{metron_home}/extension_alt_etc
%define metron_extensions_alt_etc_parsers %{metron_extensions_alt_etc}/parsers

# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Name:           %{base_name}
Version:        %{version}
Release:        %{timestamp}%{prerelease_fmt}
BuildRoot:      %{buildroot}
BuildArch:      noarch
Summary:        Apache Metron provides a scalable advanced security analytics framework
License:        ASL 2.0
Group:          Applications/Internet
URL:            %{url}
Source0:        metron-common-%{full_version}-archive.tar.gz
Source1:        metron-parsers-%{full_version}-archive.tar.gz
Source2:        metron-elasticsearch-%{full_version}-archive.tar.gz
Source3:        metron-data-management-%{full_version}-archive.tar.gz
Source4:        metron-solr-%{full_version}-archive.tar.gz
Source5:        metron-enrichment-%{full_version}-archive.tar.gz
Source6:        metron-indexing-%{full_version}-archive.tar.gz
Source7:        metron-pcap-backend-%{full_version}-archive.tar.gz
Source8:        metron-profiler-%{full_version}-archive.tar.gz
Source9:        metron-rest-%{full_version}-archive.tar.gz
Source10:       metron-config-%{full_version}-archive.tar.gz
#extensions
Source11:       metron-parser-asa-assembly-%{full_version}-archive.tar.gz
Source12:       metron-parser-bro-assembly-%{full_version}-archive.tar.gz
Source13:       metron-parser-cef-assembly-%{full_version}-archive.tar.gz
Source14:       metron-parser-fireeye-assembly-%{full_version}-archive.tar.gz
Source15:       metron-parser-ise-assembly-%{full_version}-archive.tar.gz
Source16:       metron-parser-lancope-assembly-%{full_version}-archive.tar.gz
Source17:       metron-parser-logstash-assembly-%{full_version}-archive.tar.gz
Source18:       metron-parser-paloalto-assembly-%{full_version}-archive.tar.gz
Source19:       metron-parser-snort-assembly-%{full_version}-archive.tar.gz
Source20:       metron-parser-sourcefire-assembly-%{full_version}-archive.tar.gz
Source21:       metron-parser-squid-assembly-%{full_version}-archive.tar.gz
Source22:       metron-parser-websphere-assembly-%{full_version}-archive.tar.gz
Source23:       metron-parser-yaf-assembly-%{full_version}-archive.tar.gz
Source24:       metron-management-%{full_version}-archive.tar.gz

%description
Apache Metron provides a scalable advanced security analytics framework

%prep
rm -rf %{_rpmdir}/%{buildarch}/%{versioned_app_name}*
rm -rf %{_srcrpmdir}/%{versioned_app_name}*

%build
rm -rf %{_builddir}
mkdir -p %{_builddir}/%{versioned_app_name}

%clean
rm -rf %{buildroot}
rm -rf %{_builddir}/*

%install
rm -rf %{buildroot}
mkdir -p %{buildroot}%{metron_home}
mkdir -p %{buildroot}/etc/init.d
mkdir -p %{buildroot}%{metron_extensions_lib}
mkdir -p %{buildroot}%{metron_extensions_etc}
mkdir -p %{buildroot}%{metron_extensions_etc_parsers}
mkdir -p %{buildroot}%{metron_extensions_alt_lib}
mkdir -p %{buildroot}%{metron_extensions_alt_etc}
mkdir -p %{buildroot}%{metron_extensions_alt_etc_parsers}
mkdir -p %{buildroot}%{metron_extensions_etc_parsers}/asa
mkdir -p %{buildroot}%{metron_extensions_etc_parsers}/bro
mkdir -p %{buildroot}%{metron_extensions_etc_parsers}/cef
mkdir -p %{buildroot}%{metron_extensions_etc_parsers}/fireeye
mkdir -p %{buildroot}%{metron_extensions_etc_parsers}/ise
mkdir -p %{buildroot}%{metron_extensions_etc_parsers}/lancope
mkdir -p %{buildroot}%{metron_extensions_etc_parsers}/logstash
mkdir -p %{buildroot}%{metron_extensions_etc_parsers}/paloalto
mkdir -p %{buildroot}%{metron_extensions_etc_parsers}/snort
mkdir -p %{buildroot}%{metron_extensions_etc_parsers}/sourcefire
mkdir -p %{buildroot}%{metron_extensions_etc_parsers}/squid
mkdir -p %{buildroot}%{metron_extensions_etc_parsers}/websphere
mkdir -p %{buildroot}%{metron_extensions_etc_parsers}/yaf

# copy source files and untar
tar -xzf %{SOURCE0} -C %{buildroot}%{metron_home}
tar -xzf %{SOURCE1} -C %{buildroot}%{metron_home}
tar -xzf %{SOURCE2} -C %{buildroot}%{metron_home}
tar -xzf %{SOURCE3} -C %{buildroot}%{metron_home}
tar -xzf %{SOURCE4} -C %{buildroot}%{metron_home}
tar -xzf %{SOURCE5} -C %{buildroot}%{metron_home}
tar -xzf %{SOURCE6} -C %{buildroot}%{metron_home}
tar -xzf %{SOURCE7} -C %{buildroot}%{metron_home}
tar -xzf %{SOURCE8} -C %{buildroot}%{metron_home}
tar -xzf %{SOURCE9} -C %{buildroot}%{metron_home}
tar -xzf %{SOURCE10} -C %{buildroot}%{metron_home}
tar -xzf %{SOURCE11} -C %{buildroot}%{metron_extensions_etc_parsers}/asa
tar -xzf %{SOURCE12} -C %{buildroot}%{metron_extensions_etc_parsers}/bro
tar -xzf %{SOURCE13} -C %{buildroot}%{metron_extensions_etc_parsers}/cef
tar -xzf %{SOURCE14} -C %{buildroot}%{metron_extensions_etc_parsers}/fireeye
tar -xzf %{SOURCE15} -C %{buildroot}%{metron_extensions_etc_parsers}/ise
tar -xzf %{SOURCE16} -C %{buildroot}%{metron_extensions_etc_parsers}/lancope
tar -xzf %{SOURCE17} -C %{buildroot}%{metron_extensions_etc_parsers}/logstash
tar -xzf %{SOURCE18} -C %{buildroot}%{metron_extensions_etc_parsers}/paloalto
tar -xzf %{SOURCE19} -C %{buildroot}%{metron_extensions_etc_parsers}/snort
tar -xzf %{SOURCE20} -C %{buildroot}%{metron_extensions_etc_parsers}/sourcefire
tar -xzf %{SOURCE21} -C %{buildroot}%{metron_extensions_etc_parsers}/squid
tar -xzf %{SOURCE22} -C %{buildroot}%{metron_extensions_etc_parsers}/websphere
tar -xzf %{SOURCE23} -C %{buildroot}%{metron_extensions_etc_parsers}/yaf
tar -xzf %{SOURCE24} -C %{buildroot}%{metron_home}

# move the bundles from config to extensions lib
mv %{buildroot}%{metron_extensions_etc_parsers}/asa/lib/*.bundle %{buildroot}%{metron_extensions_lib}/
mv %{buildroot}%{metron_extensions_etc_parsers}/bro/lib/*.bundle %{buildroot}%{metron_extensions_lib}/
mv %{buildroot}%{metron_extensions_etc_parsers}/cef/lib/*.bundle %{buildroot}%{metron_extensions_lib}/
mv %{buildroot}%{metron_extensions_etc_parsers}/fireeye/lib/*.bundle %{buildroot}%{metron_extensions_lib}/
mv %{buildroot}%{metron_extensions_etc_parsers}/ise/lib/*.bundle %{buildroot}%{metron_extensions_lib}/
mv %{buildroot}%{metron_extensions_etc_parsers}/lancope/lib/*.bundle %{buildroot}%{metron_extensions_lib}/
mv %{buildroot}%{metron_extensions_etc_parsers}/logstash/lib/*.bundle %{buildroot}%{metron_extensions_lib}/
mv %{buildroot}%{metron_extensions_etc_parsers}/paloalto/lib/*.bundle %{buildroot}%{metron_extensions_lib}/
mv %{buildroot}%{metron_extensions_etc_parsers}/snort/lib/*.bundle %{buildroot}%{metron_extensions_lib}/
mv %{buildroot}%{metron_extensions_etc_parsers}/sourcefire/lib/*.bundle %{buildroot}%{metron_extensions_lib}/
mv %{buildroot}%{metron_extensions_etc_parsers}/websphere/lib/*.bundle %{buildroot}%{metron_extensions_lib}/

install %{buildroot}%{metron_home}/bin/metron-rest %{buildroot}/etc/init.d/
install %{buildroot}%{metron_home}/bin/metron-management-ui %{buildroot}/etc/init.d/

# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

%package        common
Summary:        Metron Common
Group:          Applications/Internet
Provides:       common = %{version}

%description    common
This package installs the Metron common files %{metron_home}

%files          common

%defattr(-,root,root,755)
%dir %{metron_root}
%dir %{metron_home}
%dir %{metron_home}/bin
%dir %{metron_home}/lib
%{metron_home}/bin/zk_load_configs.sh
%{metron_home}/bin/stellar
%attr(0644,root,root) %{metron_home}/lib/metron-common-%{full_version}.jar

# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

%package        parsers
Summary:        Metron Parser Files
Group:          Applications/Internet
Provides:       parsers = %{version}

%description    parsers
This package installs the Metron Parser files

%files          parsers
%defattr(-,root,root,755)
%dir %{metron_root}
%dir %{metron_home}
%dir %{metron_home}/bin
%dir %{metron_home}/config
%dir %{metron_home}/config/zookeeper
%dir %{metron_home}/config/zookeeper/parsers
%dir %{metron_home}/patterns
%dir %{metron_home}/lib
%dir %{metron_extensions_etc}
%dir %{metron_extensions_etc_parsers}
%dir %{metron_extensions_lib}
%dir %{metron_extensions_alt_etc}
%dir %{metron_extensions_alt_etc_parsers}
%dir %{metron_extensions_alt_lib}
%{metron_home}/bin/start_parser_topology.sh
%{metron_home}/config/zookeeper/parsers/jsonMap.json
%{metron_home}/patterns/common
%attr(0644,root,root) %{metron_home}/lib/metron-parsers-%{full_version}-uber.jar

# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

%package        parser-extension-asa
Summary:        Metron ASA Parser Extension Files
Group:          Applications/Internet
Provides:       parser-extension-asa = %{version}

%description    parser-extension-asa
This package installs the Metron ASA Parser Extension files

%files          parser-extension-asa
%defattr(-,root,root,755)
%dir %{metron_root}
%dir %{metron_home}
%dir %{metron_extensions_etc}
%dir %{metron_extensions_etc_parsers}
%dir %{metron_extensions_etc_parsers}/asa
%dir %{metron_extensions_etc_parsers}/asa/config
%dir %{metron_extensions_etc_parsers}/asa/config/zookeeper
%dir %{metron_extensions_etc_parsers}/asa/config/zookeeper/parsers
%dir %{metron_extensions_etc_parsers}/asa/config/zookeeper/enrichments
%dir %{metron_extensions_etc_parsers}/asa/config/zookeeper/indexing
%dir %{metron_extensions_etc_parsers}/asa/patterns
%dir %{metron_extensions_lib}
%{metron_extensions_etc_parsers}/asa/config/zookeeper/parsers/asa.json
%{metron_extensions_etc_parsers}/asa/config/zookeeper/enrichments/asa.json
%{metron_extensions_etc_parsers}/asa/config/zookeeper/indexing/asa.json
%{metron_extensions_etc_parsers}/asa/patterns/asa
%{metron_extensions_etc_parsers}/asa/patterns/common
%attr(0644,root,root) %{metron_extensions_lib}/metron-parser-asa-bundle-%{full_version}.bundle

# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

%package        parser-extension-bro
Summary:        Metron BRO Parser Extension Files
Group:          Applications/Internet
Provides:       parser-extension-bro = %{version}

%description    parser-extension-bro
This package installs the Metron BRO Parser Extension files

%files          parser-extension-bro
%defattr(-,root,root,755)
%dir %{metron_root}
%dir %{metron_home}
%dir %{metron_extensions_etc}
%dir %{metron_extensions_etc_parsers}
%dir %{metron_extensions_etc_parsers}/bro
%dir %{metron_extensions_etc_parsers}/bro/config
%dir %{metron_extensions_etc_parsers}/bro/config/elasticsearch
%dir %{metron_extensions_etc_parsers}/bro/config/zookeeper
%dir %{metron_extensions_etc_parsers}/bro/config/zookeeper/parsers
%dir %{metron_extensions_etc_parsers}/bro/config/zookeeper/enrichments
%dir %{metron_extensions_etc_parsers}/bro/config/zookeeper/indexing
%dir %{metron_extensions_lib}
%{metron_extensions_etc_parsers}/bro/config/zookeeper/parsers/bro.json
%{metron_extensions_etc_parsers}/bro/config/zookeeper/enrichments/bro.json
%{metron_extensions_etc_parsers}/bro/config/zookeeper/indexing/bro.json
%{metron_extensions_etc_parsers}/bro/config/elasticsearch/bro_index.template
%attr(0644,root,root) %{metron_extensions_lib}/metron-parser-bro-bundle-%{full_version}.bundle

# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


%package        parser-extension-cef
Summary:        Metron CEF Parser Extension Files
Group:          Applications/Internet
Provides:       parser-extension-cef = %{version}

%description    parser-extension-cef
This package installs the Metron CEF Parser Extension files

%files          parser-extension-cef
%defattr(-,root,root,755)
%dir %{metron_root}
%dir %{metron_home}
%dir %{metron_extensions_etc}
%dir %{metron_extensions_etc_parsers}
%dir %{metron_extensions_etc_parsers}/cef
%dir %{metron_extensions_etc_parsers}/cef/config
%dir %{metron_extensions_etc_parsers}/cef/config/zookeeper
%dir %{metron_extensions_etc_parsers}/cef/config/zookeeper/parsers
%dir %{metron_extensions_etc_parsers}/cef/config/zookeeper/enrichments
%dir %{metron_extensions_etc_parsers}/cef/config/zookeeper/indexing
%dir %{metron_extensions_lib}
%{metron_extensions_etc_parsers}/cef/config/zookeeper/parsers/cef.json
%{metron_extensions_etc_parsers}/cef/config/zookeeper/enrichments/cef.json
%{metron_extensions_etc_parsers}/cef/config/zookeeper/indexing/cef.json
%attr(0644,root,root) %{metron_extensions_lib}/metron-parser-cef-bundle-%{full_version}.bundle

# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


%package        parser-extension-fireeye
Summary:        Metron Fireeye Parser Extension Files
Group:          Applications/Internet
Provides:       parser-extension-fireeye = %{version}

%description    parser-extension-fireeye
This package installs the Metron Fireeye Parser Extension files

%files          parser-extension-fireeye
%defattr(-,root,root,755)
%dir %{metron_root}
%dir %{metron_home}
%dir %{metron_extensions_etc}
%dir %{metron_extensions_etc_parsers}
%dir %{metron_extensions_etc_parsers}/fireeye
%dir %{metron_extensions_etc_parsers}/fireeye/config
%dir %{metron_extensions_etc_parsers}/fireeye/config/zookeeper
%dir %{metron_extensions_etc_parsers}/fireeye/config/zookeeper/parsers
%dir %{metron_extensions_etc_parsers}/fireeye/config/zookeeper/enrichments
%dir %{metron_extensions_etc_parsers}/fireeye/config/zookeeper/indexing
%dir %{metron_extensions_etc_parsers}/fireeye/patterns
%dir %{metron_extensions_lib}
%{metron_extensions_etc_parsers}/fireeye/config/zookeeper/parsers/fireeye.json
%{metron_extensions_etc_parsers}/fireeye/config/zookeeper/enrichments/fireeye.json
%{metron_extensions_etc_parsers}/fireeye/config/zookeeper/indexing/fireeye.json
%{metron_extensions_etc_parsers}/fireeye/patterns/fireeye
%{metron_extensions_etc_parsers}/fireeye/patterns/common
%attr(0644,root,root) %{metron_extensions_lib}/metron-parser-fireeye-bundle-%{full_version}.bundle

# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


%package        parser-extension-ise
Summary:        Metron ISE Parser Extension Files
Group:          Applications/Internet
Provides:       parser-extension-ise = %{version}

%description    parser-extension-ise
This package installs the Metron ISE Parser Extension files

%files          parser-extension-ise
%defattr(-,root,root,755)
%dir %{metron_root}
%dir %{metron_home}
%dir %{metron_extensions_etc}
%dir %{metron_extensions_etc_parsers}
%dir %{metron_extensions_etc_parsers}/ise
%dir %{metron_extensions_etc_parsers}/ise/config
%dir %{metron_extensions_etc_parsers}/ise/config/zookeeper
%dir %{metron_extensions_etc_parsers}/ise/config/zookeeper/parsers
%dir %{metron_extensions_etc_parsers}/ise/config/zookeeper/enrichments
%dir %{metron_extensions_etc_parsers}/ise/config/zookeeper/indexing
%dir %{metron_extensions_lib}
%{metron_extensions_etc_parsers}/ise/config/zookeeper/parsers/ise.json
%{metron_extensions_etc_parsers}/ise/config/zookeeper/enrichments/ise.json
%{metron_extensions_etc_parsers}/ise/config/zookeeper/indexing/ise.json
%attr(0644,root,root) %{metron_extensions_lib}/metron-parser-ise-bundle-%{full_version}.bundle

# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


%package        parser-extension-lancope
Summary:        Metron Lancope Parser Extension Files
Group:          Applications/Internet
Provides:       parser-extension-lancope = %{version}

%description    parser-extension-lancope
This package installs the Metron Lancope Parser Extension files

%files          parser-extension-lancope
%defattr(-,root,root,755)
%dir %{metron_root}
%dir %{metron_home}
%dir %{metron_extensions_etc}
%dir %{metron_extensions_etc_parsers}
%dir %{metron_extensions_etc_parsers}/lancope
%dir %{metron_extensions_etc_parsers}/lancope/config
%dir %{metron_extensions_etc_parsers}/lancope/config/zookeeper
%dir %{metron_extensions_etc_parsers}/lancope/config/zookeeper/parsers
%dir %{metron_extensions_etc_parsers}/lancope/config/zookeeper/enrichments
%dir %{metron_extensions_etc_parsers}/lancope/config/zookeeper/indexing
%dir %{metron_extensions_lib}
%{metron_extensions_etc_parsers}/lancope/config/zookeeper/parsers/lancope.json
%{metron_extensions_etc_parsers}/lancope/config/zookeeper/enrichments/lancope.json
%{metron_extensions_etc_parsers}/lancope/config/zookeeper/indexing/lancope.json
%attr(0644,root,root) %{metron_extensions_lib}/metron-parser-lancope-bundle-%{full_version}.bundle

# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


%package        parser-extension-logstash
Summary:        Metron Logstash Parser Extension Files
Group:          Applications/Internet
Provides:       parser-extension-logstash = %{version}

%description    parser-extension-logstash
This package installs the Metron Logstash Parser Extension files

%files          parser-extension-logstash
%defattr(-,root,root,755)
%dir %{metron_root}
%dir %{metron_home}
%dir %{metron_extensions_etc}
%dir %{metron_extensions_etc_parsers}
%dir %{metron_extensions_etc_parsers}/logstash
%dir %{metron_extensions_etc_parsers}/logstash/config
%dir %{metron_extensions_etc_parsers}/logstash/config/zookeeper
%dir %{metron_extensions_etc_parsers}/logstash/config/zookeeper/parsers
%dir %{metron_extensions_etc_parsers}/logstash/config/zookeeper/enrichments
%dir %{metron_extensions_etc_parsers}/logstash/config/zookeeper/indexing
%dir %{metron_extensions_lib}
%{metron_extensions_etc_parsers}/logstash/config/zookeeper/parsers/logstash.json
%{metron_extensions_etc_parsers}/logstash/config/zookeeper/enrichments/logstash.json
%{metron_extensions_etc_parsers}/logstash/config/zookeeper/indexing/logstash.json
%attr(0644,root,root) %{metron_extensions_lib}/metron-parser-logstash-bundle-%{full_version}.bundle

# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


%package        parser-extension-paloalto
Summary:        Metron PaloAlto Parser Extension Files
Group:          Applications/Internet
Provides:       parser-extension-paloalto = %{version}

%description    parser-extension-paloalto
This package installs the Metron PaloAlto Parser Extension files

%files          parser-extension-paloalto
%defattr(-,root,root,755)
%dir %{metron_root}
%dir %{metron_home}
%dir %{metron_extensions_etc}
%dir %{metron_extensions_etc_parsers}
%dir %{metron_extensions_etc_parsers}/paloalto
%dir %{metron_extensions_etc_parsers}/paloalto/config
%dir %{metron_extensions_etc_parsers}/paloalto/config/zookeeper
%dir %{metron_extensions_etc_parsers}/paloalto/config/zookeeper/parsers
%dir %{metron_extensions_etc_parsers}/paloalto/config/zookeeper/enrichments
%dir %{metron_extensions_etc_parsers}/paloalto/config/zookeeper/indexing
%dir %{metron_extensions_lib}
%{metron_extensions_etc_parsers}/paloalto/config/zookeeper/parsers/paloalto.json
%{metron_extensions_etc_parsers}/paloalto/config/zookeeper/enrichments/paloalto.json
%{metron_extensions_etc_parsers}/paloalto/config/zookeeper/indexing/paloalto.json
%attr(0644,root,root) %{metron_extensions_lib}/metron-parser-paloalto-bundle-%{full_version}.bundle

# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


%package        parser-extension-snort
Summary:        Metron Snort Parser Extension Files
Group:          Applications/Internet
Provides:       parser-extension-snort = %{version}

%description    parser-extension-snort
This package installs the Metron Snort Parser Extension files

%files          parser-extension-snort
%defattr(-,root,root,755)
%dir %{metron_root}
%dir %{metron_home}
%dir %{metron_extensions_etc}
%dir %{metron_extensions_etc_parsers}
%dir %{metron_extensions_etc_parsers}/snort
%dir %{metron_extensions_etc_parsers}/snort/config
%dir %{metron_extensions_etc_parsers}/snort/config/elasticsearch
%dir %{metron_extensions_etc_parsers}/snort/config/zookeeper
%dir %{metron_extensions_etc_parsers}/snort/config/zookeeper/parsers
%dir %{metron_extensions_etc_parsers}/snort/config/zookeeper/enrichments
%dir %{metron_extensions_etc_parsers}/snort/config/zookeeper/indexing
%dir %{metron_extensions_lib}
%{metron_extensions_etc_parsers}/snort/config/zookeeper/parsers/snort.json
%{metron_extensions_etc_parsers}/snort/config/zookeeper/enrichments/snort.json
%{metron_extensions_etc_parsers}/snort/config/zookeeper/indexing/snort.json
%{metron_extensions_etc_parsers}/snort/config/elasticsearch/snort_index.template
%attr(0644,root,root) %{metron_extensions_lib}/metron-parser-snort-bundle-%{full_version}.bundle

# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

%package        parser-extension-sourcefire
Summary:        Metron Sourcefire Parser Extension Files
Group:          Applications/Internet
Provides:       parser-extension-sourcefire = %{version}

%description    parser-extension-sourcefire
This package installs the Metron Sourcefire Parser Extension files

%files          parser-extension-sourcefire
%defattr(-,root,root,755)
%dir %{metron_root}
%dir %{metron_home}
%dir %{metron_extensions_etc}
%dir %{metron_extensions_etc_parsers}
%dir %{metron_extensions_etc_parsers}/sourcefire
%dir %{metron_extensions_etc_parsers}/sourcefire/config
%dir %{metron_extensions_etc_parsers}/sourcefire/config/zookeeper
%dir %{metron_extensions_etc_parsers}/sourcefire/config/zookeeper/parsers
%dir %{metron_extensions_etc_parsers}/sourcefire/config/zookeeper/enrichments
%dir %{metron_extensions_etc_parsers}/sourcefire/config/zookeeper/indexing
%dir %{metron_extensions_etc_parsers}/sourcefire/patterns
%dir %{metron_extensions_lib}
%{metron_extensions_etc_parsers}/sourcefire/config/zookeeper/parsers/sourcefire.json
%{metron_extensions_etc_parsers}/sourcefire/config/zookeeper/enrichments/sourcefire.json
%{metron_extensions_etc_parsers}/sourcefire/config/zookeeper/indexing/sourcefire.json
%{metron_extensions_etc_parsers}/sourcefire/patterns/sourcefire
%{metron_extensions_etc_parsers}/sourcefire/patterns/common
%attr(0644,root,root) %{metron_extensions_lib}/metron-parser-sourcefire-bundle-%{full_version}.bundle

# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


%package        parser-extension-squid
Summary:        Metron Squid Parser Extension Files
Group:          Applications/Internet
Provides:       parser-extension-squid = %{version}

%description    parser-extension-squid
This package installs the Metron Squid Parser Extension files

%files          parser-extension-squid
%defattr(-,root,root,755)
%dir %{metron_root}
%dir %{metron_home}
%dir %{metron_extensions_etc}
%dir %{metron_extensions_etc_parsers}
%dir %{metron_extensions_etc_parsers}/squid
%dir %{metron_extensions_etc_parsers}/squid/config
%dir %{metron_extensions_etc_parsers}/squid/config/zookeeper
%dir %{metron_extensions_etc_parsers}/squid/config/zookeeper/parsers
%dir %{metron_extensions_etc_parsers}/squid/config/zookeeper/enrichments
%dir %{metron_extensions_etc_parsers}/squid/config/zookeeper/indexing
%dir %{metron_extensions_etc_parsers}/squid/patterns
%{metron_extensions_etc_parsers}/squid/config/zookeeper/parsers/squid.json
%{metron_extensions_etc_parsers}/squid/config/zookeeper/enrichments/squid.json
%{metron_extensions_etc_parsers}/squid/config/zookeeper/indexing/squid.json
%{metron_extensions_etc_parsers}/squid/patterns/squid
%{metron_extensions_etc_parsers}/squid/patterns/common

# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


%package        parser-extension-websphere
Summary:        Metron  Parser Websphere Extension Files
Group:          Applications/Internet
Provides:       parser-extension-websphere = %{version}

%description    parser-extension-websphere
This package installs the Metron Websphere Parser Extension files

%files          parser-extension-websphere
%defattr(-,root,root,755)
%dir %{metron_root}
%dir %{metron_home}
%dir %{metron_extensions_etc}
%dir %{metron_extensions_etc_parsers}
%dir %{metron_extensions_etc_parsers}/websphere
%dir %{metron_extensions_etc_parsers}/websphere/config
%dir %{metron_extensions_etc_parsers}/websphere/config/zookeeper
%dir %{metron_extensions_etc_parsers}/websphere/config/zookeeper/parsers
%dir %{metron_extensions_etc_parsers}/websphere/config/zookeeper/enrichments
%dir %{metron_extensions_etc_parsers}/websphere/config/zookeeper/indexing
%dir %{metron_extensions_etc_parsers}/websphere/patterns
%dir %{metron_extensions_lib}
%{metron_extensions_etc_parsers}/websphere/config/zookeeper/parsers/websphere.json
%{metron_extensions_etc_parsers}/websphere/config/zookeeper/enrichments/websphere.json
%{metron_extensions_etc_parsers}/websphere/config/zookeeper/indexing/websphere.json
%{metron_extensions_etc_parsers}/websphere/patterns/websphere
%{metron_extensions_etc_parsers}/websphere/patterns/common
%attr(0644,root,root) %{metron_extensions_lib}/metron-parser-websphere-bundle-%{full_version}.bundle

# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


%package        parser-extension-yaf
Summary:        Metron Yaf Parser Extension Files
Group:          Applications/Internet
Provides:       parser-extension-yaf = %{version}

%description    parser-extension-yaf
This package installs the Metron Yaf Parser Extension files

%files          parser-extension-yaf
%defattr(-,root,root,755)
%dir %{metron_root}
%dir %{metron_home}
%dir %{metron_extensions_etc}
%dir %{metron_extensions_etc_parsers}
%dir %{metron_extensions_etc_parsers}/yaf
%dir %{metron_extensions_etc_parsers}/yaf/config
%dir %{metron_extensions_etc_parsers}/yaf/config/elasticsearch
%dir %{metron_extensions_etc_parsers}/yaf/config/zookeeper
%dir %{metron_extensions_etc_parsers}/yaf/config/zookeeper/parsers
%dir %{metron_extensions_etc_parsers}/yaf/config/zookeeper/enrichments
%dir %{metron_extensions_etc_parsers}/yaf/config/zookeeper/indexing
%dir %{metron_extensions_etc_parsers}/yaf/patterns
%{metron_extensions_etc_parsers}/yaf/config/zookeeper/parsers/yaf.json
%{metron_extensions_etc_parsers}/yaf/config/zookeeper/enrichments/yaf.json
%{metron_extensions_etc_parsers}/yaf/config/zookeeper/indexing/yaf.json
%{metron_extensions_etc_parsers}/yaf/config/elasticsearch/yaf_index.template
%{metron_extensions_etc_parsers}/yaf/patterns/yaf
%{metron_extensions_etc_parsers}/yaf/patterns/common

# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


%package        elasticsearch
Summary:        Metron Elasticsearch Files
Group:          Applications/Internet
Provides:       elasticsearch = %{version}

%description    elasticsearch
This package installs the Metron Elasticsearch files

%files          elasticsearch
%defattr(-,root,root,755)
%dir %{metron_root}
%dir %{metron_home}
%dir %{metron_home}/bin
%dir %{metron_home}/config
%dir %{metron_home}/lib
%{metron_home}/bin/start_elasticsearch_topology.sh
%{metron_home}/config/elasticsearch.properties
%attr(0644,root,root) %{metron_home}/lib/metron-elasticsearch-%{full_version}-uber.jar

# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

%package        data-management
Summary:        Metron Data Management Files
Group:          Applications/Internet
Provides:       data-management = %{version}

%description    data-management
This package installs the Metron Parser files

%files          data-management
%defattr(-,root,root,755)
%dir %{metron_root}
%dir %{metron_home}
%dir %{metron_home}/bin
%dir %{metron_home}/lib
%{metron_home}/bin/Whois_CSV_to_JSON.py
%{metron_home}/bin/geo_enrichment_load.sh
%{metron_home}/bin/flatfile_loader.sh
%{metron_home}/bin/prune_elasticsearch_indices.sh
%{metron_home}/bin/prune_hdfs_files.sh
%{metron_home}/bin/threatintel_bulk_prune.sh
%{metron_home}/bin/threatintel_taxii_load.sh
%attr(0644,root,root) %{metron_home}/lib/metron-data-management-%{full_version}.jar

# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

%package        solr
Summary:        Metron Solr Files
Group:          Applications/Internet
Provides:       solr = %{version}

%description    solr
This package installs the Metron Solr files

%files          solr
%defattr(-,root,root,755)
%dir %{metron_root}
%dir %{metron_home}
%dir %{metron_home}/bin
%dir %{metron_home}/config
%dir %{metron_home}/lib
%{metron_home}/bin/start_solr_topology.sh
%{metron_home}/config/solr.properties
%attr(0644,root,root) %{metron_home}/lib/metron-solr-%{full_version}-uber.jar

# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

%package        enrichment
Summary:        Metron Enrichment Files
Group:          Applications/Internet
Provides:       enrichment = %{version}

%description    enrichment
This package installs the Metron Enrichment files

%files          enrichment
%defattr(-,root,root,755)
%dir %{metron_root}
%dir %{metron_home}
%dir %{metron_home}/bin
%dir %{metron_home}/config
%dir %{metron_home}/flux
%dir %{metron_home}/flux/enrichment
%{metron_home}/bin/latency_summarizer.sh
%{metron_home}/bin/start_enrichment_topology.sh
%{metron_home}/config/enrichment.properties
%{metron_home}/flux/enrichment/remote.yaml
%attr(0644,root,root) %{metron_home}/lib/metron-enrichment-%{full_version}-uber.jar

# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

%package        indexing
Summary:        Metron Indexing Files
Group:          Applications/Internet
Provides:       indexing = %{version}

%description    indexing
This package installs the Metron Indexing files

%files          indexing
%defattr(-,root,root,755)
%dir %{metron_root}
%dir %{metron_home}
%dir %{metron_home}/flux
%dir %{metron_home}/flux/indexing
%{metron_home}/flux/indexing/remote.yaml
%{metron_home}/config/zookeeper/indexing/error.json
%{metron_home}/config/zeppelin/metron/metron-yaf-telemetry.json
%{metron_home}/config/zeppelin/metron/metron-connection-report.json
%{metron_home}/config/zeppelin/metron/metron-ip-report.json
%{metron_home}/config/zeppelin/metron/metron-connection-volume-report.json

# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

%package        metron-management
Summary:        Metron Management Libary
Group:          Applications/Internet
Provides:       metron-management = %{version}

%description    metron-management
This package installs the Metron Management Library

%files          metron-management
%defattr(-,root,root,755)
%dir %{metron_root}
%dir %{metron_home}/lib
%attr(0644,root,root) %{metron_home}/lib/metron-management-%{full_version}.jar


# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

%package        pcap
Summary:        Metron PCAP
Group:          Applications/Internet
Provides:       pcap = %{version}

%description    pcap
This package installs the Metron PCAP files %{metron_home}

%files          pcap
%defattr(-,root,root,755)
%dir %{metron_root}
%dir %{metron_home}
%dir %{metron_home}/config
%dir %{metron_home}/bin
%dir %{metron_home}/flux
%dir %{metron_home}/flux/pcap
%dir %{metron_home}/lib
%{metron_home}/config/pcap.properties
%{metron_home}/bin/pcap_inspector.sh
%{metron_home}/bin/pcap_query.sh
%{metron_home}/bin/start_pcap_topology.sh
%{metron_home}/bin/pcap_zeppelin_run.sh
%{metron_home}/flux/pcap/remote.yaml
%{metron_home}/config/zeppelin/metron/metron-pcap.json
%attr(0644,root,root) %{metron_home}/lib/metron-pcap-backend-%{full_version}.jar

# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

%package        profiler
Summary:        Metron Profiler
Group:          Applications/Internet
Provides:       profiler = %{version}

%description    profiler
This package installs the Metron Profiler %{metron_home}

%files          profiler
%defattr(-,root,root,755)
%dir %{metron_root}
%dir %{metron_home}
%dir %{metron_home}/config
%dir %{metron_home}/bin
%dir %{metron_home}/flux
%dir %{metron_home}/flux/profiler
%dir %{metron_home}/lib
%{metron_home}/config/profiler.properties
%{metron_home}/bin/start_profiler_topology.sh
%{metron_home}/flux/profiler/remote.yaml
%attr(0644,root,root) %{metron_home}/lib/metron-profiler-%{full_version}-uber.jar

# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

%package        rest
Summary:        Metron Rest
Group:          Applications/Internet
Provides:       rest = %{version}

%description    rest
This package installs the Metron Rest %{metron_home}

%files          rest
%defattr(-,root,root,755)
%dir %{metron_root}
%dir %{metron_home}
%dir %{metron_home}/config
%dir %{metron_home}/bin
%dir %{metron_home}/lib
%{metron_home}/config/rest_application.yml
%{metron_home}/bin/metron-rest
/etc/init.d/metron-rest
%attr(0644,root,root) %{metron_home}/lib/metron-rest-%{full_version}.jar

%post rest
chkconfig --add metron-rest

%preun rest
chkconfig --del metron-rest

# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

%package        config
Summary:        Metron Management UI
Group:          Applications/Internet
Provides:       config = %{version}

%description    config
This package installs the Metron Management UI %{metron_home}

%files          config
%defattr(-,root,root,755)
%dir %{metron_root}
%dir %{metron_home}
%dir %{metron_home}/bin
%dir %{metron_home}/web
%dir %{metron_home}/web/expressjs
%dir %{metron_home}/web/management-ui
%dir %{metron_home}/web/management-ui/assets
%dir %{metron_home}/web/management-ui/assets/ace
%dir %{metron_home}/web/management-ui/assets/ace/snippets
%dir %{metron_home}/web/management-ui/assets/fonts
%dir %{metron_home}/web/management-ui/assets/fonts/Roboto
%dir %{metron_home}/web/management-ui/assets/images
%dir %{metron_home}/web/management-ui/license
%{metron_home}/bin/metron-management-ui
/etc/init.d/metron-management-ui
%attr(0755,root,root) %{metron_home}/web/expressjs/server.js
%attr(0644,root,root) %{metron_home}/web/expressjs/package.json
%attr(0644,root,root) %{metron_home}/web/management-ui/favicon.ico
%attr(0644,root,root) %{metron_home}/web/management-ui/index.html
%attr(0644,root,root) %{metron_home}/web/management-ui/*.js
%attr(0644,root,root) %{metron_home}/web/management-ui/*.js.gz
%attr(0644,root,root) %{metron_home}/web/management-ui/*.ttf
%attr(0644,root,root) %{metron_home}/web/management-ui/*.svg
%attr(0644,root,root) %{metron_home}/web/management-ui/*.eot
%attr(0644,root,root) %{metron_home}/web/management-ui/*.woff
%attr(0644,root,root) %{metron_home}/web/management-ui/*.woff2
%attr(0644,root,root) %{metron_home}/web/management-ui/assets/ace/*.js
%attr(0644,root,root) %{metron_home}/web/management-ui/assets/ace/LICENSE
%attr(0644,root,root) %{metron_home}/web/management-ui/assets/ace/snippets/*.js
%attr(0644,root,root) %{metron_home}/web/management-ui/assets/fonts/Roboto/LICENSE.txt
%attr(0644,root,root) %{metron_home}/web/management-ui/assets/fonts/Roboto/*.ttf
%attr(0644,root,root) %{metron_home}/web/management-ui/assets/images/*
%attr(0644,root,root) %{metron_home}/web/management-ui/license/*

%post config
chkconfig --add metron-management-ui

%preun config
chkconfig --del metron-management-ui

# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

%changelog
* Thu Jun 29 2017 Apache Metron <dev@metron.apache.org> - 0.4.1
- Add Metron Management jar
* Thu May 15 2017 Apache Metron <dev@metron.apache.org> - 0.4.0
- Added Management UI
* Tue May 9 2017 Apache Metron <dev@metron.apache.org> - 0.4.0
- Add Zeppelin Connection Volume Report Dashboard
* Thu May 4 2017 Ryan Merriman <merrimanr@gmail.com> - 0.4.0
- Added REST
* Tue May 2 2017 David Lyle <dlyle65535@gmail.com> - 0.4.0
- Add Metron IP Report
* Fri Apr 28 2017 Apache Metron <dev@metron.apache.org> - 0.4.0
- Add Zeppelin Connection Report Dashboard
* Tue Apr 04 2017 Otto Fowler <ottobackwards@gmail.com> - 0.3.1
- support for parsers as extensions
* Thu Jan 19 2017 Justin Leet <justinjleet@gmail.com> - 0.3.1
- Replace GeoIP files with new implementation
* Thu Nov 03 2016 David Lyle <dlyle65535@gmail.com> - 0.2.1
- Add ASA parser/enrichment configuration files 
* Thu Jul 21 2016 Michael Miklavcic <michael.miklavcic@gmail.com> - 0.2.1
- Remove parser flux files
- Add new enrichment files
* Thu Jul 14 2016 Michael Miklavcic <michael.miklavcic@gmail.com> - 0.2.1
- Adding PCAP subpackage
- Added directory macros to files sections
* Thu Jul 14 2016 Justin Leet <justinjleet@gmail.com> - 0.2.1
- Adding Enrichment subpackage
* Thu Jul 14 2016 Justin Leet <justinjleet@gmail.com> - 0.2.1
- Adding Solr subpackage
* Thu Jul 14 2016 Justin Leet <justinjleet@gmail.com> - 0.2.1
- Adding Data Management subpackage
* Thu Jul 14 2016 Justin Leet <jsutinjleet@gmail.com> - 0.2.1
- Adding Elasticsearch subpackage
* Wed Jul 13 2016 Justin Leet <justinjleet@gmail.com> - 0.2.1
- Adding Parsers subpackage
* Tue Jul 12 2016 Michael Miklavcic <michael.miklavcic@gmail.com> - 0.2.1
- First packaging
