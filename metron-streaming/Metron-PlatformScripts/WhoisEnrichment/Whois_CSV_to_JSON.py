#!/usr/bin/python

"""
Copyright 2014 Cisco Systems, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
"""

import sys
import os
import csv
import json
import multiprocessing
import logging
logging.basicConfig(level=logging.DEBUG)


def is_field_excluded(fieldname=None):
    """
    Checks to see if a field name is a member of a list of names to exclude. Modify to suit your own list.

    :param fieldname: A string representing a field name
    :return: True or False
    """
    import re

    # List of fields names to exclude
    excluded_fields = [
        'Audit_auditUpdatedDate',
        #'domainName'
    ]

    if fieldname in excluded_fields:
        return True

    # Regexes to match for exclusion
    excluded_regexes = [
        ['_rawText$', re.IGNORECASE],
    ]

    for regex in excluded_regexes:
        if re.search(regex[0], fieldname, regex[1]):
            return True

    return False


def process_csv(in_filename, out_filename):
    """
    Processes a CSV file of WHOIS data and converts each line to a JSON element, skipping specific fields that
    are not deemed necessary (domainName, *_rawText, Audit_auditUpdatedDate)

    :param in_filename: Input CSV filename with full path
    :param out_filename: Output JSON filename with full path
    :return: None
    """
    if out_filename:
        out_fh = open(out_filename, 'wb')
        logging.debug('%s: Converting %s to %s' % (multiprocessing.current_process().name, in_filename, out_filename))
    else:
        logging.debug('%s: Analyzing %s' % (multiprocessing.current_process().name, in_filename))

    with open(in_filename, 'rb') as f:
        reader = csv.DictReader(f, delimiter=',', quotechar='"')
        line_num = 0
        try:
            for row in reader:
                line_num += 1
                try:
                    if out_filename:
                        # json conversion and output
                        new_row = {}
                        for field in reader.fieldnames:
                            # fields we don't want include these + anything with rawText
                            #if field not in ['Audit_auditUpdatedDate', 'domainName'] and not field.endswith('_rawText'):
                            if not is_field_excluded(field):
                                new_row[field] = row.get(field)
                        json.dump(new_row, out_fh)
                        out_fh.write('\n')
                    else:
                        # analysis .. check to be sure fileheader and csv row counts match
                        if len(row) != len(reader.fieldnames):
                            raise Exception('Field count mismatch: row: %s / fields: %s' % (len(row), len(reader.fieldnames)))
                except Exception, e:
                    logging.warn("Error with file %s, line %s: %s" % (in_filename, line_num, e))

            if not out_filename:
                logging.info('Analyzed %s: OK' % in_filename)
        except Exception, e:
            logging.warn(e)

        out_fh.close()


##-------------------------------------------------------------------------

def process_files(source_dir, output_dir, max_processes=10, overwrite=False):
    """
    Generates a multiprocessing.Pool() queue with a list of input and output files to be processed with processCSV.
    Files are added by walking the source_dir and adding any file with a CSV extension. Output is placed into a single
    directory for processing. Output filenames are generated using the first part of the directory name so a file
    named source_dir/com/1.csv would become outputDir/com_1.json

    :param source_dir: Source directory of CSV files
    :param output_dir: Output directory for resultant JSON files
    :param max_processes: Maximum number of processes run
    :return:
    """
    logging.info("Processing Whois files from %s" % source_dir)

    if output_dir and not os.path.exists(output_dir):
        logging.debug("Creating output directory %s" % output_dir)
        os.makedirs(output_dir)

    logging.info("Starting %s pool workers" % max_processes)

    if sys.version.startswith('2.6'):
        # no maxtaskperchild in 2.6
        pool = multiprocessing.Pool(processes=max_processes)
    else:
        pool = multiprocessing.Pool(processes=max_processes, maxtasksperchild=4)

    filecount = 0
    for dirname, dirnames, filenames in os.walk(source_dir):
        for filename in filenames:
            if filename.endswith('.csv'):
                # output files go to outputDir and are named using the last subdirectory from the dirname
                if output_dir:
                    out_filename = filename.replace('csv', 'json')
                    out_filename = os.path.join(output_dir, '%s_%s' % (os.path.split(dirname)[-1], out_filename))

                    # if file does not exist or if overwrite is true, add file process to the pool
                    if not os.path.isfile(out_filename) or overwrite:
                        pool.apply_async(process_csv, args=(os.path.join(dirname, filename), out_filename))
                        filecount += 1
                    else:
                        logging.info("Skipping %s, %s exists and overwrite is false" % (filename, out_filename))
                else:
                    # no outputdir so we just analyze the files
                    pool.apply_async(process_csv, args=(os.path.join(dirname, filename), None))
                    filecount += 1

    try:
        pool.close()
        logging.info("Starting activities on %s CSV files" % filecount)
        pool.join()
    except KeyboardInterrupt:
        logging.info("Aborting")
        pool.terminate()

    logging.info("Completed")


##-------------------------------------------------------------------------

if __name__ == "__main__":

    max_cpu = multiprocessing.cpu_count()

    from optparse import OptionParser
    parser = OptionParser()
    parser.add_option('-s', '--source', dest='source_dir', action='store',
                      help='Source directory to walk for CSV files')
    parser.add_option('-o', '--output', dest='out_dir', action='store',
                      help='Output directory for JSON files')
    parser.add_option('-O', '--overwrite', dest='overwrite', action='store_true',
                      help='Overwrite existing files in output directory')
    parser.add_option('-p', '--processes', dest='max_processes', action='store', default=max_cpu, type='int',
                      help='Max number of processes to spawn')
    parser.add_option('-a', '--analyze', dest='analyze', action='store_true',
                      help='Analyze CSV files for validity, no file output')
    parser.add_option('-d', '--debug', dest='debug', action='store_true',
                      help='Enable debug messages')

    (options, args) = parser.parse_args()

    if not options.source_dir:
        logging.error("Source directory required")
        sys.exit(-1)

    if not options.out_dir or options.analyze:
        out_dir = None
    elif not options.out_dir:
        logging.error("Ouput directory or analysis option required")
        sys.exit(-1)
    else:
        out_dir = options.out_dir

    if options.max_processes > max_cpu:
        logging.warn('Max Processes (%s) is greater than available Processors (%s)' % (options.max_processes, max_cpu))

    if options.debug:
        # enable debug level and multiprocessing debugging
        logging.basicConfig(level=logging.DEBUG)
        multiprocessing.log_to_stderr(logging.DEBUG)

    process_files(options.source_dir, options.out_dir, options.max_processes, options.overwrite)

