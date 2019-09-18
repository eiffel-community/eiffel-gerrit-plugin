TODO: Add Plugin introduction

For more Eiffel information follow this link:
https://github.com/eiffel-community/eiffel-gerrit-plugin

Parameters
----------

Project Configuration:

TODO: Add Configuration parameters and instructions.

Example:

```
    TODO: Add Plugin configuration examples
```

Global Configuration:

plugin.eiffel-agen.senderThreadPoolSize
:   Number of threads to allocate for sending messages. Defaults to 1 if not provided.

Example:

```
    [plugin "Eiffel-Agen"]
      senderThreadPoolSize = 2
```

Configuration
-------------

Plugin configuration stored as part of the project configuration and can be edited/configured in two ways - by editing
 project.conf file:

```
    cd <your repository>
    git fetch origin refs/meta/config:refs/remotes/origin/meta/config
    git checkout meta/config
    <open project.conf and plugin configuration as it shown in example above>
    git commit -a
    git push origin meta/config:meta/config
```

or by editing project configuration using web ui, (This option is available only in Gerrit 2.9 and onwards).

Plugin configuration as part of the global configuration can be edited through the gerrit.config file.

Message content and commit message parsing
------------------------------------------

TODO!

Eiffel Event Linking
--------------------

TODO: Explain event linking and show examples.
