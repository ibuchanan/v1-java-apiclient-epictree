package com.versionone.epictree;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.versionone.DB.DateTime;
import com.versionone.Duration;
import com.versionone.Oid;
import com.versionone.apiclient.*;

public class TestEpicRepository {
	
	private EnvironmentContext cx;
	private IAssetType epicType;
	private IAttributeDefinition changeAttribute;
	private Oid topProject;
	private Asset aSchedule;
	private Asset myTestProject;

	@Before
	public void setupVersionOneInstance() {
		try {
			// Given a connection to a VersionOne instance
			cx = new EnvironmentContext();
			epicType = cx.getMetaModel().getAssetType("Epic");
			changeAttribute = epicType.getAttributeDefinition("ChangeDateUTC");
			// And any top level project
			topProject = getAnyTopLevelProject();
			// And an arbitrary schedule
			aSchedule = createNewArbitrarySchedule();
			// And an arbitrary project under the top level with the arbitrary schedule
			myTestProject = createNewArbitraryProject(topProject, aSchedule);			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private DateTime findMostRecentChangeDate(Query q) {
		DateTime mostRecentChangeDateTime = null;
		QueryResult result = null;
		try {
			result = cx.getServices().retrieve(q);
			for (Asset asset : result.getAssets()) {
				DateTime changeDateTime = null;
				changeDateTime = new DateTime(asset.getAttribute(changeAttribute).getValue());
				if ((null==mostRecentChangeDateTime) || (changeDateTime.compareTo(mostRecentChangeDateTime) > 0))
				{
					mostRecentChangeDateTime = changeDateTime;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return mostRecentChangeDateTime;
	}	
	
	private Oid getAnyTopLevelProject() {
		Oid rootProjectId = null;
		try {
			// Assumption: administrative user with access to System (All Projects)
			// TODO query to get a top-most project
			rootProjectId = Oid.fromToken("Scope:0", cx.getMetaModel());
		} catch (OidException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return rootProjectId;
	}
	
	private Asset createNewArbitrarySchedule() {
		IAssetType scheduleType = cx.getMetaModel().getAssetType("Schedule");
		IAttributeDefinition nameAttr = scheduleType.getAttributeDefinition("Name");
		IAttributeDefinition timeboxGapAttr = scheduleType.getAttributeDefinition("TimeboxGap");
		IAttributeDefinition timeboxLengthAttr = scheduleType.getAttributeDefinition("TimeboxLength");
		Asset newSchedule = null;
		try {
			newSchedule = cx.getServices().createNew(scheduleType, getAnyTopLevelProject());
			newSchedule.setAttributeValue(nameAttr, "EpicRepository Test Schedule");
			newSchedule.setAttributeValue(timeboxLengthAttr, Duration.parse("7 Days"));
			newSchedule.setAttributeValue(timeboxGapAttr, Duration.parse("0 Days"));
			cx.getServices().save(newSchedule);
		} catch (V1Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return newSchedule;
	}

	private Asset createNewArbitraryProject(Oid parentProject, Asset schedule) {
		IAssetType projectType = cx.getMetaModel().getAssetType("Scope");
		IAttributeDefinition nameAttr = projectType.getAttributeDefinition("Name");
		IAttributeDefinition parentAttr = projectType.getAttributeDefinition("Parent");
		IAttributeDefinition beginDateAttr = projectType.getAttributeDefinition("BeginDate");
		IAttributeDefinition scheduleAttr = projectType.getAttributeDefinition("Schedule");
		Asset newProject = null;
		try {
			newProject = cx.getServices().createNew(projectType, parentProject);
			newProject.setAttributeValue(nameAttr, "EpicRepository Test Project");
			newProject.setAttributeValue(parentAttr, parentProject);
			newProject.setAttributeValue(scheduleAttr, schedule.getOid());
			newProject.setAttributeValue(beginDateAttr, DateTime.now());
			cx.getServices().save(newProject);
		} catch (V1Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return newProject;
	}

	private Oid createNewEpic(Asset parentProject, String name) {
		IAssetType epicType = cx.getMetaModel().getAssetType("Epic");
		IAttributeDefinition nameAttr = epicType.getAttributeDefinition("Name");
		Asset newEpic = null;
		try {
			newEpic = cx.getServices().createNew(epicType, parentProject.getOid());
			newEpic.setAttributeValue(nameAttr, name);
			cx.getServices().save(newEpic);
		} catch (V1Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return newEpic.getOid();
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
	public void query_for_epics_selects_name() {
		// Given a new repository with the connection
		EpicRepositoryApiClient repository = new EpicRepositoryApiClient(cx);
		// And a reference to the Epic asset type
		IAssetType assetType = cx.getMetaModel().getAssetType("Epic");
		// And a reference to the Name attribute
		IAttributeDefinition targetAttribute = assetType.getAttributeDefinition("Name");
		// When I build the query for request categories
		Query query = repository.buildQueryForEpics();
		// Then the query selects the Name attribute
		assertTrue(query.getSelection().contains(targetAttribute));
	}

	@Test
	public void query_for_epics_selects_number() {
		// Given a new repository with the connection
		EpicRepositoryApiClient repository = new EpicRepositoryApiClient(cx);
		// And a reference to the Epic asset type
		IAssetType assetType = cx.getMetaModel().getAssetType("Epic");
		// And a reference to the Number attribute
		IAttributeDefinition targetAttribute = assetType.getAttributeDefinition("Number");
		// When I build the query for request categories
		Query query = repository.buildQueryForEpics();
		// Then the query selects the Number attribute
		assertTrue(query.getSelection().contains(targetAttribute));
	}

	@Test
	public void query_for_epics_selects_change_date() {
		// Given a new repository with the connection
		EpicRepositoryApiClient repository = new EpicRepositoryApiClient(cx);
		// And a reference to the Epic asset type
		IAssetType assetType = cx.getMetaModel().getAssetType("Epic");
		// And a reference to the ChangeDateUTC attribute
		IAttributeDefinition targetAttribute = assetType.getAttributeDefinition("ChangeDateUTC");
		// When I build the query for request categories
		Query query = repository.buildQueryForEpics();
		// Then the query selects the ChangeDateUTC attribute
		assertTrue(query.getSelection().contains(targetAttribute));
	}
	
	@Test
	public void detect_a_new_epic_as_a_change_since_change_date() {
        // Given a new repository with the connection
		EpicRepositoryApiClient repository = new EpicRepositoryApiClient(cx);
		// And the most recent change date from all the epics
		DateTime mostRecentChange = findMostRecentChangeDate(repository.buildQueryForEpics());
		// When I add a new epic
		createNewEpic(myTestProject, "New Epic");
        // Then a change is detected
		boolean hasChanged = false;
		try {
			hasChanged = repository.areThereChangedEpicsAfter(mostRecentChange);
		} catch (EpicRepositoryException e) {
			fail(e.getMessage());
		}
		assertTrue(hasChanged);
	}
	
	@Test
	public void reload_is_clean() {
        // Given a new repository with the connection
		EpicRepositoryApiClient repository = new EpicRepositoryApiClient(cx);
		// When I reload the repository
		try {
			repository.reload();
		} catch (EpicRepositoryException e) {
			fail(e.getMessage());
		}
		// Then the repository is not dirty
		boolean dirty = false;
		try {
			dirty = repository.isDirty();
		} catch (EpicRepositoryException e) {
			fail(e.getMessage());
		}
		assertFalse(dirty);
	}
	
	@Test
	public void retreiving_epics_includes_my_new_epic() {
        // Given a new repository with the connection
		EpicRepositoryApiClient repository = new EpicRepositoryApiClient(cx);
		// And a new, uniquely-named epic
		String epicName = "Unique Epic " + DateTime.now().toString();
		createNewEpic(myTestProject, epicName);
		// When I retrieve the epics
		List<String> epics = null;
		try {
			epics = repository.retreiveEpics();
		} catch (EpicRepositoryException e) {
			fail(e.getMessage());
		}
		// Then the results include the new epic
		assertTrue(epics.contains(epicName));
	}

}
