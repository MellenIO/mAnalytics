# mAnalytics - Minecraft Analytics made "simple"

**NOTE: This is made to be extendable by plugin developers, not by end users! If you do not have a reasonable understanding of SQL and Java, consider _requesting an integration_ instead.**

NOTE 2: This plugin is **very early in development** and things may change rapidly; currently, this plugin currently only officially supports **MySQL 5.7** and **MariaDB 10.3** as backing database drivers.

mAnalytics is a "simple" analytics solution for Spigot servers. At its core, it is effectively an EAV database that supports custom entries.

## Events
All events below are shown in the following format: `event.name (custom_one, custom_two, custom_three, custom_four)`
* player.death (last damage cause, world name, vector location, **unused**)
* player.kill.player (slain player name, slain player UUID, held item, slain player death cause)
* player.kill.entity (entity name, entity type, held item, killer's location)
* player.join (world, x, y, z)
* player.leave (world, x, y, z)
* (PLANNED) player.teleport
* (PLANNED) player.world.change
* (PLANNED) player.block.break
* (PLANNED) player.command

### Third Party Events
**COMING SOON**

mAnalytics also includes **(planned)** events for the following plugins:
* Towny Advanced
* EssentialsX
* MiniaturePets
* Quests
* Jobs Reborn

## Installation
* Clone source
* **(TEMPORARY STEP)** Execute `src/resources/setup.sql` on your MySQL server (eg: `mysql analytics < src/resources/setup.sql`)
* `mvn install`
* Copy `target/manalytics-1.0-SNAPSHOT.jar` in to your `plugins` directory

mAnalytics will **work out of the box** with no extra configuration required.
However, mAnalytics can be **extended by developers** and custom events can be **pushed with commands**.

## Basic Usage
mAnalytics works by **pushing events** in to a central database table. By default, every event has **four custom parameters** that can be configured via the `/analytics push` command.
```
/analytics view <playerName> [page] - manalytics.player.view
EXAMPLE: /analytics view amellen 1 -> view the first page of events for amellen
View event history for a player

/analytics push <playerName> <eventName> ([customOne],[customTwo],[customThree],[customFour]) - manalytics.event.push
EXAMPLE: /analytics push amellen player.vote MinecraftServers,Money Reward -> push a player.vote event with custom_one=MinecraftServers, custom_two=Money Reward
Push an event for a player

(PLANNED) /analytics query [p:<playerName>] [e:event.one,event.two] [t:timeframe] (c:conditions) - manalytics.player.query
EXAMPLE 1: /analytics query p:amellen e:player.vote t:3d c:custom_one=MinecraftServers&custom_two=Money_Reward -> query all the times that amellen triggered the player.vote event in the last 3 days, with custom_one = MinecraftServers and custom_two = Money Reward (see /analytics push example)
EXAMPLE 2: /analytics query p:altmellen e:player.kill.player t:1m c:custom_one=b31333d0-7f80-429c-86d4-6b1dca49be00 -> query every time in the last week that altmellen killed amellen (UUID b3133...)
Query the event history for (a group of) player(s)

(PLANNED) /analytics execute-if [r:repeat_time] [p:<playerName>] [e:event.one,event.two] [t:timeframe] [c:conditions] [command] - manalytics.admin.executeif
EXAMPLE 1: /analytics execute-if p:amellen t:2d e:player.vote c:count(player.vote)<=3 kill %%player_name%% -> kill amellen if they have triggered the player.vote event 3 or less times in the last 2 days
EXAMPLE 2: /analytics execute-if r:30m p:all t:1w e:player.death c:count(player.death)>1 msg %%player_name%% Hey, you died recently! -> every 30 minutes, message any online player that died in the last week
EXAMPLE 3: /analytics execute-if r:1m p:all t:1m e:player.death c:custom_one=LAVA msg %%player_name%% The floor is lava!!!! -> every minute, message any player that died due to lava
Execute conditional commands based off of the conditions of a query

(PLANNED) /analytics execute-when [p:<playerName>] [e:event.one,event.two] [c:conditions] [command] - manalytics.admin.executewhen
EXAMPLE 1: /analytics execute-when p:amellen e:player.kill.player c:custom_one=altmellen kill %%player_name%% -> kill amellen every time they kill altmellen
EXAMPLE 1: /analytics execute-when p:all e:player.kill.player c:custom_one=amellen ban %%player_name%% 24h How dare you kill amellen?! -> ban players for 24h every time they kill amellen
Execute conditional commands as soon as an event that matches the conditions of a query is pushed

(PLANNED) /analytics anonymise <playerName> - manalytics.admin.anonymise
EXAMPLE: /analytics anonymise amellen -> anonymise data of amellen
Anonymise some aspects of the analytics data for GDPR purposes/other privacy concerns
```

## Analytics Dashboard
**COMING SOON**

mAnalytics comes with an optional REST API which can be hooked up to a dashboard frontend. A reference dashboard can be found at: **COMING SOON**

## How it works
Under the hood, a simple EAV (Entity-Attribute-Value) mapping is used.
Everything in the database is regarded as an `entity`, which has its own unique `entity_type_id`.

An entity can have as many `entity_attribute`'s as it wants, which makes this plugin extendable by third parties.

There are four backing database tables for EAV data:
```
entity_attribute_varchar -> string data
entity_attribute_int -> integer data
entity_attribute_datetime -> timestamp(!) data
entity_attribute_decimal -> decimal data
```


To create a new entity, using the default "player" entity as an example:
```sql
-- Initial data set up
insert into entity(entity_type_name) values ('player');
set @playerEntityId = last_insert_id();

-- Initial attribute set up
insert into entity_attribute(entity_type_id, attribute_name, attribute_type)
    values
    (@playerAttributeId, 'vault_money', 'decimal'),
    -- for each attribute needed, repeat!
    (@playerAttributeId, 'rank', 'varchar')
    ;
```

To add new attributes _after_ the entity has been included in the database:
```sql
insert into entity_attribute(entity_type_id, attribute_name, attribute_type)
    values 
    ((select entity_type_id from entity where entity_type_name = 'player'), 'vault_money', 'decimal')
    -- etc
    ;

```

