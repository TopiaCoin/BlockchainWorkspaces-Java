package io.topiacoin.model.provider.sqlite;

import io.topiacoin.core.Configuration;
import io.topiacoin.core.impl.DefaultConfiguration;
import io.topiacoin.model.DataModel;
import io.topiacoin.model.provider.DataModelFileTest;

public class SqliteDataModelFileTest extends DataModelFileTest {
	@Override public DataModel initDataModel() {
		Configuration config = new DefaultConfiguration();
		config.setConfigurationOption("model.storage.type", "Sqlite");
		DataModel.initialize(config);
		return DataModel.getInstance();
	}
}
