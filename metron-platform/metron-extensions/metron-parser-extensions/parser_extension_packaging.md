# Metron Parser Extension Packaging

Parser Extensions packages should contain all the executable code and configurations required for one or more parsers.

## The Package

The package itself is a tar.gz file created at build time.  The configuration for this packaging
in the project ( as produced by the [Metron Maven Parser Extension Archetype](../../../metron-maven-archetypes/metron-parser-extension-archetype)) by the
XXXX-assembly module, specifically in the src/main/assembly/assembly.xml file.

## The Package Contents

### config
The config directory includes the json configurations for the parsers, enrichment, and indexing.  Of these, the enrichment configuration
is optional, as some parsers will not provide default enrichment.

The directory may also include default configurations for elasticsearch and solr.  

> While this provides the means for managing and versioning these configurations, they are not used
> in the deployment of the extensions at this time.  In the future, these will be the default configurations
> deployed during installation or instantiation of a parser

### lib
The lib directory contains the [Metron Bundle](../../../bundles-lib), which itself contains the jars and dependencies 
for the one or more parsers within the extension.  The lib directory may not be present.  It is possible to define a parser extension
solely by configuration.  For an example of this, see the [Metron Yaf Parser Extension](metron-parser-yaf-extension/metron-parser-yaf).

### patterns
Many Metron Parsers are based on the [GROK](https://github.com/thekrakken/java-grok) log parsing library, and may include rules.  The rules for the one or more parsers 
within the extension are in this directory.  This directory may not be present if the parser does not use GROK rules.

## Example: The Metron ASA Parser Extension

metron-parser-asa-assembly-0.4.0-archive.tar.gz

```

├── config
│   └── zookeeper
│       ├── enrichments
│       │   └── asa.json
│       ├── indexing
│       │   └── asa.json
│       └── parsers
│           └── asa.json
├── lib
│   └── metron-parser-asa-bundle-0.4.0.bundle
└── patterns
    ├── asa
    └── common
```

### Multiple Parsers in one extension
Parser extensions support packaging multiple parsers in one extension.  This could be multiple configurations of a single 
parser class, or multiple configurations of multiple parser classes

for example:

```
├── config
│   └── zookeeper
│       ├── enrichments
│       │   └── nice.json
│       │   └── nice_other_config.json
│       │   └── amazing.json
│       │   └── amazing_other_config.json
│       ├── indexing
│       │   └── nice.json
│       │   └── nice_other_config.json
│       │   └── amazing.json
│       │   └── amazing_other_config.json
│       └── parsers
│       │   └── nice.json
│       │   └── nice_other_config.json
│       │   └── amazing.json
│       │   └── amazing_other_config.json
├── lib
│   └── metron-parser-amazinglynice-bundle-0.4.0.bundle
└── patterns
    ├── ancommon
    ├── nice
    ├── amazing 
    └── common
```