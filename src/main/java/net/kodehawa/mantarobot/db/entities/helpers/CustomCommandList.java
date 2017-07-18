package net.kodehawa.mantarobot.db.entities.helpers;

import net.kodehawa.mantarobot.utils.URLEncoding;
import org.apache.commons.collections4.iterators.AbstractListIteratorDecorator;
import org.apache.commons.collections4.list.AbstractListDecorator;

import java.util.*;
import java.util.stream.Collectors;

/**
 * List that does the URL Encoding and Decoding in the backing list.
 */
@SuppressWarnings({"NullableProblems", "SuspiciousToArrayCall"})
public class CustomCommandList extends AbstractListDecorator<String> {
    public CustomCommandList(List<String> list) {
        super(list);
    }

    private static String decode(String s) {
        return s == null ? null : URLEncoding.decode(s);
    }

    private static String encode(String s) {
        return s == null ? null : URLEncoding.encode(s);
    }

    @Override
    public void add(int index, String object) {
        super.add(index, encode(object));
    }

    @Override
    public boolean addAll(int index, Collection<? extends String> collection) {
        return super.addAll(index, collection.stream().map(CustomCommandList::encode).collect(Collectors.toList()));
    }

    @Override
    public String get(int index) {
        return decode(super.get(index));
    }

    @Override
    public int indexOf(Object object) {
        return super.indexOf(
                object instanceof String ? encode((String) object) : object
        );
    }

    @Override
    public int lastIndexOf(Object object) {
        return super.lastIndexOf(
                object instanceof String ? encode((String) object) : object
        );
    }

    @Override
    public ListIterator<String> listIterator() {
        return new CustomCommandIterator(super.listIterator());
    }

    @Override
    public ListIterator<String> listIterator(int index) {
        return new CustomCommandIterator(super.listIterator(index));
    }

    @Override
    public String remove(int index) {
        return decode(super.remove(index));
    }

    @Override
    public String set(int index, String object) {
        return decode(super.set(index, encode(object)));
    }

    @Override
    public List<String> subList(int fromIndex, int toIndex) {
        return new CustomCommandList(super.subList(fromIndex, toIndex));
    }

    @Override
    public boolean contains(Object object) {
        return super.contains(object);
    }

    @Override
    public Iterator<String> iterator() {
        return new CustomCommandIterator(super.listIterator());
    }

    @Override
    public boolean remove(Object object) {
        return super.remove(object);
    }

    @Override
    public Object[] toArray() {
        return readOnlyCopy().toArray();
    }

    @Override
    public <T> T[] toArray(T[] object) {
        return readOnlyCopy().toArray(object);
    }

    @Override
    public String toString() {
        return readOnlyCopy().toString();
    }

    private List<String> readOnlyCopy() {
        return new ArrayList<>(this);
    }

    private static class CustomCommandIterator extends AbstractListIteratorDecorator<String> {
        public CustomCommandIterator(ListIterator<String> iterator) {
            super(iterator);
        }

        @Override
        public String next() {
            return decode(super.next());
        }

        @Override
        public String previous() {
            return decode(super.previous());
        }

        @Override
        public void set(String obj) {
            super.set(encode(obj));
        }

        @Override
        public void add(String obj) {
            super.add(encode(obj));
        }
    }
}
