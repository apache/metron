/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.metron.bundles;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.SecureClassLoader;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.NameScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <p>
 * A <tt>ClassLoader</tt> for loading BUNDLES (plugin archives). BUNDLEs are designed to
 * allow isolating bundles of code (comprising one-or-more
 * plugin classes and their
 * dependencies) from other such bundles; this allows for dependencies and
 * processors that require conflicting, incompatible versions of the same
 * dependency to run in a single instance of a given process.</p>
 *
 * <p>
 * <tt>BundleClassLoader</tt> follows the delegation model described in
 * {@link ClassLoader#findClass(java.lang.String) ClassLoader.findClass(...)};
 * classes are first loaded from the parent <tt>ClassLoader</tt>, and only if
 * they cannot be found there does the <tt>BundleClassLoader</tt> provide a
 * definition. Specifically, this means that resources are loaded from the application's
 * <tt>conf</tt>
 * and <tt>lib</tt> directories first, and if they cannot be found there, are
 * loaded from the BUNDLE.</p>
 *
 * <p>
 * The packaging of a BUNDLE is such that it is a ZIP file with the following
 * directory structure:
 *
 * <pre>
 *   +META-INF/
 *   +-- bundled-dependencies/
 *   +-- &lt;JAR files&gt;
 *   +-- MANIFEST.MF
 * </pre>
 * </p>
 *
 * <p>
 * The MANIFEST.MF file contains the same information as a typical JAR file but
 * also includes two additional bundle properties: {@code Bundle-Id} and
 * {@code Bundle-Dependency-Id}.
 * </p>
 *
 * <p>
 * The {@code Bundle-Id} provides a unique identifier for this BUNDLE.
 * </p>
 *
 * <p>
 * The {@code Bundle-Dependency-Id} is optional. If provided, it indicates that
 * this BUNDLE should inherit all of the dependencies of the BUNDLE with the provided
 * ID. Often times, the BUNDLE that is depended upon is referred to as the Parent.
 * This is because its ClassLoader will be the parent ClassLoader of the
 * dependent BUNDLE.
 * </p>
 *
 * <p>
 * If a BUNDLE is built using the Bundles Maven Plugin, the {@code Bundle-Id} property
 * will be set to the artifactId of the BUNDLE. The {@code Bundle-Dependency-Id} will
 * be set to the artifactId of the BUNDLE that is depended upon. For example, if
 * BUNDLE A is defined as such:
 *
 * <pre>
 * ...
 * &lt;artifactId&gt;bundle-a&lt;/artifactId&gt;
 * &lt;packaging&gt;bundle&lt;/packaging&gt;
 * ...
 * &lt;dependencies&gt;
 *   &lt;dependency&gt;
 *     &lt;groupId&gt;group&lt;/groupId&gt;
 *     &lt;artifactId&gt;bundle-z&lt;/artifactId&gt;
 *     <b>&lt;type&gt;bundle&lt;/type&gt;</b>
 *   &lt;/dependency&gt;
 * &lt;/dependencies&gt;
 * </pre>
 * </p>
 *
 *
 * <p>
 * Then the MANIFEST.MF file that is created for Bundle A will have the following
 * properties set:
 * <ul>
 * <li>{@code {Foo}-Id: bundle-a}</li>
 * <li>{@code {Foo}-Dependency-Id: bundle-z}</li>
 * </ul>
 * Where is configurable by BundleProperty META_ID_PREFIX [ default Bundle ]
 * </p>
 *
 * <p>
 * Note, above, that the {@code type} of the dependency is set to {@code foo}.
 * </p>
 *
 * <p>
 * If the Bundle has more than one dependency of {@code type} {@code Foo}, then the
 * Maven Bundle plugin will fail to build the Bundle.
 * </p>
 *
 * This classloader is Metron Bundle aware, in that it understands that the passed in root
 * FileObjects may be Bundles,
 * This class is adapted from Apache Commons VFS
 * VFSClassLoader class v.2.1
 * And the Apache Nifi NarClassLoader v. 1.2
 *
 * @see FileSystemManager#createFileSystem
 */
public class VFSBundleClassLoader extends SecureClassLoader {
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static class Builder {

    private FileSystemManager fileSystemManager;
    private FileObject bundleFile;
    private ClassLoader parentClassLoader;

    public VFSBundleClassLoader.Builder withFileSystemManager(FileSystemManager fileSystemManager) {
      this.fileSystemManager = fileSystemManager;
      return this;
    }

    public VFSBundleClassLoader.Builder withBundleFile(FileObject bundleFile) {
      this.bundleFile = bundleFile;
      return this;
    }

    public VFSBundleClassLoader.Builder withParentClassloader(ClassLoader parentClassloader) {
      this.parentClassLoader = parentClassloader;
      return this;
    }

    public VFSBundleClassLoader build() throws FileSystemException {
      return new VFSBundleClassLoader(bundleFile, fileSystemManager, parentClassLoader);
    }
  }

  private final ArrayList<FileObject> resources = new ArrayList<FileObject>();
  private FileObject nativeDir;
  public static final String DEPENDENCY_PATH = "META-INF/bundled-dependencies";
  /**
   * Constructs a new VFSClassLoader for the given Bundle file.
   *
   * @param file the file to load the classes and resources from.
   * @param manager the FileManager to use when trying create a layered Jar file system.
   * @throws FileSystemException if an error occurs.
   */
  public VFSBundleClassLoader(final FileObject file,
      final FileSystemManager manager)
      throws FileSystemException {
    this(new FileObject[]{file}, manager, null);
  }

  /**
   * Constructs a new VFSClassLoader for the given file.
   *
   * @param file the Bundle FileObject to load the classes and resources from.
   * @param manager the FileManager to use when trying create a layered Jar file system.
   * @param parent the parent class loader for delegation.
   * @throws FileSystemException if an error occurs.
   */
  public VFSBundleClassLoader(final FileObject file,
      final FileSystemManager manager,
      final ClassLoader parent)
      throws FileSystemException {
    this(new FileObject[]{file}, manager, parent);
  }

  /**
   * Constructs a new VFSClassLoader for the given files.  The files will be searched in the order
   * specified.
   *
   * @param files the Bundle FileObjects to load the classes and resources from.
   * @param manager the FileManager to use when trying create a layered Jar file system.
   * @throws FileSystemException if an error occurs.
   */
  public VFSBundleClassLoader(final FileObject[] files,
      final FileSystemManager manager)
      throws FileSystemException {
    this(files, manager, null);
  }

  /**
   * Constructs a new VFSClassLoader for the given FileObjects. The FileObjects will be searched
   * in the order specified.
   *
   * @param files the Bundle FileObjects to load the classes and resources from.
   * @param manager the FileManager to use when trying create a layered Jar file system.
   * @param parent the parent class loader for delegation.
   * @throws FileSystemException if an error occurs.
   */
  public VFSBundleClassLoader(final FileObject[] files,
      final FileSystemManager manager,
      final ClassLoader parent) throws FileSystemException {
    super(parent);
    addFileObjects(manager, files);
  }

  /**
   * Provide access to the file objects this class loader represents.
   *
   * @return An array of FileObjects.
   * @since 2.0
   */
  public FileObject[] getFileObjects() {
    return resources.toArray(new FileObject[resources.size()]);
  }

  /**
   * Appends the specified FileObjects to the list of FileObjects to search for classes and
   * resources.  If the FileObjects represent Bundles, then the Bundle dependencies will also
   * be added as resources to the classloader.
   *
   * This is the equivelent of unzipping the Bundle and adding each jar in the dependency directory
   * by uri.  The ability of VFS to create filesystems from jar files, allows the VFSBundleClassLoader
   * to create a composite filesystem out of the Bundle zip.
   *
   * @param manager The FileSystemManager.
   * @param files the FileObjects to append to the search path.
   * @throws FileSystemException if an error occurs.
   */
  private void addFileObjects(final FileSystemManager manager,
      final FileObject[] files) throws FileSystemException {
    for (FileObject file : files) {
      if (!file.exists()) {
        // Does not exist - skip
        continue;
      }

      if (manager.canCreateFileSystem(file)) {
        // create a Jar filesystem from the bundle
        FileObject bundleFile = manager.createFileSystem(file);

        // resolve the dependency directory within the bundle
        FileObject deps = bundleFile.resolveFile(DEPENDENCY_PATH);
        if(deps.exists() && deps.isFolder()) {
          nativeDir = deps.resolveFile("native");
          FileObject[] depJars = deps.getChildren();
          for (FileObject jarFileObject : depJars) {
            // create a filesystem from each jar and add it as
            // a resource
            jarFileObject = manager.createFileSystem(jarFileObject);
            resources.add(jarFileObject);
          }
        }
      } else {
        continue;
      }
      resources.add(file);
    }
  }

  @Override
  protected String findLibrary(final String libname) {
    try {
      final FileObject libsoFile = nativeDir.resolveFile( "lib" + libname + ".so");
      final FileObject dllFile = nativeDir.resolveFile(libname + ".dll");
      final FileObject soFile = nativeDir.resolveFile(libname + ".so");
      if (libsoFile.exists()) {
        return libsoFile.getURL().toString();
      } else if (dllFile.exists()) {
        return dllFile.getURL().toString();
      } else if (soFile.exists()) {
        return soFile.getURL().toString();
      }
    }catch(FileSystemException fse){
      LOGGER.error("Failed to get dependencies",fse);
    }
    // not found in the bundle. try system native dir
    return null;
  }


  /**
   * Finds and loads the class with the specified name from the search path.
   *
   * @throws ClassNotFoundException if the class is not found.
   */
  @Override
  protected Class<?> findClass(final String name) throws ClassNotFoundException {
    try {
      final String path = name.replace('.', '/').concat(".class");
      final VFSBundleClassLoaderResource res = loadResource(path);
      if (res == null) {
        throw new ClassNotFoundException(name);
      }
      return defineClass(name, res);
    } catch (final IOException ioe) {
      throw new ClassNotFoundException(name, ioe);
    }
  }

  /**
   * Loads and verifies the class with name and located with res.
   */
  private Class<?> defineClass(final String name, final VFSBundleClassLoaderResource res)
      throws IOException {
    final URL url = res.getCodeSourceURL();
    final String pkgName = res.getPackageName();
    if (pkgName != null) {
      final Package pkg = getPackage(pkgName);
      if (pkg != null) {
        if (pkg.isSealed()) {
          if (!pkg.isSealed(url)) {
            throw new FileSystemException("vfs.impl/pkg-sealed-other-url", pkgName);
          }
        } else {
          if (isSealed(res)) {
            throw new FileSystemException("vfs.impl/pkg-sealing-unsealed", pkgName);
          }
        }
      } else {
        definePackage(pkgName, res);
      }
    }

    final byte[] bytes = res.getBytes();
    final Certificate[] certs =
        res.getFileObject().getContent().getCertificates();
    final CodeSource cs = new CodeSource(url, certs);
    return defineClass(name, bytes, 0, bytes.length, cs);
  }

  /**
   * Returns true if the we should seal the package where res resides.
   */
  private boolean isSealed(final VFSBundleClassLoaderResource res)
      throws FileSystemException {
    final String sealed = res.getPackageAttribute(Attributes.Name.SEALED);
    return "true".equalsIgnoreCase(sealed);
  }

  /**
   * Reads attributes for the package and defines it.
   */
  private Package definePackage(final String name,
      final VFSBundleClassLoaderResource res)
      throws FileSystemException {
    // TODO - check for MANIFEST_ATTRIBUTES capability first
    final String specTitle = res.getPackageAttribute(Name.SPECIFICATION_TITLE);
    final String specVendor = res.getPackageAttribute(Attributes.Name.SPECIFICATION_VENDOR);
    final String specVersion = res.getPackageAttribute(Name.SPECIFICATION_VERSION);
    final String implTitle = res.getPackageAttribute(Name.IMPLEMENTATION_TITLE);
    final String implVendor = res.getPackageAttribute(Name.IMPLEMENTATION_VENDOR);
    final String implVersion = res.getPackageAttribute(Name.IMPLEMENTATION_VERSION);

    final URL sealBase;
    if (isSealed(res)) {
      sealBase = res.getCodeSourceURL();
    } else {
      sealBase = null;
    }

    return definePackage(name, specTitle, specVersion, specVendor,
        implTitle, implVersion, implVendor, sealBase);
  }

  /**
   * Calls super.getPermissions both for the code source and also adds the permissions granted to
   * the parent layers.
   *
   * @param cs the CodeSource.
   * @return The PermissionCollections.
   */
  @Override
  protected PermissionCollection getPermissions(final CodeSource cs) {
    try {
      final String url = cs.getLocation().toString();
      final FileObject file = lookupFileObject(url);
      if (file == null) {
        return super.getPermissions(cs);
      }

      final FileObject parentLayer = file.getFileSystem().getParentLayer();
      if (parentLayer == null) {
        return super.getPermissions(cs);
      }

      final Permissions combi = new Permissions();
      PermissionCollection permCollect = super.getPermissions(cs);
      copyPermissions(permCollect, combi);

      for (FileObject parent = parentLayer;
          parent != null;
          parent = parent.getFileSystem().getParentLayer()) {
        final CodeSource parentcs =
            new CodeSource(parent.getURL(),
                parent.getContent().getCertificates());
        permCollect = super.getPermissions(parentcs);
        copyPermissions(permCollect, combi);
      }

      return combi;
    } catch (final FileSystemException fse) {
      throw new SecurityException(fse.getMessage());
    }
  }

  /**
   * Copies the permissions from src to dest.
   *
   * @param src The source PermissionCollection.
   * @param dest The destination PermissionCollection.
   */
  protected void copyPermissions(final PermissionCollection src,
      final PermissionCollection dest) {
    for (final Enumeration<Permission> elem = src.elements(); elem.hasMoreElements(); ) {
      final Permission permission = elem.nextElement();
      dest.add(permission);
    }
  }

  /**
   * Does a reverse lookup to find the FileObject when we only have the URL.
   */
  private FileObject lookupFileObject(final String name) {
    final Iterator<FileObject> it = resources.iterator();
    while (it.hasNext()) {
      final FileObject object = it.next();
      if (name.equals(object.getName().getURI())) {
        return object;
      }
    }
    return null;
  }

  /**
   * Finds the resource with the specified name from the search path. This returns null if the
   * resource is not found.
   *
   * @param name The resource name.
   * @return The URL that matches the resource.
   */
  @Override
  protected URL findResource(final String name) {
    try {
      final VFSBundleClassLoaderResource res = loadResource(name);
      if (res != null) {
        return res.getURL();
      }
      return null;
    } catch (final Exception ignored) {
      return null; // TODO: report?
    }
  }

  /**
   * Returns an Enumeration of all the resources in the search path with the specified name. <p>
   * Gets called from {@link ClassLoader#getResources(String)} after parent class loader was
   * questioned.
   *
   * @param name The resources to find.
   * @return An Enumeration of the resources associated with the name.
   * @throws FileSystemException if an error occurs.
   */
  @Override
  protected Enumeration<URL> findResources(final String name) throws IOException {
    final List<URL> result = new ArrayList<URL>(2);

    for (FileObject baseFile : resources) {
      final FileObject file = baseFile.resolveFile(name, NameScope.DESCENDENT_OR_SELF);
      if (file.exists()) {
        result.add(new VFSBundleClassLoaderResource(name, baseFile, file).getURL());
      }
    }

    return Collections.enumeration(result);
  }

  /**
   * Searches through the search path of for the first class or resource with specified name.
   *
   * @param name The resource to load.
   * @return The Resource.
   * @throws FileSystemException if an error occurs.
   */
  private VFSBundleClassLoaderResource loadResource(final String name) throws FileSystemException {
    for (final FileObject baseFile : resources) {
      final FileObject file = baseFile.resolveFile(name, NameScope.DESCENDENT_OR_SELF);
      if (file.exists()) {
        return new VFSBundleClassLoaderResource(name, baseFile, file);
      }
    }
    return null;
  }
}

