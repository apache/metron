#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

if [ "$#" -ne 5 ] || [ "$1" == "-h" ] || [ "$1" == "--help" ]; then
    echo 5 args required
    echo Usage:
    echo "  mode: [backup|restore] - backup will save configs to a directory named \"metron-backup\". Restore will take those same configs and restore them to Ambari."
    echo "  ambari_address: host and port for Ambari server, e.g. \"node1:8080\""
    echo "  username: Ambari admin username"
    echo "  password: Ambari admin user password"
    echo "  cluster_name: hadoop cluster name. Can be found in Ambari under \"Admin > Manage Ambari\""
    echo "  directory_base: (Optional) root directory location where the backup will be written to and read from. Default is the executing directory, \".\", with backup data stored to a subdirectory named \"metron-backup\""
    exit 1
fi

mode=$1
ambari_address=$2
username=$3
password=$4
cluster_name=$5
# optional base directory
outdir_base=${6:-.}

if [ -f "/etc/default/metron" ]; then
  source /etc/default/metron
fi

OUT_DIR=$outdir_base/metron-backup
AMBARI_CONFIG_DIR=$OUT_DIR/ambari-configs
ZK_CONFIG_DIR=$OUT_DIR/zk-configs

if [ "$mode" == "backup" ]; then
    if [ ! -d "$OUT_DIR" ]; then
        mkdir $OUT_DIR
    fi
    if [ ! -d "$AMBARI_CONFIG_DIR" ]; then
        mkdir $AMBARI_CONFIG_DIR
    fi
    if [ ! -d "$ZK_CONFIG_DIR" ]; then
        mkdir $ZK_CONFIG_DIR
    fi
    if [ -f "/var/lib/ambari-server/resources/scripts/configs.py" ]; then
        echo Backing up Ambari config...
        echo Checking connection...
        ret_status=$(curl -i -u $username:$password -H "X-Requested-By: ambari" -X GET  http://$ambari_address/api/v1/clusters/$cluster_name | head -n 1)
        if [ "HTTP/1.1 200 OK" == "$ret_status" ]; then
            for config_type in $(curl -u $username:$password -H "X-Requested-By: ambari" -X GET  http://$ambari_address/api/v1/clusters/$cluster_name?fields=Clusters/desired_configs | grep '" : {' | grep -v Clusters | grep -v desired_configs | cut -d'"' -f2 | grep metron); 
            do 
                echo Saving $config_type
                /var/lib/ambari-server/resources/scripts/configs.py -u $username -p $password -a get -l ${ambari_address%:*} -n $cluster_name -c $config_type -f $AMBARI_CONFIG_DIR/${config_type}.json
            done
            echo Done backing up Ambari config...
        else
            echo Unable to get cluster detail from Ambari. Check your username, password, and cluster name. Skipping.
        fi
    else
        echo Skipping Ambari config backup - Ambari not found on this host
    fi

    if [ -f "$METRON_HOME/bin/zk_load_configs.sh" ]; then
        echo Backing up Metron config
        $METRON_HOME/bin/zk_load_configs.sh -m PULL -o $ZK_CONFIG_DIR -z $ZOOKEEPER -f
        echo Done backing up Metron config
    else
        echo Skipping Metron config backup - Metron not found on this host
    fi
elif [ "$mode" == "restore" ]; then
    if [ ! -d "$OUT_DIR" ]; then
        echo Backup directory not found, aborting
        exit 1
    fi
    if [ -f "/var/lib/ambari-server/resources/scripts/configs.py" ]; then
        if [ -d "$AMBARI_CONFIG_DIR" ]; then
            echo Restoring metron config from files in $AMBARI_CONFIG_DIR
            i=0
            for filename in $AMBARI_CONFIG_DIR/*;
            do
                [ -e "$filename" ] || continue
                ((i=i+1))
                filename=${filename##*/}
                echo $i. Found config: $filename
                config_type=${filename%.json}
                echo "   Setting config_type to $config_type"
                /var/lib/ambari-server/resources/scripts/configs.py -u $username -p $password -a set -l ${ambari_address%:*} -n $cluster_name -c $config_type -f $AMBARI_CONFIG_DIR/${config_type}.json
                echo "   Done restoring $config_type"
            done
            echo Done restoring $i metron config files from $OUT_DIR
        else
            echo Ambari backup directory not found, skipping 
        fi
    else
        echo Skipping Ambari config restore - Ambari not found on this host
    fi

    if [ -f "$METRON_HOME/bin/zk_load_configs.sh" ]; then
        if [ -d "$ZK_CONFIG_DIR" ]; then
            echo Restoring Metron zookeeper config from files in $ZK_CONFIG_DIR
            $METRON_HOME/bin/zk_load_configs.sh -m PUSH -i $ZK_CONFIG_DIR -z $ZOOKEEPER -f
            echo Pulling config locally into Metron home config dir
            $METRON_HOME/bin/zk_load_configs.sh -m PULL -o ${METRON_HOME}/config/zookeeper -z $ZOOKEEPER -f
            echo Done restoring Metron zookeeper config from files in $ZK_CONFIG_DIR
        else
            echo Metron config backup directory not found, skipping 
        fi
    else
        echo Skipping Metron config restore - Metron not found on this host
    fi
else
    echo Mode "$mode" not recognized. Exiting.
fi
