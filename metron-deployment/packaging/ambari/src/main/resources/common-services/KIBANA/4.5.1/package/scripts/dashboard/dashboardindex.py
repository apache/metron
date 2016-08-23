#!/usr/bin/python
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

from elasticsearch import Elasticsearch
from elasticsearch.helpers import bulk
import cPickle as pickle
import argparse, sys, os.path
import errno
import os


class DashboardIndex(object):

    def __init__(self, host='localhost', port=9200, url_prefix='', timeout=10, **kwargs):
        """
        :arg host: hostname of the node (default: localhost)
        :arg port: port to use (integer, default: 9200)
        :arg url_prefix: optional url prefix for elasticsearch
        :arg timeout: default timeout in seconds (float, default: 10)
        """
        self.es = Elasticsearch([{'host':host,'port': port, 'url_prefix': url_prefix, 'timeout':timeout}])

    def get(self):
        """
        Get .kibana index from Elasticsearch
        """
        dotkibana = self.es.search(index='.kibana', size = 100)
        return dotkibana['hits']['hits']

    def load(self,filespec):
        """
        Save Index data on local filesystem
        :args filespec: path/filename for saved file
        """
        data=[]
        with open(filespec,'rb') as fp:
            data = pickle.load(fp)
        return data

    def save(self,filename,data):
        """
        Save Index data on local filesystem
        :args filespec: path/filename for saved file
        """
        with open(filename,'wb') as fp:
            pickle.dump(data,fp)

    def put(self,data):
        """
        Bulk write data to Elasticsearch
        :args data: data to be written (note: index name is specified in data)
        """
        bulk(self.es,data)

    def main(self,args):

        if args.save:
            print("running save with host:%s on port %d, filespec: %s" % (args.hostname, args.port, args.filespec))
            self.save(filename=args.filespec,data=di.get())
        else:
            """
            Loads Kibana Dashboard definition from disk and replaces .kibana on index
            :args filespec: path/filename for saved file
            """
            if not os.path.isfile(args.filespec):
                raise IOError(
                    errno.ENOENT, os.strerror(errno.ENOENT), args.filespec)
            self.es.indices.delete(index='.kibana', ignore=[400, 404])
            self.put(data=di.load(filespec=args.filespec))

if __name__ == '__main__':

    parser = argparse.ArgumentParser()
    parser.add_argument("hostname", help="ES Hostname or IP", type=str)
    parser.add_argument("port", help="ES Port", type=int)
    parser.add_argument("filespec", help="file to be pushed from or saved to", type=str)
    parser.add_argument("-s","--save", help="run in SAVE mode - .kibana will be read and saved to filespec",action="store_true")
    args = parser.parse_args()
    di = DashboardIndex(host=args.hostname,port=args.port)
    di.main(args)
