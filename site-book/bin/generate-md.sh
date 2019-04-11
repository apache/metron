#!/usr/bin/env bash

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


# ------------------------------------------------------------------
#
# This script collects the *.md files and other resources needed to generate
# a book-like collection of end-user documentation.
#
# The Metron development community has chosen to do most documentation in README.md
# files, because they are easy to write and maintain, and located near the code they
# document. Also they are versioned along with that code, so they are always in sync
# with the particular version being considered.
#
# However, the location of the various README.md files in github are not necessarily
# obvious to non-developers, and can therefore be difficult to find and use.
# In order to make the files easier to use as end-user documentation, we collect them
# into a book-like collection.  It should perhaps be viewed as a collection of essays,
# since each README.md file is written independently.


## fail fast in the event of a failure of any command in this script
set -e

## This script assumes it is running at $METRON_SOURCE/site-book/bin/
METRON_SOURCE=`cd $(dirname $0); cd ../..; pwd`

## Maintainers set EXCLUSION_LIST to a list of egrep-style regular expressions.
## MD files whose file path that matches any of these patterns will be excluded.
## Please note that the file paths being matched are output of 'find', rooted at
## $METRON_SOURCE.  'Find' will start each path with './', which is matched by '^\./'.
## Please place each regex in single quotes, and don't forget to backslash-escape
## literal periods and other special characters if needed.
EXCLUSION_LIST=(
    '/site/'
    '/site-book/'
    '/dev-utilities/'
    '/node_modules/'
    '/\.github/'
)

## This is a list of resources (eg .png files) needed to render the markdown files.
## Each entry is a file path, relative to $METRON_SOURCE.
## Note: any images in site-book/src/site/src-resources/images/ will also be included.
RESOURCE_LIST=(
    metron-deployment/readme-images/ambari-storm-site-properties.png
    metron-deployment/readme-images/ambari-storm-site.png
    metron-deployment/readme-images/custom-storm-site-final.png
    metron-deployment/readme-images/enable-kerberos-configure-kerberos.png
    metron-deployment/readme-images/enable-kerberos-started.png
    metron-deployment/readme-images/enable-kerberos.png
    metron-platform/metron-job/metron-job_state_statechart_diagram.svg
    metron-platform/metron-parsing/parser_arch.png
    metron-platform/metron-indexing/indexing_arch.png
    metron-platform/metron-enrichment/metron-enrichment-storm/enrichment_arch.png
    metron-platform/metron-enrichment/metron-enrichment-storm/unified_enrichment_arch.svg
    metron-analytics/metron-maas-service/maas_arch.png
    metron-contrib/metron-performance/performance_measurement.png
    use-cases/forensic_clustering/find_alerts.png
    use-cases/forensic_clustering/clustered.png
    use-cases/parser_chaining/message_routing_high_level.svg
    use-cases/parser_chaining/aggregated_parser_chaining_flow.svg
    use-cases/typosquat_detection/squid_search.png
    use-cases/typosquat_detection/drill_down.png
)

