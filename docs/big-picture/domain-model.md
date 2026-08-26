# Domain Model

## Entities by Bounded Context

### Residence
| Entity | Attributes | Actions |
|--------|-----------|---------|
| **Facility** | id, name, timezone | setup, update |
| **Wing** | id, facilityId, name, floor | add, update |
| **Room** | id, wingId, number, type | add, update |
| **Bed** | id, roomId, label, status | add, update, assign |

### Population
| Entity | Attributes | Actions |
|--------|-----------|---------|
| **Resident** | id, fullName, birthDate, admissionDate, status | admit, discharge, update |
| **Assignment** | id, residentId, bedId, assignedAt, releasedAt, isCurrent | assign, release, change |

### Policy
| Entity | Attributes | Actions |
|--------|-----------|---------|
| **AlarmPreset** | id, name, description, thresholds, isDefault | create, update |
| **AlarmProfile** | id, residentId, validFrom, validTo, mobilityAid, autopilot, mode, templateId, riskLevel | configure, update, history |

### Surveillance
| Entity | Attributes | Actions |
|--------|-----------|---------|
| **Episode** | id, residentId, bedId, severity, status, title, detail, occurredAt | trigger, acknowledge, resolve |
| **EpisodeNote** | id, episodeId, authorId, kind, body | create |

### Observation
| Entity | Attributes | Actions |
|--------|-----------|---------|
| **SensorEvent** | id, roomId, streamId, monitorKey, kind, state, sleeping, residentId | ingest |
| **CurrentBedState** | id, bedId, residentId, state, lastTransitionAt | update |
| **SceneEvent** | id, roomId, streamId, monitorKey, fromState, toState, residentId | ingest |
| **SleepSummary** | id, residentId, summaryDate, calmMinutes, restlessMinutes, awakeMinutes | compute |
| **MobilitySummary** | id, residentId, summaryDate, walkingMinutes, distanceMeters, transferCount | compute |
| **BathroomSummary** | id, residentId, summaryDate, visitCount, nightVisitCount | compute |

### Evidence
| Entity | Attributes | Actions |
|--------|-----------|---------|
| **Evidence** | id, bedId, residentId, evidenceKind, source, storagePath | create |
| **Timeline** | id, bedId, residentId, status, startedAt, endedAt | open, close |
| **ClipWindow** | id, bedId, residentId, windowStart, windowEnd, status | open, close |

### Care
| Entity | Attributes | Actions |
|--------|-----------|---------|
| **Round** | id, wingId, status, scheduledFor | start, complete |
| **RoundTask** | id, roundId, taskType, description, status, note | complete, skip |
| **CareNote** | id, residentId, roundId, authorId, kind, body | create |
| **ResidentNote** | id, residentId, authorId, kind, body, sourceEventId | create |
| **ShiftNote** | id, facilityId, wingId, shiftKey, shiftDate, authorId, kind, body | create |

### History
| Entity | Attributes | Actions |
|--------|-----------|---------|
| **IncidentDetection** | id, sourceRecordId, residentId, bedId, kind, severity, occurredAt | query |
| **IncidentReview** | id, incidentId, status, detectionVerdict, reviewNote, actorId | review |

### Identity
| Entity | Attributes | Actions |
|--------|-----------|---------|
| **User** | id, username, displayName, role | register, update |

### Audit
| Entity | Attributes | Actions |
|--------|-----------|---------|
| **AuditEntry** | id, actorId, action, entityType, entityId, metadataJson, createdAt | log |

## Aggregate Roots

| Aggregate | Root Entity | Child Entities |
|-----------|-------------|----------------|
| Residence | Facility | Wing → Room → Bed |
| Population | Resident | Assignment |
| Policy | AlarmProfile | — |
| Surveillance | Episode | EpisodeNote |
| Observation | SensorEvent | — |
| Evidence | Evidence | Timeline, ClipWindow |
| Care | Round | RoundTask, CareNote |
| History | IncidentDetection | IncidentReview |
| Identity | User | — |
| Audit | AuditEntry | — |
