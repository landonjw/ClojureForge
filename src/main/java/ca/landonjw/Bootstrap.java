package ca.landonjw;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import clojure.lang.Symbol;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod("clojureforge")
public class Bootstrap
{

    public Bootstrap()
    {
        MinecraftForge.EVENT_BUS.register(this);
        System.out.println("Bootstrapping Clojure mod...");
        initializeREPL();
    }

    public void initializeREPL()
    {
        Clojure.var("clojure.core", "require").invoke(Symbol.intern("ca.landonjw.bootstrap"));
        IFn execute = Clojure.var("ca.landonjw.bootstrap", "start-nrepl-server");
        execute.invoke();
    }

    @SubscribeEvent
    public void onEvent(Event event)
    {
        Clojure.var("clojure.core", "require").invoke(Symbol.intern("ca.landonjw.api.event"));
        IFn execute = Clojure.var("ca.landonjw.api.event", "post");
        execute.invoke(event.getClass(), event);
    }

}