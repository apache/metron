# Metron Extension Terms


### [Metron Extension](../metron-extensions)
> Additional functionality for the Metron system
> This functionality can be defined as code libraries and dependencies and configuration, or possibly configuration alone
> There are different types of extensions, each with it's own specific composition
> Extensions are package as an [Extension Package](#extension-package)
 
### [Extension Package](extension_packaging.md)
> The compressed file that contains the configurations and code libraries and dependencies of the extension
> The Extension Library and dependencies are themselves packaged as a [Metron Bundle](../../bundles-lib)
> Extension Archetypes produce projects with an assembly module that will create an extension package
> Extension packages can be installed/uninstalled into Metron through the REST-API (NOTE: METRON-942)

### [Bundle](../../bundles-lib)

> Bundles are the runtime components of an extension.
> 
> A bundle is jar package that includes the extension jar file, and all of it's dependency jars ( that are not provided by the system).
> It also includes some metadata about the extension.
> 
> Bundles are deployed into the system in HDFS, and are unpacked and loaded into applications that use extensions.
> Each bundle is loaded with it's own classloader, which will allow for isolation of dependencies between extensions (for things not loaded by
> by the host application itself).
