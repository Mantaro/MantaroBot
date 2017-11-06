/*
 *  Copyright 2017 An Tran and Adrian Todt
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.kodehawa.mantarobot.commands.custom.kaiperscript.lexer;

import xyz.avarel.kaiper.lexer.Position;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

public class KaiperScriptLexer extends Lexer<Token> {

    public KaiperScriptLexer(InputStream inputStream) {
        this(new InputStreamReader(inputStream));
    }

    public KaiperScriptLexer(String s) {
        this(new StringReader(s));
    }

    public KaiperScriptLexer(Reader reader) {
        this(reader, 2);
    }

    public KaiperScriptLexer(Reader reader, int historyBuffer) {
        super(reader, historyBuffer);
    }

    @Override
    protected void readTokens() {
        readOuter();
        if (!hasNext()) return;
        readInner();
    }


    private void readOuter() {
        StringBuilder sb = new StringBuilder();

        char c;

        while (true) {
            c = advance();

            if (c == 0) {
                break;
            }

            if (c != '<') {
                sb.append(c);
            } else {
                char c2 = advance();
                if (c2 == '$') {
                    break;
                } else {
                    sb.append(c).append(c2);
                }
            }
        }

        String value = sb.toString();
        if (!value.isEmpty()) push(make(TokenType.TEXT, value));
    }

    private void readInner() {
        StringBuilder sb = new StringBuilder();

        char c = advance();
        boolean equals = false, opt = false;

        while (Character.isSpaceChar(c) || c == '\t') c = advance();

        if (c == '=') {
            equals = true;
            c = advance();
            while (Character.isSpaceChar(c) || c == '\t') c = advance();
        } else if (c == '?') {
            c = advance();
            if (c == '=') {
                opt = true;
                equals = true;

                c = advance();
                while (Character.isSpaceChar(c) || c == '\t') c = advance();
            } else {
                back();
            }

        }

        back();

        while (true) {
            c = advance();

            if (c == 0) {
                break;
            }

            if (c != '$') {
                sb.append(c);
            } else {
                char c2 = advance();
                if (c2 == '>') {
                    break;
                } else {
                    sb.append(c).append(c2);
                }
            }
        }

        String value = sb.toString();
        if (!value.isEmpty()) push(make(equals ? opt ? TokenType.CODE_OPT_EQUALS : TokenType.CODE_EQUALS : TokenType.CODE, value));
    }

    private Token make(TokenType type) {
        return make(new Position(index, line, lineIndex), type);
    }

    private Token make(TokenType type, String value) {
        return make(new Position(index - value.length(), line, lineIndex - value.length()), type, value);
    }

    private Token make(Position position, TokenType type) {
        return new Token(position, type);
    }

    private Token make(Position position, TokenType type, String value) {
        return new Token(position, type, value);
    }
}
