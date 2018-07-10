package io.topiacoin.model.provider.sqlite;

import io.topiacoin.core.Configuration;
import io.topiacoin.core.impl.DefaultConfiguration;
import io.topiacoin.model.DataModel;
import io.topiacoin.model.provider.DataModelFileVersionTest;

import java.io.File;

public class SqliteDataModelFileVersionTest extends DataModelFileVersionTest {

	@Override public DataModel initDataModel() {
		Configuration config = new DefaultConfiguration();
		config.setConfigurationOption("model.storage.type", "Sqlite");
		config.setConfigurationOption("model.sqllite.location", "target/testdbs/sqlitedb-fileversiontest");
		File f = new File("target/sqlitedb");
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
	}
}
