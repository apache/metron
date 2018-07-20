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

%define _binaries_in_noarch_packages_terminate_build   0

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
Source11:       metron-management-%{full_version}-archive.tar.gz
Source12:       metron-maas-service-%{full_version}-archive.tar.gz
Source13:       metron-alerts-%{full_version}-archive.tar.gz
Source14:       metron-performance-%{full_version}-archive.tar.gz

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
tar -xzf %{SOURCE11} -C %{buildroot}%{metron_home}
tar -xzf %{SOURCE12} -C %{buildroot}%{metron_home}
tar -xzf %{SOURCE13} -C %{buildroot}%{metron_home}
tar -xzf %{SOURCE14} -C %{buildroot}%{metron_home}

install %{buildroot}%{metron_home}/bin/metron-management-ui %{buildroot}/etc/init.d/
install %{buildroot}%{metron_home}/bin/metron-alerts-ui %{buildroot}/etc/init.d/

# allows node dependencies to be packaged in the RPMs
npm install --prefix="%{buildroot}%{metron_home}/web/expressjs" --only=production

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
%dir %{metron_home}/config
%dir %{metron_home}/config/zookeeper
%dir %{metron_home}/lib
%{metron_home}/bin/zk_load_configs.sh
%{metron_home}/bin/stellar
%{metron_home}/bin/cluster_info.py
%{metron_home}/config/zookeeper/global.json
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
%{metron_home}/bin/start_parser_topology.sh
%{metron_home}/config/zookeeper/parsers/bro.json
%{metron_home}/config/zookeeper/parsers/jsonMap.json
%{metron_home}/config/zookeeper/parsers/jsonMapQuery.json
%{metron_home}/config/zookeeper/parsers/jsonMapWrappedQuery.json
%{metron_home}/config/zookeeper/parsers/snort.json
%{metron_home}/config/zookeeper/parsers/squid.json
%{metron_home}/config/zookeeper/parsers/websphere.json
%{metron_home}/config/zookeeper/parsers/yaf.json
%{metron_home}/config/zookeeper/parsers/asa.json
%{metron_home}/patterns/asa
%{metron_home}/patterns/common
%{metron_home}/patterns/fireeye
%{metron_home}/patterns/sourcefire
%{metron_home}/patterns/squid
%{metron_home}/patterns/websphere
%{metron_home}/patterns/yaf
%attr(0644,root,root) %{metron_home}/lib/metron-parsers-%{full_version}-uber.jar

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

%package        performance
Summary:        Metron Performance Tools
Group:          Applications/Internet
Provides:       performance = %{version}

%description    performance
This package installs performance tools useful for Metron

