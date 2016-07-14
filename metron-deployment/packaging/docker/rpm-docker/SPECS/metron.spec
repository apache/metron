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
%define url                 http://metron.incubator.apache.org/
%define base_name           metron
%define name                %{base_name}-%{vendor_version}
%define versioned_app_name  %{base_name}-%{version}
%define buildroot           %{_topdir}/BUILDROOT/%{versioned_app_name}-root
%define installpriority     %{_priority} # Used by alternatives for concurrent version installs
%define __jar_repack        %{nil}

%define metron_root         %{_prefix}/%{base_name}
%define metron_home         %{metron_root}/%{full_version}

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
Source6:        metron-pcap-backend-%{full_version}-archive.tar.gz

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

# copy source files and untar
tar -xzf %{SOURCE0} -C %{buildroot}%{metron_home}
tar -xzf %{SOURCE1} -C %{buildroot}%{metron_home}
tar -xzf %{SOURCE2} -C %{buildroot}%{metron_home}
tar -xzf %{SOURCE3} -C %{buildroot}%{metron_home}
tar -xzf %{SOURCE4} -C %{buildroot}%{metron_home}
tar -xzf %{SOURCE5} -C %{buildroot}%{metron_home}
tar -xzf %{SOURCE6} -C %{buildroot}%{metron_home}

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
%dir %{metron_home}/flux
%dir %{metron_home}/flux/asa
%dir %{metron_home}/flux/fireeye
%dir %{metron_home}/flux/ise
%dir %{metron_home}/flux/lancope
%dir %{metron_home}/flux/paloalto
%dir %{metron_home}/flux/sourcefire
%dir %{metron_home}/flux/websphere
%dir %{metron_home}/patterns
%dir %{metron_home}/lib
%{metron_home}/bin/start_parser_topology.sh
%{metron_home}/config/zookeeper/parsers/bluecoat.json
%{metron_home}/config/zookeeper/parsers/bro.json
%{metron_home}/config/zookeeper/parsers/snort.json
%{metron_home}/config/zookeeper/parsers/squid.json
%{metron_home}/config/zookeeper/parsers/websphere.json
%{metron_home}/config/zookeeper/parsers/yaf.json
%{metron_home}/flux/asa/remote.yaml
%{metron_home}/flux/asa/test.yaml
%{metron_home}/flux/fireeye/remote.yaml
%{metron_home}/flux/fireeye/test.yaml
%{metron_home}/flux/ise/remote.yaml
%{metron_home}/flux/ise/test.yaml
%{metron_home}/flux/lancope/remote.yaml
%{metron_home}/flux/lancope/test.yaml
%{metron_home}/flux/paloalto/remote.yaml
%{metron_home}/flux/paloalto/test.yaml
%{metron_home}/flux/sourcefire/remote.yaml
%{metron_home}/flux/sourcefire/test.yaml
%{metron_home}/flux/websphere/remote.yaml
%{metron_home}/flux/websphere/test.yaml
%{metron_home}/patterns/asa
%{metron_home}/patterns/common
%{metron_home}/patterns/fireeye
%{metron_home}/patterns/sourcefire
%{metron_home}/patterns/squid
%{metron_home}/patterns/websphere
%{metron_home}/patterns/yaf
%attr(0644,root,root) %{metron_home}/lib/metron-parsers-%{full_version}.jar

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
%attr(0644,root,root) %{metron_home}/lib/metron-elasticsearch-%{full_version}.jar

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
%{metron_home}/bin/flatfile_loader.sh
%{metron_home}/bin/prune_elasticsearch_indices.sh
%{metron_home}/bin/prune_hdfs_files.sh
%{metron_home}/bin/threatintel_bulk_load.sh
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
%attr(0644,root,root) %{metron_home}/lib/metron-solr-%{full_version}.jar

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
%dir %{metron_home}/config/zookeeper
%dir %{metron_home}/config/zookeeper/enrichments
%dir %{metron_home}/flux
%dir %{metron_home}/flux/enrichment
%{metron_home}/bin/latency_summarizer.sh
%{metron_home}/config/zookeeper/enrichments/bro.json
%{metron_home}/config/zookeeper/enrichments/snort.json
%{metron_home}/config/zookeeper/enrichments/websphere.json
%{metron_home}/config/zookeeper/enrichments/yaf.json
%{metron_home}/flux/enrichment/remote.yaml
%{metron_home}/flux/enrichment/test.yaml

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
%{metron_home}/flux/pcap/remote.yaml
%attr(0644,root,root) %{metron_home}/lib/metron-pcap-backend-%{full_version}.jar

# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

%changelog
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
