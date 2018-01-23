package net.kodehawa.mantarobot.utils;

/**
 * /**
 * Implement this to make your things webscale.
 *
 * @param <T> We don't actually need a type parameter here, I just felt like
 *           adding one :^)
 *
 * @author amy
 * @since 1/23/18.
 */
@SuppressWarnings("unused")
public interface Webscale<T> {
    /**
     * Makes it webscale
     */
    void webscale();
    
    /**
     * Check if webscale
     *
     * @return true if webscale, false otherwise
     */
    boolean isWebscale();
}
