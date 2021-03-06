package io.topiacoin.model.provider.sqlite;

import io.topiacoin.core.Configuration;
import io.topiacoin.core.impl.DefaultConfiguration;
import io.topiacoin.model.DataModel;
import io.topiacoin.model.provider.DataModelFileChunkTest;

import java.io.File;

public class SqliteDataModelFileChunkTest extends DataModelFileChunkTest {

	private String sqliteDbLoc = "target/testdbs/sqlitedb-messagetest.db";

	public String getDBLoc() {
		return sqliteDbLoc;
	}

	@Override public DataModel initDataModel() {
		Configuration config = new DefaultConfiguration();
		config.setConfigurationOption("model.storage.type", "Sqlite");
		config.setConfigurationOption("model.sqllite.location", getDBLoc());
		File f = new File(getDBLoc());
		if(f.exists()) {
			f.delete();
			if(f.exists()) {
				throw new RuntimeException("Couldn't delete");
			}
		}
		DataModel.initialize(config);
		return DataModel.getInstance();
	}

	@Override public void tearDownDataModel() {
		DataModel.getInstance().close();
		File f = new File(getDBLoc());
		if(f.exists()) {
			f.delete();
			if(f.exists()) {
				throw new RuntimeException("Couldn't delete");
			}
		}
	}
}
