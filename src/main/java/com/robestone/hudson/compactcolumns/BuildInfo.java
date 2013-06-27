package com.robestone.hudson.compactcolumns;

import hudson.Util;
import hudson.model.BuildHistory;
import java.util.Locale;

import hudson.model.Run;

public class BuildInfo implements Comparable<BuildInfo> {

	private BuildHistory.Record buildRecord;
	private String color;
	private String timeAgoString;
	private long buildTime;
	private String status;
	private String urlPart;
	private boolean isFirst;
	private boolean isLatestBuild;
	private boolean multipleBuilds;
	
	public BuildInfo(BuildHistory.Record buildRecord, String color, String timeAgoString,
			long buildTime, String status, String urlPart,
			boolean isLatestBuild) {
		this.buildRecord = buildRecord;
		this.color = color;
		this.timeAgoString = timeAgoString;
		this.buildTime = buildTime;
		this.status = status;
		this.urlPart = urlPart;
		this.isLatestBuild = isLatestBuild;
	}
	public Run<?, ?> getRun() {
		return buildRecord.getBuild();
	}
	public String getColor() {
		return color;
	}
	public String getTimeAgoString() {
		return timeAgoString;
	}
	public String getStatus() {
		return status;
	}
	public String getUrlPart() {
		return urlPart;
	}
	public boolean isFirst() {
		return isFirst;
	}
	public boolean isLatestBuild() {
		return isLatestBuild;
	}
	public long getBuildTime() {
		return buildTime;
	}
	
	// ----
	
	public String getLatestBuildString(Locale locale) {
    	if (isLatestBuild) {
    		return " (" + Messages.latestBuild() + ")";
    	} else {
    		return "";
    	}
	}
	public String getStartedAgo(Locale locale) {
		return Messages._startedAgo(timeAgoString).toString(locale);
	}
	public String getBuiltAt(Locale locale) {
		String time = AbstractCompactColumn.getBuildTimeString(buildTime, locale);
		return Messages._builtAt(time).toString(locale);
	}
	public String getLastedDuration(Locale locale) {
		return Messages._lastedDuration( getDurationString(buildRecord)).toString(locale);
	}
        
        private String getDurationString(BuildHistory.Record buildRecord) {
            if (buildRecord.isBuilding()) {
                return hudson.model.Messages.Run_InProgressDuration(
                        Util.getTimeSpanString(System.currentTimeMillis() - buildRecord.getTimeInMillis()));
            }
            return Util.getTimeSpanString(buildRecord.getDuration());
        }
        
	public String getFontWeight() {
    	if (isLatestBuild && multipleBuilds) {
    		return "bold";
    	} else {
    		return "normal";
    	}
	}
	public void setFirst(boolean first) {
		this.isFirst = first;
	}
	public void setMultipleBuilds(boolean multipleBuilds) {
		this.multipleBuilds = multipleBuilds;
	}
	/**
	 * Sort by build number.
	 */
	public int compareTo(BuildInfo that) {
		return new Integer(that.buildRecord.getNumber()).compareTo(this.buildRecord.getNumber());
	}
}