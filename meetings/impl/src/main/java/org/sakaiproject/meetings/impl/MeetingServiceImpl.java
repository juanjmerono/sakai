/**
 * Copyright (c) 2024 The Apereo Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://opensource.org/licenses/ecl2
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sakaiproject.meetings.impl;

import org.springframework.transaction.annotation.Transactional;
import org.sakaiproject.meetings.api.model.Meeting;
import org.sakaiproject.meetings.api.model.MeetingProperty;
import org.sakaiproject.meetings.api.persistence.MeetingAttendeeRepository;
import org.sakaiproject.meetings.api.persistence.MeetingPropertyRepository;
import org.sakaiproject.meetings.api.persistence.MeetingRepository;

import java.util.List;
import java.util.Optional;

import org.sakaiproject.meetings.api.MeetingService;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Transactional
public class MeetingServiceImpl implements MeetingService {

    @Setter
    MeetingRepository meetingRepository;

    @Setter
    MeetingPropertyRepository meetingPropertyRepository;

    @Setter
    MeetingAttendeeRepository meetingAttendeeRepository;

    public void init() {
        log.info("Initializing Meeting Service");
    }

    public Iterable<Meeting> getAllMeetings() {
        return meetingRepository.findAll();
    }

    public List<Meeting> getAllMeetingsFromSite(String siteId) {
        return meetingRepository.getSiteMeetings(siteId);
    }

    public List<Meeting> getUserMeetings(String userId, String siteId, List<String> groupIds) {
        return meetingRepository.getMeetings(userId, siteId, groupIds);
    }

    public Optional<Meeting> getMeetingById(String id) {
        return meetingRepository.findById(id);
    }

    public Meeting getMeeting(String id) {
        return meetingRepository.findMeetingById(id);
    }

    public Meeting createMeeting(Meeting meetingData) {
        return meetingRepository.save(meetingData);
    }

    public void updateMeeting(Meeting meetingData) {
        meetingRepository.update(meetingData);
    }

    public void deleteMeetingById(String id) {
        meetingPropertyRepository.deletePropertiesByMeetingId(id);
        meetingAttendeeRepository.removeAttendeesByMeetingId(id);
        meetingRepository.deleteById(id);
    }

    public void removeSiteAndGroupAttendeesByMeetingId(String id) {
        meetingAttendeeRepository.removeSiteAndGroupAttendeesByMeetingId(id);
    }

    public void setMeetingProperty(Meeting meeting, String property, String value) {
        Optional<MeetingProperty> optMeetingProp = meetingPropertyRepository.findFirstByMeetingIdAndName(meeting.getId(), property);
        if (optMeetingProp.isPresent()) {
            MeetingProperty meetingProp = optMeetingProp.get();
            meetingProp.setValue(value);
            meetingPropertyRepository.update(meetingProp);
        } else {
            MeetingProperty meetingProp = new MeetingProperty();
            meetingProp.setMeeting(meeting);
            meetingProp.setName(property);
            meetingProp.setValue(value);
            meetingPropertyRepository.save(meetingProp);
        }
    }

    public String getMeetingProperty(Meeting meeting, String property) {
        String result = null;
        Optional<MeetingProperty> optMeetingProp = meetingPropertyRepository.findFirstByMeetingIdAndName(meeting.getId(), property);
        if (optMeetingProp.isPresent()) {
            MeetingProperty prop = optMeetingProp.get();
            result = prop.getValue();
        }
        return result;
    }

    public void removeMeetingProperty(Meeting meeting, String property) {
    	meetingPropertyRepository.deletePropertyByMeetingIdAndName(meeting.getId(), property);
    }
	
}
