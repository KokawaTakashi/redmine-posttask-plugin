package jenkins.plugins.redmineposttask;

import com.taskadapter.redmineapi.RedmineException;
import com.taskadapter.redmineapi.RedmineManager;
import com.taskadapter.redmineapi.bean.Issue;
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
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 *
 * @author KokawaTakashi
 */
public class RedminePostTask extends Recorder {

    public final String siteName;
    public final String subject;
    public final String description;
    public final boolean alwaysPost;
    
    // The maximum number of log lines
    private final int LOG_MAX_LINES = 500;
    
    @DataBoundConstructor
    @SuppressWarnings("unused")
    public RedminePostTask(String siteName, String subject, String description, boolean alwaysPost) {
        this.siteName = siteName;
        this.subject = subject;
        this.description = description;
        this.alwaysPost = alwaysPost;
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
        return alwaysPost;
    }
    
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) 
                throws InterruptedException, IOException {
        
        Result result = build.getResult();
        // return if build success & perform when onBuildFailure
        if( !alwaysPost ) {
            if( result.isBetterOrEqualTo(Result.SUCCESS) ) {
                return true;
            }
        }
        boolean isSuccess = postTaskToRedmine(build, listener);

        return isSuccess;
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
        try {
            redmineDescription = getDescription(build);
        } catch (IOException ex) {
            Logger.getLogger(RedminePostTask.class.getName()).log(Level.SEVERE, null, ex);
            listener.getLogger().println(ex.toString());
            return false;
        }

        String redmineHost = site.url.toString();
        String apiAccessKey = site.apiAccessKey;
        
        String projectKey = site.projectId;

        RedmineManager mgr = new RedmineManager(redmineHost, apiAccessKey);
        Issue redmineIssue = new Issue();

        redmineIssue.setSubject(redmineSubject);
        redmineIssue.setDescription(redmineDescription);
        try {
            User currentUser = mgr.getCurrentUser();
            redmineIssue.setAssignee(currentUser);
            mgr.createIssue(projectKey, redmineIssue);
            String userName = currentUser.getFullName();
        } catch (RedmineException ex) {
            Logger.getLogger(RedminePostTask.class.getName()).log(Level.SEVERE, null, ex);
            listener.getLogger().println(ex.toString());
            return false;
        }
        
        listener.getLogger().println( "Redmine task created.");

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
        if( !"".equals(description) ) {
            return description;
        }
        // Default Description
        String defaultDescription = "";
        String[] log_lines = build.getLog(LOG_MAX_LINES);
        foreach( String log in log_lines ) {
            defaultDescription += log;
        }
        return defaultDescription;
    }
    
    /*
    @Override
    public Descriptor<Builder> getDescriptor() {
        return DESCRIPTOR;
    }
    */

    /**
     * Descriptor should be singleton.
     */
    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();
    
    
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
