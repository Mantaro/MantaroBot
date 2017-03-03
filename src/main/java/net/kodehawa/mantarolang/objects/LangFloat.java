package net.kodehawa.mantarolang.objects;

public class LangFloat implements LangWrapped<Double> {
	private final double number;

	public LangFloat(double number) {
		this.number = number;
	}

	@Override
	public Double get() {
		return number;
	}

	@Override
	public String toString() {
		return "LFloat{" + number + '}';
	}
}
