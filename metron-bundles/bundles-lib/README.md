# Apache Metron Bundles

> Apache Metron Bundles and this documentation are a derivative of the [Apache Nifi](https://nifi.apache.org) [NARs](https://nifi.apache.org/developer-guide.html).

When software from many different organizations is all hosted within
the same environment, Java ClassLoaders quickly
become a concern. If multiple components have a dependency on the same
library but each depends on a different
version, many problems arise, typically resulting in unexpected
behavior or `NoClassDefFoundError` errors occurring.
In order to prevent these issues from becoming problematic, Apache NiFi
introduces the notion of a NiFi Archive, or NAR.  The Apache Metron project has adapted and extended the NAR system as
Apache Metron Bundles.

A BUNDLE allows several components and their dependencies to be packaged
together into a single package.
The BUNDLE package is then provided ClassLoader isolation from other BUNDLES
packages. Developers should always deploy their Apache Metron Extensions as BUNDLE packages.

To achieve this, a developer creates a new Maven Artifact, which we
refer to as the BUNDLE artifact. The packaging is
set to `bundle`. The `dependencies` section of the POM is then created so
that the BUNDLE has a dependency on all Extension Components that are to be included within the BUNDLE.

In order to use a packaging of `bundle`, we must use the `bundles-maven-plugin` module.
This is included by adding the following snippet to the bundle's pom.xml:> 
> 0.4.1 below should be the current metron version
```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.metron</groupId>
            <artifactId>bundles-maven-plugin</artifactId>
            <version>0.4.1</version>
            <extensions>true</extensions>
        </plugin>
    </plugins>
</build>
```

> The `bundles-maven-plugin` may need to be built and installed.  From the Metron src root directory:
```bash
$cd bundles-maven-plugin && mvn -q install && cd ..
```

The bundles-maven-plugin is included in the projects created by Apache Metron Extension maven archetypes.


The BUNDLE is able to have one dependency that is of type `bundle`. If more
than one dependency is specified that is of type
`bundle`, then the bundles-maven-plugin will error. If BUNDLE A adds a
dependency on BUNDLE B, this will *not* result in
BUNDLE A packaging all of the components of BUNDLE B. Rather, this will add
a `Bundle-Dependency-Id` element to the `MANIFEST.MF`
file of BUNDLE A. This will result in setting the ClassLoader of BUNDLE B as
the Parent ClassLoader of BUNDLE A. In this case,
we refer to BUNDLE B as the _Parent_ of BUNDLE A.

## Exposing Classes

Bundles expose classes for loading via the Java Service Provider jar mechanism.  That is to say they
are listed in the jar's META-INF/services/$INTERFACENAME file.

The [ClassIndex library](https://github.com/atteo/classindex) is used to discover these registrations, and is recommened for use in creating them.

For example.  An interface in ModuleA would be attributed with `@IndexSubclasses` as such:

```java

import org.atteo.classindex.IndexSubclasses;

@IndexSubclasses
public interface TestInterface {...}
```

In ModuleB, an implementation class simply implements `TestInterface` and includes a dependency on ClassIndex as such:

```xml
 <dependency>
    <groupId>org.atteo.classindex</groupId>
    <artifactId>classindex</artifactId>
    <version>${global_classindex_version}</version>
    <scope>provided</scope>
 </dependency>
```
When ModuleB is packaged, the jar produced will have the services entry automatically created.
Then ModuleB is bundled using the bundles-maven-plugin.

This exposes the implementation, and allows for it's discovery by the bundles-lib system.

## Per-Instance ClassLoading

The bundles-lib provides the `@RequiresInstanceClassLoading` annotation to further expand and isolate the libraries
available on a component’s classpath. You can annotate a extension class with `@RequiresInstanceClassLoading`
to indicate that the instance ClassLoader for the component requires a copy of all the resources in the
component's BUNDLE ClassLoader. When `@RequiresInstanceClassLoading` is not present, the
instance ClassLoader simply has it's parent ClassLoader set to the BUNDLE ClassLoader, rather than
copying resources.

Because @RequiresInstanceClassLoading copies resources from the BUNDLE ClassLoader for each instance of the
extension, use this capability judiciously in an environment where many extensions may be present. If ten instances of one extension are created, all classes
from the component's BUNDLE ClassLoader are loaded into memory ten times. This could eventually increase the
memory footprint significantly when enough instances of the component are created.


## Apache VFS 

The bundles-lib utilizes the Apache VFS library for loading bundles.  Bundles, which are zip files of a known structure, containing 
jars of dependencies may be loaded by VFS as File Systems *themselves*, and in tern each jar in the bundle can be loaded by VFS as a File System.

The VFSBundleClassloader therefore loads the bundle as a Jar File System, and also each dependency jar as a filesystem.  This allows
for the complete loading of the bundle without having to unpack the bundle into a working directory. 

This is significantly different from the original Nifi implementation.

## BundleSystem

The BundleSystem class provides a useful and simple interface for using Bundles and instantiated class instances.
While the raw classes may be used, in many cases the BundleSystem will be sufficient and reduce complexity.

Without BundleSystem, the minimum required to instantiate a class (simplified):

```java
public static Optional<MessageParser<JSONObject>> loadParser(Map stormConfig, CuratorFramework client, SensorParserConfig parserConfig){
    MessageParser<JSONObject> parser = null;
    try {
      // fetch the BundleProperties from zookeeper
      Optional<BundleProperties> bundleProperties = getBundleProperties(client);
      
      // create the FileSystemManager
      FileSystemManager fileSystemManager = FileSystemManagerFactory.createFileSystemManager(new String[] {props.getArchiveExtension()});
      
      // ADD in the classes we are going to support for plugins
      ArrayList<Class> classes = new ArrayList<>();
      for( Map.Entry<String,String> entry : props.getBundleExtensionTypes().entrySet()){
        classes.add(Class.forName(entry.getValue()));
      }

      // create the SystemBundle ( the bundle for the types that may be in the system classloader already )
      Bundle systemBundle = ExtensionManager.createSystemBundle(fileSystemManager, props);
     
      // get the correct bundle library directories from properties 
      List<URI> libDirs = props.getBundleLibraryDirectories();
      List<FileObject> libFileObjects = new ArrayList<>();
      for(URI libUri : libDirs){
        FileObject fileObject = fileSystemManager.resolveFile(libUri);
        if(fileObject.exists()){
          libFileObjects.add(fileObject);
        }
      }
      
      // Initialize everything
      BundleClassLoaders.getInstance().init(fileSystemManager, libFileObjects, props);
      ExtensionManager.getInstance().init(classes, systemBundle, BundleClassLoaders.getInstance().getBundles());

      // everything is ready, create our instance
      parser = BundleThreadContextClassLoader.createInstance(parserConfig.getParserClassName(),MessageParser.class,props);

    }catch(Exception e){
      LOG.error("Failed to load parser " + parserConfig.getParserClassName(),e);
      return Optional.empty();
    }
    return Optional.of(parser);
  }

```

This is a lot to do, and a lot of unnecessary boilerplate code

Using the BundleSystem class however:

```java
public static Optional<MessageParser<JSONObject>> loadParser(Map stormConfig,
    CuratorFramework client, SensorParserConfig parserConfig) {
  MessageParser<JSONObject> parser = null;
  try {
    // fetch the BundleProperties from zookeeper
    Optional<BundleProperties> bundleProperties = getBundleProperties(client);
   BundleProperties props = bundleProperties.get();
        BundleSystem bundleSystem = new BundleSystemBuilder().withBundleProperties(props).build();
        parser = bundleSystem
            .createInstance(parserConfig.getParserClassName(), MessageParser.class);
    } else {
      LOG.error("BundleProperties are missing!");
    }
  } catch (Exception e) {
    LOG.error("Failed to load parser " + parserConfig.getParserClassName(), e);
    return Optional.empty();
  }
  return Optional.of(parser);
}

```

As we can see, this is much easier.

With the BundleSystem, you may the defaults by calling the builder with :

- withSystemBundle
- withFileSystemManager
- withExtensionClasses

BundleSystem also also supports delay loading of the BundleSystem.  This means that a BundleSystem
instance will be created, but the bundles will not be loaded or initialized until used.

This is specified with the builder by specifing BundleSystemType:

```java
 new BundleSystemBuilder().withBundleProperties(properties.get()).withBundleSystemType(
        BundleSystemType.ON_DEMAND).build();
```
 

