import json
import sys

def merge_dicts(*dict_args):
    result = {}
    for dictionary in dict_args:
        result.update(dictionary)
    return result

def get_statement(component, version, license):
    #This product bundles SuperWidget 1.2.3, which is available under a
    #"3-clause BSD" license.
    s = "This product bundles " + component + " " + version \
      + ", which is available under a \"" + license + "\" license."
    return s

licenses = {}
for i in xrange(1, len(sys.argv)):
    with open(sys.argv[i]) as f:
        license_summary = f.read()
        licenses = merge_dicts(licenses, json.loads(license_summary))
for component, value in licenses.iteritems():
    if not(value['license'].startswith("Apache")):
        print get_statement(component, value['version'], value['license'])

