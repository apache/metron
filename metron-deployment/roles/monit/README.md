# Monit Integration

This role will leverage Monit as a process watchdog to manage sensors, topologies, and core services.  

* Monit can be used to start, stop, or check status of any of the sensors or topologies.  
* When monitoring is enabled (on by default) if a process dies, it will be restarted.
* The Monit web interface is exposed at http://hostname:2812.
* The web interface username and password is defined by the `monit_user` and `monit_password` variables.  These default to `admin`/`monit`.
* Monit CLI tools can also be used to simplify the process of managing Metron components.
* The post-deployment report for Amazon-EC2 provides links to Monit's web interface labeled as 'Sensor Status' and 'Topology Status.'

  ```
  ok: [localhost] => {
    "Success": [
        "Apache Metron deployed successfully",
        "   Metron          @ http://ec2-52-39-143-62.us-west-2.compute.amazonaws.com:5000",
        "   Ambari          @ http://ec2-52-39-4-93.us-west-2.compute.amazonaws.com:8080",
        "   Sensor Status   @ http://ec2-52-39-4-93.us-west-2.compute.amazonaws.com:2812",
        "   Topology Status @ http://ec2-52-39-130-62.us-west-2.compute.amazonaws.com:2812",
        "For additional information, see https://metron.incubator.apache.org/'"
    ]
  }
  ```

## Usage


Start all Metron components

```
monit start all
```

Stop all Metron components

```
monit stop all
```

Start an individual Metron component

```
monit start bro-parser
```

Start all components required to ingest Bro data

```
monit -g bro start
```

Start all parsers

```
monit -g parsers start
```

What is running?

```
monit summary
```
