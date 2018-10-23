package com.taka7646.slack;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import javax.annotation.CheckForNull;

import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;

public class TextFileLoader extends MasterToSlaveFileCallable<List<String>>{

	private static final Logger logger = Logger.getLogger(TextFileLoader.class.getName());

	@CheckForNull
    private String propertiesFilePath;
	
	public TextFileLoader(@CheckForNull String propertiesFilePath) {
		this.propertiesFilePath = propertiesFilePath;
	}

	@Override
	public List<String> invoke(File base, VirtualChannel channel) throws IOException, InterruptedException {
		List<String> params = new LinkedList<String>();
        if (propertiesFilePath != null) {
			File file = new File(this.propertiesFilePath);
			if (!file.exists()) {
				file = new File(base, this.propertiesFilePath);
			}
        	logger.info("propertiesFilePath: " + file.getAbsolutePath());
			try(BufferedReader r = new BufferedReader(new FileReader(file))) {
				String s;
				while((s = r.readLine()) != null) {
					params.add(s);
				}
			}
        } else {
        	logger.info("propertiesFilePath is null");
        }
		return params;
	}

}
