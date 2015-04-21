##Thorvarium

[![Build Status](https://travis-ci.org/ghophp/thorvarium.svg?branch=master)](https://travis-ci.org/ghophp/thorvarium)

Multiplayer game server built with scala + akka + play. While I use this project to learn more about Scala and the reactive programming,
I homage @Olifiers (Henrique Olifers), a very talented developer that brought a lot of fun to my childhood with this multiplayer game structure.

### Before Run

Before run the project, you must set the environment variable THORVARIUM_BUILD with the path to the build folder of the client. Something like:

    export THORVARIUM_BUILD=/home/dude/thorvarium-js-client/build

### Running

The process is really straight forward, no mysterious ways, just `sbt run` at the folder should do it.

###JS Client

You can see the client that connect with this server here: [thorvarium-js-client](https://github.com/ghophp/thorvarium-js-client)