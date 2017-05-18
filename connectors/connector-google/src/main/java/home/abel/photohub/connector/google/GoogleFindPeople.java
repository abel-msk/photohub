package home.abel.photohub.connector.google;

import java.util.List;
import com.google.api.services.plus.Plus;
import com.google.api.services.plus.model.PeopleFeed;
import com.google.api.services.plus.model.Person;

public class GoogleFindPeople {

	Plus plus = null;
	
	public GoogleFindPeople( Plus plus ) {
		this.plus = plus;
	}
	
	
	public void findConnected() throws Exception {
		// This sample assumes a client object has been created.
		// To learn more about creating a client, check out the starter:
		//  https://developers.google.com/+/quickstart/java

		Plus.People.List listPeople = plus.people().list( "me", "visible");
		listPeople.setMaxResults(5L);

		PeopleFeed peopleFeed = listPeople.execute();
		List<Person> people = peopleFeed.getItems();

		// Loop through until we arrive at an empty page
		while (people != null) {
		        for (Person person : people) {
		                System.out.println(person.getDisplayName());
		        }

		        // We will know we are on the last page when the next page token is
		        // null.
		        // If this is the case, break.
		        if (peopleFeed.getNextPageToken() == null) {
		                break;
		        }

		        // Prepare the next page of results
		        listPeople.setPageToken(peopleFeed.getNextPageToken());

		        // Execute and process the next page request
		        peopleFeed = listPeople.execute();
		        people = peopleFeed.getItems();
		}
	}
	
}
