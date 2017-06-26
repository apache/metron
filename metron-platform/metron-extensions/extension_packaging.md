# Metron Extension Packaging



Extensions are packaged as a .tar.gz composed of its:

- Configurations
- Patterns
- Bundle


## Configurations

The extension's configurations may vary by type, but would include:

- Configurations to be loaded into Zookeeper
- [TBD] Log rotation scripts
- [TBD] Elasticsearch tempates

## Patterns

These may be Grok patterns used by a parser

## Bundle

Bundles are the runtime components of an extension.

A bundle is jar package that includes the extension jar file, and all of it's dependency jars ( that are not provided by the system).
It also includes some metadata about the extension.

Bundles are deployed into the system in HDFS, and are unpacked and loaded into applications that use extensions.
Each bundle is loaded with it's own classloader, which will allow for isolation of dependencies between extensions (for things not loaded by
by the host application itself).