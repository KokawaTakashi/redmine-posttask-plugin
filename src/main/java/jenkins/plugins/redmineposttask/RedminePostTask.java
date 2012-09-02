package jenkins.plugins.redmineposttask;

import com.taskadapter.redmineapi.RedmineException;
import com.taskadapter.redmineapi.RedmineManager;
import com.taskadapter.redmineapi.bean.Issue;
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
    
    @DataBoundConstructor
    @SuppressWarnings("unused")
    public RedminePostTask(String siteName) {
        this.siteName = siteName;
    }
    
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }
    
    @SuppressWarnings("unused")
    public String getSiteName() {
        return siteName;
    }
    
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) 
                throws InterruptedException, IOException {
        
        RedmineSite site = RedmineSite.get(build.getProject());
        site = RedmineSite.get();
        listener.getLogger().println("Site: " + site.url + "," + site.apiAccessKey + "," + site.projectId);
        

        Result result = build.getResult();

        listener.getLogger().println("Debug RedminePost:perform...");

        String redmineHost = site.url.toString();
        String apiAccessKey = site.apiAccessKey;
        
        String projectKey = site.projectId;

        RedmineManager mgr = new RedmineManager(redmineHost, apiAccessKey);
        Issue redmineIssue = new Issue();

        redmineIssue.setSubject("From Jenkins!");
        try {
            mgr.createIssue(projectKey, redmineIssue);
        } catch (RedmineException ex) {
            Logger.getLogger(RedminePostTask.class.getName()).log(Level.SEVERE, null, ex);
            listener.getLogger().println(ex.toString());
            return false;
        }

        return true;
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
        
        //private final CopyOnWriteList<RedmineSite> sites = new CopyOnWriteList<RedmineSite>();
        RedmineSite[] sites;
        
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

        public void setSites(RedmineSite[] sites) {
                this.sites = sites;
        }

        public RedmineSite[] getSites() {
                return sites; //.toArray(new RedmineSite[0]);
        }
        
    }

}
