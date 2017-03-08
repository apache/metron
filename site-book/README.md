# Metron Site-Book documentation

Metron's Site Book is an attempt at producing documentation that is:

- Versioned and reviewed
- Tied to code versions
- Highly local to the code being documented

The idea is that a release manager would build the site-book (following the instructions below), then publish it from the public [Metron site](http://metron.incubator.apache.org/) as the docs for the new released version. Older site-book versions should remain available for users that need them.


To build the book, do the following:

In any git clone of incubator-metron containing the site-book subdirectory,

```
cd site-book
bin/generate-md.sh
mvn site:site
```

It only takes a few seconds. You may now view your copy of the book in a browser by opening 

```
file:///your/path/to/incubator-metron/site-book/target/site/index.html
```

On a Mac, you can just type the following on the command line

```
open target/site/index.html
```

##Key Components:

###bin/generate-md.sh

- Copies all .md files from the code directory tree into the site tree
- Performs some transformations on them
- Generates the nav tree structure and labels

###bin/fix-md-dialect.py

- Called by 'generate-md.sh'
- Does transforms within the text of each file
    - Converts the Github-MD dialect of markdown into the doxia-markdown dialect

###pom.xml and src/site/site.xml

- [Doxia](https://maven.apache.org/doxia/) boilerplate, tweaked for our specific needs


