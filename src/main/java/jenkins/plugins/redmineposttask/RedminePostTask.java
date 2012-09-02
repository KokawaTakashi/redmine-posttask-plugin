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

/**
 *
 * @author KokawaTakashi
 */
public class RedminePostTask extends Recorder {

    public RedminePostTask() {
        
    }
    
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }
    
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) 
                throws InterruptedException, IOException {
            
            final RedmineSite site = RedmineSite.get(build.getProject());
            if (site == null) {
                listener.getLogger().println("No redmine site...");
                build.setResult(Result.FAILURE);
                return true;
            }
        
            Result result = build.getResult();
            
            listener.getLogger().println("Debug RedminePost:perform...");
            
            String redmineHost = "http://localhost:3000/";
            String apiAccessKey = "6dfdaa298cd1bdd896115efe1c8261cca3c33e3e";
            
            String projectKey = "sample";
            
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

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
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

        
	//@Override
	//public String getHelpFile() {
	//	return "";
	//}

	//@Override
	//public RedminePostTask newInstance(StaplerRequest req, JSONObject formData)
	//		throws Descriptor.FormException {
        //    
        //    return new RedminePostTask();
        //}
    }

}
