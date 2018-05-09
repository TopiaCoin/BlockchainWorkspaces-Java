package io.topiacoin.dht.util;

import java.io.Serializable;

public class ASerializableObject implements Serializable {
	private final static long serialVersionUID = 1;
	String var;

	ASerializableObject(String s) {
		var = s;
	}
}
