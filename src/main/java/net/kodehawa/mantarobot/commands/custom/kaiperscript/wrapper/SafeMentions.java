package net.kodehawa.mantarobot.commands.custom.kaiperscript.wrapper;

import net.dv8tion.jda.core.entities.Member;
import xyz.avarel.kaiper.runtime.Bool;
import xyz.avarel.kaiper.runtime.Obj;
import xyz.avarel.kaiper.runtime.collections.Array;
import xyz.avarel.kaiper.runtime.java.JavaObject;

import java.util.List;

public class SafeMentions extends Array {
    public SafeMentions(List<Member> mentionedMembers) {
        mentionedMembers.stream()
            .map(SafeMember::new)
            .map(JavaObject::new)
            .forEach(this::add);
    }

    @Override
    public Obj getAttr(String name) {
        switch (name) {
            case "isEmpty":
                return Bool.of(isEmpty());
            case "first":
                return get(0);
            case "last":
                return get(size() - 1);
            default:
                return super.getAttr(name);
        }
    }
}
