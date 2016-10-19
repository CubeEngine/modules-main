# VanillaPlus

VanillaPlus is a module for CubeEngine providing improved vanilla commands and more

# Features

 -
 
# Commands

### Improvements:
 
 - whitelist
     - list
     - add \<player>
     - remove \<player>
     - on
     - off
     - wipe

 - weather \<weather> [duration] [in \<world>]
 - pweather \<weather> [player \<player>] **WIP Waiting for API**
 
 - time [time] [in \<world>] [-lock]
 - ptime \<time> [player] [-lock] **WIP Waiting for API**

 - stop [message]
 
 - saveall [world]
 - saveon **WIP**
 - saveoff **WIP**
 
 - list (uses option "list-group" to group players together)
 
 - op **WIP missing API**
 - deop **WIP missing API** 
 
 - kill [player...] [-force] [-quiet] [-lightning]
 - suicide
 
 - rename \<name> [lore...]
 - lore \<lore>
 - headchange [name]
 - enchant \<enchantment> [level] [-unsafe]
 - repair [-all]
 
 - give \<player> \<item[:data]> [amount]
 - item \<item[:data]> [amount] [ench \<enchantment[:level]>...]
 - more [amount] [-all]
 - stack **WIP Overstacked Items are not possible atm**
 
 - gamemode [gamemode] [player]
 
 - difficulty [difficulty] [in \<world>]
 
 - clearinventory [player] [-removeArmor] [-quiet] [-force] (ci)
 
 - border 
    - generate [world] [-fulltick]
    - setCenter [x] [z] [in \<world>]
    - setDiameter \<size> [seconds] [in \<world>]
    - add \<size> [time] [in \<world>]
    - warningTime \<seconds> [world]
    - warningDistance \<blocks> [world]
    - info [world]
    - damage \<damage> [world]
    - damageBuffer <blocks> [world]
    
 - spawnmob \<mob>[:data][,\<ridingmob>] [amount] [player]
 
 - removeAll \<entityType>[:item] [in \<world>]
 - remove \<entityType>[:item] [radius] [in \<world>]
 - butcher [types...] [radius] [in <world>] [-lightning] [-all]
 
### Additions

 - unlimited [true|false]
 - sudo \<player> <message>
 - stash
 - plugins
 - version [plugin]
 - seen \<player>
 - whois \<player>
 - walkspeed \<speed> [player]
 - fly [speed] [player \<player>]
 - itemdb [name]
 - invsee \<player> [-ender] [-force] [-quiet]
 - biome [world] [x] [z]
 - seed [world]
 - compass
 - depth
 - getPos
 - near [radius] [player] [-entity] [-mob]
 - ping (pong)
 - lag
 - listworlds **WIP Handle duplicate cmds in module**
 - heal [player...]
 - god [player]
 - feed [player...]
 - starve [player...]
 
 


  
    
    
 
 
 
 
 
 
 
 
 
 
    
     