## This is a list of duples, flattened into a bash array.  Even fields are relative paths to a .md file
## that needs an href re-written to match a resource in the images/ directory.  Odd fields are the corresponding
## one-line sed script, in single quotes, that does the rewrite.  See below for examples.
## The first line is a special, to remove the "Build Status" variable icon widget.
HREF_REWRITE_LIST=(
    README.md '/!\[Build Status\](https:\/\/travis-ci.org/d'
    metron-deployment/Kerberos-manual-setup.md 's#(readme-images/ambari-storm-site-properties.png)#(../images/ambari-storm-site-properties.png)#g'
    metron-deployment/Kerberos-manual-setup.md 's#(readme-images/ambari-storm-site.png)#(../images/ambari-storm-site.png)#g'
    metron-deployment/Kerberos-manual-setup.md 's#(readme-images/custom-storm-site-final.png)#(../images/custom-storm-site-final.png)#g'
    metron-deployment/Kerberos-manual-setup.md 's#(readme-images/enable-kerberos-configure-kerberos.png)#(../images/enable-kerberos-configure-kerberos.png)#g'
    metron-deployment/Kerberos-manual-setup.md 's#(readme-images/enable-kerberos-started.png)#(../images/enable-kerberos-started.png)#g'
    metron-deployment/Kerberos-manual-setup.md 's#(readme-images/enable-kerberos.png)#(../images/enable-kerberos.png)#g'
    metron-deployment/Kerberos-ambari-setup.md 's#(readme-images/enable-kerberos-configure-kerberos.png)#(../images/enable-kerberos-configure-kerberos.png)#g'
    metron-deployment/Kerberos-ambari-setup.md 's#(readme-images/enable-kerberos-started.png)#(../images/enable-kerberos-started.png)#g'
    metron-deployment/Kerberos-ambari-setup.md 's#(readme-images/enable-kerberos.png)#(../images/enable-kerberos.png)#g'
    metron-platform/metron-enrichment/metron-enrichment-storm/README.md 's#(enrichment_arch.png)#(../../../images/enrichment_arch.png)#g'
    metron-platform/metron-enrichment/metron-enrichment-storm/README.md 's#(unified_enrichment_arch.svg)#(../../../images/unified_enrichment_arch.svg)#g'
    metron-platform/metron-indexing/README.md 's#(indexing_arch.png)#(../../images/indexing_arch.png)#g'
    metron-platform/metron-job/README.md 's#(metron-job_state_statechart_diagram.svg)#(../../images/metron-job_state_statechart_diagram.svg)#g'
    metron-platform/metron-parsing/README.md 's#(parser_arch.png)#(../../images/parser_arch.png)#g'
    metron-platform/metron-parsing/metron-parsers-common/ParserChaining.md 's#(../../../use-cases/parser_chaining/message_routing_high_level.svg)#(../../../images/message_routing_high_level.svg)#g'
    metron-analytics/metron-maas-service/README.md 's#(maas_arch.png)#(../../images/maas_arch.png)#g'
    metron-contrib/metron-performance/README.md 's#(performance_measurement.png)#(../../images/performance_measurement.png)#g'
    use-cases/forensic_clustering/README.md 's#(find_alerts.png)#(../../images/find_alerts.png)#g'
    use-cases/forensic_clustering/README.md 's#(clustered.png)#(../../images/clustered.png)#g'
    use-cases/parser_chaining/README.md 's#(message_routing_high_level.svg)#(../../images/message_routing_high_level.svg)#g'
    use-cases/parser_chaining/README.md 's#(aggregated_parser_chaining_flow.svg)#(../../images/aggregated_parser_chaining_flow.svg)#g'
    use-cases/typosquat_detection/README.md 's#(squid_search.png)#(../../images/squid_search.png)#g'
    use-cases/typosquat_detection/README.md 's#(drill_down.png)#(../../images/drill_down.png)#g'
)

TEMPLATES_DIR="$METRON_SOURCE/site-book/src/site/src-resources/templates"


######################
######################
# utility functions

# input: none
# output: traces, if enabled
TRACE_ENABLE=0
function trace () {
    if (( $TRACE_ENABLE == 1 )) ; then
        echo "$*"
    fi  # else do nothing
}
TREE_TRACE_ENABLE=0
function tree_trace () {
    if (( $TREE_TRACE_ENABLE == 1 )) ; then
        echo "$*"
    fi  # else do nothing
}

# file used for storing error messages during re-write routine
SCRATCH_ERR_FILE_NAME="$METRON_SOURCE/site-book/src/site/errout.dat"

