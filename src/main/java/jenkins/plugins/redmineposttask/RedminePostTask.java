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
import com.taskadapter.redmineapi.IssueManager;
import com.taskadapter.redmineapi.bean.Issue;
import com.taskadapter.redmineapi.bean.IssueFactory;
import com.taskadapter.redmineapi.UserManager;
import com.taskadapter.redmineapi.bean.User;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.tasks.junit.TestResultAction;
import hudson.tasks.junit.CaseResult;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.kohsuke.stapler.DataBoundConstructor;

public class RedminePostTask extends Recorder {

    private static final String gLineSeparetor;

    public final String siteName;
    public final String subject;
    public final String description;
    public final boolean alwaysTriggered;
    
    // The maximum number of log lines
    private final int LOG_MAX_LINES = 500;
    private String mAbsoluteUrl;

    static {
        gLineSeparetor = System.getProperty("line.separator");
    }
    
    @DataBoundConstructor
    @SuppressWarnings("unused")
    public RedminePostTask(String siteName, String subject, String description, boolean alwaysTriggered) {
        this.siteName = siteName;
        this.subject = subject;
        this.description = description;
        this.alwaysTriggered = alwaysTriggered;
    }
    
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }
    
    @SuppressWarnings("unused")
    public String getSiteName() {
        return siteName;
    }
    
    @SuppressWarnings("unused")
    public String getSubject() {
        return subject;
    }
    
    @SuppressWarnings("unused")
    public String getDescription() {
        return description;
    }

    @SuppressWarnings("unused")
    public boolean getAlwaysPost() {
        return alwaysTriggered;
    }
    
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) 
                throws InterruptedException, IOException {
        
        Result result = build.getResult();
        // return if build success & perform when onBuildFailure
        if( !alwaysTriggered ) {
            if( result.isBetterOrEqualTo(Result.SUCCESS) ) {
                return true;
            }
        }
        boolean isSuccess = postTaskToRedmine(build, listener);

        return isSuccess;
    }
    
    private void tryPostTaskToRedmine(RedmineSite site, String redmineSubject,
                                      String redmineDescription) throws RedmineException {
        String redmineHost = site.url.toString();
        String apiAccessKey = site.apiAccessKey;
        String projectKey = site.projectId;

        RedmineManager mgr = RedmineManagerFactory.createWithApiKey(redmineHost, apiAccessKey);

        ProjectManager projectMgr = mgr.getProjectManager();
        Project project = projectMgr.getProjectByKey(projectKey);
        int projectId = project.getId().intValue();

        UserManager userMgr = mgr.getUserManager();
        User currentUser = userMgr.getCurrentUser();

        Issue redmineIssue = IssueFactory.create(projectId, redmineSubject);
        redmineIssue.setDescription(redmineDescription);
        redmineIssue.setAssignee(currentUser);
        IssueManager issueMgr = mgr.getIssueManager();
        issueMgr.createIssue(redmineIssue);

        String userName = currentUser.getFullName();
    }
    
    private boolean postTaskToRedmine(AbstractBuild<?, ?> build, BuildListener listener) {
        RedmineSite site = RedmineSite.get(siteName);
        listener.getLogger().println( "Post to Redmine Site: " + site.name );
        //listener.getLogger().println("Site: " + site.name + "," + site.url + "," + site.apiAccessKey + "," + site.projectId);

        //build.getBuildVariables();
        
        // Set Subject: 
        String redmineSubject = getSubject(build);
        // Set Description: 
        String redmineDescription;        

        // Default Description
        if (description.equals("")) {
            mAbsoluteUrl = build.getAbsoluteUrl();
            redmineDescription = getResults(build);
            try {
                redmineDescription += getDescription(build);
            } catch (IOException ex) {
                Logger.getLogger(RedminePostTask.class.getName()).log(Level.SEVERE, null, ex);
                listener.getLogger().println(ex.toString());
                return false;
            }
        } else {
            redmineDescription = description;
        }

        try {
            tryPostTaskToRedmine(site, redmineSubject, redmineDescription);
        } catch (RedmineException ex) {
            Logger.getLogger(RedminePostTask.class.getName()).log(Level.SEVERE, null, ex);
            listener.getLogger().println(ex.toString());
            return false;
        }
        
        listener.getLogger().println("Redmine task created.");

        return true;
    }
    
    private String getSubject(AbstractBuild<?, ?> build) {
        if( !"".equals(subject) ) {
            return subject;
        }
        // Default Subject
        String defaultSubject = build.getProject().getName() + " " + build.getDisplayName();
        defaultSubject += " " + build.getResult().toString();
        return defaultSubject;
    }
    
    private String getDescription(AbstractBuild<?, ?> build) throws IOException {
        StringBuilder defaultDescription = new StringBuilder();

        defaultDescription.append("h2. \"*Console output*\":");
        defaultDescription.append(mAbsoluteUrl + "console");
        defaultDescription.append(gLineSeparetor + gLineSeparetor);

        List<String> log_lines = build.getLog(LOG_MAX_LINES);
        defaultDescription.append("<pre>" + gLineSeparetor);
        for (Iterator<String> it = log_lines.iterator(); it.hasNext();) {
            String log = it.next();
            defaultDescription.append(log);
            defaultDescription.append(gLineSeparetor);
        }
        defaultDescription.append("</pre>" + gLineSeparetor +
                                  gLineSeparetor);
        return defaultDescription.toString();
    }
        
    
    private String getResult(TestResultAction result) {
        StringBuilder description = new StringBuilder();

        description.append("h2. \"" + result.getDisplayName() + "\"");
        description.append(":" + mAbsoluteUrl + result.getUrlName());
        description.append(gLineSeparetor + gLineSeparetor);

        description.append(result.getTotalCount() + " tests and ");
        description.append(result.getFailCount() + " failures");
        description.append(gLineSeparetor);

        for (CaseResult fail : result.getFailedTests()) {
            description.append("* " + fail.getClassName() + "." +
                               fail.getDisplayName() + gLineSeparetor);
            description.append("<pre>" + gLineSeparetor);
            description.append(fail.getErrorDetails());
            description.append("</pre>" + gLineSeparetor);
            description.append(gLineSeparetor);
        }

        description.append(gLineSeparetor);
        return description.toString();
    }

    /**
     * Create result summary of JUnit.
     */
    private String getResults(AbstractBuild<?, ?> build) {
        StringBuilder description = new StringBuilder();
        for (TestResultAction result :
               build.getActions(TestResultAction.class))
          description.append(getResult(result));
        return description.toString();
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        
        private volatile RedmineSite[] sites = new RedmineSite[0];
        
	public DescriptorImpl() {
		super(RedminePostTask.class);
		load();
	}

	public boolean isApplicable(Class<? extends AbstractProject> jobType) {
		return true;
	}

	@Override
	public String getDisplayName() {
		return "Redmine post task";
	}

        public void setSites(RedmineSite... sites) {
                this.sites = sites;
                save();
        }

        public RedmineSite[] getSites() {
                return RedmineProjectProperty.DESCRIPTOR.getSites();
        }        
    }
    

}
