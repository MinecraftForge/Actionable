# Actionable

Custom GitHub actions for MinecraftForge repositories.

## Inputs

### `gh_app_name`

The App ID of the bot application. - <font color=red>*Required*</font>

### `gh_app_key`

The Private Key of the bot application. (in `PKCS1` or `PKCS8` format) - <font color=red>*Required*</font>

### `command_prefixes`

A command-separated list of prefixes to use for commands.  
Default value: `command_prefixes`

### `allow_commands_in_edits`

If commands should be run from edited comments.  
Default value: `false`

### `triage_team`

The name of the triage team.  
Default value: `triage`

### `triage_project`

The ID of the triage PR Management project.  
The ID is represented by the `4` in the following project URL: `https://github.com/orgs/MinecraftForge/projects/4/`  
Default value: `4`

### `labels_to_teams`

This value holds the teams which should be assigned to PRs, depending on the PR's label.  
The syntax is: command-separated `key -> value` pairs. The key
is represented by the label (`default` being the fallback), and the value
is the team to assign. It can be either a normal string (a team, like `rendering`), 
a list of users seperated by a `+` (e.g. `TheCurle + LexManos`) or a single person (e.g. `u:TheCurle`).  
Default value: `default -> api`

### `latest_version`
The latest MC version. All PRs which are opened with a version tag which isn't this version will have the `LTS Backport` label added.