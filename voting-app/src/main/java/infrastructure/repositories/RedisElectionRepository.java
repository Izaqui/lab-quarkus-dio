package infrastructure.repositories;

import domain.Candidate;
import domain.Election;
import domain.ElectionRepository;
import io.quarkus.cache.CacheResult;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.sortedset.SortedSetCommands;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class RedisElectionRepository implements ElectionRepository {
    private static final Logger LOGGER = Logger.getLogger(RedisElectionRepository.class);
    private final SortedSetCommands<String, String> sortedSetCommands;
    private final KeyCommands<String> keyCommands;

    public RedisElectionRepository(RedisDataSource redisDataSource) {
        this.sortedSetCommands = redisDataSource.sortedSet(String.class, String.class);
        this.keyCommands = redisDataSource.key(String.class);
    }

    @Override
    @CacheResult(cacheName = "memoization")
    public Optional<Election> findById(String id) {
        LOGGER.infof("Retrieving election %s from redis", id);

        List<Candidate> candidates = sortedSetCommands.zrange("election:" + id, 0, -1)
                                                      .stream()
                                                      .map(Candidate::new)
                                                      .toList();

        if (candidates.isEmpty()) {
            LOGGER.warnf("Election %s not found in Redis", id);
            return Optional.empty();
        }

        return Optional.of(new Election(id, candidates));
    }

    @Override
    public List<Election> findAll() {
        LOGGER.info("Retrieving all elections from redis");

        return keyCommands.keys("election:*")
                          .stream()
                          .map(id -> id.replace("election:", ""))
                          .map(this::findById)
                          .filter(Optional::isPresent)
                          .map(Optional::get)
                          .toList();
    }

    @Override
    public void vote(String electionId, Candidate candidate) {
        LOGGER.infof("Voting for candidate %s in election %s", candidate.id(), electionId);

        try {
            sortedSetCommands.zincrby("election:" + electionId, 1, candidate.id());
        } catch (Exception e) {
            LOGGER.errorf("Error voting for candidate %s in election %s: %s", candidate.id(), electionId, e.getMessage());
            throw new RuntimeException("Error voting in election", e);
        }
    }
}
