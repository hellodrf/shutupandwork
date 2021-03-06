package com.cervidae.shutupandwork.service;

import com.cervidae.shutupandwork.pojo.Session;
import com.cervidae.shutupandwork.pojo.User;
import com.cervidae.shutupandwork.util.Constants;
import com.cervidae.shutupandwork.dao.ICache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Set;
import java.util.logging.Logger;

/**
 * @author AaronDu
 */
@Service
public class SessionService implements IService {

    final ICache<Session> sessionICache;

    final UserService userService;

    @Autowired
    public SessionService(ICache<Session> sessionICache, UserService userService) {
        this.userService = userService;
        this.sessionICache = sessionICache;
        this.sessionICache.setIdentifier(2);
    }

    /**
     * Validate sessionID using Regex (Util.Constants.sessionIDRegex)
     * @param sessionID the session id
     */
    private void validateID(String sessionID) {
        // validate sessionID
        Assert.isTrue(sessionID.matches(Constants.SESSION_ID_REGEX), "4005");
    }

    /**
     * Remove expired sessions (see Constants.SESSION_EXPIRY)
     * Cron task scheduled 3am everyday
     */
    @Scheduled(cron = "00 03 * * * ?")
    @Async
    public void collectExpiredSessions() {
        int i = 0;
        Logger logger = Logger.getLogger(this.getClass().getName());
        logger.info("Starting Session GC...");
        Set<String> sessionIDs = sessionICache.getKeySet();
        if (!sessionIDs.isEmpty()) {
            for (String id : sessionIDs) {
                Session session = sessionICache.select(id);
                if (session != null && session.getStatus() != Session.Status.ACTIVE &&
                        System.currentTimeMillis() - session.getCreated() > Constants.SESSION_EXPIRY) {
                    sessionICache.drop(id);
                    logger.info("Session GC: dropped expired session " + id);
                    i++;
                }
            }
        }
        logger.info("Session GC Completed: dropped " + i + " sessions");
    }

    /**
     * Join the specified session. If not exist, create a session
     * @param user the user
     * @param sessionID ID of the session
     * @return the session
     */
    public Session join(User user, String sessionID) {
        validateID(sessionID);
        Session session;
        if (sessionICache.contains(sessionID)) {
            session = sessionICache.select(sessionID);
            if (session.getStatus() == Session.Status.SUCCESS || session.getStatus() == Session.Status.FAIL) {
                session.reset();
            } else if (session.getStatus() != Session.Status.WAITING) {
                throw new IllegalArgumentException("4001");
            }
            session.addUser(user);
        } else {
            session = new Session(user, sessionID);
        }
        sessionICache.put(sessionID, session);
        return session;
    }

    /**
     * Leave the specified session.
     * @param user the user
     * @param sessionID ID of the session
     */
    public void leave(User user, String sessionID) {
        validateID(sessionID);
        Session session = getSessionNotNull(sessionID);
        if (session.getUserList().containsKey(user.getUsername())) {
            if (session.getStatus()== Session.Status.ACTIVE) {
                session.fail(user);
            } else {
                session.removeUser(user);
            }
        }
        sessionICache.put(sessionID, session);
    }

    /**
     * Get a specific session with sessionID, nullable
     * @param sessionID ID of the session
     * @return the session
     */
    public Session getSession(String sessionID) {
        return sessionICache.select(sessionID);
    }

    /**
     * Get a specific session with sessionID, not nullable (will throw exception)
     * @param sessionID ID of the session
     * @return the session
     */
    public Session getSessionNotNull(String sessionID) {
        Assert.isTrue(sessionICache.contains(sessionID), "4004");
        return sessionICache.select(sessionID);
    }

    /**
     * Start the session
     * Pessimistic lock: since this function need only be called EXACTLY ONCE
     * @param sessionID ID of the session
     * @param target the target of the session
     */
    public Session start(String sessionID, long target) {
        validateID(sessionID);
        Session session = getSessionNotNull(sessionID).start(target);
        sessionICache.put(sessionID, session);
        return session;
    }

    /**
     * Mark the session as succeed
     * Pessimistic lock: since this function need only be called EXACTLY ONCE
     * @param sessionID ID of the session
     */
    public Session success(String sessionID) {
        validateID(sessionID);
        Session session = getSessionNotNull(sessionID).success();
        sessionICache.put(sessionID, session);
        return session;
    }

    /**
     * Mark the session as failed
     * Pessimistic lock: since this function need only be called EXACTLY ONCE
     * @param sessionID ID of the session
     * @param username user to blame
     */
    public Session fail(String sessionID, String username) {
        validateID(sessionID);
        User user = userService.getByUsername(username);
        Session session = getSessionNotNull(sessionID).fail(user);
        sessionICache.put(sessionID, session);
        return session;
    }

    /**
     * Reset a session to WAITING state.
     * must be in SUCCESS or FAIL state, however WAITING is tolerated
     * @param sessionID ID of the session
     * @return the session
     */
    public Session reset(String sessionID) {
        Session session = getSessionNotNull(sessionID);
        if (session.getStatus() == Session.Status.SUCCESS || session.getStatus() == Session.Status.FAIL) {
            session.reset();
            sessionICache.put(sessionID, session);
        } else if (session.getStatus() != Session.Status.WAITING) {
            // waiting state is tolerated
            throw new IllegalArgumentException("4003");
        }
        return session;
    }
}
