TODO: Add Plugin introduction

For more Eiffel information follow this link:
<https://github.com/eiffel-community/eiffel-gerrit-plugin>

Parameters
----------

Project Configuration:

eiffel-agen.enabled
:   Plugin state. if set to `true`, send message Eiffel message for every commit submitted for this repository.
    Plugin considered disabled if this option is not defined or set to `false`.

eiffel-agen.filter
:   Allow users to defined a white list of branches, i.e. messages will be sent only for a commits submitted on specified branches, all other submits will be ignored.
    Accepts Java regex's separated by spaces. If not defined, send messages for commits submitted on all branches.

eiffel-agen.flow-context
:   Optional, but expects a UUID of a EiffelFlowContextDefinedEvent if provided.
    Can accept List of UUID's seperated by `,` that tell which flow it will be part of. (More about Flow Context: <https://github.com/eiffel-community/eiffel/blob/master/eiffel-vocabulary/EiffelSourceChangeCreatedEvent.md#flow_context>) 

eiffel-agen.remrem-publish-url
:   URL of REMReM publish service.(More about REMReM publish here: <https://eiffel-community.github.io/eiffel-remrem-publish/serviceUsage.html>)

eiffel-agen.remrem-username
:   Credentials towards REMReM to authenticate a user.

eiffel-agen.remrem-password
:   Credentials towards REMReM to authenticate a user

Example:

    [plugin "eiffel-agen"]
      enabled = true
      filter = master
      flow-context = <UUID of EiffelFlowContextDefinedEvent>
      remrem-publish-url = <URL of REMReM publish service>
      remrem-username = <REMReM Username to authenticate>
      remrem-password = <Password for the above user>


    [plugin "eiffel-agen"]
      enabled = true
      filter = master
      flow-context = aaaaaaaa-bbbb-5ccc-8ddd-eeeeeeeeeee0
      remrem-publish-url = https://localhost:8080/publish
      remrem-username = dummyuser
      remrem-password = ********

Global Configuration:

plugin.eiffel-agen.senderThreadPoolSize
:   Number of threads to allocate for sending messages. Defaults to 1 if not provided.

Example:

    [plugin "Eiffel-Agen"]
        senderThreadPoolSize = 2

Configuration
-------------

Plugin configuration stored as part of the project configuration and can be edited/configured in two ways - by editing
 project.conf file:

    cd <your repository>
    git fetch origin refs/meta/config:refs/remotes/origin/meta/config
    git checkout meta/config
    <open project.conf and plugin configuration as it shown in example above>
    git commit -a
    git push origin meta/config:meta/config

or by editing project configuration using web ui, (This option is available only in Gerrit 2.9 and onwards).

Plugin configuration as part of the global configuration can be edited through the gerrit.config file.

Message content and commit message parsing
------------------------------------------

TODO!

Eiffel Event Linking
--------------------

TODO: Explain event linking and show examples.
