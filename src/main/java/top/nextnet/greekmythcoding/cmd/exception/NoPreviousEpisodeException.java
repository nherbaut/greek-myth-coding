package top.nextnet.greekmythcoding.cmd.exception;

public class NoPreviousEpisodeException extends Exception{
    public NoPreviousEpisodeException(Integer selectedEpisodeNumber) {
        super("selectedEpisodeNumber="+selectedEpisodeNumber);
    }
}
