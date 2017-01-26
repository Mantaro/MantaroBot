/*
 * This class was created by <$user.name>. It's distributed as
 * part of the Miyuki Bot. Get the Source Code in github:
 * https://github.com/BRjDevs/Miyuki
 *
 * Miyuki is Open Source and distributed under the
 * GNU Lesser General Public License v2.1:
 * https://github.com/BRjDevs/Miyuki/blob/master/LICENSE
 *
 * File Created @ [16/11/16 13:58]
 */

package net.kodehawa.mantarobot.commands.custom;

import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class Holder<T> implements Supplier<T>, Consumer<T>, UnaryOperator<T> {
	public T var;

	public Holder() {
	}

	public Holder(T object) {
		var = object;
	}

	@Override
	public void accept(T t) {
		var = t;
	}

	@Override
	public T apply(T t) {
		T r = get();
		accept(t);
		return r;
	}

	@Override
	public T get() {
		return var;
	}

	@Override
	public int hashCode() {
		return var.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Holder) {
			return ((Holder) obj).var.equals(var);
		}

		return obj.equals(var);
	}

	@Override
	public String toString() {
		return var.toString();
	}

	public void accept(Holder<T> holder) {
		var = holder.var;
	}
}