# input: cumulative directory_path, indent_level
# output: items to site.xml, as lines of text
# This function is called recursively as we descend the directory tree
# The cum_dir_path must not have a terminal "/".
function descend () {
    tree_trace "enter decend( $@ )"
    local cum_dir_path
    local -i indent
    local open_item_exists
    cum_dir_path="$1"
    indent=$2

    if [ -e "${cum_dir_path}"/index.md ] ; then
        dir_name=`basename "$cum_dir_path"`
        dir_name="${dir_name#metron-}"  #remove the "metron-" prefix if present
        dir_name=`get_prettyname "$dir_name"`  #capitalize the remainder
        # Is it a leaf node?
        num_peers=`ls -d "${cum_dir_path}"/* |wc -l`
        if (( $num_peers == 1 )) ; then #yes, it's a leaf node, do a closed item
            echo "${INDENTS[$indent]}<item name='${dir_name}' href='${cum_dir_path}/index.html'/>" >> ../site.xml
            tree_trace "exit descend due to leaf node"
            return  #nothing else to process in this directory path
        fi  #otherwise carry on with open item and child items at deeper indent
        echo "${INDENTS[$indent]}<item name='${dir_name}' href='${cum_dir_path}/index.html' collapse='true'>" >> ../site.xml
        open_item_exists=1
        indent=$(( indent + 1 ))
    else
        open_item_exists=0
    fi
    for md in "${cum_dir_path}"/*.md ; do
        if [ ! -e "$md" ] ; then continue ; fi  #globbing sometimes gives spurious results
        item_name=`basename "$md"`
	item_name="${item_name%.md}"  #strip the extension
        if [ "$item_name" != "index" ] ; then
            echo "${INDENTS[$indent]}<item name='${item_name}' href='${cum_dir_path}/${item_name}.html'/>" >> ../site.xml
        fi
    done
    for dir in "${cum_dir_path}"/* ; do
        if [ ! -e "$dir" ] ; then continue ; fi  #globbing sometimes gives spurious results
        if [ -d "$dir" ] ; then
            descend "$dir" $indent
        fi
    done
    if (( open_item_exists == 1 )) ; then
        indent=$(( indent - 1 ))  #close the item
        echo "${INDENTS[$indent]}</item>" >> ../site.xml
    fi
    tree_trace "exit descend with indent = $indent"
}

# input: a file basename
# output: a "pretty" human label, on stdout for Command Substitution
# Currently just capitalize the first letter
# In future, might do CamelCase or subst hyphens to underscores
function get_prettyname () {
    echo "$(tr '[:lower:]' '[:upper:]' <<< ${1:0:1})${1:1}"
}

# This function, with the following traps, cleans up before exiting, if interrupted during the re-write routine
function sig_handle () {
    exitCode=${1:-0}
    rm -f "$SCRATCH_ERR_FILE_NAME"
    echo "ERROR: EARLY TERMINATION with error code $exitCode" ${2:+"due to $2"}
    exit $exitCode
}
trap 'sig_handle 129 SIGHUP'  SIGHUP
trap 'sig_handle 130 SIGINT'  SIGINT
trap 'sig_handle 143 SIGTERM' SIGTERM
trap 'sig_handle $? ERR'      ERR


######################
## Proceed

cd "$METRON_SOURCE"

# Validate that the src/site directory is writable for generated content
if [ ! -w "site-book/src/site" ]; then
    echo "ERROR: 'site-book/src/site' is not writable" > /dev/stderr
    exit 126
fi

# Clean up generated directories and files in src/site/
if [ -e "$METRON_SOURCE"/site-book/src/site/markdown ] ; then
    rm -rf "$METRON_SOURCE"/site-book/src/site/markdown ; fi
if [ -e "$METRON_SOURCE"/site-book/src/site/resources/images ] ; then
    rm -rf "$METRON_SOURCE"/site-book/src/site/resources/images ; fi
if [ -e "$METRON_SOURCE"/site-book/src/site/site.xml ] ; then
    rm -f "$METRON_SOURCE"/site-book/src/site/site.xml; fi
mkdir -p "$METRON_SOURCE"/site-book/src/site/markdown \
    "$METRON_SOURCE"/site-book/src/site/resources/images

# cons up the exclude exec string
cmd=""
for exclusion in "${EXCLUSION_LIST[@]}" ; do
    cmd="${cmd} | egrep -v '${exclusion}'"
done

# Capture the hierarchical list of .md files.
# Take them all, not just README.md files.
cmd="find . -name '*.md' -print ${cmd}"
echo " "
echo Collecting markdown files with exclusions: $cmd
echo " "
MD_FILE_LIST=( `eval $cmd` )

# Pipe the files into the src/site/markdown directory tree
tar cvf - "${MD_FILE_LIST[@]}" | ( cd "$METRON_SOURCE"/site-book/src/site/markdown; tar xf -  )

# Grab the other resources needed
echo " "
echo Collecting additional resource files:
for r in "${RESOURCE_LIST[@]}" site-book/src/site/src-resources/images/* ; do
    if [ ! -e "$r" ] ; then continue ; fi  #globbing sometimes gives spurious results
    echo ./"$r"
    cp "$r" "$METRON_SOURCE"/site-book/src/site/resources/images/
done
echo " "

cd site-book/src/site/markdown

# Rewrite hrefs for resource references, using table provided by Maintainers
for (( i=0; i<${#HREF_REWRITE_LIST[@]} ; i+=2 )) ; do
    echo rewriting href in "${HREF_REWRITE_LIST[$i]}" : "${HREF_REWRITE_LIST[ $(( i + 1 )) ]}"
    case "${OSTYPE}" in
        linux*)
            # Linux sed correctly parses lack of argument after -i option
            sed -i -e "${HREF_REWRITE_LIST[ $(( i + 1 )) ]}" "${HREF_REWRITE_LIST[$i]}"
            ;;
        darwin*)
            # MacOS sed needs an empty-string argument after -i option to get the same result
            sed -i '' -e "${HREF_REWRITE_LIST[ $(( i + 1 )) ]}" "${HREF_REWRITE_LIST[$i]}"
            ;;
        *)
            echo "ERROR: Unable to determine 'sed' argument list for OS ${OSTYPE}" > /dev/stderr
            exit 126
            ;;
    esac
done
echo " "

# Rename "README" files to "index" files, so they will be the default doc for a site sub-directory, just
# like README is the default doc for a github sub-directory.  This makes some internal links (to directories)
# work instead of being broken.
echo Renaming \"README\" files to \"index\" files.
if (( `ls -R |grep -c 'index.md'` > 0 )) ; then
    echo "ERROR: index.md file exists in tree already, we currently don't handle that"
    exit 1
fi
find . -name README.md -execdir mv README.md index.md \;
echo " "

# Insert the tree of generated html files in the LHS nav menu of the site.xml
# The problem is that we want a depth-first listing, with files before subdirectories, and "index" always first.
# And we synthesize the page labels in the nav tree from the directory paths.
# So the following logic is a little complex, but we avoid having to hardwire the tree structure.

BEGIN_TAG="BEGIN_MENU_TREE"
END_TAG="END_MENU_TREE"
INDENTS=( "" "  " "    " "      " "        " "          " "            " )

echo "Generating menu tree from directory tree structure"
echo " "

# Copy the first part of the file, up to where the menu tree goes.
sed -n -e "1,/${BEGIN_TAG}/ p" "$TEMPLATES_DIR"/site.xml.template > ../site.xml

# Now start inserting menu tree items
# top level of markdown tree is special
if [ -e index.md ] ; then
    echo "<item name='Metron' href='index.html' title='Apache Metron' collapse='false'>" >> ../site.xml
    item0_exists=1
else
    item0_exists=0
fi
indent_level=1
for md in *.md ; do
    if [ ! -e "$md" ] ; then continue ; fi  #globbing sometimes gives spurious results
    if [ "$md" != "index.md" ] ; then
        item_name="${md%.md}"  #strip the extension
        echo "${INDENTS[$indent_level]}<item name='${item_name}' href='${item_name}.html' />" >> ../site.xml
    fi
done
for dir in * ; do
    if [ ! -e "$dir" ] ; then continue ; fi  #globbing sometimes gives spurious results
    if [ -d "$dir" ] ; then
        descend "$dir" $indent_level
    fi
done
if (( item0_exists == 1 )) ; then
    echo "</item>" >> ../site.xml
fi

# Copy the last part of the file, from the end of the menu tree.
sed -n -e "/${END_TAG}/,"'$ p' "$TEMPLATES_DIR"/site.xml.template >> ../site.xml

echo "Done."
echo " "

echo "Fixing up markdown dialect problems between Github-MD and doxia-markdown:"
# Detecting errors from a `find -exec` command is difficult.  We do it using an intermediary file.
rm -f "$SCRATCH_ERR_FILE_NAME"
find . -name '*.md' -print -exec python "$METRON_SOURCE"/site-book/bin/fix-md-dialect.py '{}' \; 2> "$SCRATCH_ERR_FILE_NAME"
errlines=`wc -l "$SCRATCH_ERR_FILE_NAME"`
if (( ${errlines% *} > 0 )) ; then
    echo "ERROR OR ERRORS DETECTED:"
    cat "$SCRATCH_ERR_FILE_NAME"
    rm -f "$SCRATCH_ERR_FILE_NAME"
    exit 1
else
    rm -f "$SCRATCH_ERR_FILE_NAME"
    echo "Done."
    echo " "
    exit 0
fi
