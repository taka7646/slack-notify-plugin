package com.taka7646.slack;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.message.BasicNameValuePair;

import hudson.ProxyConfiguration;
import jenkins.model.Jenkins;

public class SlackSender {
	private String url;
	private String token;
	private String room;

	private String author;
	private String authorLink;
	private String authorIcon;

	private String title;
	private String titleLink;

	private String preText;

	public SlackSender(String url, String token, String room) {
		this.url = url;
		this.token = token;
		this.room = room;
	}

	public void setAuthor(String author) {
		this.setAuthor(author, null, null);
	}

	public void setAuthor(String author, String authorLink, String authorIcon) {
		this.author = author;
		this.authorLink = authorLink;
		this.authorIcon = authorIcon;
	}

	public void setTitle(String title) {
		this.setTitle(title, null);
	}

	public void setTitle(String title, String titleLink) {
		this.title = title;
		this.titleLink = titleLink;
	}

	public void setPreText(String preText) {
		this.preText = preText;
	}

	/**
	 * 
	 * @param message メッセージ
	 * @param color   色指定 (good | warning | danger) or #439FE0
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public void send(String message, String color) throws ClientProtocolException, IOException {
//		JSONObject field = new JSONObject();
//		field.put("short", false);
//		field.put("value", message);
//		if (StringUtils.isNotEmpty(this.title)) {
//			field.put("title", this.title);
//		}
//		if (StringUtils.isNotEmpty(this.titleLink)) {
//			field.put("title_link", this.titleLink);
//		}
//
//		JSONArray fields = new JSONArray();
//		fields.add(field);

		JSONObject attachment = new JSONObject();
		attachment.put("fallback", message);
		attachment.put("color", color);
		attachment.put("text", message);
		if (StringUtils.isNotEmpty(this.preText)) {
			attachment.put("pretext", this.preText);
		}
		if (StringUtils.isNotEmpty(this.author)) {
			attachment.put("author", this.author);
		}
		if (StringUtils.isNotEmpty(this.authorLink)) {
			attachment.put("author_link", this.authorLink);
		}
		if (StringUtils.isNotEmpty(this.authorIcon)) {
			attachment.put("author_icon", this.authorIcon);
		}
		if (StringUtils.isNotEmpty(this.title)) {
			attachment.put("title", this.title);
		}
		if (StringUtils.isNotEmpty(this.titleLink)) {
			attachment.put("title_link", this.titleLink);
		}
//        attachment.put("fields", fields);
		JSONArray mrkdwn = new JSONArray();
		mrkdwn.add("pretext");
		mrkdwn.add("text");
		mrkdwn.add("fields");
		attachment.put("mrkdwn_in", mrkdwn);
		JSONArray attachments = new JSONArray();
		attachments.add(attachment);
		send(attachments);
	}

	public CloseableHttpResponse send(JSONArray attachments) throws ClientProtocolException, IOException {
		HttpPost post = new HttpPost(url + token);
		JSONObject json = new JSONObject();
		List<NameValuePair> nvps = new ArrayList<NameValuePair>();

		try {
			json.put("channel", room);
			json.put("attachments", attachments);
			json.put("link_names", "1");
			nvps.add(new BasicNameValuePair("payload", json.toString()));
			post.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));

			CloseableHttpClient client = this.getHttpClient();
			CloseableHttpResponse response = client.execute(post);
			return response;
		} finally {
			post.releaseConnection();
		}
	}

	protected CloseableHttpClient getHttpClient() {
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
					credentialsProvider.setCredentials(new AuthScope(proxyHost),
							new UsernamePasswordCredentials(username, password));
				}
			}
		}
		return clientBuilder.build();
	}
}
