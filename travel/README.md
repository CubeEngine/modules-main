# Travel

Travel is a module for CubeEngine providing home and warp commands

# Features

 - Homes (private TeleportPoints) with invites
 - Warps (public TeleportPoints) with permissions
 
# Commands
 
 - home
     - tp \<home> [owner] (home)
     - set \<name> (sethome)
     - greeting \<home> \<message...> [owner \<owner>] [-append]
     - move \<home> [owner] 
     - remove \<home> [owner] 
     - rename \<home> \<new name> [owner]
     - list [owner] [-owned] [-invited]
     - invitedList [owner]
     - invite \<player> \<home>
     - uninvite \<player> \<home>
     - clear [owner] [-selection]
 
 - warp
     - tp \<warp>
     - create \<name>
     - greeting \<warp> \<message...> [-append]
     - move \<warp>
     - remove \<warp>
     - rename \<warp> \<new name> 
     - list [owner]
     - clear [owner]