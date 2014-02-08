package com.versionone.epictree;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import com.versionone.DB.DateTime;
import com.versionone.Oid;
import com.versionone.apiclient.*;

public class EpicRepositoryApiClient implements IEpicRepository {

	private DateTime mostRecentChangeDateTime;
	private EnvironmentContext cx;
	private static final DateFormat V1STYLE = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
	private Query dataQuery;
	private Map<String, Epic> dtoCache;

	private IAssetType assetType;
	private IAttributeDefinition nameAttribute;
	private IAttributeDefinition numberAttribute;
	private IAttributeDefinition changeAttribute;
	private IAttributeDefinition parentAttribute;

	public EpicRepositoryApiClient(EnvironmentContext cx) {
		this.cx = cx;
		mostRecentChangeDateTime = null;
		assetType = cx.getMetaModel().getAssetType(getAssetTypeName());
		dataQuery = new Query(assetType);
		dataQuery = prepareQueryForData(dataQuery);
		dataQuery = prepareQueryForStructure(dataQuery);
		dataQuery = prepareQueryForChangeDetection(dataQuery);
	}

	public boolean isDirty() throws V1RepositoryException {
		if (null == mostRecentChangeDateTime) {
			return true;
		}
		return false;
	}
	
	public Query prepareQueryForData(Query q) {
		nameAttribute = assetType.getAttributeDefinition("Name");
		numberAttribute = assetType.getAttributeDefinition("Number");
		q.getSelection().add(nameAttribute);
		q.getSelection().add(numberAttribute);
		return q;
	}
	
	public Query prepareQueryForStructure(Query q) {
		parentAttribute = assetType.getAttributeDefinition(getParentAttributeName());
		q.getSelection().add(parentAttribute);
		return q;
	}
	
	public Query prepareQueryForChangeDetection(Query q) {
		changeAttribute = assetType.getAttributeDefinition("ChangeDateUTC");
		q.getSelection().add(changeAttribute);
		return q;
	}

	public boolean areThereChangedEpicsAfter(DateTime thisDate) throws V1RepositoryException {
		Query query = new Query(assetType);
		query.getSelection().add(changeAttribute);
		FilterTerm term = new FilterTerm(changeAttribute);
		term.greater(V1STYLE.format(thisDate.getValue()));
		query.setFilter(term);
		QueryResult result;
		try {
			result = cx.getServices().retrieve(query);
		} catch (ConnectionException e) {
			throw new V1RepositoryException(e);
		} catch (APIException e) {
			throw new V1RepositoryException(e);
		} catch (OidException e) {
			throw new V1RepositoryException(e);
		}
		return result.getTotalAvaliable() > 0;
	}

	public void reload() throws V1RepositoryException {
		QueryResult result;
		DateTime changeDateTime = null;
		try {
			result = cx.getServices().retrieve(dataQuery);
			dtoCache = new HashMap<String, Epic>(result.getTotalAvaliable());
			Queue<Asset> assetQueue = new ArrayDeque<Asset>(Arrays.asList(result.getAssets()));
			while (null!=assetQueue.peek()) {
				Asset asset = assetQueue.poll();
				Oid parent = (Oid)asset.getAttribute(parentAttribute).getValue();
				if (Oid.Null.equals(parent)) {
					Epic e = convert(asset);
					String oid = asset.getOid().getToken();
					e.pathname = e.name;
					dtoCache.put(oid, e);
					continue;
				}
				
				// Defer processing if the parent has not yet been mapped
				if (!dtoCache.containsKey(parent.getToken())) {
					assetQueue.add(asset);
					continue;
				}
				
				Epic e = convert(asset);
				String oid = asset.getOid().getToken();
				e.pathname = dtoCache.get(((Oid)asset.getAttribute(parentAttribute).getValue()).getToken()).pathname
						+ "\\" + e.name;
				dtoCache.get(parent.getToken()).children.add(e);
				dtoCache.put(oid, e);
				
                // Remember the most recent change to VersionOne for checking dirty state
				changeDateTime = new DateTime(asset.getAttribute(changeAttribute).getValue());
	            if ((null==mostRecentChangeDateTime) || (changeDateTime.compareTo(mostRecentChangeDateTime) > 0)) {
	                mostRecentChangeDateTime = changeDateTime;
	            }
			}
		} catch (ConnectionException e) {
			throw new V1RepositoryException(e);
		} catch (APIException e) {
			throw new V1RepositoryException(e);
		} catch (OidException e) {
			throw new V1RepositoryException(e);
		}
	}

	public Map<String, Epic> retrieve() throws V1RepositoryException {
		if (isDirty()) {
			reload();
        }
		return dtoCache;
	}
	
	public Epic convert(Asset a) throws APIException, MetaException {
		Epic e = new Epic();
		e.oid = a.getOid().getToken();
		e.number = (String)a.getAttribute(numberAttribute).getValue();
		e.name = (String)a.getAttribute(nameAttribute).getValue();
		e.parent = ((Oid)a.getAttribute(parentAttribute).getValue()).getToken();
		return e;
	}
	
	public String getAssetTypeName() {
		return "Epic";
	}
	
	public String getParentAttributeName() {
		return "Super";
	}
}
