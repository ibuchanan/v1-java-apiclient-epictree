package com.versionone.epictree;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import com.versionone.DB.DateTime;
import com.versionone.Oid;
import com.versionone.apiclient.*;

public class EpicRepositoryApiClient implements IEpicRepository {

	private DateTime mostRecentChangeDateTime;
	private EnvironmentContext cx;
	private static final DateFormat V1STYLE = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
	private Query query;
	private Map<String, Epic> dtoCache;

	private IAssetType epicType;
	private IAttributeDefinition nameAttribute;
	private IAttributeDefinition numberAttribute;
	private IAttributeDefinition changeAttribute;
	private IAttributeDefinition parentAttribute;

	public EpicRepositoryApiClient(EnvironmentContext cx) {
		this.cx = cx;
		mostRecentChangeDateTime = null;
		query = prepareQueryForEpics();
		query = prepareQueryForChangeDetection(query);
	}

	public boolean isDirty() throws EpicRepositoryException {
		if (null == mostRecentChangeDateTime) {
			return true;
		}
		return false;
	}
	
	public Query prepareQueryForEpics() {
		epicType = cx.getMetaModel().getAssetType("Epic");
		nameAttribute = epicType.getAttributeDefinition("Name");
		numberAttribute = epicType.getAttributeDefinition("Number");
		parentAttribute = epicType.getAttributeDefinition("Super");
		Query q = new Query(epicType);
		q.getSelection().add(nameAttribute);
		q.getSelection().add(numberAttribute);
		q.getSelection().add(parentAttribute);
		return q;
	}
	
	public Query prepareQueryForChangeDetection(Query q) {
		changeAttribute = epicType.getAttributeDefinition("ChangeDateUTC");
		q.getSelection().add(changeAttribute);
		return q;
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
			result = cx.getServices().retrieve(query);
			dtoCache = new HashMap<String, Epic>(result.getTotalAvaliable());
			for (Asset asset : result.getAssets()) {
				DateTime changeDateTime = null;
				dtoCache.put(asset.getOid().getToken(), convertToEpic(asset));
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
		return dtoCache;
	}
	
	public Epic convertToEpic(Asset a) throws APIException, MetaException {
		Epic e = new Epic();
		e.oid = a.getOid().getToken();
		e.number = (String)a.getAttribute(numberAttribute).getValue();
		e.name = (String)a.getAttribute(nameAttribute).getValue();
		e.parentEpic = ((Oid)a.getAttribute(parentAttribute).getValue()).getToken();
		return e;
	}
}
