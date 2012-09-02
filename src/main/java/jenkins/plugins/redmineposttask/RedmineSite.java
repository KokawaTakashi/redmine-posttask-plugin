package jenkins.plugins.redmineposttask;

import hudson.model.AbstractProject;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Represents an external Redmine installation and configuration
 * @author KokawaTakashi
 */
public class RedmineSite {
    
    /**
     * URL of Redmines.
     * Mandatory. Normalized to end with '/'
     */
    public final URL url;
    
    public final String apiAccessKey;
    
    public final String projectId;
    
    /**
     * Used to guard the computation of {@link #projects}
     */
    private transient Lock projectUpdateLock = new ReentrantLock();

    public static RedmineSite get(final AbstractProject<?, ?> p) {
        final RedmineProjectProperty mpp = p.getProperty(RedmineProjectProperty.class);
        if (mpp != null) {
            final RedmineSite site = mpp.getSite();
            if (site != null) {
                return site;
            }
        }

        final RedmineSite[] sites = RedmineProjectProperty.DESCRIPTOR.getSites();
        if (sites.length == 1) {
            return sites[0];
        }

        return null;
    }
    public static RedmineSite get() {
        final RedmineSite[] sites = RedmineProjectProperty.DESCRIPTOR.getSites();
        return sites[0];
    }
    
    @DataBoundConstructor
    public RedmineSite(URL url, String apiAccessKey, String projectId) {
        if(!url.toExternalForm().endsWith("/"))
            try {
                url = new URL(url.toExternalForm()+"/");
            } catch (MalformedURLException e) {
                throw new AssertionError(e); // impossible
            }
        this.url = url;
        this.apiAccessKey = apiAccessKey;
        this.projectId = projectId;
    }
    
    protected Object readResolve() {
        projectUpdateLock = new ReentrantLock();
        return this;
    }


    public String getName() {
        return url.toExternalForm();
    }

    public URL getUrl() throws IOException {
        return url;
    }
    
    
    
    
    public enum RedmineVersion {
        V110("RedmineSite.RedmineVersion.V110"),
        V120("RedmineSite.RedmineVersion.V120");

        private final String displayName;

        private RedmineVersion(final String displayName) {
            this.displayName = displayName;
        }

        public static RedmineVersion getVersionSafely(final String version, final RedmineVersion def) {
            RedmineVersion ret = def;
            for (final RedmineVersion v : RedmineVersion.values()) {
                if (v.name().equalsIgnoreCase(version)) {
                    ret = v;
                    break;
                }
            }
            return ret;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
    
    private static final Logger LOGGER = Logger.getLogger(RedmineSite.class.getName());
}
