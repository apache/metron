from ambari_commons import OSCheck

metron_enrichment_topology = 'enrichment'
configured_flag_file = ""

process_name = 'mysqld'
if OSCheck.is_suse_family() or OSCheck.is_ubuntu_family():
    daemon_name = 'mysql'
else:
    daemon_name = 'mysqld'