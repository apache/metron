/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;
import org.apache.maven.shared.dependency.tree.traversal.DependencyNodeVisitor;
import org.eclipse.aether.RepositorySystemSession;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Generates the listing of dependencies that is provided by the Bundle dependency of the current Bundle. This is important as artifacts that bundle dependencies will
 * not project those dependences using the traditional maven dependency plugin. This plugin will override that setting in order to print the dependencies being
 * inherited at runtime.
 */
@Mojo(name = "provided-bundle-dependencies", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = false, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class BundleProvidedDependenciesMojo extends AbstractMojo {

    /**
     * The Maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * The local artifact repository.
     */
    @Parameter(defaultValue = "${localRepository}", readonly = true)
    private ArtifactRepository localRepository;

    /**
     * The {@link RepositorySystemSession} used for obtaining the local and remote artifact repositories.
     */
    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repoSession;

    /**
     * If specified, this parameter will cause the dependency tree to be written using the specified format. Currently supported format are: <code>tree</code>
     * or <code>pom</code>.
     */
    @Parameter(property = "mode", defaultValue = "tree")
    private String mode;

    /**
     * The type we are using for dependencies, should be nar, but may
     * be changed in the configuration if the plugin is producing
     * other archive extensions, this is a 'shared' configuration
     * with the NarMojo
     */
    @Parameter(property = "type", required = false, defaultValue = "bundle")
    protected String type;

    /**
     * The dependency tree builder to use for verbose output.
     */
    @Component
    private DependencyTreeBuilder dependencyTreeBuilder;

    /**
     * *
     * The {@link ArtifactHandlerManager} into which any extension {@link ArtifactHandler} instances should have been injected when the extensions were loaded.
     */
    @Component
    private ArtifactHandlerManager artifactHandlerManager;

    /**
     * The {@link ProjectBuilder} used to generate the {@link MavenProject} for the nar artifact the dependency tree is being generated for.
     */
    @Component
    private ProjectBuilder projectBuilder;

    /*
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            // find the bundle dependency
            Artifact bundleArtifact = null;
            for (final Artifact artifact : project.getDependencyArtifacts()) {
                if (type.equals(artifact.getType())) {
                    // ensure the project doesn't have two bundle dependencies
                    if (bundleArtifact != null) {
                        throw new MojoExecutionException("Project can only have one NAR dependency.");
                    }

                    // record the bundle dependency
                    bundleArtifact = artifact;
                }
            }

            // ensure there is a bundle dependency
            if (bundleArtifact == null) {
                throw new MojoExecutionException("Project does not have any NAR dependencies.");
            }

            // build the project for the bundle artifact
            final ProjectBuildingRequest bundleRequest = new DefaultProjectBuildingRequest();
            bundleRequest.setRepositorySession(repoSession);
            bundleRequest.setSystemProperties(System.getProperties());
            final ProjectBuildingResult bundleResult = projectBuilder.build(bundleArtifact, bundleRequest);

            // get the artifact handler for excluding dependencies
            final ArtifactHandler bundleHandler = excludesDependencies(bundleArtifact);
            bundleArtifact.setArtifactHandler(bundleHandler);

            // bundle artifacts by nature includes dependencies, however this prevents the
            // transitive dependencies from printing using tools like dependency:tree.
            // here we are overriding the artifact handler for all bundles so the
            // dependencies can be listed. this is important because nar dependencies
            // will be used as the parent classloader for this nar and seeing what
            // dependencies are provided is critical.
            final Map<String, ArtifactHandler> bundleHandlerMap = new HashMap<>();
            bundleHandlerMap.put(type, bundleHandler);
            artifactHandlerManager.addHandlers(bundleHandlerMap);

            // get the dependency tree
            final DependencyNode root = dependencyTreeBuilder.buildDependencyTree(bundleResult.getProject(), localRepository, null);

            // write the appropriate output
            DependencyNodeVisitor visitor = null;
            if ("tree".equals(mode)) {
                visitor = new TreeWriter();
            } else if ("pom".equals(mode)) {
                visitor = new PomWriter();
            }

            // ensure the mode was specified correctly
            if (visitor == null) {
                throw new MojoExecutionException("The specified mode is invalid. Supported options are 'tree' and 'pom'.");
            }

            // visit and print the results
            root.accept(visitor);
            getLog().info("--- Provided BUNDLE Dependencies ---\n\n" + visitor.toString());
        } catch (DependencyTreeBuilderException | ProjectBuildingException e) {
            throw new MojoExecutionException("Cannot build project dependency tree", e);
        }
    }

    /**
     * Gets the Maven project used by this mojo.
     *
     * @return the Maven project
     */
    public MavenProject getProject() {
        return project;
    }

    /**
     * Creates a new ArtifactHandler for the specified Artifact that overrides the includeDependencies flag. When set, this flag prevents transitive
     * dependencies from being printed in dependencies plugin.
     *
     * @param artifact  The artifact
     * @return          The handler for the artifact
     */
    private ArtifactHandler excludesDependencies(final Artifact artifact) {
        final ArtifactHandler orig = artifact.getArtifactHandler();

        return new ArtifactHandler() {
            @Override
            public String getExtension() {
                return orig.getExtension();
            }

            @Override
            public String getDirectory() {
                return orig.getDirectory();
            }

            @Override
            public String getClassifier() {
                return orig.getClassifier();
            }

            @Override
            public String getPackaging() {
                return orig.getPackaging();
            }

            // mark dependencies has excluded so they will appear in tree listing
            @Override
            public boolean isIncludesDependencies() {
                return false;
            }

            @Override
            public String getLanguage() {
                return orig.getLanguage();
            }

            @Override
            public boolean isAddedToClasspath() {
                return orig.isAddedToClasspath();
            }
        };
    }

    /**
     * Returns whether the specified dependency has test scope.
     *
     * @param node  The dependency
     * @return      What the dependency is a test scoped dep
     */
    private boolean isTest(final DependencyNode node) {
        return "test".equals(node.getArtifact().getScope());
    }

    /**
     * A dependency visitor that builds a dependency tree.
     */
    private class TreeWriter implements DependencyNodeVisitor {

        private final StringBuilder output = new StringBuilder();
        private final Deque<DependencyNode> hierarchy = new ArrayDeque<>();

        @Override
        public boolean visit(DependencyNode node) {
            // add this node
            hierarchy.push(node);

            // don't print test deps, but still add to hierarchy as they will
            // be removed in endVisit below
            if (isTest(node)) {
                return false;
            }

            // build the padding
            final StringBuilder pad = new StringBuilder();
            for (int i = 0; i < hierarchy.size() - 1; i++) {
                pad.append("   ");
            }
            pad.append("+- ");

            // log it
            output.append(pad).append(node.toNodeString()).append("\n");

            return true;
        }

        @Override
        public boolean endVisit(DependencyNode node) {
            hierarchy.pop();
            return true;
        }

        @Override
        public String toString() {
            return output.toString();
        }
    }

    /**
     * A dependency visitor that generates output that can be copied into a pom's dependency management section.
     */
    private class PomWriter implements DependencyNodeVisitor {

        private final StringBuilder output = new StringBuilder();

        @Override
        public boolean visit(DependencyNode node) {
            if (isTest(node)) {
                return false;
            }

            final Artifact artifact = node.getArtifact();
            if (!type.equals(artifact.getType())) {
                output.append("<dependency>\n");
                output.append("    <groupId>").append(artifact.getGroupId()).append("</groupId>\n");
                output.append("    <artifactId>").append(artifact.getArtifactId()).append("</artifactId>\n");
                output.append("    <version>").append(artifact.getVersion()).append("</version>\n");
                output.append("    <scope>provided</scope>\n");
                output.append("</dependency>\n");
            }

            return true;
        }

        @Override
        public boolean endVisit(DependencyNode node) {
            return true;
        }

        @Override
        public String toString() {
            return output.toString();
        }
    }
}
