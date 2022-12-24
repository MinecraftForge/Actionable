# Actionable

Custom GitHub actions for MinecraftForge repositories.

## Inputs

### `gh_app_name`

The App ID of the bot application. - <font color=red>*Required*</font>

### `gh_app_key`

The Private Key of the bot application. (in `PKCS1` or `PKCS8` format) - <font color=red>*Required*</font>

### `config_directory`

The configuration directory in which the configuration(s) for the action lies. - <font color=red>*Required*</font>  
Format: `Org/RepoName:directory@main`  
Example: for the directory `ForgeForce/ActionsStore:configs@main`, the configuration for the `ForgeForce/ActionsTest` repository
will be in the `ForgeForce/ActionsStore` repository, at `configs/ForgeForce/ActionsTest.yml`, on the `main` branch.

### Configuration File
```yaml
labels: # A map of label ID -> label name
  feature: "Feature"
  latest: "1.19" # The `latest` label is special, and represents the latest version of Minecraft. When a new PR targeted for a version which is not this version is created, the `lts` label will be added.

labelLocks: # A map of label name -> lock behaviour
  Forum:
    lock: true # If this label should lock the issue
    lockReason: "spam" # If `lock` is true, the reason for locking the issue
    close: true # If this label should close the issue as not planned
    message: |- # An optional message to send before closing / locking the issue
      :wave: We use the issue tracker exclusively for final bug reports and feature requests.
      However, this issue appears to be better suited for the [Forge Support Forums](https://forums.minecraftforge.net/) or [Forge Discord](https://discord.gg/UvedJ9m).
      Please create a new topic on the support forum with this issue or ask in the `#tech-support` channel in the Discord server, and the conversation can continue there.

triage: # Optional triage information
  teamName: "test-team" # The name of the triage team. This team will have permission to run issue management commands
  projectId: 4 # The ID of the private PR Triage Tracking project. This value, is for example, the `4` in `https://github.com/orgs/MinecraftForge/projects/4`

labelTeams: # Label -> PR assignment team information
  default: # The "fallback" team, if the PR doesn't have any of the other labels
  # The value can either be a list of users, or a string, representing a team
  - "TheCurle"
  - "sciwhiz12"
  Rendering: "test-team"
  LTS Backport:
  - "AterAnimAvis"

commands: # Optional information used by commands
  prefixes: # A list of command prefixes
  - "/"
  - "@forgeforce-bot "
  allowEdits: false # If commands should be run from edited comments
  minimizeComment: true # If command-only comments should be minimized after the command is successfully run
  reactToComment: true # If comments should be reacted to with either 🚀 or 😕 depending on the result of the command
```