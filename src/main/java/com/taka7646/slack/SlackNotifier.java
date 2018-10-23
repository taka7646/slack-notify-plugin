package com.taka7646.slack;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import hudson.Util;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.ProxyConfiguration;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

public class SlackNotifier extends Notifier {

	private static final Logger logger = Logger.getLogger(SlackNotifier.class.getName());

	private final String notificationStrategy;

	private final String additionalMessageFileName;

	private final String room;

	private final String successMessage;

	private final String failureMessage;

	protected String responseBody;

	public String getNotificationStrategy() {
		return notificationStrategy;
	}

	public String getAdditionalMessageFileName() {
		return additionalMessageFileName;
	}

	public String getRoom() {
		return room;
	}

	public String getSuccessMessage() {
		return successMessage;
	}

	public String getFailureMessage() {
		return failureMessage;
	}

	@DataBoundConstructor
	public SlackNotifier(String notificationStrategy, String additionalMessageFileName, String room, String successMessage, String failureMessage) {
		this.notificationStrategy = notificationStrategy;
		this.additionalMessageFileName = additionalMessageFileName;
		this.room = room;
		this.successMessage = successMessage;
		this.failureMessage = failureMessage;
	}

	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
			throws InterruptedException, IOException {
		logger.info("perform slack notify" + build.getResult().toString());
		NotificationStrategy strategy = NotificationStrategy.fromString(notificationStrategy);
		if (!strategy.needNotification(build)) {
			return true;
		}
		
		Result result = build.getResult();
		DescriptorImpl desc = (DescriptorImpl) getDescriptor();
		EnvVars env = null;
		try {
			env = build.getEnvironment(listener);
		} catch (Exception e) {
			env = new EnvVars();
		}
		
		String room = StringUtils.isEmpty(this.room) ? desc.getRoom() : this.room;
		room = env.expand(room);

		SlackSender sender = new SlackSender(desc.getUrl(), desc.getToken(), room);
		String message = env.expand(result == Result.SUCCESS ? successMessage : failureMessage);
		String color = result == Result.SUCCESS ? "good" : "danger";
		message = String.format("%s - %s %s after %s (<%s|Log>)\n%s", 
				build.getProject().getFullDisplayName(), 
				build.getDisplayName(), 
				result == Result.SUCCESS ? "Success": "Failure",
				build.getDurationString(), 
				env.get("BUILD_URL") + "console", 
				message);
		if (StringUtils.isNotEmpty(this.additionalMessageFileName)) {
			File f = new File(this.additionalMessageFileName);
			message += "\n" + loadTextFile(f.getPath());
		}
		try {
			sender.send(message, color);
		} catch (Exception e) {
			listener.getLogger().println("Slackへの通知に失敗しました\n" + e.getMessage());
		}
		return true;
	}

	@Override
	public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
		logger.info("start slack notify" + build.toString());
		return true;
	}
	
	/**
	 * テキストファイルをロードして環境変数に登録する
	 * @param fileName
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	private String loadTextFile(String fileName) throws IOException, InterruptedException {
        Computer computer = Computer.currentComputer();
        Node node = computer.getNode();
        if (node == null) {
        	return "";
        }
        FilePath rootPath = node.getRootPath();
        if (rootPath == null) {
        	return "";
        }
    	List<String> items = rootPath.act(new TextFileLoader(fileName));
    	return String.join("\n", items);
	}

	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

		private String token;
		private String url;
		private String room;

		public static final NotificationStrategy[] STRATEGIES = NotificationStrategy.values();
		public static final MessageFormat[] FORMATS = MessageFormat.values();
		public static final SendTarget[] SEND_TARGETS = SendTarget.values();

		public String getToken() {
			return token;
		}

		public String getUrl() {
			return url;
		}

		public String getRoom() {
			return room;
		}

		public DescriptorImpl() {
			load();
		}

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}

		@Override
		public String getDisplayName() {
			return "Slack Notifier";
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
			token = formData.getString("token");
			url = formData.getString("url");
			room = formData.getString("room");
			if (!url.endsWith("/")) {
				url += "/";
			}
			save();
			return super.configure(req, formData);
		}

		public FormValidation doTestConnection(@QueryParameter("room") final String room) throws FormException {
			try {
				String targetRoom = room;
				if (StringUtils.isEmpty(targetRoom)) {
					targetRoom = this.room;
				}
				SlackSender sender = new SlackSender(this.url, this.token, targetRoom);
				sender.send("Slack/Jenkins plugin: setup success", "good");
				return FormValidation.ok("Success");
			} catch (Exception e) {
				return FormValidation.error("Client error : " + e.getMessage());
			}
		}

	}
}
