/*
 * The MIT License
 * 
 * Copyright (c) 2012, Takashi Kokawa
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package jenkins.plugins.redmineposttask;

import com.taskadapter.redmineapi.ProjectManager;
import com.taskadapter.redmineapi.bean.Project;
import com.taskadapter.redmineapi.RedmineException;
import com.taskadapter.redmineapi.RedmineManager;
import com.taskadapter.redmineapi.RedmineManagerFactory;
import hudson.Extension;
import hudson.matrix.MatrixRun;
import hudson.model.*;
import hudson.util.CopyOnWriteList;
import hudson.util.FormValidation;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

public class RedmineProjectProperty extends JobProperty<AbstractProject<?, ?>> {
    
	@Extension
	public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();
        public final String siteName;
        
        
        public static RedmineProjectProperty get(AbstractBuild<?, ?> build) {
            if (build == null) {
                return null;
            }
            Job<?, ?> job;
            if (build instanceof MatrixRun) {
                job = ((MatrixRun) build).getProject().getParent();
            } else {
                job = build.getProject();
            }
            return job.getProperty(RedmineProjectProperty.class);
        }        
    
    	@DataBoundConstructor
	public RedmineProjectProperty(String siteName) {
            if (siteName == null) {
                // defaults to the first one
                RedmineSite[] sites = DESCRIPTOR.getSites();
                if (sites.length > 0) {
                    siteName = sites[0].getName();
                }
            }
            this.siteName = siteName;
	}

    
        public String getSiteName() {
            return siteName;
        }
        
        
         public RedmineSite getSite() {
            final RedmineSite[] sites = DESCRIPTOR.getSites();
            if (siteName == null && sites.length > 0) {
                return sites[0];
            }
            for (final RedmineSite site : sites) {
                if (site.getName().equals(siteName)) {
                    return site;
                }
            }
            return null;
        }
        
        /*
        @Override
	public DescriptorImpl getDescriptor() {
            return DESCRIPTOR;
	}
        */

	public static final class DescriptorImpl extends JobPropertyDescriptor {

            private final CopyOnWriteList<RedmineSite> sites = new CopyOnWriteList<RedmineSite>();

            public DescriptorImpl() {
                    super(RedmineProjectProperty.class);
                    load();
            }

            @Override
            @SuppressWarnings("unchecked")
            public boolean isApplicable(Class<? extends Job> jobType) {
                    return AbstractProject.class.isAssignableFrom(jobType);
            }

            @Override
            public String getDisplayName() {
                    //return Messages.RedmineProjectProperty_DisplayName();
                return "Redmine Site setting";
            }

            public void setSites(RedmineSite site) {
                sites.add(site);
                //Hudson.getInstance().getDescriptorByType(RedminePostTask.DescriptorImpl.class).setSites(site);
            }

            public RedmineSite[] getSites() {
                return sites.toArray(new RedmineSite[0]);
                //return Hudson.getInstance().getDescriptorByType(RedminePostTask.DescriptorImpl.class).getSites();
            }

            void addSite(RedmineSite site) {
                sites.add(site);
            }

            @Override
            public JobProperty<?> newInstance(StaplerRequest req, JSONObject formData)
                            throws Descriptor.FormException {
                    RedmineProjectProperty jpp = req.bindParameters(
                                    RedmineProjectProperty.class, "redmine.");
                    if (jpp.siteName == null)
                            jpp = null; // not configured
                    return jpp;
            }

            @Override
            public boolean configure(StaplerRequest req, JSONObject formData) {
                    sites.replaceBy(req.bindParametersToList(RedmineSite.class, "m."));
                    save();
                    return true;
            }

            public FormValidation doCheckRequired(@QueryParameter String value) {
                return FormValidation.validateRequired(value);
            }

            public FormValidation doCheckLogin(
                @QueryParameter("m.url") String url,
                @QueryParameter("m.apiAccessKey") String apiAccessKey,
                @QueryParameter("m.projectId") String projectId) 
                    throws IOException, ServletException {
                // only administrator allowed
                Hudson.getInstance().checkPermission(Hudson.ADMINISTER);

                if (url == null) {
                    return FormValidation.error("URL null error.");
                }

                try {
                    URL urL = new URL(url);
                } catch (MalformedURLException e) {
                    return FormValidation.error("URL invalid error.");
                }

                RedmineManager redmineMgr = RedmineManagerFactory.createWithApiKey(url, apiAccessKey);
                try {
                    ProjectManager projectMgr = redmineMgr.getProjectManager();
                    projectMgr.getProjectByKey(projectId);
                } catch (RedmineException ex) {
                    return FormValidation.error(ex.toString());
                }

                return FormValidation.ok("OK.");
            }
                
	}

	private static final Logger LOGGER = Logger.getLogger(RedmineProjectProperty.class.getName());
}
