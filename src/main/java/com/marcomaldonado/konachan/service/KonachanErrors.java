package com.marcomaldonado.konachan.service;

class KonachanErrors {

	public static final int GENERIC_ERROR = 0;
	public static final int UNKNOW_ERROR = 1;

	public static String message(int error) {
		switch (error) {
			case 0:
				return "Error Genérico";
			case 1:
			default:
				return "Error desconocido";
		}
	}

}
