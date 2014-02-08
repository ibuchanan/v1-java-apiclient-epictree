package com.versionone.epictree;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.versionone.DB.DateTime;
import com.versionone.Oid;
import com.versionone.apiclient.*;

public class EpicRepositoryApiClient implements IEpicRepository {

	private DateTime mostRecentChangeDateTime;
	private EnvironmentContext cx;
	private IAssetType epicType;
	private IAttributeDefinition nameAttribute;
	private IAttributeDefinition numberAttribute;
	private IAttributeDefinition changeAttribute;
	private IAttributeDefinition parentAttribute;
	private static final DateFormat V1STYLE = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
	private Query queryForEpics;
	private Map<String, Epic> allEpics;

	public EpicRepositoryApiClient(EnvironmentContext cx) {
		this.cx = cx;
		mostRecentChangeDateTime = null;
		epicType = cx.getMetaModel().getAssetType("Epic");
		nameAttribute = epicType.getAttributeDefinition("Name");
		numberAttribute = epicType.getAttributeDefinition("Number");
		changeAttribute = epicType.getAttributeDefinition("ChangeDateUTC");
		parentAttribute = epicType.getAttributeDefinition("Super");
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
		query.getSelection().add(numberAttribute);
		query.getSelection().add(changeAttribute);
		query.getSelection().add(parentAttribute);
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
		QueryResult result;
		try {
			result = cx.getServices().retrieve(queryForEpics);
			allEpics = new HashMap<String, Epic>(result.getTotalAvaliable());
			for (Asset asset : result.getAssets()) {
				DateTime changeDateTime = null;
				Epic e = new Epic();
				e.oid = asset.getOid().getToken();
				e.number = (String)asset.getAttribute(numberAttribute).getValue();
				e.name = (String)asset.getAttribute(nameAttribute).getValue();
				e.parentEpic = ((Oid)asset.getAttribute(parentAttribute).getValue()).getToken();
				allEpics.put(asset.getOid().getToken(), e);
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

	public Map<String, Epic> retreiveEpics() throws EpicRepositoryException {
		if (isDirty()) {
			reload();
        }
		return allEpics;
	}
}
