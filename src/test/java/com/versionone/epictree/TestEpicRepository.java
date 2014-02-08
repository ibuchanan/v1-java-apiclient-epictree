package com.versionone.epictree;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

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

	private Asset createNewEpic(Asset parentProject, String name) {
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
		return newEpic;
	}

	private Asset createNewChildEpic(Asset parentProject, Asset parentEpic, String name) {
		IAssetType epicType = cx.getMetaModel().getAssetType("Epic");
		IAttributeDefinition nameAttr = epicType.getAttributeDefinition("Name");
		IAttributeDefinition superAttr = epicType.getAttributeDefinition("Super");
		Asset newEpic = null;
		try {
			newEpic = cx.getServices().createNew(epicType, parentProject.getOid());
			newEpic.setAttributeValue(nameAttr, name);
			newEpic.setAttributeValue(superAttr, parentEpic.getOid());
			cx.getServices().save(newEpic);
		} catch (V1Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return newEpic;
	}

	
	@Test
	public void new_repository_is_dirty() {
        // When I create a new repository with that connection
		IEpicRepository repository = new EpicRepositoryApiClient(cx);
        // Then it is initially dirty
		boolean dirty = false;
		try {
			dirty = repository.isDirty();
		} catch (V1RepositoryException e) {
			fail(e.getMessage());
		}
		assertTrue(dirty);
	}

	@Test
	public void query_for_epics_is_scoped_to_epic() {
		// Given a new repository with the connection
		EpicRepositoryApiClient repository = new EpicRepositoryApiClient(cx);
		// When I build the query for epics
		IAssetType assetType = cx.getMetaModel().getAssetType(repository.getAssetTypeName());
		Query q = new Query(assetType);
		q = repository.prepareQueryForData(q);
		// Then the asset type is Epic
		assertEquals("Epic", q.getAssetType().getToken());
	}
	
	@Test
	public void query_for_epics_selects_name() {
		// Given a new repository with the connection
		EpicRepositoryApiClient repository = new EpicRepositoryApiClient(cx);
		// And a reference to the Epic asset type
		IAssetType expectedType = cx.getMetaModel().getAssetType("Epic");
		// And a reference to the Name attribute
		IAttributeDefinition targetAttribute = expectedType.getAttributeDefinition("Name");
		// When I build the query for request categories
		IAssetType assetType = cx.getMetaModel().getAssetType(repository.getAssetTypeName());
		Query q = new Query(assetType);
		q = repository.prepareQueryForData(q);
		// Then the query selects the Name attribute
		assertTrue(q.getSelection().contains(targetAttribute));
	}

	@Test
	public void query_for_epics_selects_number() {
		// Given a new repository with the connection
		EpicRepositoryApiClient repository = new EpicRepositoryApiClient(cx);
		// And a reference to the Epic asset type
		IAssetType expectedType = cx.getMetaModel().getAssetType("Epic");
		// And a reference to the Number attribute
		IAttributeDefinition targetAttribute = expectedType.getAttributeDefinition("Number");
		// When I build the query for request categories
		IAssetType assetType = cx.getMetaModel().getAssetType(repository.getAssetTypeName());
		Query q = new Query(assetType);
		q = repository.prepareQueryForData(q);
		// Then the query selects the Number attribute
		assertTrue(q.getSelection().contains(targetAttribute));
	}

	@Test
	public void prepared_query_change_detection_selects_change_date() {
		// Given a new repository with the connection
		EpicRepositoryApiClient repository = new EpicRepositoryApiClient(cx);
		// And a reference to the Epic asset type
		IAssetType expectedType = cx.getMetaModel().getAssetType("Epic");
		// And a reference to the ChangeDateUTC attribute
		IAttributeDefinition targetAttribute = expectedType.getAttributeDefinition("ChangeDateUTC");
		// When I build the query for request categories
		IAssetType assetType = cx.getMetaModel().getAssetType(repository.getAssetTypeName());
		Query q = new Query(assetType);
		q = repository.prepareQueryForChangeDetection(q);
		// Then the query selects the ChangeDateUTC attribute
		assertTrue(q.getSelection().contains(targetAttribute));
	}
	
	@Test
	public void prepared_query_for_structure_selects_super() {
		// Given a new repository with the connection
		EpicRepositoryApiClient repository = new EpicRepositoryApiClient(cx);
		// And a reference to the Epic asset type
		IAssetType expectedType = cx.getMetaModel().getAssetType("Epic");
		// And a reference to the Super attribute
		IAttributeDefinition targetAttribute = expectedType.getAttributeDefinition("Super");
		// When I build the query for request categories
		IAssetType assetType = cx.getMetaModel().getAssetType(repository.getAssetTypeName());
		Query q = new Query(assetType);
		q = repository.prepareQueryForStructure(q);
		// Then the query selects the Super attribute
		assertTrue(q.getSelection().contains(targetAttribute));
	}

	@Test
	public void detect_a_new_epic_as_a_change_since_change_date() {
        // Given a new repository with the connection
		EpicRepositoryApiClient repository = new EpicRepositoryApiClient(cx);
		// And the most recent change date from all the epics
		IAssetType assetType = cx.getMetaModel().getAssetType(repository.getAssetTypeName());
		Query q = new Query(assetType);
		q = repository.prepareQueryForChangeDetection(q);
		DateTime mostRecentChange = findMostRecentChangeDate(q);
		// When I add a new epic
		createNewEpic(myTestProject, "New Epic");
        // Then a change is detected
		boolean hasChanged = false;
		try {
			hasChanged = repository.areThereChangedEpicsAfter(mostRecentChange);
		} catch (V1RepositoryException e) {
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
		} catch (V1RepositoryException e) {
			fail(e.getMessage());
		}
		// Then the repository is not dirty
		boolean dirty = false;
		try {
			dirty = repository.isDirty();
		} catch (V1RepositoryException e) {
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
		String epicOid = createNewEpic(myTestProject, epicName).getOid().getMomentless().getToken();
		// When I retrieve the epics
		Map<String, Epic> epics = null;
		try {
			epics = repository.retrieve();
		} catch (V1RepositoryException e) {
			fail(e.getMessage());
		}
		// Then the results include the new epic
		assertTrue(epics.containsKey(epicOid));
	}

	@Test
	public void retreiving_epics_includes_the_name_of_my_new_epic() {
        // Given a new repository with the connection
		EpicRepositoryApiClient repository = new EpicRepositoryApiClient(cx);
		// And a new, uniquely-named epic
		String epicName = "Unique Epic " + DateTime.now().toString();
		String epicOid = createNewEpic(myTestProject, epicName).getOid().getMomentless().getToken();
		// When I retrieve the epics
		Map<String, Epic> epics = null;
		try {
			epics = repository.retrieve();
		} catch (V1RepositoryException e) {
			fail(e.getMessage());
		}
		// Then the results include the new epic
		assertEquals(epicName, epics.get(epicOid).name);
	}

	@Test
	public void retreiving_epics_includes_the_parent_epic_of_my_new_child_epic() {
        // Given a new repository with the connection
		EpicRepositoryApiClient repository = new EpicRepositoryApiClient(cx);
		// And a new, uniquely-named epic
		String epicName = "Unique Epic " + DateTime.now().toString();
		Asset parentEpic = createNewEpic(myTestProject, epicName);
		// And a new child epic under that
		String epicOid = createNewChildEpic(myTestProject, parentEpic, epicName).getOid().getMomentless().getToken();
		// When I retrieve the epics
		Map<String, Epic> epics = null;
		try {
			epics = repository.retrieve();
		} catch (V1RepositoryException e) {
			fail(e.getMessage());
		}
		// Then the results include the new epic
		String expected = parentEpic.getOid().getMomentless().getToken(); 
		assertEquals(expected, epics.get(epicOid).parent);
	}

}
