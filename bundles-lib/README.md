# Apache Metron Bundles

Apache Metron Bundles and this documentation are a derivitave of the [Apache Nifi](http://www.nifi.apache.org) [NARs](http://nifi.apache.org/developer-guide.html).

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
This is included by adding the following snippet to the bundle's pom.xml:


```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.metron</groupId>
            <artifactId>bundles-maven-plugin</artifactId>
            <version>0.4.0</version>
            <extensions>true</extensions>
        </plugin>
    </plugins>
</build>
```

The bundles-maven-plugin is included in the projects created by Apache Metron Extension maven archetypes.


The BUNDLE is able to have one dependency that is of type `bundle`. If more
than one dependency is specified that is of type
`bundle`, then the bundles-maven-plugin will error. If BUNDLE A adds a
dependency on BUNDLE B, this will *not* result in
BUNDLE B packaging all of the components of BUNDLE A. Rather, this will add
a `Bundle-Dependency-Id` element to the `MANIFEST.MF`
file of BUNDLE A. This will result in setting the ClassLoader of BUNDLE B as
the Parent ClassLoader of BUNDLE A. In this case,
we refer to BUNDLE B as the _Parent_ of BUNDLE A.

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



