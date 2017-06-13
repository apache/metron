# Metron Extensions

Metron Extensions allow the Metron system to be extended with new capabilities and support for new types of data.
Extensions may be new telemetry parsers or libraries of Stellar functions (Coming Soon)

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


## Exposing functionality

Metron Extensions expose functionality through the [ClassIndex Library](https://github.com/atteo/classindex).
The default dependency on ClassIndex produced by the Metron Extension archetypes ensures that any classes in an
extension bundle that derive from an interface or base class in the system are exposed automatically at compile time
without the need to manual expose provided service interfaces ( such as you would have to do with ServiceLoader implementations).

As an example: Parsers.

The MessageParser is defined as such:
```
@IndexSubclasses
public interface MessageParser<T> extends Configurable
```
Any class in a parser extension that implements this interface will therefore be exposed to the extension host/consumer.
