package org.apache.metron.integration.util.integration.components;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.generated.StormTopology;
import org.apache.metron.integration.util.integration.InMemoryComponent;
import org.apache.metron.integration.util.integration.UnableToStartException;
import org.apache.storm.flux.FluxBuilder;
import org.apache.storm.flux.model.ExecutionContext;
import org.apache.storm.flux.model.TopologyDef;
import org.apache.storm.flux.parser.FluxParser;
import org.apache.thrift7.TException;
import org.junit.Assert;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

/**
 * Created by cstella on 1/28/16.
 */
public class FluxTopologyComponent implements InMemoryComponent {
    LocalCluster stormCluster;
    String topologyName;
    File topologyLocation;
    Properties topologyProperties;

    public static class Builder {
        String topologyName;
        File topologyLocation;
        Properties topologyProperties;
        public Builder withTopologyName(String name) {
            this.topologyName = name;
            return this;
        }
        public Builder withTopologyLocation(File location) {
            this.topologyLocation = location;
            return this;
        }
        public Builder withTopologyProperties(Properties properties) {
            this.topologyProperties = properties;
            return this;
        }

        public FluxTopologyComponent build() {
            return new FluxTopologyComponent(topologyName, topologyLocation, topologyProperties);
        }
    }

    public FluxTopologyComponent(String topologyName, File topologyLocation, Properties topologyProperties) {
        this.topologyName = topologyName;
        this.topologyLocation = topologyLocation;
        this.topologyProperties = topologyProperties;
    }

    public LocalCluster getStormCluster() {
        return stormCluster;
    }

    public String getTopologyName() {
        return topologyName;
    }

    public File getTopologyLocation() {
        return topologyLocation;
    }

    public Properties getTopologyProperties() {
        return topologyProperties;
    }

    public void start() throws UnableToStartException{
        try {
            stormCluster = new LocalCluster();
        } catch (Exception e) {
            throw new UnableToStartException("Unable to start flux topology: " + getTopologyLocation(), e);
        }
    }

    public void stop() {
        stormCluster.shutdown();
    }

    public void submitTopology() throws NoSuchMethodException, IOException, InstantiationException, TException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
        startTopology(getTopologyName(), getTopologyLocation(), getTopologyProperties());
    }
    private void startTopology(String topologyName, File topologyLoc, Properties properties) throws IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, TException {
        TopologyDef topologyDef = loadYaml(topologyName, topologyLoc, properties);
        Config conf = FluxBuilder.buildConfig(topologyDef);
        ExecutionContext context = new ExecutionContext(topologyDef, conf);
        StormTopology topology = FluxBuilder.buildTopology(context);
        Assert.assertNotNull(topology);
        topology.validate();
        stormCluster.submitTopology(topologyName, conf, topology);
    }

    private static TopologyDef loadYaml(String topologyName, File yamlFile, Properties properties) throws IOException {
        File tmpFile = File.createTempFile(topologyName, "props");
        tmpFile.deleteOnExit();
        FileWriter propWriter = null;
        try {
            propWriter = new FileWriter(tmpFile);
            properties.store(propWriter, topologyName + " properties");
        }
        finally {
            if(propWriter != null) {
                propWriter.close();
                return FluxParser.parseFile(yamlFile.getAbsolutePath(), false, true, tmpFile.getAbsolutePath(), false);
            }

            return null;
        }
    }


}
