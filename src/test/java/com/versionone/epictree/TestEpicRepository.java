package com.versionone.epictree;

import static org.junit.Assert.*;

import org.junit.Test;

import com.versionone.apiclient.*;

public class TestEpicRepository {

	@Test
	public void new_repository_is_dirty() {
		// Given a connection to a VersionOne instance
		EnvironmentContext cx = null;
		try {
			cx = new EnvironmentContext();
		} catch (Exception e) {
			fail(e.getMessage());
		}
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

}
