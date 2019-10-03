# Gerrit scenarios

These scenarios describes possible use cases that needs consideration during the
implementation and update of the plugin

<!-- TOC -->

- [Gerrit scenarios](#gerrit-scenarios)
  - [Create patch set - CASE1](#create-patch-set---case1)
  - [Create patch set with rebase - CASE2](#create-patch-set-with-rebase---case2)
  - [Create patch set with merge - CASE3](#create-patch-set-with-merge---case3)
  - [Create patch set no previous events - CASE4](#create-patch-set-no-previous-events---case4)

<!-- /TOC -->

Glossary:

- **SCS** SourceChangeSubmitted
- **SCC** SourceChangeCreated
- **E?** Events
- **C?** Commits
- **P?** Push commands

## Create patch set - CASE1

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
- SCC(`E2`) sent for `P2` push with `PREVIOUS_VERSION` link set `E1` and `BASE` link set to `E0`
- SCS(`E3`) at submit with `CHANGE` link set `E2`

Commit history after submit:

```git
* C2 Review changes
* C0 Commit from previous user
```

Trigger table:

| Event | Triggered by | Comment |
| ---   | ---          | ---     |
| E0    |              | Submit  |
| E1    | P1           |
| E2    | P2           |
| E3    |              | Submit  |

SCC Event table:

| Event | BASE | PREVIOUS_VERSION |
| ---   | ---  | ---              |
| E1    | E0   |
| E2    | E0   | E1               |

## Create patch set with rebase - CASE2

Scenario overview:

This scenario describes a workflow where a user performs a standard review
cycle. Before the review finishes another review finishes and submits to
master(`C01`). The project settings does not allow merge commits.

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
- Forced to rebase the code due to `C01` (`C3`)  event `E01`
- Pushes to `refs/for/[branch name]`  (`P3`)
- Hits the submit button in Gerrit

Eiffel events sent from the plugin:

- SCC(`E1`) sent for `P1` push with `BASE` link set to `E0`
- SCC(`E2`) sent for `P2` push with `PREVIOUS_VERSION` link set `E1` and `BASE` link set to `E0`
- SCC(`E3`) sent for `P3` push with `PREVIOUS_VERSION` link set `E2` and `BASE` link set to `E01`
- SCS(`E4`) sent at submit with `CHANGE` link set `E3`

Commit history after submit:

```git
* C3 Rebased changes
* C01 Other code change
* C0 Commit from previous user
```

Trigger table:

| Event | Triggered by | Comment |
| ---   | ---          | ---     |
| E0    |              | Submit  |
| E1    | P1           |
| E2    | P2           |
| E3    | P3           |
| E4    |              | Submit  |

SCC Event table:

| Event | BASE | PREVIOUS_VERSION |
| ---   | ---  | ---              |
| E1    | E0   |
| E2    | E0   | E1               |
| E3    | E01  | E2               |

## Create patch set with merge - CASE3

Scenario overview:

This scenario describes a workflow where a user performs a standard review
cycle. Before the review finishes another review finishes and submits to
master(`C01`). The project settings does not allow rebase.

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
- Forced to do a merge due to `C01` (`C3`) event `E01`
- Pushes to `refs/for/[branch name]`  (`P3`)
- Hits the submit button in Gerrit

Eiffel events sent from the plugin:

- SCC(`E1`) sent for `P1` push with `BASE` link set to `E0`
- SCC(`E2`) sent for `P2` push with `PREVIOUS_VERSION` link set `E1` and `BASE` link set to `E0`
- SCS(`E3`) sent for `P3` push with `PREVIOUS_VERSION` link set `E2` and `BASE` link set to `?`
- SCS(`E4`) sent at submit with `CHANGE` link set `E3`

Commit history after submit:

```git
* C3 Merged changes
|\
* | C01 Other code change
| * C2  Review changes
|/
* C0 Commit from previous user
```

Trigger table:

| Event | Triggered by | Comment |
| ---   | ---          | ---     |
| E0    |              | Submit  |
| E1    | P1           |
| E2    | P2           |
| E3    | P3           |
| E4    |              | Submit  |

SCC Event table:

| Event | BASE | PREVIOUS_VERSION |
| ---   | ---  | ---              |
| E1    | E0   |
| E2    | E0   | E1               |
| E3    | ?    | E2               |

## Create patch set with no previous events - CASE4

Scenario overview:

This scenario describes a standard review cycle. The plugin cannot find the SCS
event from a previous review.

This scenario represents the situations:
- Plugin enabled for the first time
- An error occurred during the sending of the event

Preconditions:

- Previous review submitted to the master with hash `C0`
- No event available for the commit

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

- SCC(`E1`) sent for `P1` push with no `BASE` link set
- SCC(`E2`) sent for `P2` push with `PREVIOUS_VERSION` link set `E1`
- SCS(`E3`) at submit with `CHANGE` link set `E2`

Commit history after submit:

```git
* C2 Review changes
* C0 Commit from previous user
```

Trigger table:

| Event | Triggered by | Comment |
| ---   | ---          | ---     |
| E1    | P1           |
| E2    | P2           |
| E3    |              | Submit  |

SCC Event table:

| Event | BASE | PREVIOUS_VERSION |
| ---   | ---  | ---              |
| E1    |      |
| E2    |      | E1               |
