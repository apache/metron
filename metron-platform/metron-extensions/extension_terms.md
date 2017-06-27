# Metron Extension Terms


### Metron Extension
> Additional functionality for the Metron system
> This functionality can be defined as code libraries and dependencies and configuration, or possibly configuration alone
> There are different types of extensions, each with it's own specific composition
> Extensions are package as an [Extension Package](#extension-package)
 
### Extension Package
> The compressed file that contains the configurations and code libraries and dependencies of the extension
> The Extension Library and dependencies are themselves packaged as a [Metron Bundle](../../bundles-lib)
> Extension Archetypes produce projects with an assembly module that will create an extension package
> Extension packages can be installed/uninstalled into Metron through the REST-API (NOTE: METRON-942)
