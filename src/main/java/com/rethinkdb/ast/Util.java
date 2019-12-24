package com.rethinkdb.ast;

import com.rethinkdb.gen.ast.Datum;
import com.rethinkdb.gen.ast.Func;
import com.rethinkdb.gen.ast.Iso8601;
import com.rethinkdb.gen.ast.MakeArray;
import com.rethinkdb.gen.ast.MakeObj;
import com.rethinkdb.gen.ast.ReqlExpr;
import com.rethinkdb.gen.exc.ReqlDriverCompileError;
import com.rethinkdb.gen.exc.ReqlDriverError;
import com.rethinkdb.model.Arguments;
import com.rethinkdb.model.MapObject;
import com.rethinkdb.model.ReqlLambda;
import net.kodehawa.mantarobot.utils.Mapifier;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class Util {
    private Util() {
    }
    
    /**
     * Coerces objects from their native type to ReqlAst
     *
     * @param val val
     * @return ReqlAst
     */
    public static ReqlAst toReqlAst(Object val) {
        return toReqlAst(val, 100);
    }
    
    public static ReqlExpr toReqlExpr(Object val) {
        ReqlAst converted = toReqlAst(val);
        if(converted instanceof ReqlExpr) {
            return (ReqlExpr) converted;
        } else {
            throw new ReqlDriverError("Cannot convert %s to ReqlExpr", val);
        }
    }
    
    /**
     * Converts a POJO to a map of its public properties collected using bean introspection.<br>
     * The POJO's class must be public, or a ReqlDriverError would be thrown.<br>
     * Numeric properties should be Long instead of Integer
     *
     * @param pojo POJO to be introspected
     * @return Map of POJO's public properties
     */
    private static Map<String, Object> toMap(Object pojo) {
        try {
            return Mapifier.toMap(pojo);
        } catch(IllegalArgumentException e) {
            throw new ReqlDriverError("Can't convert %s to a ReqlAst: %s", pojo, e.getMessage());
        }
    }
    
    @SuppressWarnings("unchecked")
    private static ReqlAst toReqlAst(Object val, int remainingDepth) {
        if(remainingDepth <= 0) {
            throw new ReqlDriverCompileError("Recursion limit reached converting to ReqlAst");
        }
        if(val instanceof ReqlAst) {
            return (ReqlAst) val;
        }
        
        if(val instanceof Object[]) {
            Arguments innerValues = new Arguments();
            for(Object innerValue : (Object[]) val) {
                innerValues.add(toReqlAst(innerValue, remainingDepth - 1));
            }
            return new MakeArray(innerValues, null);
        }
        
        if(val instanceof List) {
            Arguments innerValues = new Arguments();
            for(Object innerValue : (List<?>) val) {
                innerValues.add(toReqlAst(innerValue, remainingDepth - 1));
            }
            return new MakeArray(innerValues, null);
        }
        
        if(val instanceof Map) {
            Map<String, ReqlAst> obj = new MapObject<>();
            for(Map.Entry<Object, Object> entry : ((Map<Object, Object>) val).entrySet()) {
                if(!(entry.getKey() instanceof String)) {
                    throw new ReqlDriverCompileError("Object keys can only be strings");
                }
                
                obj.put((String) entry.getKey(), toReqlAst(entry.getValue()));
            }
            return MakeObj.fromMap(obj);
        }
        
        if(val instanceof ReqlLambda) {
            return Func.fromLambda((ReqlLambda) val);
        }
        
        final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        
        if(val instanceof LocalDateTime) {
            ZoneId zid = ZoneId.systemDefault();
            DateTimeFormatter fmt2 = fmt.withZone(zid);
            return Iso8601.fromString(((LocalDateTime) val).format(fmt2));
        }
        if(val instanceof ZonedDateTime) {
            return Iso8601.fromString(((ZonedDateTime) val).format(fmt));
        }
        if(val instanceof OffsetDateTime) {
            return Iso8601.fromString(((OffsetDateTime) val).format(fmt));
        }
        
        if(val instanceof Number || val instanceof Boolean || val instanceof String) {
            return new Datum(val);
        }
        
        if(val == null) {
            return new Datum(null);
        }
        
        // val is a non-null POJO, let's introspect its public properties
        return toReqlAst(toMap(val));
    }
}
