/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2003, 2004, 2005, 2006, 2007, 2008 The Sakai Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.opensource.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/

package org.sakaiproject.assignment.impl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.Resource;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sakaiproject.assignment.api.AssignmentConstants;
import org.sakaiproject.assignment.api.AssignmentReferenceReckoner;
import org.sakaiproject.assignment.api.AssignmentService;
import org.sakaiproject.assignment.api.AssignmentServiceConstants;
import org.sakaiproject.assignment.api.model.Assignment;
import org.sakaiproject.assignment.api.model.AssignmentSubmission;
import org.sakaiproject.assignment.api.model.AssignmentSubmissionSubmitter;
import org.sakaiproject.authz.api.AuthzGroupService;
import org.sakaiproject.authz.api.Member;
import org.sakaiproject.authz.api.Role;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.entity.api.EntityManager;
import org.sakaiproject.exception.IdInvalidException;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.IdUsedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.site.api.Group;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.time.api.UserTimeService;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.util.ResourceLoader;
import org.sakaiproject.util.api.FormattedText;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.github.javafaker.Faker;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {AssignmentTestConfiguration.class})
public class AssignmentServiceTest extends AbstractTransactionalJUnit4SpringContextTests {

    private static final Faker faker = new Faker();

    @Autowired private AuthzGroupService authzGroupService;
    @Autowired private SecurityService securityService;
    @Autowired private SessionManager sessionManager;
    @Autowired private AssignmentService assignmentService;
    @Autowired private EntityManager entityManager;
    @Autowired private ServerConfigurationService serverConfigurationService;
    @Autowired private SiteService siteService;
    @Autowired private FormattedText formattedText;
    @Resource(name = "org.sakaiproject.time.api.UserTimeService")
    private UserTimeService userTimeService;

    private ResourceLoader resourceLoader;

    private static java.net.ServerSocket server = createServer(9999);
    private static String testContext;
    private static String testAssignmentId;
    private static String testSubmitterId;

    @Before
    public void setUp() {
        when(serverConfigurationService.getAccessUrl()).thenReturn("http://localhost:8080/access");
        resourceLoader = mock(ResourceLoader.class);
        when(resourceLoader.getLocale()).thenReturn(Locale.ENGLISH);
        when(resourceLoader.getString("gen.inpro")).thenReturn("In progress");
        when(resourceLoader.getString("gen.dra2")).thenReturn("Draft -");
        when(resourceLoader.getString("gen.subm4")).thenReturn("Submitted");
        when(resourceLoader.getString("gen.nograd")).thenReturn("No Grade");
        when(resourceLoader.getString("ungra")).thenReturn("Ungraded");
        when(resourceLoader.getString("pass")).thenReturn("Pass");
        when(resourceLoader.getString("fail")).thenReturn("Fail");
        when(resourceLoader.getString("gen.checked")).thenReturn("Checked");
        when(resourceLoader.getString("assignment.copy")).thenReturn("Copy");
        ((AssignmentServiceImpl) assignmentService).setResourceLoader(resourceLoader);
        when(userTimeService.getLocalTimeZone()).thenReturn(TimeZone.getDefault());
    }

    // mvn clean test -Dtest=AssignmentServiceTest
    // mvn clean test -Dtest=AssignmentServiceTest -Pnewtarget
    

    @Test
    @org.springframework.test.annotation.Rollback(false)
    public void waitTesting() throws Exception {
        log.info("Server: "+server);
        if (server!=null) {
            log.info("Waiting for student !!");
            testContext = UUID.randomUUID().toString();
            testSubmitterId = UUID.randomUUID().toString();
            // Instructor creo la tarea y espero
            Assignment assignment = createNewAssignment(testContext);
            testAssignmentId = assignment.getId();
            String assignmentReference = AssignmentReferenceReckoner.reckoner().assignment(assignment).reckon().getReference();
            log.info("Reference: "+assignmentReference);
            log.info("Context: "+testContext);
            log.info("SubmitterId: "+testSubmitterId);
            log.info("AssignmentId: "+testAssignmentId);
            Collection assignments = assignmentService.getAssignmentsForContext(testContext);
        } else {
            log.info("Here is the student !!");
            // Student hace submission y avisa
            sendStudent(9999);
        }
        Assert.assertTrue(true);
    }

