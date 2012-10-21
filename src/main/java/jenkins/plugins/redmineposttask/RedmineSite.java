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
    
    public final String name;

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
    public static RedmineSite get(final String name) {
        final RedmineSite[] sites = RedmineProjectProperty.DESCRIPTOR.getSites();
        // find site with name
        for (RedmineSite site : sites ) {
            if( site.name.equals(name) ) {
                return site;
            }
        }
        
        return null;
    }
    
    @DataBoundConstructor
    public RedmineSite(String name, URL url, String apiAccessKey, String projectId) {
        if(!url.toExternalForm().endsWith("/")) {
            try {
                url = new URL(url.toExternalForm()+"/");
            } catch (MalformedURLException e) {
                throw new AssertionError(e); // impossible
            }
        }
        this.name = name;
        this.url = url;
        this.apiAccessKey = apiAccessKey;
        this.projectId = projectId;
    }
    
    protected Object readResolve() {
        projectUpdateLock = new ReentrantLock();
        return this;
    }


    public String getName() {
        return name;
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
