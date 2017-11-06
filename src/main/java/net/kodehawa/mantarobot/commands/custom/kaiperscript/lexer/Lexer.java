package net.kodehawa.mantarobot.commands.custom.kaiperscript.lexer;

import xyz.avarel.kaiper.exceptions.SyntaxException;
import xyz.avarel.kaiper.lexer.Position;

import java.io.*;
import java.util.LinkedList;
import java.util.List;

public abstract class Lexer<T> {
    protected final List<T> tokens = new LinkedList<>();

    protected final Reader reader;
    protected final Entry[] history;
    protected int previous;
    protected boolean eof;

    protected long lineIndex;
    protected long index;
    protected long line;
    protected char current;

    public Lexer(InputStream inputStream) {
        this(new InputStreamReader(inputStream));
    }

    public Lexer(String s) {
        this(new StringReader(s));
    }

    public Lexer(Reader reader) {
        this(reader, 2);
    }

    public Lexer(Reader reader, int historyBuffer) {
        this.eof = false;
        this.reader = reader.markSupported() ? reader : new BufferedReader(reader);
        history = new Entry[historyBuffer];

        this.current = 0;
        this.index = -1;
        this.lineIndex = 0;
        this.line = 1;

        beforeReading();

        do {
            readTokens();
        } while (hasNext());

        afterReading();

        try {
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Make a printable string of this KaiperLexer.
     *
     * @return " at {index} [{character} : {line}]"
     */
    @Override
    public String toString() {
        return getPosition().toString();
    }

    protected abstract void readTokens();

    /**
     * Get and remove a token from the tokens list.
     *
     * @return The next token.
     */
    public T next() {
        return tokens.remove(0);
    }

    protected void push(T token) {
        if (token == null) throw new IllegalArgumentException();
        tokens.add(token);
    }

    /**
     * Get the current list of tokens.
     *
     * @return Current list of tokens in the lexer.
     */
    public List<T> getTokens() {
        return tokens;
    }

    protected T lastToken() {
        return tokens.get(tokens.size() - 1);
    }

    protected void beforeReading() {
    }

    protected void afterReading() {
    }

    public boolean hasNext() {
        return !(previous == 0 && this.eof);
    }

    // useful for lexer-phase desugaring
    protected void queue(char character) {
        System.arraycopy(history, previous + 1, history, 3, history.length - 3);
        history[previous] = new Entry(index, line, lineIndex, character);
        back();
    }

    /**
     * Get the readToken character in the source string.
     *
     * @return The readToken character, or 0 if past the end of the source string.
     */
    protected char advance() {
        int c;
        if (this.previous != 0) {
            this.previous--;

            Entry entry = history[previous];

            current = entry.character;
            index = entry.index;
            line = entry.line;
            lineIndex = entry.lineIndex;

            return this.current;
        } else {
            try {
                c = this.reader.read();
            } catch (IOException exception) {
                throw new SyntaxException("Exception occurred while lexing", getPosition(), exception);
            }

            if (c <= 0) { // End of stream
                this.eof = true;
                c = 0;
            }
        }

        this.index += 1;
        if (this.current == '\r') {
            this.line += 1;
            this.lineIndex = c == '\n' ? 0 : 1;
        } else if (c == '\n') {
            this.line += 1;
            this.lineIndex = 0;
        } else {
            this.lineIndex += 1;
        }
        this.current = (char) c;

        System.arraycopy(history, 0, history, 1, history.length - 1);

        history[0] = new Entry(index, line, lineIndex, current);

        return this.current;
    }

    /**
     * Consume the next character, and check that
     * it matches a specified character.
     *
     * @param c The character to match.
     * @return The character.
     */
    private char advance(char c) {
        char n = this.advance();
        if (n != c) {
            throw new SyntaxException("Expected '" + c + "' and instead saw '" + n + "'", getPosition());
        }
        return n;
    }

    /**
     * Get the next n characters.
     *
     * @param n The number of characters to take.
     * @return A string of n characters.
     * @throws SyntaxException Substring bounds error if there are not
     *                         n characters remaining in the source string.
     */
    protected String advance(int n) {
        if (n == 0) {
            return "";
        }

        char[] chars = new char[n];
        int pos = 0;

        while (pos < n) {
            chars[pos] = this.advance();
            if (!this.hasNext()) {
                throw new SyntaxException("Substring bounds error", getPosition());
            }
            pos += 1;
        }
        return new String(chars);
    }

    /**
     * Get the next char in the string, skipping whitespace.
     *
     * @return A character, or 0 if there are no more characters.
     */
    private char advanceClean() {
        while (true) {
            char c = this.advance();
            if (c == 0 || c > ' ') {
                return c;
            }
        }
    }

    /**
     * @return The next character.
     */
    protected char peek() {
        char c = advance();
        back();
        return c;
    }

    /**
     * Peek and advance if the prompt is the same as the peeked character.
     *
     * @param prompt The character to match.
     * @return if the prompt is the same as the peeked character.
     */
    protected boolean match(char prompt) {
        if (advance() == prompt) {
            return true;
        }
        back();
        return false;
    }

    /**
     * Back up one character. This provides a sort of lookahead capability,
     * so that you can test for a digit or letter before attempting to parse
     * the readToken number or identifier.
     */
    protected void back() {
        previous++;
    }

    /**
     * Get the text up but not including the specified character or the
     * end of line, whichever comes first.
     *
     * @param delimiter A delimiter character.
     * @return A string.
     */
    private String advanceTo(char delimiter) {
        StringBuilder sb = new StringBuilder();
        while (true) {
            char c = this.advance();
            if (c == delimiter || c == 0 || c == '\n' || c == '\r') {
                if (c != 0) {
                    this.back();
                }
                return sb.toString().trim();
            }
            sb.append(c);
        }
    }

    /**
     * Get the text up but not including one of the specified delimiter
     * characters or the end of line, whichever comes first.
     *
     * @param delimiters A set of delimiter characters.
     * @return A string, trimmed.
     */
    private String advanceTo(String delimiters) {
        char c;
        StringBuilder sb = new StringBuilder();
        while (true) {
            c = this.advance();
            if (delimiters.indexOf(c) >= 0 || c == 0 ||
                    c == '\n' || c == '\r') {
                if (c != 0) {
                    this.back();
                }
                return sb.toString().trim();
            }
            sb.append(c);
        }
    }

    /**
     * Skip characters until the readToken character is the requested character.
     * If the requested character is not found, no characters are skipped.
     *
     * @param to A character to skip to.
     * @return The requested character, or zero if the requested character
     * is not found.
     */
    private char skipTo(char to) {
        char c;
        try {
            long startIndex = this.index;
            long startCharacter = this.lineIndex;
            long startLine = this.line;
            this.reader.mark(1000000);
            do {
                c = this.advance();
                if (c == 0) {
                    this.reader.reset();
                    this.index = startIndex;
                    this.lineIndex = startCharacter;
                    this.line = startLine;
                    return c;
                }
            } while (c != to);
        } catch (IOException exception) {
            throw new SyntaxException("Exception occurred while lexing", getPosition(), exception);
        }
        this.back();
        return c;
    }

    /**
     * Return the current lexer's positional data.
     *
     * @return A plain object with position data.
     */
    public Position getPosition() {
        return new Position(index, line, lineIndex);
    }

    protected static final class Entry {
        private final long index;
        private final long line;
        private final long lineIndex;
        private final char character;

        private Entry(long index, long line, long lineIndex, char character) {
            this.index = index;
            this.line = line;
            this.lineIndex = lineIndex;
            this.character = character;
        }

        @Override
        public String toString() {
            return String.valueOf(character);
        }
    }
}
