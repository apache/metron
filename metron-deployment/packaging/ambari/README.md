# Ambari Management Pack Development
Typically, Ambari Management Pack development will be done in the Vagrant environments. These instructions are specific to Vagrant, but can be adapted for other environemnts (e.g. make sure to be on the correct nodes for server vs agent files)


# Adding a new property
1. Add the property to the appropriate `*-env.xml` file found in `METRON.CURRENT/configuration`.
  ```
    <property>
        <name>new_property</name>
        <description>New Property description</description>
        <value>Default Value</value>
        <display-name>New Property Pretty Name</display-name>
    </property>
  ```

2.  Reference the property in `METRON.CURRENT/package/scriptes/params/params_linux.py`, unless it will be used in Ambari's status command.  It will be stored in a variable. The name doesn't have to match, but it's preferred that it does.
Make sure to use replace `metron-env` the correct `*-env` file, as noted above.
  ```
  new_property = config['configurations']['metron-env']['new_property']
  ```
If this property will be used in the status command, instead make this change in `METRON.CURRENT/package/scriptes/params/status_params.py`.
Afterwards, in `params_linux.py`, reference the new property:
  ```
  new_property = status_params.new_property
  ```
This behavior is because Ambari doesn't send all parameters to the status, so it needs to be explicitly provided.

3. Ambari master services can then import the params as such

  ```
  from params import params
  env.set_params(params)
  ```

4. The `*_commands.py` files receive the params as an input from the master services.  Once this is done, they can be accessed via the variable we set above:
  ```
  self.__params.new_property
  ```

  Ambari also has a custom `format` function that can access properties when producing strings as output:
  ```
  from resource_management.libraries.functions import format as ambari_format
  # ...
  Foo(ambari_format("{new_property}-adjusted")),
  ```

## How to identify errors in MPack changes
Typically, any errors are thrown at one of two times:

1. Attempting to install Metron as a whole.  These are typically service level definition errors, not property errors. Logs will often be found in `/var/log/ambari-server/ambari-server.log`.  Often the log will indicate it was unable to load a particular service or subservice with some more detail.
2. Running the actual functionality.  These typically tend to cause errors in the UI at runtime.  Logs are usually found in `/var/log/ambari-agent/ambari-agent.log`.

Unfortunately, because errors tend to occur at runtime, it's often necessary to add things like logging statements, or even just throw errors to print out in the Ambari UI.

The primary solution to these is to look in the logs for exceptions, see what's going wrong (Property doesn't exist?  Malformed file couldn't be loaded?), and adjust appropriately.

## Testing changes without cycling Vagrant build
To avoid spinning down and spinning back up Vagrant, we'll want to instead modify the files Ambari uses directly.
Ambari stores the Python files from the service in a couple places.

Specifically, the server files live in
```
/var/lib/ambari-server/resources/mpacks/metron-ambari.mpack-0.4.0.0/common-services
/var/lib/ambari-agent/cache/common-services
```

e.g. enrichment_commands.py can be found in:

```
/var/lib/ambari-server/resources/mpacks/metron-ambari.mpack-0.4.0.0/common-services/METRON/0.4.0/package/scripts/enrichment_commands.py
/var/lib/ambari-agent/cache/common-services/METRON/0.4.0/package/scripts/enrichment_commands.py
```

A `find` command can also be useful in quickly locating the exact location of a file, e.g.
```
[root@node1 ~]# find /var/lib/ -name enrichment_commands.py
/var/lib/ambari-server/resources/mpacks/metron-ambari.mpack-0.4.0.0/common-services/METRON/0.4.0/package/scripts/enrichment_commands.py
/var/lib/ambari-agent/cache/common-services/METRON/0.4.0/package/scripts/enrichment_commands.py
```

The steps to update are:

1. Edit the file(s) with your changes.  The ambari-agent file must be edited, but generally better to update both for consistency.
2. Restart the Ambari Agent to get the cache to pick up the modified file
`service ambari-agent restart`

