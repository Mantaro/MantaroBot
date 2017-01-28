package com.marcomaldonado.web.callback;

public interface Callback {

	void onFailure(int error, String message);

	void onStart();

}
