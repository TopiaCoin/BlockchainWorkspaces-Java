package io.topiacoin.chunks.impl;


import io.topiacoin.chunks.intf.ChunkTransferHandler;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * When the ChunkTransferer wants to spawn a Runnable to transfer a chunk, it uses this class to do so. This class can support multiple methodologies for
 * transferring Chunks
 */
public class ChunkTransferRunnableFactory {

	private Map<String, Class<?>> _runnableClasses = new HashMap<>();

	public ChunkTransferRunnableFactory() {
		addRunnable("TCP", "io.topiacoin.chunks.impl.transferRunnables.ChunkTransferTCPRunnable");
	}

	public Runnable getTransferRunnable(String identifier, ChunkTransferHandler handler, String location, String chunkID) {
		Class<?> clazz = _runnableClasses.get(identifier);
		Constructor<?> constructor;
		try {
			constructor = clazz.getConstructor(ChunkTransferHandler.class, String.class, String.class);
			Object instance = constructor.newInstance(handler, location, chunkID);
			return (Runnable)instance;
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (ClassCastException e) {
			e.printStackTrace();
		}
		return null;
	}

	private void addRunnable(String identifier, String fullyQualifiedClassName) {
		//Reflection, even in its best state, is but a necessary evil; in its worst state, an intolerable one
		try {
			Class<?> clazz = Class.forName(fullyQualifiedClassName);
			_runnableClasses.put(identifier, clazz);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
}
