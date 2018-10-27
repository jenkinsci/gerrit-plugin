// Copyright (C) 2018 GerritForge Ltd
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package jenkins.plugins.gerrit.workflow;

import com.google.gerrit.extensions.api.changes.NotifyHandling;
import com.google.gerrit.extensions.api.changes.ReviewInput;
import com.urswolfer.gerrit.client.rest.GerritRestApi;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.TaskListener;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import jenkins.plugins.gerrit.GerritChange;
import jenkins.plugins.gerrit.GerritRestApiBuilder;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class GerritReviewStep extends Step {
  @Deprecated
  private String label = "Verified";
  @Deprecated
  private int score;
  private Map<String, Integer> labels;
  private String message = "";

  @DataBoundConstructor
  public GerritReviewStep() {}

  public class Execution extends SynchronousStepExecution<Void> {
    private final TaskListener listener;
    private final EnvVars envVars;

    protected Execution(@Nonnull StepContext context) throws IOException, InterruptedException {
      super(context);
      this.envVars = context.get(EnvVars.class);
      this.listener = getContext().get(TaskListener.class);
    }

    @Override
    protected Void run() throws Exception {

      GerritRestApi gerritRestApi = new GerritRestApiBuilder().stepContext(getContext()).build();
      if (gerritRestApi != null) {
        GerritChange change = new GerritChange(getContext());
        if (change.valid()) {
          listener.getLogger().format("Gerrit review change %d/%d label %s=%d (%s)%n", change.getChangeId(), change.getRevision(), label, score, message);
          ReviewInput reviewInput = new ReviewInput().message(message);
          boolean notifyOwner = false;
          if (labels == null && label != null) {
            labels = Collections.singletonMap(label, score);
          }
          if (labels != null) {
            for (Map.Entry<String, Integer> l : labels.entrySet()) {
              reviewInput.label(l.getKey(), l.getValue());
              if (l.getValue() < 0) {
                notifyOwner = true;
              }
            }
          }
          reviewInput.drafts = ReviewInput.DraftHandling.PUBLISH;
          reviewInput.tag = "autogenerated:jenkins";
          if (notifyOwner) {
            reviewInput.notify = NotifyHandling.OWNER;
          }
          gerritRestApi.changes().id(change.getChangeId()).revision(change.getRevision()).review(reviewInput);
        }
      }
      return null;
    }
  }

  @Deprecated
  public int getScore() {
    return score;
  }

  @Deprecated
  @DataBoundSetter
  public void setScore(int score) {
    this.score = score;
  }

  public Map<String, Integer> getLabels() {
    return labels;
  }

  @DataBoundSetter
  public void setLabels(Map<String, Integer> labels) {
    this.labels = labels;
  }

  public String getMessage() {
    return message;
  }

  @DataBoundSetter
  public void setMessage(String message) {
    this.message = message;
  }

  @Deprecated
  public String getLabel() {
    return label;
  }

  @Deprecated
  @DataBoundSetter
  public void setLabel(String label) {
    this.label = label;
  }

  @Override
  public StepExecution start(StepContext stepContext) throws Exception {
    return new Execution(stepContext);
  }

  @Extension
  public static class DescriptorImpl extends StepDescriptor {

    @Override
    public Set<Class<?>> getRequiredContext() {
      return Collections.emptySet();
    }

    @Override
    public String getFunctionName() {
      return "gerritReview";
    }

    @Nonnull
    @Override
    public String getDisplayName() {
      return "Gerrit Review Label";
    }
  }
}
