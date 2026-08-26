# Domain Language

## Canonical Terms

| Term | Definition | Context |
|------|------------|---------|
| **Perception** | Raw sensor reading | Observation |
| **Scene Change** | Confirmed state transition | Observation |
| **Notification** | Visual-only alert (no episode) | Observation |
| **Episode** | Incident requiring attention | Surveillance |
| **Finding (Hallazgo)** | Clinical insight or pattern | Care |

## Context Groups

| Group | Contexts | Language |
|-------|----------|----------|
| Resident Lifecycle | Population + Policy + Surveillance | Admit, Assign, Configure, Monitor |
| Clinical Monitoring | Observation + Surveillance + Evidence | Perceive, Detect, Record, Capture |
| Care Operations | Care + Surveillance + Audit | Round, Note, Review, Log |
| Facility Management | Residence + Streams + Identity | Build, Assign, Register |
| Clinical History | History + Evidence + Care | Timeline, Summarize, Review |

## Rules of Use

### Say
- "The sensor emitted a **perception**"
- "There was a **scene change** at 03:12"
- "That only generated a **notification**, not an episode"
- "The **episode** #123 auto-resolved when resident returned to bed"
- "The doctor recorded a **finding**: insomnia pattern"

### Don't Say
- ~~"The observation says..."~~ → ambiguous
- ~~"Alert"~~ → use notification or episode
- ~~"Event"~~ alone → specify: perception, scene change, episode
