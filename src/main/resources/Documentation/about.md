Eiffel Gerrit plugin is used to send Eiffel events when changes occur in a project in Gerrit.
This plugin simplifies communication in continuous integration and delivery pipelines and helps keeping
source code management traceability.

For more information about Eiffel, follow this link:
<https://github.com/eiffel-community/eiffel>

##### Eiffel Events

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
      filter = (release-).*,master
      flow-context = <UUID of EiffelFlowContextDefinedEvent>
      remrem-publish-url = <URL of REMReM publish service>
      remrem-username = <REMReM Username to authenticate>
      remrem-password = <Password for the above user>


    [plugin "eiffel-integration"]
      enabled = true
      filter = (release-).*,master
      flow-context = aaaaaaaa-bbbb-5ccc-8ddd-eeeeeeeeeee0
      remrem-publish-url = https://localhost:8080/publish
      remrem-username = dummyuser
      remrem-password = ********

<span style="color:red">Note</span>
-----------------
Manually editing the Eiffel plugin configurations will show the configurations in the GUI using comma-seperated values.
If you are changing them back in the UI, it will only contain one pair in the project configurations.

-----------------

Global Configuration:

plugin.eiffel-integration.senderThreadPoolSize
:   Number of threads to allocate for sending messages. Defaults to 1 if not provided.

Example:

    [plugin "Eiffel-Integration"]
        senderThreadPoolSize = 2

Configuration
-------------

Plugin configuration stored as part of the project configuration and can be edited/configured in two ways - by editing
project.config file:

    cd <your repository>
    git fetch origin refs/meta/config:refs/remotes/origin/meta/config
    git checkout meta/config
    <open project.config and plugin configuration as it shown in example above>
    git commit -a
    git push origin meta/config:meta/config

or by editing project configuration using web ui.

Plugin configuration as part of the global configuration can be edited through the gerrit.config file.

Message content
------------------------------------------

When a patchset is pushed, the plugin will parse commit info and form a EiffelSourceChangeCreatedEvent that will contain the following fields.

* repository name
* branch name
* repository url
* author name
* author username
* author e-mail
* commit id
* change id
* line insertions
* lines deletions

There will also be a EiffelSourceChangeSubmittedEvent when a patchset is submitted containing the following fields.

* repository name
* branch name
* repository url
* author name
* author username
* author e-mail
* commit id

Eiffel Event Linking
--------------------

When a patchset is pushed to a gerrit repository and branch the EiffelSourceChangeCreatedEvent that is created will have the
BASE link set to the previous EiffelSourceChangeSubmittedEvent. If there is no previous EiffelSourceChangeSubmittedEvent 
for that particular repository and branch then no BASE link will be set. Any following patchsets pushed to the same review will
be connected in a one way chain from newest to oldest through the PREVIOUS_VERSION link.
When it's time to submit the patchset an EiffelSourceChangeSubmittedEvent is created with a CHANGE link set to the last
patchset pushed to that particular review and a PREVIOUS_VERSION link set to the last EiffelSourceChangeSubmittedEvent
for that repository and branch. If there is no previous EiffelSourceChangeSubmittedEvent then no PREVIOUS_VERSION link will be set.

Example of a basic scenario can be seen below.

Scenario overview:

This scenario describes a standard review cycle where a user uploads a patch
set, receives some comments, corrects the comments and submits the changes. The
plugin can find the SCS event from the previous successful review.

Preconditions:

- Previous review submitted to the master with hash `C0`
- The plugin sends SCS with id `E0`

The user does the following:

- Creates a branch
- Updates the code
- Squashes the commits to one commits (`C1`)
- Pushes to `refs/for/[branch name]` (`P1`)
- Receives comments from reviewer
- Updates the code
- Does `commit --amend` (`C2`)
- Pushes to `refs/for/[branch name]` (`P2`)
- Gets ok from the reviewer
- Hits the submit button in Gerrit

Eiffel events sent from the plugin:

- SCC(`E1`) sent for `P1` push with `BASE` link set to `E0`
- SCC(`E2`) sent for `P2` push with `PREVIOUS_VERSION` link set to `E1` and `BASE` link set to `E0`
- SCS(`E3`) at submit with `CHANGE` link set `E2` and `PREVIOUS_VERSION` link set to `E0`
