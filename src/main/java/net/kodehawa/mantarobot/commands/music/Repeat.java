package net.kodehawa.mantarobot.commands.music;

public enum Repeat {
    SONG("Current song"), QUEUE("Full queue");

    String s;

    Repeat(String s){
        this.s = s;
    }

    @Override
    public String toString(){
        return s;
    }
}
