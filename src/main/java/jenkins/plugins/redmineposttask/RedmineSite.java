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
    
    
    
    /*
    public enum RedmineVersion {
        V110(Messages.RemineSite_RedmineVersion_V110()),
        V120(Messages.RemineSite_RedmineVersion_V120());

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
    */
    
    
    /*
    @Extension
    public static class DescriptorImpl extends Descriptor<RedmineSite> {

        @Override
        public String getDisplayName() {
            return "Redmine Site";
        }
        
        public FormValidation doUrlCheck(@QueryParameter final String value)
                throws IOException, ServletException {
            if (!Hudson.getInstance().hasPermission(Hudson.ADMINISTER)) {
                return FormValidation.ok();
            }
            return new FormValidation.URLCheck() {
                @Override
                protected FormValidation check() throws IOException,
                        ServletException {
                    String url = Util.fixEmpty(value);
                    if (url == null) {
                        return FormValidation.error("FormValidation Error.");
                    }
                    return FormValidation.ok();
                }
            }.check();
        }

        public FormValidation doCheckUserPattern(@QueryParameter String value) throws IOException {
            String userPattern = Util.fixEmpty(value);
            if (userPattern == null) {// userPattern not entered yet
                return FormValidation.ok();
            }
            try {
                Pattern.compile(userPattern);
                return FormValidation.ok();
            } catch (PatternSyntaxException e) {
                return FormValidation.error(e.getMessage());
            }
        }
        */

        /**
         * Checks if the user name and apiAccessKey are valid.
         */
        /*public FormValidation doValidate(@QueryParameter String url,
                                          @QueryParameter String apiAccessKey,
                                          @QueryParameter String projectKey)
                throws IOException {
            url = Util.fixEmpty(url);
            if (url == null) {// URL not entered yet
                return FormValidation.error("No URL given");
            }
            
            return FormValidation.ok("Success");
        }
        
    }
*/
    
    private static final Logger LOGGER = Logger.getLogger(RedmineSite.class.getName());
}
