Eiffel Gerrit plugin is used to send Eiffel events when changes occur in a project in Gerrit.
This plugin simplifies communication in continuous integration and delivery pipelines and helps keeping
source code management traceability.

For more information about Eiffel, follow this link:
<https://github.com/eiffel-community/eiffel>

#####Eiffel Events:

Version 0.0.1
:   SourceChangeCreatedEvent
    SourceChangeSubmittedEvent

Parameters
----------

Project Configuration:

eiffel-integration.enabled
:   If set to `true` the plugin will send Eiffel events.
    Is set to `false` or not defined no Eiffel events will be sent.

eiffel-integration.filter
:   Allow users to define a white list of branches, i.e. messages will be sent only for source change on
    specified branches. Source Change to any other branch will be ignored. Accepts full branch name and or
    regex separated by `,`. If not defined, messages will be sent for source changes in all branches.
    See example configuration where release branch may be triggered on release-(Any version).

eiffel-integration.flow-context
:   Optional, but expects a UUID of a EiffelFlowContextDefinedEvent if provided.
    Can accept List of UUID's separated by `,` that tells which flow it will be part of. (More about Flow Context:
    <https://github.com/eiffel-community/eiffel/blob/master/eiffel-vocabulary/EiffelSourceChangeCreatedEvent.md#flow_context>)

eiffel-integration.remrem-publish-url
:   URL of REMReM publish service.(More about REMReM publish here:
    <https://eiffel-community.github.io/eiffel-remrem-publish/serviceUsage.html>)

eiffel-integration.remrem-username
:   Required, if you have this enabled in REMReM Publish configurations. (More about REMReM publish configurations here:
    <https://eiffel-community.github.io/eiffel-remrem-publish/serviceUsage.html>)
    Credentials towards REMReM to authenticate a user.

eiffel-integration.remrem-password
:   Required, if you have this enabled in REMReM Publish configurations. (More about REMReM publish configurations here:
    <https://eiffel-community.github.io/eiffel-remrem-publish/serviceUsage.html>)
    Credentials towards REMReM to authenticate a user

Example:

    [plugin "eiffel-integration"]
      enabled = true
      filter = (release-).* master
      flow-context = <UUID of EiffelFlowContextDefinedEvent>
      remrem-publish-url = <URL of REMReM publish service>
      remrem-username = <REMReM Username to authenticate>
      remrem-password = <Password for the above user>


    [plugin "eiffel-integration"]
      enabled = true
      filter = (release-).* master
      flow-context = aaaaaaaa-bbbb-5ccc-8ddd-eeeeeeeeeee0
      remrem-publish-url = https://localhost:8080/publish
      remrem-username = dummyuser
      remrem-password = ********

Global Configuration:

plugin.eiffel-integration.senderThreadPoolSize
:   Number of threads to allocate for sending messages. Defaults to 1 if not provided.

Example:

    [plugin "Eiffel-Integration"]
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

or by editing project configuration using web ui.

Plugin configuration as part of the global configuration can be edited through the gerrit.config file.

Message content and commit message parsing
------------------------------------------

TODO!

Eiffel Event Linking
--------------------

TODO: Explain event linking and show examples.
