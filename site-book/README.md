<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->
# Metron Site-Book documentation

Metron's Site Book is an attempt at producing documentation that is:

- Versioned and reviewed
- Tied to code versions
- Highly local to the code being documented

The idea is that a release manager would build the site-book (following the instructions below), then publish it from the public [Metron site](http://metron.apache.org/) as the docs for the new released version. Older site-book versions should remain available for users that need them.

The site-book is also part of the Maven site lifecycle, and will be included by the full site from the top level.  However, the site as a whole takes longer than just the site-book:

To build only the book, do the following:

In any git clone of metron containing the site-book subdirectory,

```
cd site-book
mvn site
```

It only takes a few seconds. You may now view your copy of the book in a browser by opening 

```
file:///your/path/to/metron/site-book/target/site/index.html
```

On a Mac, you can just type the following on the command line

```
open target/site/index.html
```


## Key Components:

### bin/generate-md.sh

- Copies all .md files from the code directory tree into the site tree
- Performs some transformations on them
- Generates the nav tree structure and labels
- Happens during the site:pre-site phase of Maven.

### bin/fix-md-dialect.py

- Called by 'generate-md.sh'
- Does transforms within the text of each file
    - Converts the Github-MD dialect of markdown into the doxia-markdown dialect

### pom.xml and src/site/site.xml

- [Doxia](https://maven.apache.org/doxia/) boilerplate, tweaked for our specific needs