    private void sendStudent(int port) {
        try {
            java.net.Socket s = new java.net.Socket("localhost", port);
            java.io.PrintWriter out = new java.io.PrintWriter(s.getOutputStream(), true);
            java.io.BufferedReader input = new java.io.BufferedReader(new java.io.InputStreamReader(s.getInputStream()));
            String context = input.readLine();
            String assignmentId = input.readLine();
            String submitterId = input.readLine();
            log.info("Context: "+context);
            log.info("SubmitterId: "+submitterId);
            log.info("AssignmentId: "+assignmentId);

            Site site = mock(Site.class);
            Collection<Group> siteGroups = new HashSet<>();
            Set<String> groupARef = new HashSet<>();
            Group groupA = mock(Group.class);
            String groupAId = UUID.randomUUID().toString();
            when(groupA.getId()).thenReturn(groupAId);
            when(groupA.getReference()).thenReturn("/site/" + context + "/group/" + groupAId);
            siteGroups.add(groupA);
            groupARef.add(groupA.getReference());
            when(site.getGroups()).thenReturn(siteGroups);
            when(siteService.getSite(context)).thenReturn(site);
            when(securityService.unlock(AssignmentServiceConstants.SECURE_ADD_ASSIGNMENT, AssignmentReferenceReckoner.reckoner().context(context).reckon().getReference())).thenReturn(true);

            Assignment assignment = assignmentService.getAssignment(assignmentId);
            log.info("AssignmentId2: "+assignment.getId());

            String assignmentReference = AssignmentReferenceReckoner.reckoner().assignment(assignment).reckon().getReference();
            when(site.getGroup(submitterId)).thenReturn(mock(Group.class));
            when(site.getMember(submitterId)).thenReturn(mock(Member.class));
            when(securityService.unlock(AssignmentServiceConstants.SECURE_ADD_ASSIGNMENT_SUBMISSION, assignmentReference)).thenReturn(true);
            AssignmentSubmission submission = null;
            try {
                submission = assignmentService.addSubmission(assignment.getId(), submitterId);
                log.info("SubmissionId: "+submission.getId());
            } catch (PermissionException e) {
                Assert.fail(e.getMessage());
            }
            log.info("AssignmentSubmissions: "+assignment.getSubmissions().size());
            Assert.assertTrue(assignment.getSubmissions().size()>0);
            out.println(submission.getId());

        } catch (Exception ex) {
            log.error("Error:::",ex);
        }
    }

    @Test
    public void waitInstructor() throws Exception {
        log.info("Server: "+server);
        if (server!=null) {
            try {
                java.net.Socket socket = server.accept();
                try {
                    java.io.PrintWriter out = new java.io.PrintWriter(socket.getOutputStream(), true);
                    java.io.BufferedReader input = new java.io.BufferedReader(new java.io.InputStreamReader(socket.getInputStream()));
                    out.println(testContext);
                    out.println(testAssignmentId);
                    out.println(testSubmitterId);

                    String done = input.readLine();
                    log.info("SubmissionID: "+done);
                    Thread.sleep(5000);
                    Assignment assignment = assignmentService.getAssignment(testAssignmentId);
                    log.info("AssignmentSubmissions: "+assignment.getSubmissions().size());
                    Assert.assertTrue(assignment.getSubmissions().size()>0);                

                } finally {
                    socket.close();
                }
            } finally {
                server.close();
            }
        }
        Assert.assertTrue(true);
    }

    private static java.net.ServerSocket createServer(int port) {
        try {
            java.net.ServerSocket listener = new java.net.ServerSocket(port);
            return listener;
        } catch (Exception ex) {
            return null;
        }
    }

    private AssignmentSubmission createNewSubmission(String context, String submitterId) throws UserNotDefinedException, IdUnusedException {
        Assignment assignment = createNewAssignment(context);
        String assignmentReference = AssignmentReferenceReckoner.reckoner().assignment(assignment).reckon().getReference();
        Site site = mock(Site.class);
        when(site.getGroup(submitterId)).thenReturn(mock(Group.class));
        when(site.getMember(submitterId)).thenReturn(mock(Member.class));
        when(siteService.getSite(context)).thenReturn(site);
        when(securityService.unlock(AssignmentServiceConstants.SECURE_ADD_ASSIGNMENT_SUBMISSION, assignmentReference)).thenReturn(true);
        AssignmentSubmission submission = null;
        try {
            submission = assignmentService.addSubmission(assignment.getId(), submitterId);
        } catch (PermissionException e) {
            Assert.fail(e.getMessage());
        }
        return submission;
    }

