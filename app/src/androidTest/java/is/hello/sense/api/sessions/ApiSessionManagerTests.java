package is.hello.sense.api.sessions;

import android.support.annotation.Nullable;

import junit.framework.TestCase;

public class ApiSessionManagerTests extends TestCase {
    public void testFacade() {
        OAuthSession testSession = new OAuthSession();
        TestApiSessionManager testApiSessionManager = new TestApiSessionManager();
        testApiSessionManager.setSession(testSession);

        assertTrue(testApiSessionManager.hasSession());
        assertNotNull(testApiSessionManager.getSession());

        assertTrue(testApiSessionManager.storeOAuthSessionCalled);
        assertTrue(testApiSessionManager.retrieveOAuthSessionCalled);
    }

    private static class TestApiSessionManager extends TransientApiSessionManager {
        boolean storeOAuthSessionCalled = false;
        boolean retrieveOAuthSessionCalled = false;

        @Override
        protected void storeOAuthSession(@Nullable OAuthSession session) {
            this.storeOAuthSessionCalled = true;
            super.storeOAuthSession(session);
        }

        @Nullable
        @Override
        protected OAuthSession retrieveOAuthSession() {
            this.retrieveOAuthSessionCalled = true;
            return super.retrieveOAuthSession();
        }
    }
}
