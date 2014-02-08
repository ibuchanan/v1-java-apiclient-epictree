package com.versionone.epictree;

import com.versionone.DB.DateTime;
import com.versionone.apiclient.EnvironmentContext;

public class EpicRepositoryApiClient implements IEpicRepository {
	private DateTime mostRecentChangeDateTime;
	
	public EpicRepositoryApiClient(EnvironmentContext cx) {
		mostRecentChangeDateTime = null;
	}

	public boolean isDirty() throws EpicRepositoryException {
		if (null==mostRecentChangeDateTime) {
			return true;
		}
        return false;
    }

}
