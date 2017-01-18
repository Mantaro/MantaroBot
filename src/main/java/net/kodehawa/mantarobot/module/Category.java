package net.kodehawa.mantarobot.module;

public enum Category {
    ACTION, FUN, AUDIO, INFO, MISC, GAMES, MODERATION;

    @Override
    public String toString(){
        switch(this){
            case INFO:
                return "Info";
            case ACTION:
                return "Action";
            case AUDIO:
                return "Audio";
            case FUN:
                return "Fun";
            case GAMES:
                return "Games";
            case MISC:
                return "Misc";
            case MODERATION:
                return "Moderation";
        }
        return "magic";
    }
}
