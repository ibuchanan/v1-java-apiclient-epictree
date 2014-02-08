package com.versionone.epictree;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import com.versionone.DB.DateTime;
import com.versionone.apiclient.*;

public class EpicRepositoryApiClient implements IEpicRepository {

	private DateTime mostRecentChangeDateTime;
	private EnvironmentContext cx;
	private IAssetType epicType;
	private IAttributeDefinition changeAttribute;
	private static final DateFormat V1STYLE = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

	public EpicRepositoryApiClient(EnvironmentContext cx) {
		this.cx = cx;
		mostRecentChangeDateTime = null;
		epicType = cx.getMetaModel().getAssetType("Epic");
		changeAttribute = epicType.getAttributeDefinition("ChangeDateUTC");
	}

	public boolean isDirty() throws EpicRepositoryException {
		if (null == mostRecentChangeDateTime) {
			return true;
		}
		return false;
	}

	public Query buildQueryForEpics() {
		Query query = new Query(epicType);
		query.getSelection().add(changeAttribute);
		return query;
	}

	public boolean areThereChangedEpicsAfter(DateTime thisDate) throws EpicRepositoryException {
		Query query = new Query(epicType);
		query.getSelection().add(changeAttribute);
		FilterTerm term = new FilterTerm(changeAttribute);
		term.greater(V1STYLE.format(thisDate.getValue()));
		query.setFilter(term);
		QueryResult result;
		try {
			result = cx.getServices().retrieve(query);
		} catch (ConnectionException e) {
			throw new EpicRepositoryException(e);
		} catch (APIException e) {
			throw new EpicRepositoryException(e);
		} catch (OidException e) {
			throw new EpicRepositoryException(e);
		}
		return result.getTotalAvaliable() > 0;
	}
}
