package infrastructure.lifecycle;

import domain.Election;
import infrastructure.repositories.RedisElectionRepository;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.runtime.Startup;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import java.util.Optional;

@Startup
@ApplicationScoped
public class Subscribe {
    private static final Logger LOGGER = Logger.getLogger(Subscribe.class);

    public Subscribe(ReactiveRedisDataSource reactiveRedisDataSource,
                     RedisElectionRepository repository) {
        LOGGER.info("Startup: Subscribing to Redis elections channel");

        Multi<String> sub = reactiveRedisDataSource.pubsub(String.class)
                                                   .subscribe("elections");

        sub.emitOn(Infrastructure.getDefaultWorkerPool())
           .subscribe()
           .with(id -> {
               LOGGER.infof("Election ID %s received from subscription", id);
               try {
                   Optional<Election> electionOpt = repository.findById(id);
                   if (electionOpt.isPresent()) {
                       Election election = electionOpt.get();
                       LOGGER.infof("Election %s found: Starting", election.id());
                   } else {
                       LOGGER.warnf("Election ID %s not found in repository", id);
                   }
               } catch (Exception e) {
                   LOGGER.errorf("Error processing election ID %s: %s", id, e.getMessage(), e);
               }
           }, failure -> {
               LOGGER.error("Error in Redis subscription: " + failure.getMessage(), failure);
           });
    }
}
