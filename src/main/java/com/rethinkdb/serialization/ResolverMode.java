package com.rethinkdb.serialization;

import java.beans.ConstructorProperties;
import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.util.function.Function;

public enum ResolverMode implements Function<Parameter, String> {
	ANNOTATIONS(
		parameter -> parameter.getAnnotation(Value.class).value()
	),
	CONSTRUCTOR_PROPERTIES(
		parameter -> {
			Executable constructor = parameter.getDeclaringExecutable();
			Parameter[] parameters = constructor.getParameters();
			for (int i = 0; i < parameters.length; i++) {
				if (parameter.equals(parameters[i])) {
					return constructor.getAnnotation(ConstructorProperties.class).value()[i];
				}
			}

			return null;
		}
	),
	PARAMETER_NAMES(Parameter::getName);

	private final Function<Parameter, String> mapper;

	ResolverMode(Function<Parameter, String> mapper) {
		this.mapper = mapper;
	}

	@Override
	public String apply(Parameter parameter) {
		return mapper.apply(parameter);
	}
}
