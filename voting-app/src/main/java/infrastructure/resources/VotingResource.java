package infrastructure.resources;

import api.ElectionApi;
import api.dto.out.Election;
import org.jboss.resteasy.reactive.ResponseStatus;
import org.jboss.resteasy.reactive.RestResponse;

import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import org.jboss.logging.Logger;

@Produces(MediaType.APPLICATION_JSON)
@Path("/api/voting")
public class VotingResource {
    private static final Logger LOGGER = Logger.getLogger(VotingResource.class);
    private final ElectionApi api;

    public VotingResource(ElectionApi api) {
        this.api = api;
    }

    @GET
    public RestResponse<List<Election>> elections() {
        try {
            LOGGER.info("Fetching all elections");
            List<Election> elections = api.findAll();
            return RestResponse.ok(elections);
        } catch (Exception e) {
            LOGGER.error("Error fetching elections: " + e.getMessage(), e);
            return RestResponse.status(Response.Status.INTERNAL_SERVER_ERROR, "Error fetching elections");
        }
    }

    @POST
    @Path("/elections/{electionId}/candidates/{candidateId}")
    @ResponseStatus(RestResponse.StatusCode.ACCEPTED)
    @Transactional
    public RestResponse<String> vote(@PathParam("electionId") String electionId, 
                                     @PathParam("candidateId") String candidateId) {
        LOGGER.infof("Voting for candidate %s in election %s", candidateId, electionId);
        
        if (electionId == null || electionId.isEmpty()) {
            LOGGER.warn("Invalid election ID provided");
            return RestResponse.status(Response.Status.BAD_REQUEST, "Invalid election ID");
        }

        if (candidateId == null || candidateId.isEmpty()) {
            LOGGER.warn("Invalid candidate ID provided");
            return RestResponse.status(Response.Status.BAD_REQUEST, "Invalid candidate ID");
        }

        try {
            api.vote(electionId, candidateId);
            LOGGER.infof("Vote successfully registered for candidate %s in election %s", candidateId, electionId);
            return RestResponse.accepted("Vote registered successfully");
        } catch (Exception e) {
            LOGGER.errorf("Error voting for candidate %s in election %s: %s", candidateId, electionId, e.getMessage(), e);
            return RestResponse.status(Response.Status.INTERNAL_SERVER_ERROR, "Error registering vote");
        }
    }
}
