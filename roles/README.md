# Roles

Roles is a module for CubeEngine providing permissions

# Features

 - Permission Inheritance with priorities
 - Temporary (Transient) Permissions
 - Assign multiple roles to one player
 - Permissions and Options per Context
 - Live-Reload
 - Powerful Tab-Completion
   - including registered PermissionDescriptions

# Commands

 - roles role (manrole)
    - Informative:
        - list (listroles)
        - listPermission \<role> [in \<context>] (listRPerm)
        - listOption \<role> [in \<context>] (listROption)
        - listParent \<role> [in \<context>] (listRParent)
        - listDefaultRoles     
        - priority \<role>
        - checkPermission \<role> \<permission> [in \<context>] (checkRPerm)
    - Management:
        - create \<name> (createRole)
        - delete \<role> [-force] (deleteRole)
        - rename \<role> \<new name>(renameRole)
        - setPermission \<role> \<permission> \[type] [in \<context>] (setRPerm)
        - setOption \<role> \<key> \<value> [in \<context>] (setROption)
        - resetOption \<role> \<key> [in \<context>] (resetROption)
        - clearOption \<role> [in \<context>] (clearROption)
        - addParent \<role> \<parent role> [in \<context>] (addRParent)
        - removeParent \<role> \<parent role> [in \<context>] (remRParent)
        - clearParent \<role> [in \<context>] (clearRParent)
        - toggleDefaultRole \<role> 
        - setPriority \<role> \<priority> (setRolePriority) 
 
 - roles user (manuser)
    - Informative:
        - listPermission \<player> [in \<context>] [-all] (listUPerm)
        - listOption \<player> [in \<context>] [-all] (listUOption)
        - list \<player> (listURole)
        - checkPermission \<player> \<permission> [in \<context>] (checkUPerm)
        - checkOption \<player> \<key> [in \<context>] (checkUOption)
    - Management:
        - assign \<player> \<role> [-temp] (manUAdd/assignURole)
        - setPermission \<player> \<permission> \<type> [in \<context>] (setUPerm)
        - setOption \<player> \<key> \<value> [in \<context>] (setUOption)
        - remove \<player> \<role> (remURole)
        - clearOption \<player> [in \<context>] (clearUOption)
        - clear \<player> (clearURole)

 - roles admin
    - reload (manload)
    - save (mansave)
    - findPermission \<permission>