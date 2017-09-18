# Metron Extensions

[Metron Extensions](Terms.md#metron-extensions) allow the Metron system to be extended with new capabilities and support for new types of data.
Extensions may be new telemetry parsers or libraries of Stellar functions (Coming Soon).

Providing an extension mechanism, which includes maven archetype support, installation and loading is important not only for allowing new capabilities to be added to Metron,
but for allowing those capabilities to be developed and maintained outside the Metron code tree, without the need or rather the requiement to fork the project in order to integrate changes.

The may be multiple types of [Metron Extensions](Terms.md#metron-extensions), and each extension type will define it's own specific structure
for packaging, and have it's own deployment and installation details.

The important idea in [extension packaging](Packaging.md) is that a parser extension should be able to be deployed as a single
self contained unit.

## Exposing functionality

[Metron Extensions](Terms.md#metron-extensions) expose functionality through the [ClassIndex Library](https://github.com/atteo/classindex).
The default dependency on ClassIndex produced by the Metron Extension archetypes ensures that any classes in an
extension bundle that derive from an interface or base class in the system are exposed automatically at compile time
without the need to manual expose provided service interfaces ( such as you would have to do with ServiceLoader implementations).

As an example: Parsers.  Metron parsers implement the MessageParser interface.

The MessageParser is defined as such:

```
@IndexSubclasses
public interface MessageParser<T> extends Configurable

```
Therefore any class in a parser extension that implements this interface will therefore be exposed to the extension host/consumer.
Also, there may be more than one extension type exposed with in a given extension.  This means, for example, that a parser extension may include
more than one parser classes, each with it's own configurations but sharing a bundle. See [Parser Extension Packaging](metron-parser-extensions/Packaging.md).


## Metron System Extensions

### Parsers

The parsers that are developed and deployed with Metron are developed as [Metron Extensions](Terms.md#metron-extensions) as well.
See [Adding a new Parser to Metron System](metron-parser-extensions/Developing.md) for a guide to adding a new parser to be built and deployed as part of Metron itself.



[Extension Terms](Terms.md)

