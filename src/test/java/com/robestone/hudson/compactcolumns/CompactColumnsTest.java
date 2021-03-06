/*
 * The MIT License
 * 
 * Copyright (c) 2009, Sun Microsystems, Inc., Jesse Glick
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
package com.robestone.hudson.compactcolumns;

import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;

import java.awt.Color;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.SortedMap;
import hudson.model.RunMap;

import com.robestone.hudson.compactcolumns.AbstractStatusesColumn.TimeAgoType;
import hudson.model.BuildHistory;
import hudson.model.FreeStyleProject;
import hudson.model.Hudson;
import hudson.model.ItemGroup;
import hudson.model.User;
import hudson.security.Permission;
import java.io.IOException;

import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import static org.easymock.EasyMock.expect;
import org.eclipse.hudson.graph.ColorPalette;
import org.eclipse.hudson.security.team.TeamManager;
import org.junit.runner.RunWith;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Hudson.class, User.class})
public class CompactColumnsTest extends TestCase {

    private static final String USER = "admin";
    public void testDateFormats() {
        doTestDateFormats(Locale.US, DateFormat.SHORT, "6/24/10");
        doTestDateFormats(Locale.US, DateFormat.MEDIUM, "Jun 24, 2010");
        doTestDateFormats(Locale.GERMAN, DateFormat.SHORT, "24.06.10");
    }

    public void doTestDateFormats(Locale locale, int formatType, String expect) {
        DateFormat format = DateFormat.getDateInstance(formatType, locale);
        long time = 1277416568304L;
        Date date = new Date(time);
        String output = format.format(date);
        assertEquals(expect, output);
    }

    public void testDateTimeFormats() {
        doTestDateTimeFormats(Locale.US, DateFormat.SHORT, DateFormat.SHORT, "6/24/10 2:56 PM");
        doTestDateTimeFormats(Locale.US, DateFormat.MEDIUM, DateFormat.SHORT, "Jun 24, 2010 2:56 PM");
        doTestDateTimeFormats(Locale.GERMAN, DateFormat.SHORT, DateFormat.SHORT, "24.06.10 14:56");
    }

    public void doTestDateTimeFormats(Locale locale, int dateFormatType, int timeFormatType, String expect) {
        DateFormat format = DateFormat.getDateTimeInstance(dateFormatType, timeFormatType, locale);
        long time = 1277416568304L;
        Date date = new Date(time);
        String output = format.format(date);
        assertEquals(expect, output);
    }

    /**
     * Shows that all locale handling will be okay.
     */
    public void testNoBadLocale() {
        Locale[] locales = Locale.getAvailableLocales();
        for (Locale locale : locales) {
            String s = AbstractStatusesColumn.getBuildTimeString(1277416568304L, locale);
            assertNotNull(s);
        }
    }

    public void testShowDate() {
        doTestShowDate(Locale.GERMAN, "14:56", "24.06.2010");
        doTestShowDate(Locale.US, "2:56 PM", "6/24/2010");
    }

    private void doTestShowDate(Locale locale, String expectTime, String expectDate) {
        long time = 1277416568304L;
        String expectDateTime = expectTime + ", " + expectDate;
        String expectDateTimeLong = expectDate + " " + expectTime;

        String ago;

        ago = AbstractStatusesColumn.getTimeAgoString(locale, time, false, TimeAgoType.PREFER_DATE_TIME);
        assertEquals(expectDateTimeLong, ago);

        ago = AbstractStatusesColumn.getTimeAgoString(locale, time, true, TimeAgoType.PREFER_DATE_TIME);
        assertEquals(expectDate, ago);

        ago = AbstractStatusesColumn.getTimeAgoString(locale, time, true, TimeAgoType.PREFER_DATES);
        assertEquals(expectDate, ago);

        // can't easily test this format with current API, but can do a negative test
        ago = AbstractStatusesColumn.getTimeAgoString(locale, time, false, TimeAgoType.DIFF);
        assertFalse(expectDate.equals(ago));
        assertFalse(expectDateTime.equals(ago));
        assertFalse(expectTime.equals(ago));
    }

    public void testLocalizeDate() {
        long time = 1277416568304L;
        doTestLocalizeDate(time, Locale.ENGLISH, "2:56 PM, 6/24/2010");
        doTestLocalizeDate(time, Locale.GERMAN, "14:56, 24.06.2010");
        doTestLocalizeDate(time, Locale.CANADA, "2:56 PM, 24/06/2010");
    }

    private void doTestLocalizeDate(long time, Locale locale, String expect) {
        String found = AbstractStatusesColumn.getBuildTimeString(time, locale);
        assertEquals(expect, found);
    }

    /**
     * This just shows the weird way the Hudson.util is working.
     */
    public void testTime() {
        doTestTime(0, "0 sec");
        doTestTime(500, "0 sec");
        doTestTime(999, "0 sec");
        doTestTime(1000, "1 sec");
        doTestTime(1001, "1 sec");
        doTestTime(1500, "1.5 sec");
        doTestTime(1730, "1.7 sec");
        doTestTime(17300, "17 sec");
        doTestTime(1999, "2 sec");
        doTestTime(66000, "1.1 min");
        doTestTime(60000, "1 min");
        doTestTime(360000, "6 min");
        doTestTime(LastSuccessAndFailedColumn.ONE_DAY_MS + 1, "1 day");
        doTestTime(LastSuccessAndFailedColumn.ONE_DAY_MS * 1.5f, "1.5 days");
        doTestTime(LastSuccessAndFailedColumn.ONE_DAY_MS * 10.5f, "10 days");
    }

    private void doTestTime(float diff, String expect) {
        String found = AbstractStatusesColumn.getShortTimestamp(diff);
        assertEquals(expect, found);
    }

    public void testGetBuilds() {
        doTestBuilds("SSFFUFUS", "SU", "SF", "SFU");
        doTestBuilds("FSSFFUFUS", "FSU", "FS", "FSU");
        doTestBuilds("FSSFF", "FS", "FS", "FS");
        doTestBuilds("FSS", "FS", "FS", "FS");
        doTestBuilds("F", "F", "F", "F");
        doTestBuilds("FFF", "F", "F", "F");
        doTestBuilds("SFFF", "S", "SF", "SF");
        doTestBuilds("UFF", "U", "UF", "UF");
        doTestBuilds("FFUU", "FU", "F", "FU");
        doTestBuilds("USF", "US", "USF", "USF");
        doTestBuilds("AAUSFAA", "US", "SF", "USF");
        doTestBuilds("USAF", "US", "USF", "USF");
        doTestBuilds("A", "A", "A", "A");
    }

    private void doTestBuilds(String buildsSpec,
            String expectForLastStableAndUnstable, String expectForLastSuccessAndFailed, String expectForAllStatuses) {
        doTestBuilds(buildsSpec, expectForLastStableAndUnstable, new LastStableAndUnstableColumn());
        doTestBuilds(buildsSpec, expectForLastSuccessAndFailed, new LastSuccessAndFailedColumn());
        doTestBuilds(buildsSpec, expectForAllStatuses, new AllStatusesColumn(null, false, null, 0));
    }

    /**
     * @param buildsSpec most recent build first
     * @param expectToShow most recent status first
     */
    private void doTestBuilds(String buildsSpec, String expectToShow, AbstractStatusesColumn col) {
        TestJobMock job = createJobMock();
        TestRun previous = null;
        long time = 1000;
        for (int i = 0; i < buildsSpec.length(); i++) {
            char c = buildsSpec.charAt(i);
            Result result = null;
            switch (c) {
                case 'S':
                    result = Result.SUCCESS;
                    break;
                case 'U':
                    result = Result.UNSTABLE;
                    break;
                case 'F':
                    result = Result.FAILURE;
                    break;
                case 'A':
                    result = Result.ABORTED;
                    break;
            }
            TestRun run = new TestRun(job, time - i, result);
            job.addRun(run);
            if (previous != null) {
                previous.setPrevious(run);
            }
            previous = run;
        }
        assertEquals(String.valueOf(-time), job._getRuns().firstKey().toString());
        List<BuildInfo> builds = col.getBuilds(job, Locale.US);
        assertEquals(expectToShow.length(), builds.size());
        for (int i = 0; i < builds.size(); i++) {
            char c = expectToShow.charAt(i);
            BuildInfo build = builds.get(i);
            switch (c) {
                case 'S':
                    assertEquals("Stable", build.getStatus());
                    break;
                case 'U':
                    assertEquals("Unstable", build.getStatus());
                    break;
                case 'F':
                    assertEquals("Failed", build.getStatus());
                    break;
                case 'A':
                    assertEquals("Aborted", build.getStatus());
                    break;
            }
        }
    }

    public void testStableColor() throws Exception {
        assertEquals(Color.BLUE, BuildInfo.getStableColor());
        assertFalse(ColorPalette.BLUE.equals(BuildInfo.getStableColor()));

        Field colorValue = Color.class.getDeclaredField("value");
        colorValue.setAccessible(true);
        colorValue.setInt(ColorPalette.BLUE, new Color(172, 218, 0).getRGB());

        assertEquals(ColorPalette.BLUE, BuildInfo.getStableColor());
        assertFalse(Color.BLUE.equals(BuildInfo.getStableColor()));
    }

    public void testColorString() {
        assertEquals("#0000ff", BuildInfo.getStableColorString());
        assertEquals("#ef2929", BuildInfo.FAILED_COLOR);
        assertEquals("#000303", BuildInfo.toColorString(new Color(0, 3, 3)));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static class TestRun extends Run {

        public TestRun(Job job, long timestamp, Result result) {
            super(job, timestamp);
            this.number = (int) timestamp;
            this.setResult(result);
        }

        public void setPrevious(Run previous) {
            this.previousBuild = previous;
        }

        @Override
        public boolean isBuilding() {
            return false;
        }

        @Override
        public String toString() {
            return "tj:" + number;
        }
        
        public void save() throws IOException {
             
        }
    }
    
    private TestJobMock createJobMock(){
        Hudson hudson = createMock(Hudson.class);
        TeamManager teamManager = new TeamManager(FileUtils.getTempDirectory());
        expect(hudson.getTeamManager()).andReturn(teamManager).anyTimes();
        expect(hudson.isTeamManagementEnabled()).andReturn(false).anyTimes();
        expect(hudson.getRootDir()).andReturn(FileUtils.getTempDirectory()).anyTimes();
        mockStatic(Hudson.class);
        expect(Hudson.getInstance()).andReturn(hudson).anyTimes();
        mockStatic(User.class);
        expect(User.current()).andReturn(null);
        replayAll();
        replayAll();
        TestJobMock testJobMock = new TestJobMock("testJob");
        testJobMock.onCreatedFromScratch();
        verifyAll();
        return testJobMock;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static class TestJobMock extends FreeStyleProject {

        private RunMap runs = new RunMap(this);

        public TestJobMock(String name) {
            super((ItemGroup) null, name);
            setAllowSave(false);
        }

        public void addRun(Run run) {
            runs.put(-run.number, run);
        }

        @Override
        public SortedMap _getRuns() {
            return runs;
        }

        @Override
        public boolean isBuildable() {
            return false;
        }

        @Override
        protected void updateTransientActions() {
        }

        @Override
        public boolean hasPermission(Permission p) {
            return true; // bypass non-existent ACL
        }

        @Override
        public BuildHistory getBuildHistoryData() {
            return runs;
        }
    }
}
