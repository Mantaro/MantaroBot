package com.marcomaldonado.konachan.entities;

import java.io.Serializable;

/**
 * Created by Mxrck on 05/12/15.
 */
public class Tag implements Serializable {

	private static final long serialVersionUID = 7884285067021077468L;
	private boolean ambiguos;
	private int count;
	private int id;
	private String name;
	private int type;

	public Tag(int id, String name, int count, int type, boolean ambiguos) {
		this.id = id;
		this.name = name;
		this.count = count;
		this.type = type;
		this.ambiguos = ambiguos;
	}

	public int getCount() {
		return count;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public int getType() {
		return type;
	}

	public boolean isAmbiguos() {
		return ambiguos;
	}
}
