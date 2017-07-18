package net.kodehawa.lib.customfunc;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.iterators.ArrayIterator;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

@Slf4j
public class CustomFunc {
    private static Object EMPTY_ARRAY = new Object[0];

    @SuppressWarnings("unchecked")
    public static String embeddedEval(String string, Environiment env) {
        StringBuilder builder = new StringBuilder();
        int i;
        while((i = string.indexOf("$")) != -1) {
            builder.append(string.substring(0, i));

            string = string.substring(i);
            char[] array = string.toCharArray();

            if(array.length > 1 && array[1] == '(') {
                boolean valid = false;
                i = 2;
                for(; i < array.length; i++) {
                    char c = array[i];
                    if(c == ')') {
                        valid = true;
                        break;
                    }
                    if(!Character.isJavaIdentifierPart(c) && c != '.') {
                        break;
                    }
                }

                if(valid) {
                    i++;
                    String token = string.substring(2, i - 1);

                    builder.append(env.containsResolvedToken(token) ? env.getResolvedToken(token) : token);
                } else {
                    builder.append("$(");
                    i = 2;
                }
            } else {
                boolean valid = false;
                i = 1;
                for(; i < array.length; i++) {
                    char c = array[i];
                    if(c == '{') {
                        valid = true;
                        break;
                    }
                    if(!Character.isJavaIdentifierPart(c) && c != '.') {
                        break;
                    }
                }

                if(valid) {
                    int pCount = 0;
                    i++;

                    for(; i < array.length; i++) {
                        char c = array[i];
                        if(c == '{') pCount++;
                        if(c == '}') pCount--;
                        if(pCount == -1) break;
                    }

                    i++;

                    Object o = parseCode(string.substring(0, i), env);

                    if(o instanceof Object[]) {
                        o = new ArrayIterator<>(o);
                    }

                    if(o instanceof Iterator) {
                        List<Object> objects = new LinkedList<>();
                        ((Iterator<Object>) o).forEachRemaining(objects::add);
                        o = objects;
                    }

                    builder.append(o);
                } else {
                    builder.append("$");
                    i = 1;
                }
            }
            string = string.substring(i);
        }

        builder.append(string);

        return builder.toString();
    }

    public static char escape(char c) {
        switch(c) {
            case 'n':
                return '\n';
            case 'r':
                return '\r';
            case 't':
                return '\t';
            default:
                return c;
        }
    }

