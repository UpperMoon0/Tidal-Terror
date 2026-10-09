package net.minecraftforge.event.server;
public final class ServerStoppedEvent {
    private final Object server;
    public ServerStoppedEvent(Object server){this.server=server;}
    public Object getServer(){return server;}
}
