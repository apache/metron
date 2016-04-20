#!/usr/bin/env python
from cabby import create_client

try:

    # create a connection
    client = create_client('{{ opentaxii_domain }}', discovery_path='/services/discovery')

    # iterate through each defined collection
    collections = client.get_collections(uri='/services/collection')
    for collection in collections:

        # how many records in each collection?
        count = client.get_content_count(collection_name=collection.name)
        print "%-50s %-10d" % (collection.name, count.count)

except:

    print "Services not defined"
