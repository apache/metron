# Site Book Metron documentation 

Metron's Site Book is an attempt at producing documentation that is

-Versioned and reviewed,
- Only committers can edit
- Tied to code versions
- Highly local to the code being documented

Currently it is in a stand-alone, versioned-with-the-code sub-directory and sub-project. The idea is that a release
manager would build the site-book (following the instructions below), then copy it into a versioned subdirectory of the (unversioned) public site, to publish it along with each code release.

To build the book, do the following:
In any git clone of incubator-metron containing the site-book subdirectory,

    cd site-book
    bin/generate-md.sh
    mvn site:site

It only takes a few seconds. You may now view your copy of the book in a browser by opening 

    file:///your/path/to/incubator-metron/site-book/target/site/index.html. 

On a Mac, you can just type the following on the command line

    open target/site/index.html

The key files under site-book/ are:

###bin/generate-md.sh

- copies all .md files from the code directory tree into the site tree, performs some transformations on them, and generates the nav tree structure and labels.

###bin/fix-md-dialect.awk

- is called by 'generate-md.sh'. It does transforms within the text of each file, related to converting the Github-MD dialect of markdown into the doxia-markdown dialect.

###pom.xml and src/site/site.xml

- doxia boilerplate, tweaked for our specific needs. 


