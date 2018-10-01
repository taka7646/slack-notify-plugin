package com.taka7646.slack;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
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

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.ProxyConfiguration;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
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

	private final String propFileName;

	private final String room;

	private final String successMessage;

	private final String failureMessage;

	protected String responseBody;

	public String getNotificationStrategy() {
		return notificationStrategy;
	}

	public String getPropFileName() {
		return propFileName;
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
	public SlackNotifier(String notificationStrategy, String propFileName, String room, String successMessage, String failureMessage) {
		this.notificationStrategy = notificationStrategy;
		this.propFileName = propFileName;
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
		if (StringUtils.isNotEmpty(this.propFileName)) {
			loadAndExportVariables(build, env, this.propFileName);
		}
		String room = StringUtils.isEmpty(this.room) ? desc.getRoom() : this.room;
		room = env.expand(room);

		SlackSender sender = new SlackSender(desc.getUrl(), desc.getToken(), room);
		String message = env.expand(result == Result.SUCCESS ? successMessage : failureMessage);
		String color = result == Result.SUCCESS ? "good" : "danger";
		listener.getLogger().println(message);
		message = String.format("%s - %s %s after %s (<%s|Open>)\n%s", 
				build.getProject().getFullDisplayName(), 
				build.getDisplayName(), 
				result == Result.SUCCESS ? "Success": "Failure",
				build.getDurationString(), 
				env.expand("${BUILD_URL}"), 
				message);
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
	 * プロパティファイルをロードして環境変数に登録する
	 * @param build
	 * @param env
	 * @param propName
	 * @throws IOException
	 */
	private void loadAndExportVariables(AbstractBuild<?, ?> build, EnvVars env, String propName) throws IOException {
		Properties prop = new Properties();
		File file = new File(build.getProject().getRootDir(), propName);
		try(Reader r = new FileReader(file)) {
			prop.load(r);
			for(Object key: prop.keySet()) {
				String value = (String)prop.get(key);
				EnvVars.masterEnvVars.put((String)key, value);
				env.put((String)key, value);
			}
		}
	}

	protected static CloseableHttpClient getHttpClient() {
		final HttpClientBuilder clientBuilder = HttpClients.custom();
		final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
		clientBuilder.setDefaultCredentialsProvider(credentialsProvider);

		if (Jenkins.getInstance() != null) {
			ProxyConfiguration proxy = Jenkins.getInstance().proxy;
			if (proxy != null) {
				final HttpHost proxyHost = new HttpHost(proxy.name, proxy.port);
				final HttpRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxyHost);
				clientBuilder.setRoutePlanner(routePlanner);

				String username = proxy.getUserName();
				String password = proxy.getPassword();
				// Consider it to be passed if username specified. Sufficient?
				if (username != null && !"".equals(username.trim())) {
					logger.info("Using proxy authentication (user=" + username + ")");
					credentialsProvider.setCredentials(new AuthScope(proxyHost),
							new UsernamePasswordCredentials(username, password));
				}
			}
		}
		return clientBuilder.build();
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
