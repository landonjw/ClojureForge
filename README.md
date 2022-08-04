# ClojureForge

Basic Clojure API layer for MinecraftForge development.

## Why?

To learn Clojure and play with one of my favourite programming sandboxes.

## Should I use this for my super cool, massive Minecraft network?

Probably not, this is highly experimental work, and likely has large performance issues, especially when used in high-traffic areas.

## FAQ

### Can I REPL into my server with this?

There is an nREPL server initiated on server startup, but unless you are running it the server from gradle, Minecraft will be obfuscated.
This may cause issues if you attempt to execute any code integrating with Minecraft or Forge.
