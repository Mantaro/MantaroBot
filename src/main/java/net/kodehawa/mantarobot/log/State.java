package net.kodehawa.mantarobot.log;

public enum State {
    PRELOAD, LOADING, LOADED, POSTLOAD;

    public String verbose() {
        String result = null;
        switch(this){
            case PRELOAD:
                result = "Starting...";
                break;
            case LOADING:
                result = "Loading...";
                break;
            case LOADED:
                result = "Loaded.";
                break;
            case POSTLOAD:
                result = "Ready.";
                break;
        }
        return result;
    }

    @Override
    public String toString() {
        String result = null;
        switch(this){
            case PRELOAD:
                result = "Preload";
                break;
            case LOADING:
                result = "Loading";
                break;
            case LOADED:
                result = "Loaded";
                break;
            case POSTLOAD:
                result = "Ready";
                break;
        }
        return result;
    }
}
