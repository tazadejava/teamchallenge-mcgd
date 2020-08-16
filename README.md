# TeamChallengeMCGD

Plugin developed for use in HSSP summer 2020 class: **Game Design and Development with Minecraft in Java**

Specifically, this plugin will handle the weekly challenges given to the students (6 total).

A backend webserver is required to obtain group data and accomplish challenge level 6, since the level is inherently competitive against other groups. However, the challenges should be able to run without the backend running.

To load a specific level in the server, edit the config.yml file within the plugins folder and change the number after "current-level:" to the level desired (1-6). 

# Package structure

## me.tazadejava.levels

Handles specific level events and restrictions. Separated by class to handle a particular level.

## me.tazadejava.main

Main package that handles the events and commands for general challenge structure.

* TeamChallengeMCGD.java loads the challenge up.

## me.tazadejava.specialitems

Helper classes developed for schematic use-case in OpCommandHandler. Implements a generic special item class that is used to create a "wand" that can select block ranges and duplicate blocks. Not required for the challenges; used for helper methods only.

# Backend webserver

If you want to replicate the backend webserver to create groups and manage level 6, you will need to define a few API endpoints to retrieve group data, user data, and challenge 6 data.

* The website, mcgamedev.port0.org:3075, points to the backend webserver in the code. You will need to replicate all of these calls with your own internal API calls.

* See https://minecraftgamedev.github.io/teach/ for more details on how to locally implement this backend to teach this class.