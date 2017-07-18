package net.kodehawa.dataporter;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.AbstractDualBidiMap;

import java.io.Serializable;
import java.util.Map;

/**
 * Hack to use with DataPorter.
 *
 * @param <K>
 */
public class MirrorMap<K> extends AbstractDualBidiMap<K, K> implements Serializable {
    public MirrorMap(final Map<K, K> map) {
        super(map, map);
    }

    protected MirrorMap(final Map<K, K> normalMap, final Map<K, K> reverseMap,
                        final BidiMap<K, K> inverseBidiMap) {
        super(normalMap, reverseMap, inverseBidiMap);
    }

    @Override
    protected BidiMap<K, K> createBidiMap(final Map<K, K> normalMap, final Map<K, K> reverseMap, final BidiMap<K, K> inverseBidiMap) {
        return new MirrorMap<>(normalMap, reverseMap, inverseBidiMap);
    }
}
