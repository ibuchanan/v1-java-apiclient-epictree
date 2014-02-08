package com.versionone.epictree;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import com.versionone.DB.DateTime;
import com.versionone.apiclient.*;

public class EpicRepositoryApiClient implements IEpicRepository {

	private DateTime mostRecentChangeDateTime;
	private EnvironmentContext cx;
	private IAssetType epicType;
	private IAttributeDefinition nameAttribute;
	private IAttributeDefinition changeAttribute;
	private static final DateFormat V1STYLE = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
	private Query queryForEpics;
	private List<String> allEpics;

	public EpicRepositoryApiClient(EnvironmentContext cx) {
		this.cx = cx;
		mostRecentChangeDateTime = null;
		epicType = cx.getMetaModel().getAssetType("Epic");
		nameAttribute = epicType.getAttributeDefinition("Name");
		changeAttribute = epicType.getAttributeDefinition("ChangeDateUTC");
		queryForEpics = buildQueryForEpics();
	}

	public boolean isDirty() throws EpicRepositoryException {
		if (null == mostRecentChangeDateTime) {
			return true;
		}
		return false;
	}

	public Query buildQueryForEpics() {
		Query query = new Query(epicType);
		query.getSelection().add(nameAttribute);
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

	public void reload() throws EpicRepositoryException {
		allEpics = new ArrayList<String>();
		QueryResult result;
		try {
			result = cx.getServices().retrieve(queryForEpics);
			for (Asset asset : result.getAssets()) {
				DateTime changeDateTime = null;
				allEpics.add((String)asset.getAttribute(nameAttribute).getValue());
                // Remember the most recent change to VersionOne for checking dirty state
				changeDateTime = new DateTime(asset.getAttribute(changeAttribute).getValue());
	            if ((null==mostRecentChangeDateTime) || (changeDateTime.compareTo(mostRecentChangeDateTime) > 0)) {
	                mostRecentChangeDateTime = changeDateTime;
	            }
			}
		} catch (ConnectionException e) {
			throw new EpicRepositoryException(e);
		} catch (APIException e) {
			throw new EpicRepositoryException(e);
		} catch (OidException e) {
			throw new EpicRepositoryException(e);
		}
	}

	public List<String> retreiveEpics() throws EpicRepositoryException {
		if (isDirty()) {
			reload();
        }
		return allEpics;
	}
}
