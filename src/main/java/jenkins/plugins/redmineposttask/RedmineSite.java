/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jenkins.plugins.redmineposttask;

import hudson.Extension;
import hudson.Util;
import hudson.model.*;
import hudson.util.FormValidation;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.servlet.ServletException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 * Represents an external Redmine installation and configuration
 * @author KokawaTakashi
 */
public class RedmineSite  extends AbstractDescribableImpl<RedmineSite> {
    
    /**
     * URL of Redmines.
     * Mandatory. Normalized to end with '/'
     */
    public final URL url;
    
    public final String apiAccessKey;
    
    public final String projectKey;
    
    /**
     * Used to guard the computation of {@link #projects}
     */
    private transient Lock projectUpdateLock = new ReentrantLock();

    
    @DataBoundConstructor
    public RedmineSite(URL url, String apiAccessKey, String projectKey) {
        if(!url.toExternalForm().endsWith("/"))
            try {
                url = new URL(url.toExternalForm()+"/");
            } catch (MalformedURLException e) {
                throw new AssertionError(e); // impossible
            }
        this.url = url;
        this.apiAccessKey = apiAccessKey;
        this.projectKey = projectKey;
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

        /**
         * Checks if the user name and password are valid.
         */
        public FormValidation doValidate(@QueryParameter String userName,
                                          @QueryParameter String url,
                                          @QueryParameter String password,
                                          @QueryParameter String groupVisibility,
                                          @QueryParameter String roleVisibility)
                throws IOException {
            url = Util.fixEmpty(url);
            if (url == null) {// URL not entered yet
                return FormValidation.error("No URL given");
            }
            
            return FormValidation.ok("Success");
        }
        
    }
    
    private static final Logger LOGGER = Logger.getLogger(RedmineSite.class.getName());
}