%files          performance
%defattr(-,root,root,755)
%dir %{metron_root}
%dir %{metron_home}
%dir %{metron_home}/bin
%dir %{metron_home}/lib
%{metron_home}/bin/load_tool.sh
%attr(0644,root,root) %{metron_home}/lib/metron-performance-%{full_version}.jar

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
%{metron_home}/bin/flatfile_summarizer.sh
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
%{metron_home}/bin/create_collection.sh
%{metron_home}/bin/delete_collection.sh
%{metron_home}/bin/install_solr.sh
%{metron_home}/bin/start_solr.sh
%{metron_home}/bin/start_solr_topology.sh
%{metron_home}/bin/stop_solr.sh
%{metron_home}/config/solr.properties
%{metron_home}/config/schema/bro/schema.xml
%{metron_home}/config/schema/bro/solrconfig.xml
%{metron_home}/config/schema/error/schema.xml
%{metron_home}/config/schema/error/solrconfig.xml
%{metron_home}/config/schema/metaalert/schema.xml
%{metron_home}/config/schema/metaalert/solrconfig.xml
%{metron_home}/config/schema/snort/schema.xml
%{metron_home}/config/schema/snort/solrconfig.xml
%{metron_home}/config/schema/yaf/schema.xml
%{metron_home}/config/schema/yaf/solrconfig.xml
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
%dir %{metron_home}/config/zookeeper
%dir %{metron_home}/config/zookeeper/enrichments
%dir %{metron_home}/flux
%dir %{metron_home}/flux/enrichment
%{metron_home}/bin/latency_summarizer.sh
%{metron_home}/bin/start_enrichment_topology.sh
%{metron_home}/config/enrichment-splitjoin.properties
%{metron_home}/config/enrichment-unified.properties
%{metron_home}/config/zookeeper/enrichments/bro.json
%{metron_home}/config/zookeeper/enrichments/snort.json
%{metron_home}/config/zookeeper/enrichments/websphere.json
%{metron_home}/config/zookeeper/enrichments/yaf.json
%{metron_home}/config/zookeeper/enrichments/asa.json
%{metron_home}/flux/enrichment/remote-splitjoin.yaml
%{metron_home}/flux/enrichment/remote-unified.yaml
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
%dir %{metron_home}/bin
%dir %{metron_home}/flux
%dir %{metron_home}/flux/indexing
%{metron_home}/bin/start_hdfs_topology.sh
%{metron_home}/flux/indexing/batch/remote.yaml
%{metron_home}/flux/indexing/random_access/remote.yaml
%{metron_home}/config/zookeeper/indexing/bro.json
%{metron_home}/config/zookeeper/indexing/snort.json
%{metron_home}/config/zookeeper/indexing/websphere.json
%{metron_home}/config/zookeeper/indexing/yaf.json
%{metron_home}/config/zookeeper/indexing/asa.json
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
%{metron_home}/bin/metron-rest.sh
%attr(0644,root,root) %{metron_home}/lib/metron-rest-%{full_version}.jar

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
%dir %{metron_home}/web/expressjs/node_modules
%dir %{metron_home}/web/expressjs/node_modules/.bin
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
%attr(0755,root,root) %{metron_home}/web/expressjs/node_modules/*
%attr(0755,root,root) %{metron_home}/web/expressjs/node_modules/.bin/*
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

# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

%package        maas-service
Summary:        Metron MaaS service
Group:          Application/Internet
Provides:       maas-service = %{version}

%description    maas-service
This package install the Metron MaaS Service files %{metron_home}

%files          maas-service
%defattr(-,root,root,755)
%dir %{metron_root}
%dir %{metron_home}
%dir %{metron_home}/bin
%{metron_home}/bin/maas_service.sh
%{metron_home}/bin/maas_deploy.sh
%attr(0644,root,root) %{metron_home}/lib/metron-maas-service-%{full_version}-uber.jar

# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

%package        alerts
Summary:        Metron Alerts UI
Group:          Applications/Internet
Provides:       alerts = %{version}

%description    alerts
This package installs the Metron Alerts UI %{metron_home}

%files          alerts
%defattr(-,root,root,755)
%dir %{metron_root}
%dir %{metron_home}
%dir %{metron_home}/bin
%dir %{metron_home}/web
%dir %{metron_home}/web/alerts-ui
%dir %{metron_home}/web/alerts-ui/assets
%dir %{metron_home}/web/alerts-ui/assets/ace
%dir %{metron_home}/web/alerts-ui/assets/fonts
%dir %{metron_home}/web/alerts-ui/assets/fonts/Roboto
%dir %{metron_home}/web/alerts-ui/assets/images
%{metron_home}/bin/metron-alerts-ui
/etc/init.d/metron-alerts-ui
%attr(0755,root,root) %{metron_home}/web/expressjs/alerts-server.js
%attr(0644,root,root) %{metron_home}/web/alerts-ui/favicon.ico
%attr(0644,root,root) %{metron_home}/web/alerts-ui/index.html
%attr(0644,root,root) %{metron_home}/web/alerts-ui/*.bundle.css
%attr(0644,root,root) %{metron_home}/web/alerts-ui/*.js
%attr(0644,root,root) %{metron_home}/web/alerts-ui/*.ttf
%attr(0644,root,root) %{metron_home}/web/alerts-ui/*.svg
%attr(0644,root,root) %{metron_home}/web/alerts-ui/*.jpg
%attr(0644,root,root) %{metron_home}/web/alerts-ui/*.eot
%attr(0644,root,root) %{metron_home}/web/alerts-ui/*.woff
%attr(0644,root,root) %{metron_home}/web/alerts-ui/*.woff2
%attr(0644,root,root) %{metron_home}/web/alerts-ui/3rdpartylicenses.txt
%attr(0644,root,root) %{metron_home}/web/alerts-ui/assets/ace/*.js
%attr(0644,root,root) %{metron_home}/web/alerts-ui/assets/ace/LICENSE
%attr(0644,root,root) %{metron_home}/web/alerts-ui/assets/fonts/font.css
%attr(0644,root,root) %{metron_home}/web/alerts-ui/assets/fonts/Roboto/LICENSE.txt
%attr(0644,root,root) %{metron_home}/web/alerts-ui/assets/fonts/Roboto/*.ttf
%attr(0644,root,root) %{metron_home}/web/alerts-ui/assets/images/*

# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

%post config
chkconfig --add metron-management-ui
chkconfig --add metron-alerts-ui

%preun config
chkconfig --del metron-management-ui
chkconfig --del metron-alerts-ui

%changelog
* Thu Feb 1 2018 Apache Metron <dev@metron.apache.org> - 0.4.3
- Add Solr install script to Solr RPM
* Tue Sep 25 2017 Apache Metron <dev@metron.apache.org> - 0.4.2
- Add Alerts UI
* Tue Sep 19 2017 Apache Metron <dev@metron.apache.org> - 0.4.2
- Updated and renamed metron-rest script
* Tue Aug 29 2017 Apache Metron <dev@metron.apache.org> - 0.4.1
- Add Metron MaaS service
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