    private AssignmentSubmission createNewGroupSubmission(String context, String groupSubmitter, Set<String> submitters) throws UserNotDefinedException, IdUnusedException, PermissionException {

        // Setup an Assignment for Group Submission
        Assignment assignment = createNewAssignment(context);
        assignment.setTypeOfAccess(Assignment.Access.GROUP);
        assignment.setIsGroup(true);
        assignment.setOpenDate(Instant.now().minus(Period.ofDays(1)));
        String groupRef = "/site/" + context + "/group/" + groupSubmitter;
        assignment.getGroups().add(groupRef);
        String assignmentReference = AssignmentReferenceReckoner.reckoner().assignment(assignment).reckon().getReference();
        when(securityService.unlock(AssignmentServiceConstants.SECURE_UPDATE_ASSIGNMENT, assignmentReference)).thenReturn(true);
        when(securityService.unlock(AssignmentServiceConstants.SECURE_UPDATE_ASSIGNMENT, AssignmentReferenceReckoner.reckoner().context(context).reckon().getReference())).thenReturn(true);
        assignmentService.updateAssignment(assignment);

        // configure mock group objects
        Site site = mock(Site.class);
        Group group = mock(Group.class);
        when(group.getReference()).thenReturn(groupRef);
        Collection<Group> groups = new HashSet<>();
        groups.add(group);
        when(site.getGroups()).thenReturn(groups);
        when(site.getGroup(groupSubmitter)).thenReturn(group);
        Set<Member> members = new HashSet<>();
        submitters.forEach(s -> {
                Member member = mock(Member.class);
                when(member.getUserId()).thenReturn(s);
                Role r = mock(Role.class);
                when(member.getRole()).thenReturn(r);
                when(r.isAllowed(AssignmentServiceConstants.SECURE_ADD_ASSIGNMENT_SUBMISSION)).thenReturn(true);
                when(r.isAllowed(AssignmentServiceConstants.SECURE_GRADE_ASSIGNMENT_SUBMISSION)).thenReturn(false);
                members.add(member);
        });
        when(group.getMembers()).thenReturn(members);
        when(siteService.getSite(context)).thenReturn(site);
        Set<String> groupRefs = groups.stream().map(Group::getReference).collect(Collectors.toSet());

        // pick a submitter to be the current user
        String currentUser = submitters.stream().findAny().get();
        when(sessionManager.getCurrentSessionUserId()).thenReturn(currentUser);

        // drop security to student permissions
        when(authzGroupService.getAuthzGroupsIsAllowed(currentUser, AssignmentServiceConstants.SECURE_ACCESS_ASSIGNMENT, groupRefs)).thenReturn(groupRefs);
        when(securityService.unlock(AssignmentServiceConstants.SECURE_ACCESS_ASSIGNMENT, groupSubmitter)).thenReturn(true);
        when(securityService.unlock(AssignmentServiceConstants.SECURE_ADD_ASSIGNMENT, AssignmentReferenceReckoner.reckoner().context(context).reckon().getReference())).thenReturn(false);
        when(securityService.unlock(AssignmentServiceConstants.SECURE_ADD_ASSIGNMENT_SUBMISSION, groupRef)).thenReturn(true);

        try {
            return assignmentService.addSubmission(assignment.getId(), groupSubmitter);
        } catch (PermissionException e) {
            Assert.fail(e.getMessage());
        }
        return null;
    }

    private Assignment createNewAssignment(String context) {
        String contextReference = AssignmentReferenceReckoner.reckoner().context(context).reckon().getReference();
        when(securityService.unlock(AssignmentServiceConstants.SECURE_ADD_ASSIGNMENT, contextReference)).thenReturn(true);
        when(securityService.unlock(AssignmentServiceConstants.SECURE_ACCESS_ASSIGNMENT, contextReference)).thenReturn(true);
        when(sessionManager.getCurrentSessionUserId()).thenReturn(UUID.randomUUID().toString());
        Assignment assignment = null;
        try {
            assignment = assignmentService.addAssignment(context);
        } catch (PermissionException e) {
            Assert.fail(e.getMessage());
        }
        return assignment;
    }

    private void configureScale(int scale) {
        int dec = new Double(Math.log10(scale)).intValue();
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(dec);
        nf.setMinimumFractionDigits(dec);
        nf.setGroupingUsed(false);
        when(formattedText.getNumberFormat(dec, dec, false)).thenReturn(nf);
    }
}