    public static void main(String[] args) {
        try {
            System.out.print("Parsing...");
            long millis = -System.currentTimeMillis();
            Object result = parseCode("${}", Environiment.of(Collections.emptyMap(), Collections.emptyMap()));
            millis += System.currentTimeMillis();
            System.out.println(" took " + millis + " ms");
            System.out.println("\n\nResult: " + result);
        } catch(Exception e) {
            System.out.print("Exception!");
            System.out.flush();
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public static Object[] parseArgs(String args, Environiment env) {
        List<Object> objects = new LinkedList<>();
        char[] array = args.toCharArray();
        for(int i = 0; i < array.length; i++) {
            char c = array[i];

            //region HANDLE SPACES
            switch(c) {
                case ' ':
                case '\r':
                case '\n':
                case '\t':
                    continue;
                default:
                    break;
            }
            //endregion

            //region OPERATIONS $...{...} [...] "..." 0.0
            if(c == '$') {
                //region OPERATION $...{...}
                StringBuilder functionBlock = new StringBuilder("$");
                i++;

                for(; i < array.length; i++) {
                    c = array[i];
                    if(c == '{') {
                        break;
                    }
                    if(!Character.isJavaIdentifierPart(c)) throw new CustomFuncException(
                            "Invalid character '" + c + "' near '" + near(args, i) + "'"
                    );

                    functionBlock.append(c);
                }

                int pCount = 0;
                functionBlock.append('{');
                i++;
                for(; i < array.length; i++) {
                    c = array[i];
                    if(c == '{') pCount++;
                    if(c == '}') pCount--;
                    if(pCount == -1) break;
                    functionBlock.append(c);
                }
                functionBlock.append("}");
                i++;

                if(pCount != -1) throw new CustomFuncException(
                        "Unbalanced brackets near '" + near(args, i) + "'"
                );

                Object result = parseCode(functionBlock.toString(), env);

                if(result instanceof Iterator) {
                    ((Iterator) result).forEachRemaining(objects::add);
                } else {
                    objects.add(result);
                }
                //endregion
            } else if(c == '[') {
                //region OPERATION [...]
                int pCount = 0;
                StringBuilder functionBlock = new StringBuilder();
                i++;

                for(; i < array.length; i++) {
                    c = array[i];
                    if(c == '[') pCount++;
                    if(c == ']') pCount--;
                    if(pCount == -1) break;
                    functionBlock.append(c);
                }
                i++;

                if(pCount != -1) throw new CustomFuncException(
                        "Unbalanced squared brackets near '" + near(args, i) + "'"
                );

                String block = functionBlock.toString().trim();

                if(block.isEmpty()) {
                    objects.add(EMPTY_ARRAY);
                } else {
                    objects.add(parseArgs(block, env));
                }
                //endregion
            } else if(c == '"' || c == '\'') {
                //region OPERATION "..."
                boolean invalid = true, escaping = false;
                StringBuilder s = new StringBuilder();

                char closeChar = c;
                i++;

                for(; i < array.length; i++) {
                    c = array[i];

                    if(escaping) {
                        escaping = false;
                        s.append(escape(c));
                        continue;
                    }

                    if(c == closeChar) {
                        invalid = false;
                        break;
                    }

                    if(c == '\\') {
                        escaping = true;
                        continue;
                    }

                    s.append(c);
                }

                if(invalid) throw new CustomFuncException(
                        "Unclosed quote near '" + near(args, i) + "'");

                objects.add(s.toString());
                i++;
                //endregion
            } else if(Character.isDigit(c) || c == '#' || c == '-') {
                //region OPERATION NUMBERS
                StringBuilder num = new StringBuilder();

                loop:
                for(; i < array.length; i++) {
                    c = array[i];

                    switch(c) {
                        case '\r':
                        case '\n':
                        case ' ':
                        case '\t':
                        case ',':
                            i--;
                            break loop;
                    }

                    num.append(c);
                }

                String number = num.toString();
                i++;

                try {
                    objects.add(Long.decode(number));
                } catch(NumberFormatException e) {
                    try {
                        objects.add(Double.valueOf(number));
                    } catch(NumberFormatException e2) {
                        throw new CustomFuncException(
                                "'" + number + "' is not a valid number near '" + near(args, i) + "'"
                        );
                    }
                }
                //endregion
            }
            //endregion
            else if(Character.isJavaIdentifierStart(c)) {
                StringBuilder name = new StringBuilder();

                loop:
                for(; i < array.length; i++) {
                    switch(c = array[i]) {
                        case ' ':
                        case '\r':
                        case '\n':
                        case '\t':
                            break loop;
                        case ',':
                            break loop;
                        default: {
                            if(Character.isJavaIdentifierPart(c) || c == '.') {
                                name.append(c);
                            } else throw new CustomFuncException(
                                    "Invalid character '" + c + "' near '" + near(args, i) + "'");
                            break;
                        }
                    }
                }

                String block = name.toString();
                objects.add(env.containsResolvedToken(block) ? env.getResolvedToken(block) : new Token(block));
            } else {
                throw new CustomFuncException("Invalid character '" + c + "' near '" + near(args, i) + "'");
            }

            loop:
            for(; i < array.length; i++) {
                switch(c = array[i]) {
                    case ' ':
                    case '\r':
                    case '\n':
                    case '\t':
                        continue;
                    case ',':
                        break loop;
                    default:
                        throw new CustomFuncException(
                                "Invalid character '" + c + "' near '" + near(args, i) + "'");
                }
            }
        }

        return objects.toArray();
    }

    public static Object parseCode(String code, Environiment env) {
        if(!code.startsWith("$")) throw new IllegalStateException("Must begin with $");
        if(!code.endsWith("}")) throw new IllegalStateException("Must end with }");
        if(!code.contains("{")) throw new IllegalStateException("Must have {");

        int indexOf = code.indexOf('{');

        String functionName = code.substring(1, indexOf);

        if(!env.containsFunction(functionName)) return "`" + functionName + " isn't a function`";
        CustomFunction f = env.getFunction(functionName);

        try {
            return f.run(parseArgs(code.substring(indexOf + 1, code.length() - 1), env));
        } catch(CustomFuncException cfe) {
            return "Error on `" + functionName + "`:" + cfe.getMessage();
        } catch(Exception e) {
            String errorCode = String.valueOf((Long.hashCode(System.currentTimeMillis()) ^ code.hashCode()));
            log.error("Error on CustomFunc (Error Code: `" + errorCode + "`):", e);
            return "Unladen Error `" + errorCode + "` on `" + functionName + "`. Report to Devs.";
        }
    }

    private static String near(String string, int i) {
        int bot = i - 9, top = i + 9;
        int nBot = Math.max(0, bot), nTop = Math.min(string.length(), top);

        StringBuilder s = new StringBuilder("``");
        if(bot == nBot) s.append("...");
        s.append(string.substring(nBot + (bot == nBot ? 3 : 0), nTop - (top == nTop ? 3 : 0)));
        if(top == nTop) s.append("...");
        return s.append("``").toString();
    }
}
