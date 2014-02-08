package com.versionone.epictree;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.versionone.apiclient.*;

public class TestEpicRepository {
	
	private EnvironmentContext cx;
	
	@Before
	public void setupVersionOneInstance() {
		// Given a connection to a VersionOne instance
		try {
			cx = new EnvironmentContext();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	public void new_repository_is_dirty() {
        // When I create a new repository with that connection
		IEpicRepository repository = new EpicRepositoryApiClient(cx);
        // Then it is initially dirty
		boolean dirty = false;
		try {
			dirty = repository.isDirty();
		} catch (EpicRepositoryException e) {
			fail(e.getMessage());
		}
		assertTrue(dirty);
	}

	@Test
	public void query_for_epics_is_scoped_to_epic() {
		// Given a new repository with the connection
		EpicRepositoryApiClient repository = new EpicRepositoryApiClient(cx);
		// When I build the query for epics
		Query query = repository.buildQueryForEpics();
		// Then the asset type is Epic
		assertEquals("Epic", query.getAssetType().getToken());
	}
	
	@Test
	public void query_for_epics_selects_change_date() {
		// Given a new repository with the connection
		EpicRepositoryApiClient repository = new EpicRepositoryApiClient(cx);
		// And a reference to the Epic asset type
		IAssetType assetType = cx.getMetaModel().getAssetType("Epic");
		// And a reference to the ChangeDateUTC attribute
		IAttributeDefinition changeAttribute = assetType.getAttributeDefinition("ChangeDateUTC");
		// When I build the query for request categories
		Query query = repository.buildQueryForEpics();
		// Then the query selects the ChangeDateUTC attribute
		assertTrue(query.getSelection().contains(changeAttribute));
	}
}
