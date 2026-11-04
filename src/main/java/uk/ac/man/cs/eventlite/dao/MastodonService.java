package uk.ac.man.cs.eventlite.dao; 

import com.google.gson.Gson;
import com.sys1yagi.mastodon4j.MastodonClient;
import okhttp3.OkHttpClient;
import org.springframework.stereotype.Service;
import com.sys1yagi.mastodon4j.MastodonRequest;
import com.sys1yagi.mastodon4j.api.Pageable;
import com.sys1yagi.mastodon4j.api.Range;
import com.sys1yagi.mastodon4j.api.entity.Status;
import com.sys1yagi.mastodon4j.api.method.Statuses;
import com.sys1yagi.mastodon4j.api.method.Timelines;
import com.sys1yagi.mastodon4j.api.exception.Mastodon4jRequestException;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
public class MastodonService {

    private MastodonClient client;
// object and access token
    public MastodonService() {
        
        
        this.client = new MastodonClient.Builder("mstdn.social", new OkHttpClient.Builder(), new Gson())
                .accessToken(System.getenv().getOrDefault("MASTODON_ACCESS_TOKEN", ""))
                .useStreamingApi()
                .build();
    }
    
 
    public List<Status> getRecentTimeline() {
        
        Timelines timelines = new Timelines(this.client);
        
        try {
            MastodonRequest<Pageable<Status>> request = timelines.getHome(new Range());
            
            Pageable<Status> pageable = request.execute();

            List<Status> allPosts = pageable.getPart();
            
            //return allPosts; 
            return allPosts.stream()
                    .filter(s -> s.getAccount().getUserName().equals("manc_eventlite_g17"))
                    .limit(3)
                    .collect(Collectors.toList());
            

        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    public String shareEvent(String content) {

        Statuses statuses = new Statuses(this.client);
        
        try {

            Status status = statuses.postStatus(content, null, null, false, null, Status.Visibility.Unlisted).execute();
            
            return "Your post: " + status.getContent() + " was posted"; 
        } catch (Exception e) {
            e.printStackTrace();
            return "Error posting to Mastodon";
        }
    }

    
}