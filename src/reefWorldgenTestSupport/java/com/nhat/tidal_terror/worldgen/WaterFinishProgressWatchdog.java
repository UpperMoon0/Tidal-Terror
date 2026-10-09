package com.nhat.tidal_terror.worldgen;

/** Read-only scheduling assertions shared by the native restart and handler checks. */
public final class WaterFinishProgressWatchdog {
    private int lastTick,lastCursor,advances;
    private long turnTicks,progressDeadline,completionDeadline;

    public WaterFinishProgressWatchdog(int restoredCursor) {
        require(restoredCursor>=0 && restoredCursor<256,"Invalid restored cursor");
        lastCursor=restoredCursor;
    }

    /** Call once after each ordinary server tick; null means the queue entry completed. */
    public boolean observe(int tick,Integer pendingCursor,int pendingChunks,boolean blockTicking) {
        require(tick==lastTick+1,"Missed a normal server-tick observation");
        lastTick=tick;
        int cursor=pendingCursor==null?256:pendingCursor;
        require(cursor>=lastCursor && cursor<=Math.min(256,lastCursor+64),
            "Automatic cursor regressed or exceeded one tick's budget: "+lastCursor+" -> "+cursor);
        boolean advanced=cursor>lastCursor;
        if(advanced) { advances++;lastCursor=cursor; }
        if(cursor==256) {
            require(advances>0,"No automatic repair progress was observed");
            return true;
        }
        if(tick<200)return false; // Existing native fixture warm-up, not a completion deadline.
        require(blockTicking,"Restored witness did not become block-ticking during warm-up");
        if(turnTicks==0) {
            require(pendingChunks>0,"Pending witness is missing its queue");
            // In this closed fixture no further chunks are requested while waiting.
            // Even if every queued chunk consumes the soft time budget, the real
            // handler must rotate at least one entry per tick. One full queue
            // turn therefore bounds the next visit; that visit advances >=1
            // column. Using the entire depth is conservative for nonready chunks,
            // which can rotate 32 at a time. Do not assume 64 columns always fit.
            turnTicks=(long)pendingChunks+1;
            progressDeadline=tick+turnTicks;
            completionDeadline=tick+turnTicks*(256-cursor);
        } else if(advanced) {
            progressDeadline=tick+turnTicks;
        }
        require(tick<=progressDeadline,"Automatic repair starved for a complete queue turn at cursor "+cursor);
        require(tick<=completionDeadline,"Automatic repair exceeded its queue-depth/remaining-column bound");
        return false;
    }

    public boolean armed(){return turnTicks!=0;}
    public int cursor(){return lastCursor;}
    public int advances(){return advances;}
    public long turnTicks(){return turnTicks;}
    public long completionDeadline(){return completionDeadline;}

    private static void require(boolean value,String message) {
        if(!value)throw new AssertionError(message);
    }
}
