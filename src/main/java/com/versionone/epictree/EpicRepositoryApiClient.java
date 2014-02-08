package com.versionone.epictree;

import com.versionone.DB.DateTime;
import com.versionone.apiclient.*;

public class EpicRepositoryApiClient implements IEpicRepository {
	
	private DateTime mostRecentChangeDateTime;
	private EnvironmentContext cx;
	private IAssetType epicType;
	
	public EpicRepositoryApiClient(EnvironmentContext cx) {
		this.cx = cx;
		mostRecentChangeDateTime = null;
		epicType = cx.getMetaModel().getAssetType("Epic");
	}

	public boolean isDirty() throws EpicRepositoryException {
		if (null==mostRecentChangeDateTime) {
			return true;
		}
		return false;
    }

	public Query buildQueryForEpics() {
		Query query = new Query(epicType);
		return query;
	}
}
